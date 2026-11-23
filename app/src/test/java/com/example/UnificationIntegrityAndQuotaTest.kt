package com.example

import android.content.SharedPreferences
import com.example.data.download.PdfIntegrityResult
import com.example.data.download.PdfIntegrityVerifier
import com.example.data.local.AppPreferencesRepository
import com.example.data.local.DevoirAssetLoader
import com.example.data.local.DevoirCatalogueUnifier
import com.example.data.local.DevoirDao
import com.example.data.local.DevoirEntity
import com.example.data.local.DevoirJsonParser
import com.example.data.local.DevoirVersionTuple
import com.example.data.local.RemoteCatalogueService
import com.example.data.local.StarredIdeaDao
import com.example.data.local.StarredIdeaEntity
import com.example.data.local.toDomain
import com.example.data.model.CycleLevel
import com.example.data.model.DevoirExercise
import com.example.data.model.DevoirItem
import com.example.data.model.DevoirType
import com.example.data.model.Trimester
import com.example.data.model.TunisianSection
import com.example.data.repository.DevoirRepository
import java.io.File
import java.net.UnknownHostException
import java.nio.file.Files
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Tests unitaires JVM purs vérifiant les 7 points de l'unification et du durcissement :
 * 1. Unification PDF (principal) + anciens JSON (consultables en texte) sans doublons (clé = id)
 *    et préservation des favoris / progression lors de la migration texte -> PDF.
 * 2. Vérification d'intégrité SHA-256 (succès, échec avec isSha256Mismatch, rétrocompatibilité si absent).
 * 3. Validation et gestion non bloquante de CATALOGUE_URL (BuildConfig) injoignable / invalide.
 * 4. Quota quotidien du Tuteur IA (20 requêtes/jour par appareil), remboursement et réinitialisation journalière.
 * 5. Persistance de l'interrupteur de contrôle parental du Tuteur IA (`aiTutorEnabled`).
 * 6. Exactitude de `DevoirItem.hasCorrige` (badge « Corrigé inclus »).
 */
class UnificationIntegrityAndQuotaTest {

    private fun resolveAssetsDir(): File {
        val candidates = listOf(
            File("src/main/assets"),
            File("app/src/main/assets")
        )
        return candidates.firstOrNull { it.exists() && it.isDirectory }
            ?: error("Dossier assets introuvable depuis ${File(".").absolutePath}")
    }

    @Test
    fun unification_pdfPrimaryWinsOnDuplicateIdAndLegacyJsonBecomesTextOnly() {
        val pdfDevoir = DevoirItem(
            id = "shared_id_1",
            title = "Version PDF officielle",
            subject = "Mathématiques",
            level = CycleLevel.BAC,
            section = TunisianSection.MATHS,
            type = DevoirType.SYNTHESE,
            trimester = Trimester.T1,
            durationMinutes = 180,
            difficulty = "Bac",
            chapterTags = listOf("Nombres complexes"),
            source = "Sujet d'entraînement",
            version = 3,
            sizeMb = 3.2,
            sujetUrl = "https://cdn.devoirstn.example/pdfs/shared_sujet.pdf",
            corrigeUrl = "https://cdn.devoirstn.example/pdfs/shared_corrige.pdf"
        )
        val duplicateLegacy = DevoirItem(
            id = "shared_id_1",
            title = "Ancienne version texte (doublon)",
            subject = "Mathématiques",
            level = CycleLevel.BAC,
            section = TunisianSection.MATHS,
            type = DevoirType.SYNTHESE,
            trimester = Trimester.T1,
            durationMinutes = 180,
            difficulty = "Bac",
            chapterTags = listOf("Nombres complexes"),
            source = "Sujet d'entraînement",
            version = 1,
            sizeMb = 0.0,
            sujetUrl = "",
            corrigeUrl = null,
            exercises = listOf(
                DevoirExercise(1, "Ex 1", "5 pts", "Énoncé", "Corrigé")
            )
        )
        val uniqueLegacy = duplicateLegacy.copy(
            id = "legacy_only_1",
            title = "Ancien devoir texte unique"
        )

        val unified = DevoirCatalogueUnifier.unify(
            pdfDevoirs = listOf(pdfDevoir),
            legacyTextDevoirs = listOf(duplicateLegacy, uniqueLegacy)
        )

        assertEquals("Aucun doublon ne doit subsister", 2, unified.size)
        assertEquals(setOf("shared_id_1", "legacy_only_1"), unified.map { it.id }.toSet())

        val first = unified.first { it.id == "shared_id_1" }
        assertTrue("La source PDF doit primer en cas de même id", first.hasPdfSource)
        assertFalse(first.isTextOnly)
        assertEquals("Version PDF officielle", first.title)

        val second = unified.first { it.id == "legacy_only_1" }
        assertTrue("Un ancien JSON sans PDF doit devenir consultable en texte", second.isTextOnly)
        assertFalse(second.hasPdfSource)
        assertTrue(second.isAvailableOffline)
        assertEquals(1, second.exercises.size)
    }

    @Test
    fun realBundledAssets_unifyPdfCatalogueAndLegacyDevoirsWithoutDuplicates() {
        val parser = DevoirJsonParser()
        val assetsDir = resolveAssetsDir()

        val catalogueFile = File(assetsDir, DevoirAssetLoader.CATALOGUE_FILE_NAME)
        val pdfReport = parser.parseCatalogueJson(
            catalogueFile.name,
            catalogueFile.readText(Charsets.UTF_8)
        )
        assertEquals(0, pdfReport.skippedCount)

        val legacyDir = File(assetsDir, DevoirAssetLoader.LEGACY_DEVOIRS_DIR)
        assertTrue("assets/devoirs doit exister", legacyDir.exists() && legacyDir.isDirectory)

        val legacyItems = mutableListOf<DevoirItem>()
        legacyDir.listFiles()
            .orEmpty()
            .filter { it.name.endsWith(".json") && it.name != "devoir_schema.json" }
            .sortedBy { it.name }
            .forEach { file ->
                val report = parser.parseLegacyTextDevoirsJson(file.name, file.readText(Charsets.UTF_8))
                assertEquals("Erreur dans ${file.name}: ${report.errors}", 0, report.skippedCount)
                legacyItems += report.devoirs
            }

        assertTrue("Des devoirs JSON texte existants doivent être migrés", legacyItems.size >= 30)

        val unified = DevoirCatalogueUnifier.unify(pdfReport.devoirs, legacyItems)
        assertEquals(
            "Tous les identifiants unifiés doivent être uniques",
            unified.size,
            unified.map { it.id }.toSet().size
        )
        assertTrue("Doit contenir les sujets PDF", unified.any { it.hasPdfSource })
        assertTrue("Doit contenir les anciens sujets texte", unified.any { it.isTextOnly })
        assertTrue(
            "Les anciens sujets migrés doivent normaliser source = Sujet d'entraînement",
            unified.filter { it.isTextOnly }.all { it.source == "Sujet d'entraînement" }
        )
    }

    @Test
    fun repositorySync_upgradingTextDevoirToPdfPreservesFavoriteAndCompleted() = runTest {
        val dao = InMemoryDevoirDao()
        val ideaDao = InMemoryStarredIdeaDao()

        val legacyText = DevoirItem(
            id = "devoir_migrated_42",
            title = "Devoir texte initial",
            subject = "Physique-Chimie",
            level = CycleLevel.LYCEE_3,
            section = TunisianSection.SCIENCES_EXP,
            type = DevoirType.CONTROLE,
            trimester = Trimester.T1,
            durationMinutes = 60,
            difficulty = "Standard",
            chapterTags = listOf("Cinématique"),
            source = "Sujet d'entraînement",
            version = 1,
            sizeMb = 0.0,
            sujetUrl = "",
            corrigeUrl = null,
            exercises = listOf(DevoirExercise(1, "Ex 1", "10 pts", "Énoncé", "Corrigé"))
        )

        val repo1 = DevoirRepository(dao, ideaDao, loadAssetDevoirs = { listOf(legacyText) })
        repo1.ensureSeedDataLoaded()
        repo1.toggleFavorite("devoir_migrated_42")
        repo1.toggleCompleted("devoir_migrated_42")

        // Même id désormais disponible en PDF dans catalogue.json (même version 1 -> doit quand même migrer)
        val upgradedPdf = legacyText.copy(
            title = "Devoir migré en PDF",
            version = 1,
            sizeMb = 2.4,
            sujetUrl = "https://cdn.devoirstn.example/pdfs/devoir_42_sujet.pdf",
            corrigeUrl = "https://cdn.devoirstn.example/pdfs/devoir_42_corrige.pdf",
            exercises = emptyList()
        )
        val repo2 = DevoirRepository(dao, ideaDao, loadAssetDevoirs = { listOf(upgradedPdf) })
        repo2.ensureSeedDataLoaded()

        val saved = dao.getById("devoir_migrated_42")!!.toDomain()
        assertTrue("Doit basculer en mode PDF", saved.hasPdfSource)
        assertFalse(saved.isTextOnly)
        assertEquals("Devoir migré en PDF", saved.title)
        assertTrue("isFavorite doit être préservé", saved.isFavorite)
        assertTrue("isCompleted doit être préservé", saved.isCompleted)
    }

    @Test
    fun sha256IntegrityVerifier_validatesMatchesRejectsMismatchAndSupportsOptionalHash() {
        val tempFile = Files.createTempFile("devoirs_tn_sha256_test", ".pdf").toFile()
        try {
            val payload = "%PDF-1.7\n1 0 obj\n<< /Type /Catalog >>\nendobj\n%%EOF\n".toByteArray()
            tempFile.writeBytes(payload)
            val realSha256 = PdfIntegrityVerifier.computeSha256(tempFile)

            // 1. SHA-256 correct (insensible à la casse)
            val ok = PdfIntegrityVerifier.verify(
                file = tempFile,
                expectedSizeBytes = payload.size.toLong(),
                expectedSha256 = realSha256.uppercase()
            )
            assertTrue("Le hash SHA-256 valide doit réussir", ok is PdfIntegrityResult.Valid)

            // 2. SHA-256 erroné -> Invalid avec isSha256Mismatch = true
            val badHash = "0".repeat(64)
            val mismatch = PdfIntegrityVerifier.verify(
                file = tempFile,
                expectedSizeBytes = payload.size.toLong(),
                expectedSha256 = badHash
            )
            assertTrue(mismatch is PdfIntegrityResult.Invalid)
            val invalid = mismatch as PdfIntegrityResult.Invalid
            assertTrue("Doit signaler un échec SHA-256", invalid.isSha256Mismatch)
            assertTrue(invalid.reason.contains("SHA-256"))

            // 3. SHA-256 absent/null -> rétrocompatible (vérifie %PDF- et taille)
            val compat = PdfIntegrityVerifier.verify(
                file = tempFile,
                expectedSizeBytes = payload.size.toLong(),
                expectedSha256 = null
            )
            assertTrue("Un hash absent doit rester rétrocompatible", compat is PdfIntegrityResult.Valid)
        } finally {
            tempFile.delete()
        }
    }

    @Test
    fun catalogueUrlValidationAndErrorFormatting_neverCrashOnInvalidOrUnreachableUrl() {
        assertFalse(RemoteCatalogueService.isValidCatalogueUrl("https://cdn.devoirstn.example/catalogue.json"))
        assertTrue(RemoteCatalogueService.isValidCatalogueUrl("http://10.0.2.2:8080/catalogue.json"))
        assertFalse(RemoteCatalogueService.isValidCatalogueUrl(""))
        assertFalse(RemoteCatalogueService.isValidCatalogueUrl("not-a-url"))
        assertFalse(RemoteCatalogueService.isValidCatalogueUrl("file:///etc/passwd"))

        val errMsg = RemoteCatalogueService.formatRemoteError(
            url = "https://cdn.inexistant.example/catalogue.json",
            error = UnknownHostException("cdn.inexistant.example")
        )
        assertTrue("Le message doit expliquer que l'URL est injoignable", errMsg.contains("injoignable", ignoreCase = true))
        assertTrue("Le message doit mentionner l'URL concernée", errMsg.contains("cdn.inexistant.example"))
    }

    @Test
    fun tutorDailyQuotaAndParentalControl_enforcesLimitResetsNextDayAndPersistsToggle() {
        val prefs = InMemorySharedPreferences()
        var fakeDay = "2026-05-10"
        val repo = AppPreferencesRepository(
            prefs = prefs,
            currentDateProvider = { fakeDay }
        )

        assertTrue("Tuteur IA activé par défaut", repo.state.value.aiTutorEnabled)
        assertEquals(20, repo.state.value.tutorDailyQuotaLimit)
        assertEquals(20, repo.state.value.tutorDailyRemaining)

        // Consomme les 20 requêtes quotidiennes
        repeat(20) { idx ->
            assertTrue("Requête #${idx + 1} doit être autorisée", repo.tryConsumeTutorQuota())
        }
        assertTrue("Le quota quotidien doit être atteint à 20", repo.state.value.isTutorDailyQuotaReached)
        assertEquals(0, repo.state.value.tutorDailyRemaining)
        assertFalse("La 21e requête doit être refusée", repo.tryConsumeTutorQuota())

        // Remboursement (ex. erreur réseau) -> 1 requête restituée
        repo.refundTutorQuota()
        assertEquals(19, repo.state.value.tutorDailyUsedCount)
        assertEquals(1, repo.state.value.tutorDailyRemaining)
        assertTrue(repo.tryConsumeTutorQuota())
        assertTrue(repo.state.value.isTutorDailyQuotaReached)

        // Jour suivant -> compteur réinitialisé automatiquement à 0
        fakeDay = "2026-05-11"
        repo.refreshDailyTutorQuota()
        assertEquals(0, repo.state.value.tutorDailyUsedCount)
        assertEquals(20, repo.state.value.tutorDailyRemaining)
        assertFalse(repo.state.value.isTutorDailyQuotaReached)

        // Contrôle parental : désactivation persistée dans SharedPreferences
        repo.setAiTutorEnabled(false)
        assertFalse(repo.state.value.aiTutorEnabled)

        val reloaded = AppPreferencesRepository(prefs = prefs, currentDateProvider = { fakeDay })
        assertFalse("L'état désactivé par le parent doit persister", reloaded.state.value.aiTutorEnabled)
    }

    @Test
    fun devoirHasCorrige_isTrueOnlyWhenCorrigeUrlOrTextSolutionExists() {
        val base = DevoirItem(
            id = "test_corrige_check",
            title = "Devoir test",
            subject = "Mathématiques",
            level = CycleLevel.COLLEGE_9,
            section = TunisianSection.TRONC_COMMUN,
            type = DevoirType.CONTROLE,
            trimester = Trimester.T1,
            durationMinutes = 60,
            difficulty = "Standard",
            chapterTags = listOf("Algèbre"),
            source = "Sujet d'entraînement",
            version = 1,
            sizeMb = 1.5,
            sujetUrl = "https://cdn.devoirstn.example/s.pdf",
            corrigeUrl = null,
            exercises = emptyList()
        )

        // 1. PDF sans corrigeUrl ni exercices -> false
        assertFalse(base.hasCorrige)

        // 2. PDF avec corrigeUrl blanc -> false
        assertFalse(base.copy(corrigeUrl = "   ").hasCorrige)

        // 3. PDF avec corrigeUrl valide -> true
        assertTrue(base.copy(corrigeUrl = "https://cdn.devoirstn.example/c.pdf").hasCorrige)

        // 4. Texte avec exercice sans solution -> false
        val textWithoutSolution = base.copy(
            sujetUrl = "",
            corrigeUrl = null,
            exercises = listOf(DevoirExercise(1, "Ex 1", "5 pts", "Énoncé", "   "))
        )
        assertFalse(textWithoutSolution.hasCorrige)

        // 5. Texte avec au moins un corrigé non vide -> true
        val textWithSolution = base.copy(
            sujetUrl = "",
            corrigeUrl = null,
            exercises = listOf(
                DevoirExercise(1, "Ex 1", "5 pts", "Énoncé 1", ""),
                DevoirExercise(2, "Ex 2", "5 pts", "Énoncé 2", "Solution détaillée")
            )
        )
        assertTrue(textWithSolution.hasCorrige)
    }

    private class InMemorySharedPreferences : SharedPreferences {
        private val map = LinkedHashMap<String, Any?>()

        override fun getAll(): MutableMap<String, *> = LinkedHashMap(map)
        override fun getString(key: String?, defValue: String?): String? =
            (map[key] as? String) ?: defValue
        @Suppress("UNCHECKED_CAST")
        override fun getStringSet(key: String?, defValues: MutableSet<String>?): MutableSet<String>? =
            (map[key] as? MutableSet<String>) ?: defValues
        override fun getInt(key: String?, defValue: Int): Int = (map[key] as? Int) ?: defValue
        override fun getLong(key: String?, defValue: Long): Long = (map[key] as? Long) ?: defValue
        override fun getFloat(key: String?, defValue: Float): Float = (map[key] as? Float) ?: defValue
        override fun getBoolean(key: String?, defValue: Boolean): Boolean =
            (map[key] as? Boolean) ?: defValue
        override fun contains(key: String?): Boolean = map.containsKey(key)
        override fun registerOnSharedPreferenceChangeListener(
            listener: SharedPreferences.OnSharedPreferenceChangeListener?
        ) = Unit
        override fun unregisterOnSharedPreferenceChangeListener(
            listener: SharedPreferences.OnSharedPreferenceChangeListener?
        ) = Unit

        override fun edit(): SharedPreferences.Editor = object : SharedPreferences.Editor {
            private val pending = LinkedHashMap<String, Any?>()
            private var clearAll = false

            override fun putString(key: String?, value: String?) = apply { if (key != null) pending[key] = value }
            override fun putStringSet(key: String?, values: MutableSet<String>?) = apply { if (key != null) pending[key] = values }
            override fun putInt(key: String?, value: Int) = apply { if (key != null) pending[key] = value }
            override fun putLong(key: String?, value: Long) = apply { if (key != null) pending[key] = value }
            override fun putFloat(key: String?, value: Float) = apply { if (key != null) pending[key] = value }
            override fun putBoolean(key: String?, value: Boolean) = apply { if (key != null) pending[key] = value }
            override fun remove(key: String?) = apply { if (key != null) pending[key] = null }
            override fun clear() = apply { clearAll = true }
            override fun commit(): Boolean {
                apply()
                return true
            }
            override fun apply() {
                if (clearAll) map.clear()
                pending.forEach { (k, v) ->
                    if (v == null) map.remove(k) else map[k] = v
                }
            }
        }
    }

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

        override suspend fun clearDownload(devoirId: String) = Unit
        override suspend fun clearDownloads(devoirIds: List<String>) = Unit
        override suspend fun clearAllDownloads() = Unit
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
        private val flow = MutableStateFlow<List<String>>(emptyList())
        override fun observeStarredIdeaIds(): Flow<List<String>> = flow
        override suspend fun starIdea(entity: StarredIdeaEntity) = Unit
        override suspend fun unstarIdea(ideaId: String) = Unit
        override suspend fun isIdeaStarred(ideaId: String): Boolean = false
    }
}
