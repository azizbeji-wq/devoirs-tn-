package com.example.data.ai

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.pdf.PdfRenderer
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.os.ParcelFileDescriptor
import android.util.Log
import com.example.data.local.DevoirPdfContentDao
import com.example.data.local.DevoirPdfContentFtsEntity
import com.google.android.gms.tasks.Task
import com.google.firebase.Firebase
import com.google.firebase.ai.ai
import com.google.firebase.ai.type.content
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.latin.TextRecognizerOptions
import java.io.File
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException
import kotlin.math.roundToInt
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeout

/** Résumé du résultat d'indexation OCR d'un devoir PDF. */
data class PdfExtractionSummary(
    val devoirId: String,
    val pagesIndexed: Int,
    val charCount: Int,
    val ocrSource: String
)

/**
 * Pipeline d'extraction de texte plein-texte pour les devoirs PDF téléchargés :
 * 1. Rendu page par page via [PdfRenderer] (sans dépendance PDF tierce),
 * 2. Reconnaissance locale ML Kit Text Recognition (écriture latine : français, anglais, maths),
 * 3. Repli (fallback) Gemini multimodal pour les pages en arabe ou peu reconnues en latin,
 * 4. Persistance dans la table virtuelle Room FTS4 (`devoir_pdf_contents_fts`).
 */
class PdfTextExtractor(
    private val appContext: Context,
    private val contentDao: DevoirPdfContentDao
) {
    // Initialisation paresseuse afin que Firebase.ai ne soit appelé qu'après configureAppCheck()
    private val ocrFallbackModel by lazy {
        Firebase.ai.generativeModel(modelName = OCR_MODEL_NAME)
    }

    /**
     * Extrait et indexe dans Room FTS4 le texte du sujet PDF (et du corrigé s'il est disponible).
     * L'opération est tolérante aux erreurs (ne bloque jamais un téléchargement hors ligne).
     */
    suspend fun extractAndIndexDevoir(
        devoirId: String,
        title: String,
        subject: String,
        chapters: List<String>,
        sujetFile: File,
        corrigeFile: File? = null
    ): PdfExtractionSummary = withContext(Dispatchers.IO) {
        val combinedText = StringBuilder()
        var totalPagesIndexed = 0
        var usedLatin = false
        var usedGeminiArabic = false

        val filesToScan = buildList {
            if (sujetFile.exists() && sujetFile.length() > 0L) add(sujetFile)
            if (corrigeFile != null && corrigeFile.exists() && corrigeFile.length() > 0L) add(corrigeFile)
        }

        if (filesToScan.isEmpty()) {
            return@withContext PdfExtractionSummary(devoirId, 0, 0, SOURCE_NONE)
        }

        val recognizer = TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS)
        try {
            var geminiPagesUsed = 0
            for (pdfFile in filesToScan) {
                val pfd = try {
                    ParcelFileDescriptor.open(pdfFile, ParcelFileDescriptor.MODE_READ_ONLY)
                } catch (e: Exception) {
                    Log.w(TAG, "Impossible d'ouvrir ${pdfFile.name} pour OCR : ${e.message}")
                    continue
                }

                try {
                    PdfRenderer(pfd).use { renderer ->
                        val pagesToScan = renderer.pageCount.coerceAtMost(MAX_PAGES_PER_FILE)
                        for (pageIdx in 0 until pagesToScan) {
                            val pageBitmap = renderPageForOcr(renderer, pageIdx) ?: continue
                            try {
                                val latinText = runCatching {
                                    recognizer.process(InputImage.fromBitmap(pageBitmap, 0))
                                        .awaitMlKit()
                                        .text
                                        .trim()
                                }.getOrDefault("")

                                if (latinText.isNotBlank()) {
                                    usedLatin = true
                                }

                                val needsArabicFallback = shouldUseArabicFallback(subject, latinText)
                                val finalPageText = if (
                                    needsArabicFallback &&
                                    geminiPagesUsed < MAX_GEMINI_FALLBACK_PAGES &&
                                    isNetworkAvailable()
                                ) {
                                    geminiPagesUsed++
                                    val geminiText = extractTextWithGeminiFallback(pageBitmap)
                                    if (!geminiText.isNullOrBlank()) {
                                        usedGeminiArabic = true
                                        geminiText
                                    } else {
                                        latinText
                                    }
                                } else {
                                    latinText
                                }

                                if (finalPageText.isNotBlank()) {
                                    if (combinedText.isNotEmpty()) combinedText.append('\n')
                                    combinedText.append(finalPageText)
                                }
                                totalPagesIndexed++
                            } finally {
                                if (!pageBitmap.isRecycled) {
                                    pageBitmap.recycle()
                                }
                            }
                        }
                    }
                } catch (ce: CancellationException) {
                    throw ce
                } catch (e: Exception) {
                    Log.w(TAG, "Erreur PdfRenderer OCR sur ${pdfFile.name} : ${e.message}")
                } finally {
                    runCatching { pfd.close() }
                }
            }
        } finally {
            runCatching { recognizer.close() }
        }

        val ocrSource = when {
            usedLatin && usedGeminiArabic -> SOURCE_MIXED
            usedGeminiArabic -> SOURCE_GEMINI_ARABIC
            usedLatin -> SOURCE_MLKIT_LATIN
            else -> SOURCE_METADATA_ONLY
        }

        val extracted = combinedText.toString().trim().take(MAX_STORED_CHARS)
        contentDao.upsertByDevoirId(
            DevoirPdfContentFtsEntity(
                devoirId = devoirId,
                title = title,
                chapters = chapters.joinToString(" "),
                extractedText = extracted,
                ocrSource = ocrSource,
                indexedAtMillis = System.currentTimeMillis()
            )
        )

        PdfExtractionSummary(
            devoirId = devoirId,
            pagesIndexed = totalPagesIndexed,
            charCount = extracted.length,
            ocrSource = ocrSource
        )
    }

    /** Rend une page PDF sur fond blanc à une largeur adaptée à l'OCR tout en limitant la RAM. */
    private fun renderPageForOcr(renderer: PdfRenderer, pageIndex: Int): Bitmap? {
        return try {
            renderer.openPage(pageIndex).use { page ->
                val rawW = page.width.coerceAtLeast(1)
                val rawH = page.height.coerceAtLeast(1)
                val (targetW, targetH) = PdfPageImageScaler.computeScaledDimensions(
                    width = rawW * 2,
                    height = rawH * 2,
                    maxDimensionPx = OCR_RENDER_MAX_PX
                )
                val bitmap = Bitmap.createBitmap(targetW, targetH, Bitmap.Config.ARGB_8888)
                val canvas = Canvas(bitmap)
                canvas.drawColor(Color.WHITE)
                page.render(bitmap, null, null, PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY)
                bitmap
            }
        } catch (oom: OutOfMemoryError) {
            Log.w(TAG, "Mémoire insuffisante pendant le rendu OCR page $pageIndex", oom)
            null
        } catch (e: Exception) {
            Log.w(TAG, "Échec rendu OCR page $pageIndex : ${e.message}")
            null
        }
    }

    /** Fallback Gemini multimodal pour extraire le texte arabe ou bilingue d'une page PDF. */
    private suspend fun extractTextWithGeminiFallback(pageBitmap: Bitmap): String? {
        val scaled = PdfPageImageScaler.prepareBitmapForGemini(
            source = pageBitmap,
            maxDimensionPx = PdfPageImageScaler.DEFAULT_MAX_DIMENSION_PX,
            jpegQuality = 75
        )
        return try {
            withTimeout(GEMINI_OCR_TIMEOUT_MS) {
                val response = ocrFallbackModel.generateContent(
                    content {
                        image(scaled)
                        text(
                            "Transcris fidèlement tout le texte visible sur cette page de devoir scolaire " +
                                "(arabe, français, titres, numéros d'exercices et expressions mathématiques simples). " +
                                "Réponds uniquement par le texte extrait brut, sans commentaire ni Markdown."
                        )
                    }
                )
                response.text?.trim()?.takeIf { it.isNotBlank() }
            }
        } catch (ce: CancellationException) {
            throw ce
        } catch (e: Exception) {
            Log.w(TAG, "Fallback OCR Gemini indisponible : ${e.message}")
            null
        } finally {
            if (scaled !== pageBitmap && !scaled.isRecycled) {
                scaled.recycle()
            }
        }
    }

    private fun isNetworkAvailable(): Boolean {
        return try {
            val cm = appContext.getSystemService(Context.CONNECTIVITY_SERVICE) as? ConnectivityManager
                ?: return false
            val network = cm.activeNetwork ?: return false
            val caps = cm.getNetworkCapabilities(network) ?: return false
            caps.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
        } catch (_: Exception) {
            false
        }
    }

    companion object {
        private const val TAG = "PdfTextExtractor"
        private const val OCR_MODEL_NAME = "gemini-3.8-flash"
        private const val OCR_RENDER_MAX_PX = 1024
        private const val MAX_PAGES_PER_FILE = 10
        private const val MAX_GEMINI_FALLBACK_PAGES = 5
        private const val MAX_STORED_CHARS = 60_000
        private const val GEMINI_OCR_TIMEOUT_MS = 25_000L
        private const val MIN_LATIN_CHARS_THRESHOLD = 28

        const val SOURCE_MLKIT_LATIN = "MLKIT_LATIN"
        const val SOURCE_GEMINI_ARABIC = "GEMINI_ARABIC"
        const val SOURCE_MIXED = "MIXED"
        const val SOURCE_METADATA_ONLY = "METADATA_ONLY"
        const val SOURCE_NONE = "NONE"

        private val ARABIC_SUBJECT_KEYWORDS = listOf(
            "arabe",
            "العربية",
            "histoire",
            "géographie",
            "geographie",
            "éducation islamique",
            "education islamique",
            "éducation civique",
            "education civique",
            "فلسفة",
            "تاريخ",
            "جغرافيا"
        )

        /**
         * Détermine s'il faut déclencher le fallback Gemini (OCR arabe / bilingue) :
         * - si la matière est rédigée en arabe dans le programme tunisien, ou
         * - si l'OCR Latin ML Kit a reconnu très peu de caractères alphanumériques (page arabe ou scan complexe).
         */
        fun shouldUseArabicFallback(subject: String, latinOcrText: String): Boolean {
            val normalizedSubject = subject.trim().lowercase()
            if (ARABIC_SUBJECT_KEYWORDS.any { normalizedSubject.contains(it) }) {
                return true
            }
            val meaningfulChars = latinOcrText.count { it.isLetterOrDigit() }
            return meaningfulChars < MIN_LATIN_CHARS_THRESHOLD
        }
    }
}

/** Conversion coroutine légère d'une [Task] Google Play Services / ML Kit sans dépendance supplémentaire. */
private suspend fun <T> Task<T>.awaitMlKit(): T = suspendCancellableCoroutine { cont ->
    addOnSuccessListener { result ->
        if (cont.isActive) cont.resume(result)
    }
    addOnFailureListener { error ->
        if (cont.isActive) cont.resumeWithException(error)
    }
    addOnCanceledListener {
        cont.cancel()
    }
}
