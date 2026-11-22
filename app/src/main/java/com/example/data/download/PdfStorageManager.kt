package com.example.data.download

import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import java.io.File
import java.io.FileInputStream
import java.security.MessageDigest

/** Résultat d'une vérification d'intégrité sur un fichier PDF téléchargé. */
sealed interface PdfIntegrityResult {
    data class Valid(val sizeBytes: Long, val sha256: String) : PdfIntegrityResult
    data class Invalid(
        val reason: String,
        val isSha256Mismatch: Boolean = false
    ) : PdfIntegrityResult
}

/**
 * Vérificateur pur JVM/Android d'intégrité des PDF téléchargés :
 * 1. Fichier existant et non vide,
 * 2. En-tête magique `%PDF-`,
 * 3. Taille exacte en octets (si renseignée dans le catalogue ou par l'en-tête HTTP Content-Length),
 * 4. Empreinte SHA-256 (champs optionnels et rétrocompatibles `sujetSha256` / `corrigeSha256`).
 */
object PdfIntegrityVerifier {

    private val PDF_MAGIC = byteArrayOf('%'.code.toByte(), 'P'.code.toByte(), 'D'.code.toByte(), 'F'.code.toByte(), '-'.code.toByte())

    fun verify(
        file: File,
        expectedSizeBytes: Long = 0L,
        expectedSha256: String? = null
    ): PdfIntegrityResult {
        if (!file.exists() || !file.isFile) {
            return PdfIntegrityResult.Invalid("Fichier introuvable sur le stockage local.")
        }
        val actualSize = file.length()
        if (actualSize < PDF_MAGIC.size) {
            return PdfIntegrityResult.Invalid("Fichier vide ou incomplet ($actualSize octets).")
        }

        // Vérification de la signature %PDF-
        val header = ByteArray(PDF_MAGIC.size)
        FileInputStream(file).use { input ->
            val read = input.read(header)
            if (read < PDF_MAGIC.size || !header.contentEquals(PDF_MAGIC)) {
                return PdfIntegrityResult.Invalid("Le fichier téléchargé n'est pas un PDF valide (en-tête %PDF- absent).")
            }
        }

        // Vérification de la taille exacte si attendue
        if (expectedSizeBytes > 0L && actualSize != expectedSizeBytes) {
            return PdfIntegrityResult.Invalid(
                "Taille inattendue : $actualSize octets reçus au lieu de $expectedSizeBytes octets."
            )
        }

        // Calcul et vérification SHA-256 (optionnelle et rétrocompatible si non renseignée)
        val actualSha = computeSha256(file)
        val normalizedExpectedSha = expectedSha256?.trim()?.lowercase()?.takeIf { it.isNotEmpty() }
        if (normalizedExpectedSha != null && actualSha != normalizedExpectedSha) {
            return PdfIntegrityResult.Invalid(
                reason = "Échec de vérification SHA-256 : le fichier téléchargé a été supprimé par sécurité car son empreinte ne correspond pas au catalogue.",
                isSha256Mismatch = true
            )
        }

        return PdfIntegrityResult.Valid(sizeBytes = actualSize, sha256 = actualSha)
    }

    fun computeSha256(file: File): String {
        val digest = MessageDigest.getInstance("SHA-256")
        val buffer = ByteArray(DEFAULT_BUFFER_SIZE)
        FileInputStream(file).use { input ->
            var read: Int
            while (input.read(buffer).also { read = it } != -1) {
                digest.update(buffer, 0, read)
            }
        }
        return digest.digest().joinToString("") { "%02x".format(it) }
    }
}

/** État du réseau actif de l'appareil pour appliquer la règle Wi-Fi par défaut et l'alerte > 20 Mo. */
enum class NetworkConnectionKind {
    NONE,
    WIFI_OR_UNMETERED,
    MOBILE_METERED
}

/**
 * Gestionnaire du stockage privé des PDF téléchargés (`context.filesDir/pdfs/<devoirId>/`).
 * Aucune permission de stockage externe n'est requise.
 */
class PdfStorageManager(private val context: Context) {

    private val rootDir: File
        get() = File(context.filesDir, PDFS_ROOT_DIR).apply { mkdirs() }

    fun devoirDir(devoirId: String): File {
        val safeId = devoirId.replace(Regex("[^A-Za-z0-9_.-]"), "_")
        return File(rootDir, safeId).apply { mkdirs() }
    }

    fun sujetFile(devoirId: String): File = File(devoirDir(devoirId), SUJET_FILE_NAME)
    fun sujetPartFile(devoirId: String): File = File(devoirDir(devoirId), "$SUJET_FILE_NAME.part")

    fun corrigeFile(devoirId: String): File = File(devoirDir(devoirId), CORRIGE_FILE_NAME)
    fun corrigePartFile(devoirId: String): File = File(devoirDir(devoirId), "$CORRIGE_FILE_NAME.part")

    /** Retourne true si un téléchargement partiel (.part) existe et peut être repris. */
    fun hasPartialDownload(devoirId: String): Boolean =
        sujetPartFile(devoirId).let { it.exists() && it.length() > 0L } ||
            corrigePartFile(devoirId).let { it.exists() && it.length() > 0L }

    /** Nombre total d'octets occupés sur disque pour un devoir (PDF validés + éventuels .part). */
    fun getDevoirDiskUsageBytes(devoirId: String): Long {
        val dir = File(rootDir, devoirId.replace(Regex("[^A-Za-z0-9_.-]"), "_"))
        if (!dir.exists() || !dir.isDirectory) return 0L
        return dir.listFiles()?.sumOf { if (it.isFile) it.length() else 0L } ?: 0L
    }

    /** Espace total occupé par tous les devoirs téléchargés dans le stockage privé. */
    fun getTotalUsedBytes(): Long {
        if (!rootDir.exists()) return 0L
        return rootDir.walkTopDown().filter { it.isFile }.sumOf { it.length() }
    }

    /** Supprime tous les fichiers PDF et fichiers temporaires d'un devoir. */
    fun deleteDevoirFiles(devoirId: String): Boolean {
        val dir = File(rootDir, devoirId.replace(Regex("[^A-Za-z0-9_.-]"), "_"))
        return if (dir.exists()) dir.deleteRecursively() else true
    }

    /** Supprime les fichiers PDF de toute une liste de devoirs (suppression d'un pack). */
    fun deleteDevoirsFiles(devoirIds: List<String>) {
        devoirIds.forEach { deleteDevoirFiles(it) }
    }

    /** Supprime l'intégralité des PDF téléchargés hors ligne. */
    fun deleteAllDownloadedPdfs(): Boolean {
        if (!rootDir.exists()) return true
        val ok = rootDir.deleteRecursively()
        rootDir.mkdirs()
        return ok
    }

    /** Détecte le type de connexion réseau actif (Wi-Fi / non mesuré, données mobiles ou hors ligne). */
    fun getActiveNetworkKind(): NetworkConnectionKind {
        val cm = context.getSystemService(Context.CONNECTIVITY_SERVICE) as? ConnectivityManager
            ?: return NetworkConnectionKind.NONE
        val activeNetwork = cm.activeNetwork ?: return NetworkConnectionKind.NONE
        val capabilities = cm.getNetworkCapabilities(activeNetwork) ?: return NetworkConnectionKind.NONE

        val hasInternet = capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
        if (!hasInternet) return NetworkConnectionKind.NONE

        val isUnmetered = capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_NOT_METERED) ||
            capabilities.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) ||
            capabilities.hasTransport(NetworkCapabilities.TRANSPORT_ETHERNET)

        return if (isUnmetered && !cm.isActiveNetworkMetered) {
            NetworkConnectionKind.WIFI_OR_UNMETERED
        } else {
            NetworkConnectionKind.MOBILE_METERED
        }
    }

    companion object {
        private const val PDFS_ROOT_DIR = "pdfs"
        private const val SUJET_FILE_NAME = "sujet.pdf"
        private const val CORRIGE_FILE_NAME = "corrige.pdf"

        /** Seuil légal/UX d'avertissement en données mobiles : 20 Mo. */
        const val MOBILE_WARNING_THRESHOLD_MB = 20.0
        const val MOBILE_WARNING_THRESHOLD_BYTES = 20L * 1024L * 1024L

        fun formatBytes(bytes: Long): String {
            if (bytes <= 0L) return "0 Mo"
            val kb = bytes / 1024.0
            val mb = kb / 1024.0
            return when {
                mb >= 100.0 -> String.format("%.0f Mo", mb)
                mb >= 1.0 -> String.format("%.1f Mo", mb)
                kb >= 1.0 -> String.format("%.0f Ko", kb)
                else -> "$bytes o"
            }
        }
    }
}
