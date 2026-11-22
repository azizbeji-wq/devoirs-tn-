package com.example

import com.example.data.download.PdfDownloadCoordinator
import com.example.data.local.DevoirAssetLoader
import com.example.data.local.DevoirDao
import com.example.data.local.DevoirEntity
import com.example.data.local.DevoirJsonParser
import com.example.data.local.DevoirVersionTuple
import com.example.data.local.StarredIdeaDao
import com.example.data.local.StarredIdeaEntity
import com.example.data.model.CycleLevel
import com.example.data.model.DevoirItem
import com.example.data.model.DevoirType
import com.example.data.model.Trimester
import com.example.data.model.TunisianSection
import com.example.data.repository.DevoirRepository
import java.io.File
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Tests unitaires du catalogue JSON distant (`assets/catalogue.json` v3),
 * de l'absence de PDF embarqués dans l'APK, de la résilience du parseur Moshi
 * et de la synchronisation incrémentale par `version` préservant favoris, progression et téléchargements locaux.
 */
class DevoirJsonParserTest {

    private fun resolveAssetsDir(): File {
        val candidates = listOf(
            File("src/main/assets"),
            File("app/src/main/assets")
        )
        return candidates.firstOrNull { it.exists() && it.isDirectory }
            ?: error("Dossier assets introuvable depuis ${File(".").absolutePath}")
    }

    @Test
    fun catalogueJson_usesRemoteHttpsUrlsAndNoPdfFilesBundledInApk() {
        val loggedErrors = mutableListOf<String>()
        val parser = DevoirJsonParser { msg, _ -> loggedErrors.add(msg) }
        val assetsDir = resolveAssetsDir()
        val catalogueFile = File(assetsDir, DevoirAssetLoader.CATALOGUE_FILE_NAME)
        assertTrue("assets/catalogue.json doit exister", catalogueFile.exists())

        // Aucun PDF embarqué dans l'APK
        val bundledPdfDir = File(assetsDir, "pdf")
        assertFalse(
            "Les PDF ne doivent plus être embarqués dans assets/pdf/",
            bundledPdfDir.exists() && (bundledPdfDir.listFiles()?.isNotEmpty() == true)
        )

        val report = parser.parseCatalogueJson(
            sourceName = catalogueFile.name,
            jsonContent = catalogueFile.readText(Charsets.UTF_8)
        )

        assertEquals("Erreurs dans catalogue.json : ${report.errors}", 0, report.skippedCount)
        assertTrue("Aucune erreur attendue : $loggedErrors", loggedErrors.isEmpty())
        assertTrue("catalogueVersion >= 3 attendu", report.catalogueVersion >= 3)
        assertTrue("Au moins 10 devoirs attendus dans catalogue.json", report.devoirs.size >= 10)

        // Unicité des identifiants
        val uniqueIds = report.devoirs.map { it.id }.toSet()
        assertEquals("Tous les identifiants doivent être uniques", report.devoirs.size, uniqueIds.size)

        // Toutes les URL de sujets doivent être HTTPS valides
        assertTrue(
            "Toutes les sujetUrl doivent commencer par https://",
            report.devoirs.all { it.sujetUrl.startsWith("https://") }
        )
        assertTrue(
            "Toutes les corrigeUrl non nulles doivent commencer par https://",
            report.devoirs.mapNotNull { it.corrigeUrl }.all { it.startsWith("https://") }
        )
        assertTrue(
            "Au moins un devoir sans corrigé (corrigeUrl = null) doit être présent",
            report.devoirs.any { it.corrigeUrl == null && !it.hasCorrigePdf }
        )

        // Vérifie la présence du pack phare « Trimestre 1 — 9ème année » en tête de liste
        val packs = PdfDownloadCoordinator.buildPacks(report.devoirs)
        assertTrue(packs.isNotEmpty())
        val firstPack = packs.first()
        assertEquals(CycleLevel.COLLEGE_9, firstPack.level)
        assertEquals(Trimester.T1, firstPack.trimester)
        assertTrue("Le pack 9ème T1 doit contenir plusieurs devoirs", firstPack.totalCount >= 4)
        assertTrue("Le pack 9ème T1 doit dépasser 20 Mo pour tester l'alerte cellulaire", firstPack.totalSizeMb > 20.0)
    }

    @Test
    fun malformedJsonFile_isIgnoredAndLoggedWithoutCrash() {
        val loggedErrors = mutableListOf<String>()
        val parser = DevoirJsonParser { msg, _ -> loggedErrors.add(msg) }

        val report = parser.parseCatalogueJson("corrupted.json", "{ \"devoirs\": [ { invalide ")

        assertTrue(report.devoirs.isEmpty())
        assertEquals(1, report.skippedCount)
        assertEquals(1, loggedErrors.size)
    }

    @Test
    fun invalidRemoteUrlEntry_isSkippedWhileValidSiblingIsLoaded() {
        val loggedErrors = mutableListOf<String>()
        val parser = DevoirJsonParser { msg, _ -> loggedErrors.add(msg) }

        val json = """
            {
              "schemaVersion": 3,
              "catalogueVersion": 5,
              "devoirs": [
                {
                  "id": "bad_entry",
                  "titre": "Devoir avec URL locale invalide",
                  "matiere": "Mathématiques",
                  "niveau": "COLLEGE_9",
                  "section": "TRONC_COMMUN",
                  "type": "CONTROLE",
                  "trimestre": "T1",
                  "duree": 60,
                  "difficulte": "Standard",
                  "chapitres": ["Fractions"],
                  "source": "Sujet d'entraînement",
                  "version": 1,
                  "tailleMo": 1.2,
                  "sujetUrl": "file:///etc/passwd"
                },
                {
                  "id": "valid_entry",
                  "titre": "Devoir distant valide",
                  "matiere": "Mathématiques",
                  "niveau": "COLLEGE_9",
                  "section": "TRONC_COMMUN",
                  "type": "CONTROLE",
                  "trimestre": "T1",
                  "duree": 60,
                  "difficulte": "Fondamental",
                  "chapitres": ["Fractions"],
                  "source": "Sujet d'entraînement",
                  "version": 4,
                  "tailleMo": 4.2,
                  "sujetUrl": "https://cdn.devoirstn.example/pdfs/9eme_t1_maths_sujet.pdf",
                  "sujetSizeBytes": 4404019,
                  "sujetSha256": "aaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaaa",
                  "corrigeUrl": null
                }
              ]
            }
        """.trimIndent()

        val report = parser.parseCatalogueJson("mixed.json", json)

        assertEquals(5, report.catalogueVersion)
        assertEquals(1, report.devoirs.size)
        val loaded = report.devoirs.first()
        assertEquals("valid_entry", loaded.id)
        assertEquals(4, loaded.version)
        assertEquals(4.2, loaded.sizeMb, 0.0001)
        assertEquals("https://cdn.devoirstn.example/pdfs/9eme_t1_maths_sujet.pdf", loaded.sujetUrl)
        assertEquals(4404019L, loaded.sujetSizeBytes)
        assertNotNull(loaded.sujetSha256)
        assertNull(loaded.corrigeUrl)
        assertFalse(loaded.hasCorrigePdf)
        assertEquals(1, report.skippedCount)
        assertEquals(1, loggedErrors.size)
    }

    @Test
    fun versionSync_updatesNewerContentWithoutErasingFavoritesCompletionOrLocalDownload() = runTest {
        val fakeDevoirDao = InMemoryDevoirDao()
        val fakeStarredDao = InMemoryStarredIdeaDao()

        val v1Item = sampleDevoir(
            id = "devoir_pdf_1",
            title = "Sujet v1",
            version = 1,
            corrigeUrl = null
        )
        val repoV1 = DevoirRepository(
            devoirDao = fakeDevoirDao,
            starredIdeaDao = fakeStarredDao,
            loadAssetDevoirs = { listOf(v1Item) }
        )
        repoV1.ensureSeedDataLoaded()

        // L'élève met en favori, marque terminé et télécharge le PDF en local
        repoV1.toggleFavorite("devoir_pdf_1")
        repoV1.toggleCompleted("devoir_pdf_1")
        fakeDevoirDao.markDownloaded(
            devoirId = "devoir_pdf_1",
            localSujetPath = "/data/user/0/com.example/files/pdfs/devoir_pdf_1/sujet.pdf",
            localCorrigePath = null,
            downloadedBytes = 1_500_000L,
            downloadedVersion = 1
        )

        // Nouvelle version distante du catalogue (v2 ajoute le corrigé URL + nouveau devoir)
        val v2Item = sampleDevoir(
            id = "devoir_pdf_1",
            title = "Sujet v2 corrigé",
            version = 2,
            corrigeUrl = "https://cdn.devoirstn.example/pdfs/devoir_pdf_1_corrige.pdf"
        )
        val newItem = sampleDevoir(
            id = "devoir_pdf_2",
            title = "Nouveau devoir PDF",
            version = 1,
            corrigeUrl = null
        )
        val repoV2 = DevoirRepository(
            devoirDao = fakeDevoirDao,
            starredIdeaDao = fakeStarredDao,
            loadAssetDevoirs = { listOf(v2Item, newItem) }
        )
        repoV2.ensureSeedDataLoaded()

        val updated = fakeDevoirDao.getById("devoir_pdf_1")!!
        assertEquals("Sujet v2 corrigé", updated.title)
        assertEquals(2, updated.version)
        assertEquals("https://cdn.devoirstn.example/pdfs/devoir_pdf_1_corrige.pdf", updated.corrigeUrl)
        assertTrue("isFavorite doit être préservé après mise à jour", updated.isFavorite)
        assertTrue("isCompleted doit être préservé après mise à jour", updated.isCompleted)
        assertEquals(
            "Le chemin local téléchargé doit être préservé après synchro catalogue",
            "/data/user/0/com.example/files/pdfs/devoir_pdf_1/sujet.pdf",
            updated.localSujetPath
        )
        assertEquals(1, updated.downloadedVersion)

        val inserted = fakeDevoirDao.getById("devoir_pdf_2")!!
        assertEquals("Nouveau devoir PDF", inserted.title)
        assertFalse(inserted.isFavorite)
        assertFalse(inserted.isCompleted)
    }

    private fun sampleDevoir(
        id: String,
        title: String,
        version: Int,
        corrigeUrl: String?
    ) = DevoirItem(
        id = id,
        title = title,
        subject = "Mathématiques",
        level = CycleLevel.COLLEGE_9,
        section = TunisianSection.TRONC_COMMUN,
        type = DevoirType.CONTROLE,
        trimester = Trimester.T1,
        durationMinutes = 60,
        difficulty = "Standard",
        chapterTags = listOf("Algèbre"),
        source = "Sujet d'entraînement",
        version = version,
        sizeMb = 2.5,
        sujetUrl = "https://cdn.devoirstn.example/pdfs/${id}_sujet.pdf",
        corrigeUrl = corrigeUrl,
        sujetSizeBytes = 1_500_000L,
        corrigeSizeBytes = if (corrigeUrl != null) 1_100_000L else 0L
    )

    private class InMemoryDevoirDao : DevoirDao {
        private val store = LinkedHashMap<String, DevoirEntity>()
        private val flow = MutableStateFlow<List<DevoirEntity>>(emptyList())

        override fun observeAllDevoirs(): Flow<List<DevoirEntity>> = flow
        override suspend fun getById(id: String): DevoirEntity? = store[id]
        override suspend fun getDevoirCount(): Int = store.size
        override suspend fun getExistingVersions(): List<DevoirVersionTuple> =
            store.values.map { DevoirVersionTuple(it.id, it.version, it.sujetUrl) }

        override suspend fun insertAll(devoirs: List<DevoirEntity>) {
            devoirs.forEach { store.putIfAbsent(it.id, it) }
            flow.value = store.values.toList()
        }

        override suspend fun insertIgnore(devoir: DevoirEntity) {
            store.putIfAbsent(devoir.id, devoir)
            flow.value = store.values.toList()
        }

        override suspend fun updateContentPreservingProgress(
            id: String,
            title: String,
            subject: String,
            levelId: String,
            sectionId: String,
            typeId: String,
            trimesterId: String,
            durationMinutes: Int,
            difficulty: String,
            chapterTagsSerialized: String,
            source: String,
            version: Int,
            sizeMb: Double,
            sujetUrl: String,
            corrigeUrl: String?,
            sujetSizeBytes: Long,
            corrigeSizeBytes: Long,
            sujetSha256: String?,
            corrigeSha256: String?,
            exercisesSerialized: String
        ) {
            val existing = store[id] ?: return
            store[id] = existing.copy(
                title = title,
                subject = subject,
                levelId = levelId,
                sectionId = sectionId,
                typeId = typeId,
                trimesterId = trimesterId,
                durationMinutes = durationMinutes,
                difficulty = difficulty,
                chapterTagsSerialized = chapterTagsSerialized,
                source = source,
                version = version,
                sizeMb = sizeMb,
                sujetUrl = sujetUrl,
                corrigeUrl = corrigeUrl,
                sujetSizeBytes = sujetSizeBytes,
                corrigeSizeBytes = corrigeSizeBytes,
                sujetSha256 = sujetSha256,
                corrigeSha256 = corrigeSha256,
                exercisesSerialized = exercisesSerialized
            )
            flow.value = store.values.toList()
        }

        override suspend fun markDownloaded(
            devoirId: String,
            localSujetPath: String?,
            localCorrigePath: String?,
            downloadedBytes: Long,
            downloadedVersion: Int
        ) {
            val existing = store[devoirId] ?: return
            store[devoirId] = existing.copy(
                localSujetPath = localSujetPath,
                localCorrigePath = localCorrigePath,
                downloadedBytes = downloadedBytes,
                downloadedVersion = downloadedVersion
            )
            flow.value = store.values.toList()
        }

        override suspend fun clearDownload(devoirId: String) {
            val existing = store[devoirId] ?: return
            store[devoirId] = existing.copy(
                localSujetPath = null,
                localCorrigePath = null,
                downloadedBytes = 0L,
                downloadedVersion = 0
            )
            flow.value = store.values.toList()
        }

        override suspend fun clearDownloads(devoirIds: List<String>) {
            devoirIds.forEach { clearDownload(it) }
        }

        override suspend fun clearAllDownloads() {
            store.keys.toList().forEach { clearDownload(it) }
        }

        override suspend fun toggleFavorite(devoirId: String) {
            store[devoirId]?.let { store[devoirId] = it.copy(isFavorite = !it.isFavorite) }
            flow.value = store.values.toList()
        }

        override suspend fun toggleCompleted(devoirId: String) {
            store[devoirId]?.let { store[devoirId] = it.copy(isCompleted = !it.isCompleted) }
            flow.value = store.values.toList()
        }
    }

    private class InMemoryStarredIdeaDao : StarredIdeaDao {
        private val ids = linkedSetOf<String>()
        private val flow = MutableStateFlow<List<String>>(emptyList())
        override fun observeStarredIdeaIds(): Flow<List<String>> = flow
        override suspend fun starIdea(entity: StarredIdeaEntity) {
            ids.add(entity.ideaId)
            flow.value = ids.toList()
        }
        override suspend fun unstarIdea(ideaId: String) {
            ids.remove(ideaId)
            flow.value = ids.toList()
        }
        override suspend fun isIdeaStarred(ideaId: String): Boolean = ideaId in ids
    }
}
