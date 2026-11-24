package com.example

import android.content.Context
import android.content.res.Configuration
import android.view.View
import androidx.test.core.app.ApplicationProvider
import androidx.work.Configuration as WorkConfiguration
import androidx.work.WorkManager
import com.example.data.download.NetworkConnectionKind
import com.example.data.download.PdfDownloadCoordinator
import com.example.data.download.PdfIntegrityResult
import com.example.data.download.PdfIntegrityVerifier
import com.example.data.download.PdfStorageManager
import com.example.data.local.AppLanguage
import com.example.data.local.AppPreferencesRepository
import com.example.data.local.CatalogueOrigin
import com.example.data.local.DevoirDao
import com.example.data.local.DevoirEntity
import com.example.data.local.DevoirVersionTuple
import com.example.data.local.RemoteCatalogueService
import com.example.data.local.ThemeMode
import com.example.data.model.CycleLevel
import com.example.data.model.DevoirItem
import com.example.data.model.DevoirType
import com.example.data.model.Trimester
import com.example.data.model.TunisianSection
import com.example.ui.pdf.PdfDocumentController
import com.example.ui.pdf.PdfLoadError
import com.example.ui.viewmodel.PendingDownloadConfirmation
import com.example.ui.viewmodel.StorageViewModel
import java.io.File
import java.io.IOException
import java.util.Locale
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/**
 * Tests Robolectric (JVM, sans émulateur) vérifiant :
 * - les ressources multilingues FR / AR / EN et la direction RTL en arabe,
 * - la persistance des préférences (langue, thème, données mobiles = false par défaut),
 * - la récupération du catalogue en ligne avec cache disque et bascule hors ligne,
 * - la vérification d'intégrité PDF (%PDF-, taille exacte, SHA-256),
 * - le gestionnaire de stockage privé (espace utilisé, reprise .part, suppression),
 * - les règles Wi-Fi par défaut et avertissement > 20 Mo en données mobiles dans StorageViewModel,
 * - la gestion propre d'un fichier PDF manquant sans crash.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class DevoirsAppRobolectricTest {

    @Before
    fun initWorkManagerForTest() {
        val ctx = ApplicationProvider.getApplicationContext<Context>()
        if (!WorkManager.isInitialized()) {
            WorkManager.initialize(ctx, WorkConfiguration.Builder().build())
        }
    }

    private fun localizedContext(languageTag: String): Context {
        val base = ApplicationProvider.getApplicationContext<Context>()
        val locale = Locale.forLanguageTag(languageTag)
        val config = Configuration(base.resources.configuration).apply {
            setLocale(locale)
            setLayoutDirection(locale)
        }
        return base.createConfigurationContext(config)
    }

    @Test
    fun `les ressources francaises chargent les chaines attendues`() {
        val ctx = localizedContext("fr")
        assertEquals("Devoirs TN", ctx.getString(R.string.app_name))
        assertEquals("Devoirs", ctx.getString(R.string.nav_devoirs))
        assertEquals("Disponible hors ligne", ctx.getString(R.string.status_available_offline))
        assertEquals("À télécharger", ctx.getString(R.string.status_to_download))
        assertEquals("Stockage", ctx.getString(R.string.space_tab_storage))
        assertEquals("Télécharger tout le trimestre 1 — 9ème année", ctx.getString(R.string.download_pack_9eme_t1_cta))
        assertTrue(ctx.getString(R.string.privacy_dialog_body).contains("Firebase Firestore"))
    }

    @Test
    fun `la locale arabe active la mise en page RTL et traduit l interface`() {
        val ctx = localizedContext("ar")
        assertEquals(View.LAYOUT_DIRECTION_RTL, ctx.resources.configuration.layoutDirection)
        assertEquals("الفروض", ctx.getString(R.string.nav_devoirs))
        assertNotEquals("Stockage", ctx.getString(R.string.space_tab_storage))
        assertTrue(ctx.getString(R.string.status_available_offline).isNotBlank())
    }

    @Test
    fun `la locale anglaise traduit l interface et reste en LTR`() {
        val ctx = localizedContext("en")
        assertEquals(View.LAYOUT_DIRECTION_LTR, ctx.resources.configuration.layoutDirection)
        assertEquals("Exams", ctx.getString(R.string.nav_devoirs))
        assertEquals("System", ctx.getString(R.string.theme_mode_system))
        assertEquals("View exam paper", ctx.getString(R.string.pdf_open_subject_btn))
        assertEquals("Storage", ctx.getString(R.string.space_tab_storage))
    }

    @Test
    fun `AppPreferencesRepository persiste langue theme et Wi-Fi uniquement par defaut`() {
        val ctx = ApplicationProvider.getApplicationContext<Context>()
        val repo1 = AppPreferencesRepository(ctx)
        assertFalse("Les données mobiles doivent être désactivées par défaut (Wi-Fi seul)", repo1.state.value.allowMobileDataDownloads)

        repo1.setLanguage(AppLanguage.AR)
        repo1.setThemeMode(ThemeMode.DARK)
        repo1.setAllowMobileDataDownloads(true)

        val repo2 = AppPreferencesRepository(ctx)
        assertEquals(AppLanguage.AR, repo2.state.value.language)
        assertEquals(ThemeMode.DARK, repo2.state.value.themeMode)
        assertTrue(repo2.state.value.allowMobileDataDownloads)

        repo2.setAllowMobileDataDownloads(false)
        repo2.setLanguage(AppLanguage.EN)
        repo2.setThemeMode(ThemeMode.SYSTEM)
        val repo3 = AppPreferencesRepository(ctx)
        assertFalse(repo3.state.value.allowMobileDataDownloads)
        assertEquals(AppLanguage.EN, repo3.state.value.language)
        assertEquals(ThemeMode.SYSTEM, repo3.state.value.themeMode)
    }

    @Test
    fun `RemoteCatalogueService met en cache le catalogue distant et reste utilisable hors ligne`() = runTest {
        val ctx = ApplicationProvider.getApplicationContext<Context>()
        val remoteJson = """
            {
              "schemaVersion": 3,
              "catalogueVersion": 9,
              "devoirs": [
                {
                  "id": "remote_cached_1",
                  "titre": "Devoir distant mis en cache",
                  "matiere": "Mathématiques",
                  "niveau": "COLLEGE_9",
                  "section": "TRONC_COMMUN",
                  "type": "SYNTHESE",
                  "trimestre": "T1",
                  "duree": 120,
                  "difficulte": "Standard",
                  "chapitres": ["Thalès"],
                  "source": "Sujet d'entraînement",
                  "version": 9,
                  "tailleMo": 4.5,
                  "sujetUrl": "https://cdn.devoirstn.example/pdfs/9eme_t1_cached_sujet.pdf",
                  "corrigeUrl": "https://cdn.devoirstn.example/pdfs/9eme_t1_cached_corrige.pdf"
                }
              ]
            }
        """.trimIndent()

        // 1er démarrage : réseau disponible -> charge le catalogue distant v9 et l'écrit en cache disque
        val onlineService = RemoteCatalogueService(ctx, catalogueUrl = "https://cdn.devoirstn.com/catalogue.json", remoteFetcher = { remoteJson }, legacyTextLoader = { emptyList() })
        val onlineResult = onlineService.fetchCatalogueWithFallback()
        assertEquals(CatalogueOrigin.REMOTE, onlineResult.origin)
        assertEquals(9, onlineResult.catalogueVersion)
        assertEquals(1, onlineResult.devoirs.size)
        assertEquals("remote_cached_1", onlineResult.devoirs.first().id)

        // 2e démarrage : réseau absent -> bascule transparente sur le cache local v9
        val offlineService = RemoteCatalogueService(ctx, catalogueUrl = "https://cdn.devoirstn.com/catalogue.json", remoteFetcher = { throw IOException("Pas de réseau") }, legacyTextLoader = { emptyList() })
        val offlineResult = offlineService.fetchCatalogueWithFallback()
        assertEquals(CatalogueOrigin.LOCAL_CACHE, offlineResult.origin)
        assertEquals(9, offlineResult.catalogueVersion)
        assertEquals(1, offlineResult.devoirs.size)
        assertEquals("remote_cached_1", offlineResult.devoirs.first().id)
    }

    @Test
    fun `PdfIntegrityVerifier valide en-tete PDF taille et SHA-256 et rejette fichiers corrompus`() {
        val ctx = ApplicationProvider.getApplicationContext<Context>()
        val tmpDir = File(ctx.cacheDir, "integrity_test").apply { mkdirs() }
        val validPdf = File(tmpDir, "valid.pdf")
        val content = "%PDF-1.4\n% Devoirs TN test payload\n%%EOF\n".toByteArray(Charsets.UTF_8)
        validPdf.writeBytes(content)
        val expectedSha = PdfIntegrityVerifier.computeSha256(validPdf)

        val ok = PdfIntegrityVerifier.verify(
            file = validPdf,
            expectedSizeBytes = content.size.toLong(),
            expectedSha256 = expectedSha
        )
        assertTrue("Un PDF valide doit passer l'intégrité", ok is PdfIntegrityResult.Valid)

        val wrongSize = PdfIntegrityVerifier.verify(
            file = validPdf,
            expectedSizeBytes = content.size.toLong() + 10L,
            expectedSha256 = expectedSha
        )
        assertTrue("Une taille erronée doit être rejetée", wrongSize is PdfIntegrityResult.Invalid)

        val wrongSha = PdfIntegrityVerifier.verify(
            file = validPdf,
            expectedSizeBytes = content.size.toLong(),
            expectedSha256 = "0".repeat(64)
        )
        assertTrue("Un SHA-256 différent doit être rejeté", wrongSha is PdfIntegrityResult.Invalid)

        val notPdf = File(tmpDir, "html_error.pdf").apply { writeText("<html>404 Not Found</html>") }
        val badMagic = PdfIntegrityVerifier.verify(file = notPdf)
        assertTrue("Un fichier sans en-tête %PDF- doit être rejeté", badMagic is PdfIntegrityResult.Invalid)
    }

    @Test
    fun `PdfStorageManager calcule l espace utilise gere les fichiers partiels et supprime proprement`() {
        val ctx = ApplicationProvider.getApplicationContext<Context>()
        val storage = PdfStorageManager(ctx)
        storage.deleteAllDownloadedPdfs()
        assertEquals(0L, storage.getTotalUsedBytes())

        val bytesA = "%PDF-1.4\n".toByteArray() + ByteArray(2048) { 1 }
        val bytesB = "%PDF-1.4\n".toByteArray() + ByteArray(1024) { 2 }
        storage.sujetFile("dev_a").writeBytes(bytesA)
        storage.sujetPartFile("dev_b").writeBytes(bytesB)

        assertTrue(storage.hasPartialDownload("dev_b"))
        assertFalse(storage.hasPartialDownload("dev_a"))
        assertEquals((bytesA.size + bytesB.size).toLong(), storage.getTotalUsedBytes())

        storage.deleteDevoirFiles("dev_a")
        assertEquals(bytesB.size.toLong(), storage.getTotalUsedBytes())

        storage.deleteAllDownloadedPdfs()
        assertEquals(0L, storage.getTotalUsedBytes())
    }

    @Test
    fun `StorageViewModel applique Wi-Fi par defaut et avertit au dela de 20 Mo en donnees mobiles`() {
        val ctx = ApplicationProvider.getApplicationContext<Context>()
        val coordinator = PdfDownloadCoordinator(ctx, NoOpDevoirDao())
        val viewModel = StorageViewModel(
            coordinator = coordinator,
            networkKindProvider = { NetworkConnectionKind.MOBILE_METERED }
        )

        val heavyDevoir = DevoirItem(
            id = "bac_svt_heavy",
            title = "Pack Bac SVT complet",
            subject = "SVT",
            level = CycleLevel.BAC,
            section = TunisianSection.SCIENCES_EXP,
            type = DevoirType.SYNTHESE,
            trimester = Trimester.T1,
            durationMinutes = 180,
            difficulty = "Bac",
            chapterTags = listOf("Génétique"),
            source = "Sujet d'entraînement",
            version = 3,
            sizeMb = 24.5,
            sujetUrl = "https://cdn.devoirstn.example/pdfs/bac_svt_heavy_sujet.pdf"
        )

        // 1. Données mobiles non autorisées -> bloque et propose Wi-Fi ou activation
        viewModel.requestDownloadDevoir(heavyDevoir, allowMobileData = false)
        assertTrue(
            "Doit proposer le dialogue Wi-Fi uniquement par défaut",
            viewModel.pendingConfirmation.value is PendingDownloadConfirmation.WifiOnlyBlocked
        )

        // 2. Données mobiles autorisées mais fichier > 20 Mo -> avertissement obligatoire > 20 Mo
        viewModel.dismissPendingConfirmation()
        viewModel.requestDownloadDevoir(heavyDevoir, allowMobileData = true)
        val warning = viewModel.pendingConfirmation.value
        assertTrue(
            "Doit afficher LargeMobileWarning pour un fichier > 20 Mo en données mobiles",
            warning is PendingDownloadConfirmation.LargeMobileWarning
        )
        assertEquals(24.5, (warning as PendingDownloadConfirmation.LargeMobileWarning).sizeMb, 0.001)

        // Nettoyage WorkManager
        WorkManager.getInstance(ctx).cancelAllWork()
    }

    @Test
    fun `PdfDocumentController renvoie FileNotFound proprement si le fichier PDF est manquant`() = runTest {
        val ctx = ApplicationProvider.getApplicationContext<Context>()
        val result = PdfDocumentController.openFromAsset(ctx, "/chemin/local/inexistant_test.pdf")

        assertTrue("Un fichier manquant doit retourner un échec maîtrisé", result.isFailure)
        val error = result.exceptionOrNull()
        assertTrue(
            "L'erreur attendue est PdfLoadError.FileNotFound, obtenu : $error",
            error is PdfLoadError.FileNotFound
        )
    }

    private class NoOpDevoirDao : DevoirDao {
        override fun observeAllDevoirs(): Flow<List<DevoirEntity>> = MutableStateFlow(emptyList())
        override suspend fun getById(id: String): DevoirEntity? = null
        override suspend fun getDevoirCount(): Int = 0
        override suspend fun getExistingVersions(): List<DevoirVersionTuple> = emptyList()
        override suspend fun insertAll(devoirs: List<DevoirEntity>) = Unit
        override suspend fun insertIgnore(devoir: DevoirEntity) = Unit
        override suspend fun updateContentPreservingProgress(
            id: String, title: String, subject: String, levelId: String, sectionId: String,
            typeId: String, trimesterId: String, durationMinutes: Int, difficulty: String,
            chapterTagsSerialized: String, source: String, version: Int, sizeMb: Double,
            sujetUrl: String, corrigeUrl: String?, sujetSizeBytes: Long, corrigeSizeBytes: Long,
            sujetSha256: String?, corrigeSha256: String?, exercisesSerialized: String
        ) = Unit
        override suspend fun markDownloaded(
            devoirId: String, localSujetPath: String?, localCorrigePath: String?,
            downloadedBytes: Long, downloadedVersion: Int
        ) = Unit
        override suspend fun clearDownload(devoirId: String) = Unit
        override suspend fun clearDownloads(devoirIds: List<String>) = Unit
        override suspend fun clearAllDownloads() = Unit
        override suspend fun toggleFavorite(devoirId: String) = Unit
        override suspend fun toggleCompleted(devoirId: String) = Unit
    }
}
