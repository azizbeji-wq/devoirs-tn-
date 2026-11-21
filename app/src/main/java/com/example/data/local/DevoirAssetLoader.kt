package com.example.data.local

import android.content.Context
import android.util.Log
import com.example.data.model.DevoirItem

/**
 * Charge le catalogue des devoirs PDF depuis `assets/catalogue.json`.
 * Les entrées ou fichiers mal formés sont ignorés et journalisés sans jamais planter l'application.
 */
class DevoirAssetLoader(
    private val context: Context
) {
    private val parser = DevoirJsonParser { message, throwable ->
        if (throwable != null) {
            Log.e(TAG, message, throwable)
        } else {
            Log.w(TAG, message)
        }
    }

    fun loadAllDevoirs(): List<DevoirItem> {
        val rawJson = try {
            context.assets.open(CATALOGUE_FILE_NAME)
                .bufferedReader(Charsets.UTF_8)
                .use { it.readText() }
        } catch (e: Exception) {
            Log.e(TAG, "Impossible de lire assets/$CATALOGUE_FILE_NAME", e)
            return emptyList()
        }

        val report = parser.parseCatalogueJson(CATALOGUE_FILE_NAME, rawJson)
        if (report.skippedCount > 0) {
            Log.w(
                TAG,
                "$CATALOGUE_FILE_NAME : ${report.devoirs.size} chargé(s), ${report.skippedCount} ignoré(s)"
            )
        }

        // Déduplication par id en conservant la version la plus récente
        val loadedById = LinkedHashMap<String, DevoirItem>()
        for (devoir in report.devoirs) {
            val existing = loadedById[devoir.id]
            if (existing == null || devoir.version >= existing.version) {
                loadedById[devoir.id] = devoir
            }
        }

        Log.i(TAG, "Catalogue assets/$CATALOGUE_FILE_NAME chargé : ${loadedById.size} devoirs PDF.")
        return loadedById.values.toList()
    }

    companion object {
        private const val TAG = "DevoirAssetLoader"
        const val CATALOGUE_FILE_NAME = "catalogue.json"
    }
}
