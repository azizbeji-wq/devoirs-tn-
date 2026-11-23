package com.example.data.download

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import androidx.work.workDataOf
import com.example.R
import com.example.data.ai.PdfTextExtractor
import com.example.data.local.DevoirsDatabase
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.net.HttpURLConnection
import java.net.URL
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Worker WorkManager téléchargeant en arrière-plan le sujet PDF (et le corrigé s'il existe)
 * d'un devoir vers le dossier privé de l'application (`filesDir/pdfs/<devoirId>/`).
 *
 * Fonctionnalités :
 * - Reprise automatique via l'en-tête HTTP `Range: bytes=<offset>-` grâce aux fichiers `.part`,
 * - Progression en temps réel remontée à Compose (`setProgress`) et dans une notification Android,
 * - Annulation propre sans perte de l'avancement déjà téléchargé,
 * - Vérification d'intégrité (`%PDF-`, taille exacte et/ou SHA-256) avant validation dans Room,
 * - Aucune clé API ni secret embarqué.
 */
class PdfDownloadWorker(
    appContext: Context,
    params: WorkerParameters
) : CoroutineWorker(appContext, params) {

    private val storageManager = PdfStorageManager(appContext)
    private val database = DevoirsDatabase.getInstance(appContext)
    private val devoirDao = database.devoirDao()
    private val pdfTextExtractor = PdfTextExtractor(appContext, database.devoirPdfContentDao())

    override suspend fun doWork(): Result = withContext(Dispatchers.IO) {
        val devoirId = inputData.getString(KEY_DEVOIR_ID)?.trim().orEmpty()
        val devoirTitle = inputData.getString(KEY_DEVOIR_TITLE)?.trim().orEmpty().ifEmpty { devoirId }
        val sujetUrl = inputData.getString(KEY_SUJET_URL)?.trim().orEmpty()
        val corrigeUrl = inputData.getString(KEY_CORRIGE_URL)?.trim()?.takeIf { it.isNotEmpty() }
        val expectedSujetSize = inputData.getLong(KEY_SUJET_SIZE_BYTES, 0L)
        val expectedCorrigeSize = inputData.getLong(KEY_CORRIGE_SIZE_BYTES, 0L)
        val expectedSujetSha = inputData.getString(KEY_SUJET_SHA256)?.takeIf { it.isNotBlank() }
        val expectedCorrigeSha = inputData.getString(KEY_CORRIGE_SHA256)?.takeIf { it.isNotBlank() }
        val targetVersion = inputData.getInt(KEY_VERSION, 1)

        if (devoirId.isEmpty() || sujetUrl.isEmpty()) {
            return@withContext Result.failure(
                workDataOf(
                    KEY_DEVOIR_ID to devoirId,
                    KEY_ERROR_MESSAGE to applicationContext.getString(R.string.download_err_invalid_url)
                )
            )
        }

        ensureNotificationChannel()
        val notifId = NOTIFICATION_BASE_ID + (devoirId.hashCode() and 0x0FFF)

        val totalEstimated = (expectedSujetSize + expectedCorrigeSize).coerceAtLeast(1L)
        val sujetTarget = storageManager.sujetFile(devoirId)
        val sujetPart = storageManager.sujetPartFile(devoirId)
        val corrigeTarget = storageManager.corrigeFile(devoirId)
        val corrigePart = storageManager.corrigePartFile(devoirId)

        var lastProgressReported = -1

        suspend fun publishProgress(downloadedSoFar: Long, totalBytes: Long) {
            val denom = totalBytes.coerceAtLeast(1L)
            val pct = ((downloadedSoFar * 100L) / denom).toInt().coerceIn(0, 100)
            if (pct != lastProgressReported) {
                lastProgressReported = pct
                setProgress(
                    workDataOf(
                        KEY_DEVOIR_ID to devoirId,
                        KEY_PROGRESS to pct,
                        KEY_BYTES_DOWNLOADED to downloadedSoFar,
                        KEY_TOTAL_BYTES to totalBytes
                    )
                )
                postProgressNotification(notifId, devoirTitle, pct)
            }
        }

        try {
            publishProgress(0L, totalEstimated)

            // 1. Téléchargement & vérification du sujet PDF (avec reprise si .part existe)
            val sujetAlreadyValid = sujetTarget.exists() &&
                PdfIntegrityVerifier.verify(sujetTarget, expectedSujetSize, expectedSujetSha) is PdfIntegrityResult.Valid

            val actualSujetBytes = if (sujetAlreadyValid) {
                sujetTarget.length()
            } else {
                downloadWithResumeAndVerify(
                    urlString = sujetUrl,
                    partFile = sujetPart,
                    finalFile = sujetTarget,
                    expectedSizeBytes = expectedSujetSize,
                    expectedSha256 = expectedSujetSha
                ) { fileDownloaded, fileTotal ->
                    val combinedTotal = (fileTotal + expectedCorrigeSize).coerceAtLeast(totalEstimated)
                    publishProgress(fileDownloaded, combinedTotal)
                }
            }

            // 2. Téléchargement & vérification du corrigé PDF (optionnel)
            var actualCorrigeBytes = 0L
            var localCorrigePath: String? = null
            if (corrigeUrl != null) {
                val corrigeAlreadyValid = corrigeTarget.exists() &&
                    PdfIntegrityVerifier.verify(corrigeTarget, expectedCorrigeSize, expectedCorrigeSha) is PdfIntegrityResult.Valid

                actualCorrigeBytes = if (corrigeAlreadyValid) {
                    corrigeTarget.length()
                } else {
                    downloadWithResumeAndVerify(
                        urlString = corrigeUrl,
                        partFile = corrigePart,
                        finalFile = corrigeTarget,
                        expectedSizeBytes = expectedCorrigeSize,
                        expectedSha256 = expectedCorrigeSha
                    ) { fileDownloaded, fileTotal ->
                        val combinedTotal = (actualSujetBytes + fileTotal).coerceAtLeast(totalEstimated)
                        publishProgress(actualSujetBytes + fileDownloaded, combinedTotal)
                    }
                }
                localCorrigePath = corrigeTarget.absolutePath
            }

            val totalDownloaded = actualSujetBytes + actualCorrigeBytes
            publishProgress(totalDownloaded, totalDownloaded)

            // 3. Enregistrement dans Room (préserve favoris & progression)
            devoirDao.markDownloaded(
                devoirId = devoirId,
                localSujetPath = sujetTarget.absolutePath,
                localCorrigePath = localCorrigePath,
                downloadedBytes = totalDownloaded,
                downloadedVersion = targetVersion
            )

            // 4. Indexation plein texte OCR (ML Kit Latin + fallback Gemini Arabe) dans Room FTS4
            runCatching {
                val entity = devoirDao.getById(devoirId)
                val chapters = entity?.chapterTagsSerialized
                    ?.split("||")
                    ?.map { it.trim() }
                    ?.filter { it.isNotEmpty() }
                    .orEmpty()
                pdfTextExtractor.extractAndIndexDevoir(
                    devoirId = devoirId,
                    title = entity?.title ?: devoirTitle,
                    subject = entity?.subject.orEmpty(),
                    chapters = chapters,
                    sujetFile = sujetTarget,
                    corrigeFile = localCorrigePath?.let { File(it) }
                )
            }.onFailure { err ->
                Log.w(TAG, "Indexation OCR différée pour $devoirId : ${err.message}")
            }

            postCompletedNotification(notifId, devoirTitle)
            Result.success(
                workDataOf(
                    KEY_DEVOIR_ID to devoirId,
                    KEY_PROGRESS to 100,
                    KEY_BYTES_DOWNLOADED to totalDownloaded,
                    KEY_TOTAL_BYTES to totalDownloaded
                )
            )
        } catch (ce: CancellationException) {
            // Conserve les fichiers .part pour permettre la reprise ultérieure
            cancelNotification(notifId)
            throw ce
        } catch (integrity: IntegrityCheckException) {
            Log.e(TAG, "Échec de vérification d'intégrité pour $devoirId : ${integrity.message}")
            postErrorNotification(notifId, devoirTitle, integrity.message ?: "")
            Result.failure(
                workDataOf(
                    KEY_DEVOIR_ID to devoirId,
                    KEY_ERROR_MESSAGE to (integrity.message ?: applicationContext.getString(R.string.download_err_integrity))
                )
            )
        } catch (io: IOException) {
            Log.w(TAG, "Erreur réseau pour $devoirId (tentative $runAttemptCount) : ${io.message}")
            if (runAttemptCount < MAX_AUTO_RETRIES) {
                Result.retry()
            } else {
                val msg = io.message ?: applicationContext.getString(R.string.download_err_network)
                postErrorNotification(notifId, devoirTitle, msg)
                Result.failure(
                    workDataOf(
                        KEY_DEVOIR_ID to devoirId,
                        KEY_ERROR_MESSAGE to msg
                    )
                )
            }
        } catch (e: Exception) {
            Log.e(TAG, "Erreur inattendue lors du téléchargement de $devoirId", e)
            val msg = e.message ?: applicationContext.getString(R.string.download_err_network)
            postErrorNotification(notifId, devoirTitle, msg)
            Result.failure(
                workDataOf(
                    KEY_DEVOIR_ID to devoirId,
                    KEY_ERROR_MESSAGE to msg
                )
            )
        }
    }

    /**
     * Télécharge un fichier HTTP/HTTPS avec prise en charge de la reprise (`Range: bytes=N-`),
     * vérifie son intégrité (%PDF-, taille, SHA-256) puis le promeut atomiquement vers [finalFile].
     */
    private suspend fun downloadWithResumeAndVerify(
        urlString: String,
        partFile: File,
        finalFile: File,
        expectedSizeBytes: Long,
        expectedSha256: String?,
        onProgress: suspend (downloaded: Long, total: Long) -> Unit
    ): Long {
        val existingOffset = if (partFile.exists()) partFile.length().coerceAtLeast(0L) else 0L
        val connection = (URL(urlString).openConnection() as HttpURLConnection).apply {
            requestMethod = "GET"
            connectTimeout = CONNECT_TIMEOUT_MS
            readTimeout = READ_TIMEOUT_MS
            instanceFollowRedirects = true
            if (existingOffset > 0L) {
                setRequestProperty("Range", "bytes=$existingOffset-")
            }
        }

        try {
            val responseCode = connection.responseCode
            val isResuming = responseCode == HttpURLConnection.HTTP_PARTIAL && existingOffset > 0L
            if (responseCode != HttpURLConnection.HTTP_OK && responseCode != HttpURLConnection.HTTP_PARTIAL) {
                throw IOException("HTTP $responseCode pour $urlString")
            }

            val startOffset = if (isResuming) existingOffset else 0L
            val contentLength = connection.contentLengthLong
            val resolvedTotal = when {
                contentLength > 0L -> startOffset + contentLength
                expectedSizeBytes > 0L -> expectedSizeBytes
                else -> 0L
            }

            var writtenTotal = startOffset
            val buffer = ByteArray(16 * 1024)
            var lastEmitTime = 0L

            connection.inputStream.use { input ->
                FileOutputStream(partFile, isResuming).use { output ->
                    var read: Int
                    while (input.read(buffer).also { read = it } != -1) {
                        if (isStopped) {
                            output.flush()
                            throw CancellationException("Téléchargement annulé par l'utilisateur")
                        }
                        output.write(buffer, 0, read)
                        writtenTotal += read

                        val now = System.currentTimeMillis()
                        if (now - lastEmitTime >= 150L) {
                            lastEmitTime = now
                            onProgress(writtenTotal, resolvedTotal.coerceAtLeast(writtenTotal))
                        }
                    }
                    output.flush()
                }
            }

            onProgress(writtenTotal, resolvedTotal.coerceAtLeast(writtenTotal))

            // Vérification d'intégrité (taille attendue du catalogue ou Content-Length + SHA-256 + %PDF-)
            val sizeToVerify = when {
                expectedSizeBytes > 0L -> expectedSizeBytes
                resolvedTotal > 0L -> resolvedTotal
                else -> 0L
            }
            when (val check = PdfIntegrityVerifier.verify(partFile, sizeToVerify, expectedSha256)) {
                is PdfIntegrityResult.Valid -> {
                    if (finalFile.exists()) finalFile.delete()
                    if (!partFile.renameTo(finalFile)) {
                        partFile.copyTo(finalFile, overwrite = true)
                        partFile.delete()
                    }
                    return check.sizeBytes
                }

                is PdfIntegrityResult.Invalid -> {
                    // Supprime immédiatement les fichiers corrompus ou dont le hash SHA-256 ne correspond pas
                    if (partFile.exists()) partFile.delete()
                    if (finalFile.exists()) finalFile.delete()
                    val errorMsg = if (check.isSha256Mismatch) {
                        applicationContext.getString(R.string.download_err_sha256_mismatch)
                    } else {
                        check.reason
                    }
                    throw IntegrityCheckException(errorMsg)
                }
            }
        } finally {
            connection.disconnect()
        }
    }

    private fun ensureNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val nm = applicationContext.getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager
                ?: return
            val channel = NotificationChannel(
                CHANNEL_ID,
                applicationContext.getString(R.string.download_notif_channel_name),
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = applicationContext.getString(R.string.download_notif_channel_desc)
            }
            nm.createNotificationChannel(channel)
        }
    }

    private fun canPostNotifications(): Boolean {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) return true
        return ContextCompat.checkSelfPermission(
            applicationContext,
            Manifest.permission.POST_NOTIFICATIONS
        ) == PackageManager.PERMISSION_GRANTED
    }

    private fun postProgressNotification(notifId: Int, title: String, progress: Int) {
        if (!canPostNotifications()) return
        val notif = NotificationCompat.Builder(applicationContext, CHANNEL_ID)
            .setSmallIcon(android.R.drawable.stat_sys_download)
            .setContentTitle(applicationContext.getString(R.string.download_notif_in_progress_title))
            .setContentText("$title · $progress%")
            .setOngoing(true)
            .setOnlyAlertOnce(true)
            .setProgress(100, progress, false)
            .build()
        if (ContextCompat.checkSelfPermission(applicationContext, Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED) {
            runCatching { NotificationManagerCompat.from(applicationContext).notify(notifId, notif) }
        }
    }

    private fun postCompletedNotification(notifId: Int, title: String) {
        if (!canPostNotifications()) return
        val notif = NotificationCompat.Builder(applicationContext, CHANNEL_ID)
            .setSmallIcon(android.R.drawable.stat_sys_download_done)
            .setContentTitle(applicationContext.getString(R.string.download_notif_done_title))
            .setContentText(title)
            .setAutoCancel(true)
            .build()
        if (ContextCompat.checkSelfPermission(applicationContext, Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED) {
            runCatching { NotificationManagerCompat.from(applicationContext).notify(notifId, notif) }
        }
    }

    private fun postErrorNotification(notifId: Int, title: String, reason: String) {
        if (!canPostNotifications()) return
        val notif = NotificationCompat.Builder(applicationContext, CHANNEL_ID)
            .setSmallIcon(android.R.drawable.stat_notify_error)
            .setContentTitle(applicationContext.getString(R.string.download_notif_error_title))
            .setContentText("$title — $reason")
            .setAutoCancel(true)
            .build()
        if (ContextCompat.checkSelfPermission(applicationContext, Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED) {
            runCatching { NotificationManagerCompat.from(applicationContext).notify(notifId, notif) }
        }
    }

    private fun cancelNotification(notifId: Int) {
        runCatching { NotificationManagerCompat.from(applicationContext).cancel(notifId) }
    }

    private class IntegrityCheckException(message: String) : Exception(message)

    companion object {
        private const val TAG = "PdfDownloadWorker"
        private const val CONNECT_TIMEOUT_MS = 10_000
        private const val READ_TIMEOUT_MS = 20_000
        private const val MAX_AUTO_RETRIES = 2

        const val CHANNEL_ID = "devoirs_pdf_downloads"
        private const val NOTIFICATION_BASE_ID = 42_000

        const val TAG_ALL_PDF_DOWNLOADS = "tag_devoirs_pdf_downloads"
        const val KEY_DEVOIR_ID = "devoir_id"
        const val KEY_DEVOIR_TITLE = "devoir_title"
        const val KEY_SUJET_URL = "sujet_url"
        const val KEY_CORRIGE_URL = "corrige_url"
        const val KEY_SUJET_SIZE_BYTES = "sujet_size_bytes"
        const val KEY_CORRIGE_SIZE_BYTES = "corrige_size_bytes"
        const val KEY_SUJET_SHA256 = "sujet_sha256"
        const val KEY_CORRIGE_SHA256 = "corrige_sha256"
        const val KEY_VERSION = "version"

        const val KEY_PROGRESS = "progress_percent"
        const val KEY_BYTES_DOWNLOADED = "bytes_downloaded"
        const val KEY_TOTAL_BYTES = "total_bytes"
        const val KEY_ERROR_MESSAGE = "error_message"

        fun uniqueWorkName(devoirId: String): String = "pdf_download_$devoirId"
        fun packTag(packId: String): String = "pdf_pack_$packId"
    }
}
