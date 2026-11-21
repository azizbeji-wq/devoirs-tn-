package com.example.data.download

import android.content.Context
import androidx.work.BackoffPolicy
import androidx.work.Constraints
import androidx.work.ExistingWorkPolicy
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkInfo
import androidx.work.WorkManager
import androidx.work.workDataOf
import com.example.data.local.DevoirDao
import com.example.data.local.DevoirPdfContentDao
import com.example.data.model.CycleLevel
import com.example.data.model.DevoirItem
import com.example.data.model.Trimester
import java.util.concurrent.TimeUnit
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext

/** État de téléchargement d'un devoir issu de WorkManager + stockage local. */
enum class DownloadTaskStatus {
    IDLE,
    QUEUED,
    DOWNLOADING,
    PAUSED_RESUMABLE,
    FAILED
}

/** Suivi temps réel du téléchargement d'un devoir. */
data class DevoirDownloadProgress(
    val devoirId: String,
    val status: DownloadTaskStatus = DownloadTaskStatus.IDLE,
    val progressPercent: Int = 0,
    val bytesDownloaded: Long = 0L,
    val totalBytes: Long = 0L,
    val errorMessage: String? = null
) {
    val isActive: Boolean
        get() = status == DownloadTaskStatus.QUEUED || status == DownloadTaskStatus.DOWNLOADING
}

/** Représentation d'un pack de devoirs téléchargeable en un clic (ex. Trimestre 1 — 9ème année). */
data class DownloadPack(
    val packId: String,
    val level: CycleLevel,
    val trimester: Trimester,
    val devoirs: List<DevoirItem>
) {
    val totalCount: Int get() = devoirs.size
    val downloadedCount: Int get() = devoirs.count { it.isAvailableOffline }
    val missingDevoirs: List<DevoirItem> get() = devoirs.filterNot { it.isAvailableOffline }
    val isFullyDownloaded: Boolean get() = devoirs.isNotEmpty() && downloadedCount == totalCount
    val totalSizeMb: Double get() = devoirs.sumOf { it.sizeMb }
    val remainingSizeMb: Double get() = missingDevoirs.sumOf { it.sizeMb }
    val remainingBytes: Long get() = missingDevoirs.sumOf { it.estimatedTotalBytes }
    val downloadedBytes: Long get() = devoirs.filter { it.isAvailableOffline }.sumOf { it.downloadedBytes }
}

/**
 * Coordinateur des téléchargements PDF via WorkManager :
 * - Planifie les téléchargements unitaires ou par pack avec contrainte Wi-Fi par défaut (`UNMETERED`),
 * - Permet l'annulation (en conservant `.part`) et la reprise,
 * - Expose la progression temps réel de tous les devoirs en cours,
 * - Gère la suppression sur disque et le nettoyage Room (par devoir, par pack ou global).
 */
class PdfDownloadCoordinator(
    private val context: Context,
    private val devoirDao: DevoirDao,
    val storageManager: PdfStorageManager = PdfStorageManager(context),
    private val pdfContentDao: DevoirPdfContentDao? = null
) {
    private val workManager: WorkManager by lazy { WorkManager.getInstance(context) }

    /** Flux réactif de l'état de téléchargement par `devoirId` observé depuis WorkManager. */
    fun observeDownloadStates(): Flow<Map<String, DevoirDownloadProgress>> =
        workManager.getWorkInfosByTagFlow(PdfDownloadWorker.TAG_ALL_PDF_DOWNLOADS)
            .map { infos ->
                val map = LinkedHashMap<String, DevoirDownloadProgress>()
                for (info in infos) {
                    val devoirId = extractDevoirId(info) ?: continue
                    val mapped = mapWorkInfo(devoirId, info) ?: continue
                    val existing = map[devoirId]
                    // Priorise un travail actif (RUNNING/ENQUEUED) sur un historique terminé/annulé
                    if (existing == null || isHigherPriority(mapped.status, existing.status)) {
                        map[devoirId] = mapped
                    }
                }
                map
            }

    /** Planifie le téléchargement (ou la reprise) d'un devoir unique. */
    fun enqueueDevoirDownload(
        devoir: DevoirItem,
        allowMobileData: Boolean,
        packId: String? = null
    ) {
        val networkType = if (allowMobileData) NetworkType.CONNECTED else NetworkType.UNMETERED
        val constraints = Constraints.Builder()
            .setRequiredNetworkType(networkType)
            .build()

        val input = workDataOf(
            PdfDownloadWorker.KEY_DEVOIR_ID to devoir.id,
            PdfDownloadWorker.KEY_DEVOIR_TITLE to devoir.title,
            PdfDownloadWorker.KEY_SUJET_URL to devoir.sujetUrl,
            PdfDownloadWorker.KEY_CORRIGE_URL to (devoir.corrigeUrl ?: ""),
            PdfDownloadWorker.KEY_SUJET_SIZE_BYTES to devoir.sujetSizeBytes,
            PdfDownloadWorker.KEY_CORRIGE_SIZE_BYTES to devoir.corrigeSizeBytes,
            PdfDownloadWorker.KEY_SUJET_SHA256 to (devoir.sujetSha256 ?: ""),
            PdfDownloadWorker.KEY_CORRIGE_SHA256 to (devoir.corrigeSha256 ?: ""),
            PdfDownloadWorker.KEY_VERSION to devoir.version
        )

        val requestBuilder = OneTimeWorkRequestBuilder<PdfDownloadWorker>()
            .setConstraints(constraints)
            .setInputData(input)
            .setBackoffCriteria(BackoffPolicy.EXPONENTIAL, 15, TimeUnit.SECONDS)
            .addTag(PdfDownloadWorker.TAG_ALL_PDF_DOWNLOADS)
            .addTag(devoirTag(devoir.id))

        if (!packId.isNullOrBlank()) {
            requestBuilder.addTag(PdfDownloadWorker.packTag(packId))
        }

        workManager.enqueueUniqueWork(
            PdfDownloadWorker.uniqueWorkName(devoir.id),
            ExistingWorkPolicy.REPLACE,
            requestBuilder.build()
        )
    }

    /** Planifie en un clic tous les devoirs non encore téléchargés d'un pack. */
    fun enqueuePackDownload(
        pack: DownloadPack,
        allowMobileData: Boolean
    ) {
        pack.missingDevoirs.forEach { devoir ->
            enqueueDevoirDownload(
                devoir = devoir,
                allowMobileData = allowMobileData,
                packId = pack.packId
            )
        }
    }

    /** Annule un téléchargement en cours (le fichier `.part` reste présent pour la reprise). */
    fun cancelDevoirDownload(devoirId: String) {
        workManager.cancelUniqueWork(PdfDownloadWorker.uniqueWorkName(devoirId))
    }

    /** Annule tous les téléchargements actifs d'un pack. */
    fun cancelPackDownload(packId: String) {
        workManager.cancelAllWorkByTag(PdfDownloadWorker.packTag(packId))
    }

    /** Supprime les PDF d'un devoir du dossier privé et met à jour Room + index FTS4. */
    suspend fun deleteDevoirDownload(devoirId: String) = withContext(Dispatchers.IO) {
        cancelDevoirDownload(devoirId)
        storageManager.deleteDevoirFiles(devoirId)
        devoirDao.clearDownload(devoirId)
        pdfContentDao?.deleteByDevoirId(devoirId)
    }

    /** Supprime tous les PDF d'un pack et nettoie les colonnes locales dans Room + index FTS4. */
    suspend fun deletePackDownloads(pack: DownloadPack) = withContext(Dispatchers.IO) {
        cancelPackDownload(pack.packId)
        val ids = pack.devoirs.map { it.id }
        storageManager.deleteDevoirsFiles(ids)
        if (ids.isNotEmpty()) {
            devoirDao.clearDownloads(ids)
            pdfContentDao?.deleteByDevoirIds(ids)
        }
    }

    /** Vide l'ensemble du stockage PDF hors ligne de l'application ainsi que l'index FTS4. */
    suspend fun deleteAllDownloads() = withContext(Dispatchers.IO) {
        workManager.cancelAllWorkByTag(PdfDownloadWorker.TAG_ALL_PDF_DOWNLOADS)
        storageManager.deleteAllDownloadedPdfs()
        devoirDao.clearAllDownloads()
        pdfContentDao?.deleteAll()
    }

    /** Vérifie si un devoir a un téléchargement partiel reprenable sur disque. */
    fun hasResumablePart(devoirId: String): Boolean = storageManager.hasPartialDownload(devoirId)

    /** Retourne le type de réseau actif de l'appareil. */
    fun getActiveNetworkKind(): NetworkConnectionKind = storageManager.getActiveNetworkKind()

    /** Espace total occupé par les PDF sur disque. */
    fun getTotalUsedBytes(): Long = storageManager.getTotalUsedBytes()

    private fun extractDevoirId(info: WorkInfo): String? {
        val fromProgress = info.progress.getString(PdfDownloadWorker.KEY_DEVOIR_ID)
        if (!fromProgress.isNullOrBlank()) return fromProgress
        val fromOutput = info.outputData.getString(PdfDownloadWorker.KEY_DEVOIR_ID)
        if (!fromOutput.isNullOrBlank()) return fromOutput
        val tagPrefix = DEVOIR_TAG_PREFIX
        return info.tags.firstOrNull { it.startsWith(tagPrefix) }?.removePrefix(tagPrefix)
    }

    private fun mapWorkInfo(devoirId: String, info: WorkInfo): DevoirDownloadProgress? = when (info.state) {
        WorkInfo.State.ENQUEUED, WorkInfo.State.BLOCKED -> DevoirDownloadProgress(
            devoirId = devoirId,
            status = DownloadTaskStatus.QUEUED,
            progressPercent = info.progress.getInt(PdfDownloadWorker.KEY_PROGRESS, 0)
        )

        WorkInfo.State.RUNNING -> DevoirDownloadProgress(
            devoirId = devoirId,
            status = DownloadTaskStatus.DOWNLOADING,
            progressPercent = info.progress.getInt(PdfDownloadWorker.KEY_PROGRESS, 0),
            bytesDownloaded = info.progress.getLong(PdfDownloadWorker.KEY_BYTES_DOWNLOADED, 0L),
            totalBytes = info.progress.getLong(PdfDownloadWorker.KEY_TOTAL_BYTES, 0L)
        )

        WorkInfo.State.CANCELLED -> if (storageManager.hasPartialDownload(devoirId)) {
            DevoirDownloadProgress(
                devoirId = devoirId,
                status = DownloadTaskStatus.PAUSED_RESUMABLE
            )
        } else {
            null
        }

        WorkInfo.State.FAILED -> DevoirDownloadProgress(
            devoirId = devoirId,
            status = DownloadTaskStatus.FAILED,
            errorMessage = info.outputData.getString(PdfDownloadWorker.KEY_ERROR_MESSAGE)
        )

        WorkInfo.State.SUCCEEDED -> null
    }

    private fun isHigherPriority(candidate: DownloadTaskStatus, existing: DownloadTaskStatus): Boolean {
        fun rank(s: DownloadTaskStatus) = when (s) {
            DownloadTaskStatus.DOWNLOADING -> 4
            DownloadTaskStatus.QUEUED -> 3
            DownloadTaskStatus.PAUSED_RESUMABLE -> 2
            DownloadTaskStatus.FAILED -> 1
            DownloadTaskStatus.IDLE -> 0
        }
        return rank(candidate) >= rank(existing)
    }

    companion object {
        private const val DEVOIR_TAG_PREFIX = "devoir_id:"
        private fun devoirTag(devoirId: String): String = "$DEVOIR_TAG_PREFIX$devoirId"

        /**
         * Construit dynamiquement les packs de téléchargement par (Niveau, Trimestre),
         * en plaçant « Trimestre 1 — 9ème année » en tête de liste.
         */
        fun buildPacks(devoirs: List<DevoirItem>): List<DownloadPack> {
            return devoirs
                .groupBy { it.level to it.trimester }
                .map { (key, group) ->
                    val (level, trimester) = key
                    DownloadPack(
                        packId = "${level.id}_${trimester.id}".lowercase(),
                        level = level,
                        trimester = trimester,
                        devoirs = group.sortedBy { it.subject }
                    )
                }
                .sortedWith(
                    compareByDescending<DownloadPack> {
                        it.level == CycleLevel.COLLEGE_9 && it.trimester == Trimester.T1
                    }
                        .thenByDescending { it.devoirs.size }
                        .thenBy { it.level.ordinal }
                        .thenBy { it.trimester.ordinal }
                )
        }
    }
}
