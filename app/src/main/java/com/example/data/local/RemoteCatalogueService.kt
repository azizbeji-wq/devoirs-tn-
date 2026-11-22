package com.example.data.local

import android.content.Context
import android.util.Log
import com.example.BuildConfig
import com.example.data.model.DevoirItem
import java.io.File
import java.io.IOException
import java.net.ConnectException
import java.net.HttpURLConnection
import java.net.MalformedURLException
import java.net.SocketTimeoutException
import java.net.URL
import java.net.UnknownHostException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/** Origine effectivement utilisée lors du chargement du catalogue. */
enum class CatalogueOrigin {
    REMOTE,
    LOCAL_CACHE,
    BUNDLED_FALLBACK
}

/** Résultat complet d'une synchronisation de `catalogue.json` unifié avec les sujets texte. */
data class CatalogueFetchResult(
    val catalogueVersion: Int,
    val devoirs: List<DevoirItem>,
    val origin: CatalogueOrigin,
    val syncedAtMillis: Long,
    val errorMessage: String? = null
)

/**
 * Service de récupération en ligne de `catalogue.json` avec cache disque local et unification
 * des anciens sujets texte (`assets/devoirs/` JSON) :
 * - L'URL cible est lue depuis `BuildConfig.CATALOGUE_URL` (configurable dans `.env`).
 * - Aucune clé API ni secret n'est embarqué.
 * - Si l'URL est invalide ou injoignable, l'application ne plante jamais, retourne un message
 *   explicite et bascule sur le cache local puis sur le catalogue embarqué + sujets texte.
 */
class RemoteCatalogueService(
    private val context: Context,
    private val catalogueUrl: String = DEFAULT_CATALOGUE_URL,
    private val remoteFetcher: (String) -> String = ::defaultHttpGet,
    private val legacyTextLoader: () -> List<DevoirItem> = { DevoirAssetLoader(context).loadLegacyTextDevoirs() }
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
     * l'unifie avec les anciens sujets texte sans PDF, puis retombe proprement sur le cache
     * local ou les assets en cas d'indisponibilité réseau (sans jamais lever d'exception).
     */
    suspend fun fetchCatalogueWithFallback(): CatalogueFetchResult = withContext(Dispatchers.IO) {
        val now = System.currentTimeMillis()
        val legacyTextDevoirs = runCatching { legacyTextLoader() }.getOrDefault(emptyList())
        val trimmedUrl = catalogueUrl.trim()
        var remoteError: String? = null

        // 1. Validation de l'URL et tentative de récupération distante
        if (!isValidCatalogueUrl(trimmedUrl)) {
            remoteError = buildUnreachableUrlMessage(
                url = trimmedUrl,
                reason = "URL du catalogue absente ou mal formée (vérifie CATALOGUE_URL dans .env)."
            )
            Log.w(TAG, remoteError)
        } else {
            try {
                val rawRemote = remoteFetcher(trimmedUrl)
                val report = parser.parseCatalogueJson(trimmedUrl, rawRemote)
                if (report.devoirs.isNotEmpty()) {
                    val unified = DevoirCatalogueUnifier.unify(
                        pdfDevoirs = report.devoirs,
                        legacyTextDevoirs = legacyTextDevoirs
                    )
                    writeCacheSafely(rawRemote)
                    Log.i(
                        TAG,
                        "Catalogue distant chargé (v${report.catalogueVersion}, ${unified.size} devoirs unifiés)."
                    )
                    return@withContext CatalogueFetchResult(
                        catalogueVersion = report.catalogueVersion,
                        devoirs = unified,
                        origin = CatalogueOrigin.REMOTE,
                        syncedAtMillis = now
                    )
                } else {
                    remoteError = buildUnreachableUrlMessage(
                        url = trimmedUrl,
                        reason = "le fichier JSON distant est vide ou ne contient aucun devoir PDF valide."
                    )
                }
            } catch (e: Exception) {
                remoteError = formatRemoteError(trimmedUrl, e)
                Log.w(TAG, "Récupération distante du catalogue échouée, bascule sur le cache local : $remoteError")
            }
        }

        // 2. Repli sur le dernier cache local enregistré + sujets texte
        val cached = readCachedCatalogue(legacyTextDevoirs)
        if (cached != null) {
            return@withContext cached.copy(errorMessage = remoteError)
        }

        // 3. Repli initial sur assets/catalogue.json + assets/devoirs/*.json (premier lancement hors ligne)
        val bundled = readBundledFallback(legacyTextDevoirs)
        return@withContext bundled.copy(errorMessage = remoteError)
    }

    /** Lit uniquement le cache local ou les assets unifiés (sans appel réseau). */
    fun loadLocalCachedOrBundled(): CatalogueFetchResult {
        val legacyTextDevoirs = runCatching { legacyTextLoader() }.getOrDefault(emptyList())
        return readCachedCatalogue(legacyTextDevoirs) ?: readBundledFallback(legacyTextDevoirs)
    }

    private fun readCachedCatalogue(legacyTextDevoirs: List<DevoirItem>): CatalogueFetchResult? {
        val file = cacheFile
        if (!file.exists() || file.length() <= 0L) return null
        return try {
            val raw = file.readText(Charsets.UTF_8)
            val report = parser.parseCatalogueJson(file.name, raw)
            if (report.devoirs.isEmpty()) return null
            CatalogueFetchResult(
                catalogueVersion = report.catalogueVersion,
                devoirs = DevoirCatalogueUnifier.unify(report.devoirs, legacyTextDevoirs),
                origin = CatalogueOrigin.LOCAL_CACHE,
                syncedAtMillis = file.lastModified()
            )
        } catch (e: Exception) {
            Log.w(TAG, "Cache local de catalogue illisible", e)
            null
        }
    }

    private fun readBundledFallback(legacyTextDevoirs: List<DevoirItem>): CatalogueFetchResult {
        val raw = try {
            context.assets.open(DevoirAssetLoader.CATALOGUE_FILE_NAME)
                .bufferedReader(Charsets.UTF_8)
                .use { it.readText() }
        } catch (e: Exception) {
            Log.e(TAG, "Impossible de lire le catalogue de secours dans assets/", e)
            return CatalogueFetchResult(
                catalogueVersion = if (legacyTextDevoirs.isNotEmpty()) 1 else 0,
                devoirs = DevoirCatalogueUnifier.unify(emptyList(), legacyTextDevoirs),
                origin = CatalogueOrigin.BUNDLED_FALLBACK,
                syncedAtMillis = 0L,
                errorMessage = e.message
            )
        }
        val report = parser.parseCatalogueJson(DevoirAssetLoader.CATALOGUE_FILE_NAME, raw)
        return CatalogueFetchResult(
            catalogueVersion = report.catalogueVersion,
            devoirs = DevoirCatalogueUnifier.unify(report.devoirs, legacyTextDevoirs),
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

    companion object {
        private const val TAG = "RemoteCatalogueService"
        private const val CACHE_DIR_NAME = "catalogue"
        private const val CACHE_FILE_NAME = "catalogue_cache.json"
        private const val CONNECT_TIMEOUT_MS = 8_000
        private const val READ_TIMEOUT_MS = 12_000

        /** URL publique du catalogue distant lue depuis BuildConfig (configurable dans `.env`). */
        val DEFAULT_CATALOGUE_URL: String
            get() = BuildConfig.CATALOGUE_URL

        internal fun isValidCatalogueUrl(url: String): Boolean {
            if (url.isBlank()) return false
            val lower = url.lowercase()
            if (!lower.startsWith("https://") && !lower.startsWith("http://")) return false
            return runCatching {
                val parsed = URL(url)
                parsed.host.isNotBlank()
            }.getOrDefault(false)
        }

        internal fun formatRemoteError(url: String, error: Throwable): String {
            val reason = when (error) {
                is MalformedURLException -> "adresse URL mal formée."
                is UnknownHostException -> "serveur introuvable ou appareil hors ligne."
                is SocketTimeoutException -> "délai de connexion dépassé."
                is ConnectException -> "connexion refusée par le serveur."
                is IOException -> error.message?.takeIf { it.isNotBlank() } ?: "erreur réseau."
                else -> error.message?.takeIf { it.isNotBlank() } ?: "serveur temporairement indisponible."
            }
            return buildUnreachableUrlMessage(url, reason)
        }

        private fun buildUnreachableUrlMessage(url: String, reason: String): String {
            val hostLabel = runCatching { URL(url).host.takeIf { it.isNotBlank() } }.getOrNull() ?: url.ifBlank { "non configurée" }
            return "Catalogue distant injoignable ($hostLabel) : $reason Mode hors ligne activé."
        }

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
                    throw IOException("réponse HTTP $code")
                }
                connection.inputStream.bufferedReader(Charsets.UTF_8).use { it.readText() }
            } finally {
                connection.disconnect()
            }
        }
    }
}
