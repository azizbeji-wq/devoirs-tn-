package com.example.data.local

import android.content.Context
import android.util.Log
import com.example.data.model.DevoirItem

/**
 * Charge le catalogue unifié depuis les assets :
 * - Source principale : `assets/catalogue.json` (devoirs PDF),
 * - Source secondaire : `assets/devoirs/ (JSON)` (anciens sujets migrés en « consultables en texte »
 *   uniquement lorsqu'aucun devoir PDF n'a le même `id`, sans doublon).
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

    /** Charge uniquement les devoirs PDF déclarés dans `assets/catalogue.json`. */
    fun loadBundledPdfDevoirs(): List<DevoirItem> {
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
        return report.devoirs
    }

    /**
     * Charge les anciens sujets JSON depuis `assets/devoirs/ (JSON)` (hors schéma)
     * afin de les rendre consultables en texte lorsqu'ils n'ont pas encore de version PDF.
     */
    fun loadLegacyTextDevoirs(): List<DevoirItem> {
        val fileNames = try {
            context.assets.list(LEGACY_DEVOIRS_DIR)
                ?.filter { it.endsWith(".json", ignoreCase = true) && it != LEGACY_SCHEMA_FILE }
                ?.sorted()
                .orEmpty()
        } catch (e: Exception) {
            Log.w(TAG, "Impossible de lister assets/$LEGACY_DEVOIRS_DIR", e)
            return emptyList()
        }

        val loaded = mutableListOf<DevoirItem>()
        for (fileName in fileNames) {
            val assetPath = "$LEGACY_DEVOIRS_DIR/$fileName"
            val rawJson = try {
                context.assets.open(assetPath)
                    .bufferedReader(Charsets.UTF_8)
                    .use { it.readText() }
            } catch (e: Exception) {
                Log.w(TAG, "Impossible de lire assets/$assetPath", e)
                continue
            }

            val report = parser.parseLegacyTextDevoirsJson(fileName, rawJson)
            if (report.skippedCount > 0) {
                Log.w(TAG, "$assetPath : ${report.devoirs.size} chargé(s), ${report.skippedCount} ignoré(s)")
            }
            loaded.addAll(report.devoirs)
        }

        Log.i(TAG, "Anciens sujets texte chargés depuis assets/$LEGACY_DEVOIRS_DIR : ${loaded.size}")
        return loaded
    }

    /** Retourne le catalogue complet unifié (PDF prioritaire + anciens sujets texte sans doublon). */
    fun loadAllDevoirs(): List<DevoirItem> {
        val pdfDevoirs = loadBundledPdfDevoirs()
        val legacyTextDevoirs = loadLegacyTextDevoirs()
        val unified = DevoirCatalogueUnifier.unify(
            pdfDevoirs = pdfDevoirs,
            legacyTextDevoirs = legacyTextDevoirs
        )
        Log.i(
            TAG,
            "Catalogue unifié prêt : ${unified.size} devoirs (${pdfDevoirs.size} PDF, ${unified.count { it.isTextOnly }} texte)."
        )
        return unified
    }

    companion object {
        private const val TAG = "DevoirAssetLoader"
        const val CATALOGUE_FILE_NAME = "catalogue.json"
        const val LEGACY_DEVOIRS_DIR = "devoirs"
        private const val LEGACY_SCHEMA_FILE = "devoir_schema.json"
    }
}
