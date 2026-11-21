package com.example.data.local

import android.content.Context
import android.util.Log
import com.example.data.model.DevoirItem
import java.io.File
import java.net.HttpURLConnection
import java.net.URL
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/** Origine effectivement utilisée lors du chargement du catalogue. */
enum class CatalogueOrigin {
    REMOTE,
    LOCAL_CACHE,
    BUNDLED_FALLBACK
}

/** Résultat complet d'une synchronisation de `catalogue.json`. */
data class CatalogueFetchResult(
    val catalogueVersion: Int,
    val devoirs: List<DevoirItem>,
    val origin: CatalogueOrigin,
    val syncedAtMillis: Long,
    val errorMessage: String? = null
)

/**
 * Service de récupération en ligne de `catalogue.json` avec cache disque local.
 * - Aucune clé API ni secret n'est embarqué : l'URL cible est un endpoint public HTTPS.
 * - En cas d'absence de réseau ou d'erreur serveur, l'application bascule silencieusement
 *   sur le dernier cache local (`filesDir/catalogue/catalogue_cache.json`) puis sur le catalogue
 *   initial embarqué afin de rester 100 % utilisable hors ligne.
 */
class RemoteCatalogueService(
    private val context: Context,
    private val catalogueUrl: String = DEFAULT_CATALOGUE_URL,
    private val remoteFetcher: (String) -> String = ::defaultHttpGet
) {
    private val parser = DevoirJsonParser { message, throwable ->
        if (throwable != null) {
            Log.e(TAG, message, throwable)
        } else {
            Log.w(TAG, message)
        }
    }

    private val cacheFile: File
        get() = File(File(context.filesDir, CACHE_DIR_NAME).apply { mkdirs() }, CACHE_FILE_NAME)

    /**
     * Tente de télécharger le catalogue distant, le valide, le met en cache disque,
     * puis retombe sur le cache local ou les assets en cas d'indisponibilité réseau.
     */
    suspend fun fetchCatalogueWithFallback(): CatalogueFetchResult = withContext(Dispatchers.IO) {
        val now = System.currentTimeMillis()
        var remoteError: String? = null

        // 1. Tentative de récupération distante
        try {
            val rawRemote = remoteFetcher(catalogueUrl)
            val report = parser.parseCatalogueJson(catalogueUrl, rawRemote)
            if (report.devoirs.isNotEmpty()) {
                val deduped = deduplicateById(report.devoirs)
                writeCacheSafely(rawRemote)
                Log.i(TAG, "Catalogue distant chargé (v${report.catalogueVersion}, ${deduped.size} devoirs).")
                return@withContext CatalogueFetchResult(
                    catalogueVersion = report.catalogueVersion,
                    devoirs = deduped,
                    origin = CatalogueOrigin.REMOTE,
                    syncedAtMillis = now
                )
            } else {
                remoteError = "Catalogue distant vide ou invalide."
            }
        } catch (e: Exception) {
            remoteError = e.message ?: "Réseau indisponible"
            Log.w(TAG, "Récupération distante du catalogue échouée, bascule sur le cache local : $remoteError")
        }

        // 2. Repli sur le dernier cache local enregistré
        val cached = readCachedCatalogue()
        if (cached != null) {
            return@withContext cached.copy(errorMessage = remoteError)
        }

        // 3. Repli initial sur assets/catalogue.json (premier lancement hors ligne)
        val bundled = readBundledFallback()
        return@withContext bundled.copy(errorMessage = remoteError)
    }

    /** Lit uniquement le cache local ou les assets (sans appel réseau). */
    fun loadLocalCachedOrBundled(): CatalogueFetchResult =
        readCachedCatalogue() ?: readBundledFallback()

    private fun readCachedCatalogue(): CatalogueFetchResult? {
        val file = cacheFile
        if (!file.exists() || file.length() <= 0L) return null
        return try {
            val raw = file.readText(Charsets.UTF_8)
            val report = parser.parseCatalogueJson(file.name, raw)
            if (report.devoirs.isEmpty()) return null
            CatalogueFetchResult(
                catalogueVersion = report.catalogueVersion,
                devoirs = deduplicateById(report.devoirs),
                origin = CatalogueOrigin.LOCAL_CACHE,
                syncedAtMillis = file.lastModified()
            )
        } catch (e: Exception) {
            Log.w(TAG, "Cache local de catalogue illisible", e)
            null
        }
    }

    private fun readBundledFallback(): CatalogueFetchResult {
        val raw = try {
            context.assets.open(DevoirAssetLoader.CATALOGUE_FILE_NAME)
                .bufferedReader(Charsets.UTF_8)
                .use { it.readText() }
        } catch (e: Exception) {
            Log.e(TAG, "Impossible de lire le catalogue de secours dans assets/", e)
            return CatalogueFetchResult(
                catalogueVersion = 0,
                devoirs = emptyList(),
                origin = CatalogueOrigin.BUNDLED_FALLBACK,
                syncedAtMillis = 0L,
                errorMessage = e.message
            )
        }
        val report = parser.parseCatalogueJson(DevoirAssetLoader.CATALOGUE_FILE_NAME, raw)
        return CatalogueFetchResult(
            catalogueVersion = report.catalogueVersion,
            devoirs = deduplicateById(report.devoirs),
            origin = CatalogueOrigin.BUNDLED_FALLBACK,
            syncedAtMillis = 0L
        )
    }

    private fun writeCacheSafely(rawJson: String) {
        try {
            val tmp = File(cacheFile.parentFile, "${CACHE_FILE_NAME}.tmp")
            tmp.writeText(rawJson, Charsets.UTF_8)
            if (!tmp.renameTo(cacheFile)) {
                cacheFile.writeText(rawJson, Charsets.UTF_8)
                tmp.delete()
            }
        } catch (e: Exception) {
            Log.w(TAG, "Impossible d'écrire le cache local du catalogue", e)
        }
    }

    private fun deduplicateById(items: List<DevoirItem>): List<DevoirItem> {
        val map = LinkedHashMap<String, DevoirItem>()
        for (item in items) {
            val existing = map[item.id]
            if (existing == null || item.version >= existing.version) {
                map[item.id] = item
            }
        }
        return map.values.toList()
    }

    companion object {
        private const val TAG = "RemoteCatalogueService"
        private const val CACHE_DIR_NAME = "catalogue"
        private const val CACHE_FILE_NAME = "catalogue_cache.json"
        private const val CONNECT_TIMEOUT_MS = 8_000
        private const val READ_TIMEOUT_MS = 12_000

        /** URL publique du catalogue distant (sans aucune clé ni secret). */
        const val DEFAULT_CATALOGUE_URL = "https://cdn.devoirstn.example/catalogue.json"

        internal fun defaultHttpGet(urlString: String): String {
            val connection = (URL(urlString).openConnection() as HttpURLConnection).apply {
                requestMethod = "GET"
                connectTimeout = CONNECT_TIMEOUT_MS
                readTimeout = READ_TIMEOUT_MS
                instanceFollowRedirects = true
                setRequestProperty("Accept", "application/json")
            }
            return try {
                val code = connection.responseCode
                if (code !in 200..299) {
                    throw IllegalStateException("HTTP $code lors de la récupération du catalogue")
                }
                connection.inputStream.bufferedReader(Charsets.UTF_8).use { it.readText() }
            } finally {
                connection.disconnect()
            }
        }
    }
}
