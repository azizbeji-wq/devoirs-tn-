package com.example.data.ai

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Color
import java.io.ByteArrayOutputStream
import kotlin.math.max
import kotlin.math.roundToInt

/**
 * Utilitaire de réduction et compression d'image avant envoi à l'API Gemini.
 *
 * Garantit :
 * - une dimension maximale maîtrisée (1024 px par défaut) pour limiter la bande passante
 *   et rester largement sous les limites de taille d'image de Gemini,
 * - un fond blanc opaque (sans canal alpha inutile),
 * - une préservation totale du bitmap source utilisé par le lecteur PDF.
 */
object PdfPageImageScaler {

    const val DEFAULT_MAX_DIMENSION_PX = 1024
    const val DEFAULT_JPEG_QUALITY = 78
    private const val SOFT_BYTE_BUDGET = 600 * 1024 // ~600 Ko pour un envoi rapide en 4G/Wi-Fi

    /**
     * Calcule les dimensions redimensionnées en préservant le ratio largeur/hauteur.
     * Fonction pure directement testable en JUnit.
     */
    fun computeScaledDimensions(
        width: Int,
        height: Int,
        maxDimensionPx: Int = DEFAULT_MAX_DIMENSION_PX
    ): Pair<Int, Int> {
        val safeW = width.coerceAtLeast(1)
        val safeH = height.coerceAtLeast(1)
        val safeMax = maxDimensionPx.coerceAtLeast(64)
        val largest = max(safeW, safeH)
        if (largest <= safeMax) return safeW to safeH

        val ratio = safeMax.toDouble() / largest.toDouble()
        val targetW = (safeW * ratio).roundToInt().coerceAtLeast(1)
        val targetH = (safeH * ratio).roundToInt().coerceAtLeast(1)
        return targetW to targetH
    }

    /**
     * Prépare une copie légère et compressée (JPEG) d'une page PDF avant envoi à Gemini.
     * Ne recycle jamais le bitmap [source].
     */
    fun prepareBitmapForGemini(
        source: Bitmap,
        maxDimensionPx: Int = DEFAULT_MAX_DIMENSION_PX,
        jpegQuality: Int = DEFAULT_JPEG_QUALITY
    ): Bitmap {
        val (targetW, targetH) = computeScaledDimensions(source.width, source.height, maxDimensionPx)

        // Copie sur fond blanc pour supprimer toute transparence
        val scaled = if (targetW == source.width && targetH == source.height) {
            Bitmap.createBitmap(targetW, targetH, Bitmap.Config.ARGB_8888).also { copy ->
                val canvas = Canvas(copy)
                canvas.drawColor(Color.WHITE)
                canvas.drawBitmap(source, 0f, 0f, null)
            }
        } else {
            val resized = Bitmap.createScaledBitmap(source, targetW, targetH, true)
            Bitmap.createBitmap(targetW, targetH, Bitmap.Config.ARGB_8888).also { copy ->
                val canvas = Canvas(copy)
                canvas.drawColor(Color.WHITE)
                canvas.drawBitmap(resized, 0f, 0f, null)
                if (resized !== source && !resized.isRecycled) {
                    resized.recycle()
                }
            }
        }

        // Compression JPEG adaptative pour garantir une empreinte réseau réduite
        val stream = ByteArrayOutputStream()
        scaled.compress(Bitmap.CompressFormat.JPEG, jpegQuality.coerceIn(45, 92), stream)
        var bytes = stream.toByteArray()
        if (bytes.size > SOFT_BYTE_BUDGET) {
            stream.reset()
            scaled.compress(Bitmap.CompressFormat.JPEG, 60, stream)
            bytes = stream.toByteArray()
        }

        val decoded = BitmapFactory.decodeByteArray(bytes, 0, bytes.size)
        return if (decoded != null) {
            if (decoded !== scaled && !scaled.isRecycled) {
                scaled.recycle()
            }
            decoded
        } else {
            scaled
        }
    }
}
