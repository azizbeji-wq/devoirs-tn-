package com.example.ui.pdf

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.pdf.PdfRenderer
import android.os.ParcelFileDescriptor
import android.util.Log
import android.util.LruCache
import java.io.Closeable
import java.io.File
import java.io.FileNotFoundException
import java.io.IOException
import java.util.concurrent.atomic.AtomicBoolean
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext

/**
 * Erreurs typées pouvant survenir lors de l'ouverture ou du rendu d'un PDF.
 */
sealed class PdfLoadError(val detail: String? = null) : Exception(detail) {
    class FileNotFound(val path: String) : PdfLoadError("Fichier introuvable : $path")
    class CorruptedPdf(detail: String? = null) : PdfLoadError(detail)
    class OutOfMemory(detail: String? = null) : PdfLoadError(detail)
    class Unexpected(detail: String? = null) : PdfLoadError(detail)
}

/**
 * Contrôleur natif basé sur [PdfRenderer] (sans aucune bibliothèque externe) :
 * - ouvre directement un fichier PDF téléchargé dans le dossier privé de l'application,
 * - rendu asynchrone sur [Dispatchers.IO] protégé par un [Mutex],
 * - cache mémoire [LruCache] de bitmaps dimensionné selon la RAM disponible,
 * - gestion robuste des erreurs (fichier manquant, PDF corrompu, mémoire insuffisante),
 * - libération garantie de [PdfRenderer] et [ParcelFileDescriptor] sans bloquer le thread principal.
 */
class PdfDocumentController private constructor(
    private val fileDescriptor: ParcelFileDescriptor,
    private val renderer: PdfRenderer,
    val pageCount: Int
) : Closeable {

    private val renderMutex = Mutex()
    private val closed = AtomicBoolean(false)

    // Cache LRU limité à ~1/8 de la mémoire max JVM (plafonné à 24 Mo) pour éviter les OOM
    private val cacheSizeKb: Int = run {
        val maxMemKb = (Runtime.getRuntime().maxMemory() / 1024L).toInt()
        (maxMemKb / 8).coerceIn(4 * 1024, 24 * 1024)
    }

    private val bitmapCache = object : LruCache<String, Bitmap>(cacheSizeKb) {
        override fun sizeOf(key: String, value: Bitmap): Int =
            (value.allocationByteCount / 1024).coerceAtLeast(1)
    }

    /**
     * Retourne un [Bitmap] déjà en cache ou rend la page [pageIndex] en arrière-plan.
     */
    suspend fun renderPage(
        pageIndex: Int,
        targetWidthPx: Int
    ): Result<Bitmap> = withContext(Dispatchers.IO) {
        if (closed.get()) {
            return@withContext Result.failure(PdfLoadError.Unexpected("Document fermé"))
        }
        if (pageIndex !in 0 until pageCount) {
            return@withContext Result.failure(PdfLoadError.Unexpected("Index de page invalide : $pageIndex"))
        }

        val clampedWidth = targetWidthPx.coerceIn(MIN_RENDER_WIDTH_PX, MAX_RENDER_WIDTH_PX)
        val cacheKey = "${pageIndex}_w$clampedWidth"

        synchronized(bitmapCache) {
            bitmapCache.get(cacheKey)?.takeIf { !it.isRecycled }?.let { cached ->
                return@withContext Result.success(cached)
            }
        }

        renderMutex.withLock {
            if (closed.get()) {
                return@withContext Result.failure(PdfLoadError.Unexpected("Document fermé"))
            }

            synchronized(bitmapCache) {
                bitmapCache.get(cacheKey)?.takeIf { !it.isRecycled }?.let { cached ->
                    return@withContext Result.success(cached)
                }
            }

            try {
                val bitmap = renderPageLocked(pageIndex, clampedWidth)
                synchronized(bitmapCache) {
                    bitmapCache.put(cacheKey, bitmap)
                }
                Result.success(bitmap)
            } catch (oom: OutOfMemoryError) {
                Log.w(TAG, "Mémoire insuffisante lors du rendu de la page $pageIndex, vidage du cache", oom)
                clearCache()
                try {
                    val reducedWidth = (clampedWidth / 2).coerceAtLeast(MIN_RENDER_WIDTH_PX)
                    Result.success(renderPageLocked(pageIndex, reducedWidth))
                } catch (oomRetry: OutOfMemoryError) {
                    Log.e(TAG, "Mémoire insuffisante persistante sur la page $pageIndex", oomRetry)
                    Result.failure(PdfLoadError.OutOfMemory(oomRetry.message))
                }
            } catch (e: SecurityException) {
                Result.failure(PdfLoadError.CorruptedPdf(e.message))
            } catch (e: IOException) {
                Result.failure(PdfLoadError.CorruptedPdf(e.message))
            } catch (e: IllegalStateException) {
                Result.failure(PdfLoadError.CorruptedPdf(e.message))
            } catch (e: Exception) {
                Result.failure(PdfLoadError.Unexpected(e.message))
            }
        }
    }

    private fun renderPageLocked(pageIndex: Int, widthPx: Int): Bitmap {
        val page = renderer.openPage(pageIndex)
        try {
            val aspectRatio = page.height.toFloat() / page.width.toFloat().coerceAtLeast(1f)
            val heightPx = (widthPx * aspectRatio).toInt().coerceIn(240, MAX_RENDER_HEIGHT_PX)
            val bitmap = Bitmap.createBitmap(widthPx, heightPx, Bitmap.Config.ARGB_8888)
            Canvas(bitmap).drawColor(Color.WHITE)
            page.render(bitmap, null, null, PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY)
            return bitmap
        } finally {
            page.close()
        }
    }

    fun clearCache() {
        synchronized(bitmapCache) {
            bitmapCache.evictAll()
        }
    }

    override fun close() {
        if (!closed.compareAndSet(false, true)) return
        clearCache()
        cleanupScope.launch {
            renderMutex.withLock {
                runCatching { renderer.close() }
                runCatching { fileDescriptor.close() }
            }
        }
    }

    companion object {
        private const val TAG = "PdfDocumentController"
        private const val CACHE_SUBDIR = "pdf_cache"
        private const val MIN_RENDER_WIDTH_PX = 360
        private const val MAX_RENDER_WIDTH_PX = 1800
        private const val MAX_RENDER_HEIGHT_PX = 3000

        private val cleanupScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

        /**
         * Ouvre un fichier PDF stocké dans le dossier privé de l'application ([path] absolu)
         * ou, par compatibilité, depuis `assets/` s'il s'agit d'un chemin relatif.
         */
        suspend fun open(
            context: Context,
            path: String
        ): Result<PdfDocumentController> = withContext(Dispatchers.IO) {
            val trimmed = path.trim()
            if (trimmed.isEmpty()) {
                return@withContext Result.failure(PdfLoadError.FileNotFound(path))
            }

            var pfd: ParcelFileDescriptor? = null
            var renderer: PdfRenderer? = null
            try {
                val targetFile = resolveReadableFile(context, trimmed)
                    ?: return@withContext Result.failure(PdfLoadError.FileNotFound(trimmed))

                if (!targetFile.exists() || targetFile.length() <= 0L) {
                    return@withContext Result.failure(
                        PdfLoadError.CorruptedPdf("Fichier PDF vide ou absent ($trimmed)")
                    )
                }

                pfd = ParcelFileDescriptor.open(targetFile, ParcelFileDescriptor.MODE_READ_ONLY)
                renderer = PdfRenderer(pfd)
                val count = renderer.pageCount
                if (count <= 0) {
                    renderer.close()
                    pfd.close()
                    return@withContext Result.failure(
                        PdfLoadError.CorruptedPdf("Le document ne contient aucune page.")
                    )
                }

                Result.success(
                    PdfDocumentController(
                        fileDescriptor = pfd,
                        renderer = renderer,
                        pageCount = count
                    )
                )
            } catch (e: FileNotFoundException) {
                runCatching { renderer?.close() }
                runCatching { pfd?.close() }
                Result.failure(PdfLoadError.FileNotFound(trimmed))
            } catch (e: SecurityException) {
                runCatching { renderer?.close() }
                runCatching { pfd?.close() }
                Result.failure(PdfLoadError.CorruptedPdf(e.message))
            } catch (e: IOException) {
                runCatching { renderer?.close() }
                runCatching { pfd?.close() }
                Result.failure(PdfLoadError.CorruptedPdf(e.message))
            } catch (e: IllegalArgumentException) {
                runCatching { renderer?.close() }
                runCatching { pfd?.close() }
                Result.failure(PdfLoadError.CorruptedPdf(e.message))
            } catch (oom: OutOfMemoryError) {
                runCatching { renderer?.close() }
                runCatching { pfd?.close() }
                Result.failure(PdfLoadError.OutOfMemory(oom.message))
            } catch (e: Exception) {
                runCatching { renderer?.close() }
                runCatching { pfd?.close() }
                Result.failure(PdfLoadError.Unexpected(e.message))
            }
        }

        /** Alias conservé pour la rétrocompatibilité des appels existants. */
        suspend fun openFromAsset(
            context: Context,
            assetPath: String
        ): Result<PdfDocumentController> = open(context, assetPath)

        private fun resolveReadableFile(context: Context, path: String): File? {
            val directFile = File(path)
            if (directFile.isAbsolute) {
                return directFile.takeIf { it.exists() && it.isFile }
            }

            val normalizedAsset = path.removePrefix("/")
            val cacheDir = File(context.cacheDir, CACHE_SUBDIR).apply { mkdirs() }
            val safeFileName = normalizedAsset.replace('/', '_').replace('\\', '_')
            val outFile = File(cacheDir, safeFileName)
            context.assets.open(normalizedAsset).use { input ->
                outFile.outputStream().use { output -> input.copyTo(output) }
            }
            return outFile.takeIf { it.exists() }
        }
    }
}
