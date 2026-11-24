package com.example

import com.example.data.local.DevoirDao
import com.example.data.local.DevoirEntity
import com.example.data.local.DevoirVersionTuple
import com.example.data.local.StarredIdeaDao
import com.example.data.local.StarredIdeaEntity
import com.example.data.model.CycleLevel
import com.example.data.model.DevoirItem
import com.example.data.model.DevoirType
import com.example.data.model.Trimester
import com.example.data.model.TunisianSection
import com.example.data.repository.DevoirRepository
import com.example.domain.bac.BacConfig
import com.example.domain.bac.BacSectionConfig
import com.example.domain.bac.BacSubjectConfig
import com.example.domain.bac.TrimesterKey
import com.example.ui.viewmodel.DevoirsViewModel
import com.example.ui.viewmodel.MainTab
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

/**
 * Tests unitaires du DevoirsViewModel avec kotlinx-coroutines-test et DAO en mémoire.
 * Vérifie la synchronisation du catalogue distant, les filtres réactifs, les favoris,
 * la progression par matière et le chronomètre.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class DevoirsViewModelTest {

    private val testDispatcher = StandardTestDispatcher()

    private val seedDevoirs = listOf(
        DevoirItem(
            id = "dev_math_1",
            title = "DS N°1 — Fonctions & Limites",
            subject = "Mathématiques",
            level = CycleLevel.BAC,
            section = TunisianSection.MATHS,
            type = DevoirType.SYNTHESE,
            trimester = Trimester.T1,
            durationMinutes = 120,
            difficulty = "Bac",
            chapterTags = listOf("Limites", "Continuité"),
            source = "Sujet d'entraînement",
            version = 3,
            sizeMb = 5.2,
            sujetUrl = "https://cdn.devoirstn.example/pdfs/bac_maths_ds1_sujet.pdf",
            corrigeUrl = "https://cdn.devoirstn.example/pdfs/bac_maths_ds1_corrige.pdf"
        ),
        DevoirItem(
            id = "dev_phys_1",
            title = "DC N°1 — Circuits RL",
            subject = "Physique-Chimie",
            level = CycleLevel.BAC,
            section = TunisianSection.SCIENCES_EXP,
            type = DevoirType.CONTROLE,
            trimester = Trimester.T1,
            durationMinutes = 60,
            difficulty = "Moyen",
            chapterTags = listOf("Dipôle RL", "Bobine"),
            source = "Sujet d'entraînement",
            version = 3,
            sizeMb = 4.1,
            sujetUrl = "https://cdn.devoirstn.example/pdfs/lycee_3eme_physique_dc1_sujet.pdf",
            corrigeUrl = "https://cdn.devoirstn.example/pdfs/lycee_3eme_physique_dc1_corrige.pdf"
        ),
        DevoirItem(
            id = "dev_math_2",
            title = "DC N°2 — Théorème de Thalès",
            subject = "Mathématiques",
            level = CycleLevel.COLLEGE_9,
            section = TunisianSection.TRONC_COMMUN,
            type = DevoirType.CONTROLE,
            trimester = Trimester.T2,
            durationMinutes = 60,
            difficulty = "Facile",
            chapterTags = listOf("Thalès", "Géométrie"),
            source = "Sujet d'entraînement",
            version = 2,
            sizeMb = 3.0,
            sujetUrl = "https://cdn.devoirstn.example/pdfs/college_9eme_maths_dc2_sujet.pdf",
            corrigeUrl = null
        )
    )

    @Before
    fun setup() {
        Dispatchers.setMain(testDispatcher)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    private fun createViewModel(): DevoirsViewModel {
        val fakeDevoirDao = FakeDevoirDao()
        val fakeIdeaDao = FakeStarredIdeaDao()
        val repo = DevoirRepository(
            devoirDao = fakeDevoirDao,
            starredIdeaDao = fakeIdeaDao,
            loadAssetDevoirs = { seedDevoirs }
        )
        return DevoirsViewModel(repository = repo, bacConfig = BacConfig())
    }

    private fun TestScope.subscribeFlows(vm: DevoirsViewModel) {
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { vm.allDevoirs.collect() }
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { vm.filteredDevoirs.collect() }
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { vm.selectedDevoir.collect() }
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { vm.subjectProgressStats.collect() }
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { vm.starredIdeaIds.collect() }
    }

    @Test
    fun `initialisation charge les devoirs PDF depuis le catalogue dans le repository`() = runTest {
        val vm = createViewModel()
        subscribeFlows(vm)
        advanceUntilIdle()

        assertEquals(3, vm.allDevoirs.value.size)
        assertEquals(3, vm.filteredDevoirs.value.size)
        assertEquals(3, vm.catalogueSyncStatus.value.catalogueVersion)
    }

    @Test
    fun `recherche plein texte dans les chapitres filtre reactivement puis resetFilters reinitialise`() = runTest {
        val vm = createViewModel()
        subscribeFlows(vm)
        advanceUntilIdle()

        vm.updateSearchQuery("bobine")
        advanceUntilIdle()
        assertEquals(1, vm.filteredDevoirs.value.size)
        assertEquals("dev_phys_1", vm.filteredDevoirs.value.first().id)

        vm.resetFilters()
        advanceUntilIdle()
        assertEquals(3, vm.filteredDevoirs.value.size)
        assertEquals("", vm.filterState.value.searchQuery)
    }

    @Test
    fun `filtres combines niveau matiere et type affinent correctement la bibliotheque`() = runTest {
        val vm = createViewModel()
        subscribeFlows(vm)
        advanceUntilIdle()

        vm.selectLevel(CycleLevel.BAC)
        vm.selectSubject("Mathématiques")
        vm.selectType(DevoirType.SYNTHESE)
        advanceUntilIdle()

        assertEquals(1, vm.filteredDevoirs.value.size)
        assertEquals("dev_math_1", vm.filteredDevoirs.value.first().id)
    }

    @Test
    fun `toggleFavorite et filtre favoris mettent a jour la liste en temps reel`() = runTest {
        val vm = createViewModel()
        subscribeFlows(vm)
        advanceUntilIdle()

        vm.toggleFavorite("dev_math_2")
        vm.toggleFavoritesOnlyFilter()
        advanceUntilIdle()

        assertEquals(1, vm.filteredDevoirs.value.size)
        assertEquals("dev_math_2", vm.filteredDevoirs.value.first().id)
        assertTrue(vm.filteredDevoirs.value.first().isFavorite)
    }

    @Test
    fun `toggleCompleted met a jour les statistiques de progression par matiere`() = runTest {
        val vm = createViewModel()
        subscribeFlows(vm)
        advanceUntilIdle()

        vm.toggleCompleted("dev_math_1")
        advanceUntilIdle()

        val mathStat = vm.subjectProgressStats.value.find { it.subject == "Mathématiques" }
        assertNotNull(mathStat)
        assertEquals(2, mathStat!!.total)
        assertEquals(1, mathStat.completed)
    }

    @Test
    fun `openDevoir initialise le chronometre et closeDevoirDetail nettoie la selection`() = runTest {
        val vm = createViewModel()
        subscribeFlows(vm)
        advanceUntilIdle()

        val target = vm.allDevoirs.value.first { it.id == "dev_math_1" }
        vm.openDevoir(target)
        advanceUntilIdle()

        assertEquals("dev_math_1", vm.selectedDevoir.value?.id)
        assertEquals(120 * 60, vm.timerState.value.totalSeconds)
        assertEquals(120 * 60, vm.timerState.value.remainingSeconds)
        assertFalse(vm.timerState.value.isRunning)

        vm.closeDevoirDetail()
        advanceUntilIdle()
        assertNull(vm.selectedDevoir.value)
    }

    @Test
    fun `selectTab et toggleStarIdea mettent a jour l etat sans effet de bord`() = runTest {
        val vm = createViewModel()
        subscribeFlows(vm)
        advanceUntilIdle()

        vm.selectTab(MainTab.MOYENNE)
        assertEquals(MainTab.MOYENNE, vm.selectedTab.value)

        vm.toggleStarIdea("idea_offline_pack")
        advanceUntilIdle()
        assertTrue("idea_offline_pack" in vm.starredIdeaIds.value)

        vm.toggleStarIdea("idea_offline_pack")
        advanceUntilIdle()
        assertFalse("idea_offline_pack" in vm.starredIdeaIds.value)
    }

    @Test
    fun `updateSubjectCoeff stepSubjectCoeff et resetSubjectCoeffs recalculent la moyenne officielle`() = runTest {
        val fakeDevoirDao = FakeDevoirDao()
        val fakeIdeaDao = FakeStarredIdeaDao()
        val repo = DevoirRepository(
            devoirDao = fakeDevoirDao,
            starredIdeaDao = fakeIdeaDao,
            loadAssetDevoirs = { seedDevoirs }
        )
        val config = BacConfig(
            sections = listOf(
                BacSectionConfig(
                    id = "bac_maths",
                    title = "Bac Mathématiques",
                    shortTitle = "Maths",
                    description = "Description Maths",
                    subjects = listOf(
                        BacSubjectConfig(id = "maths", name = "Mathématiques", annualCoeff = 4.0, examCoeff = 4.0, fgCoeff = 4.0),
                        BacSubjectConfig(id = "phys", name = "Physique", annualCoeff = 4.0, examCoeff = 4.0, fgCoeff = 4.0)
                    )
                )
            )
        )
        val vm = DevoirsViewModel(repository = repo, bacConfig = config)
        subscribeFlows(vm)
        advanceUntilIdle()

        // Saisie de notes : Maths = 18, Physique = 10 -> moyenne officielle initiale (coeff 4 / 4) = 14.0
        vm.updateAnnualGrade("maths", TrimesterKey.T1, dc = "18", ds = "18")
        vm.updateAnnualGrade("phys", TrimesterKey.T1, dc = "10", ds = "10")
        vm.updateExamGrade("maths", "18")
        vm.updateExamGrade("phys", "10")

        assertEquals(14.0, vm.moyenneState.value.annualResult.annualAverage!!, 0.001)
        assertEquals(14.0, vm.moyenneState.value.examResult.examAverage!!, 0.001)
        assertEquals(14.0, vm.moyenneState.value.generalAverage!!, 0.001)
        assertFalse(vm.moyenneState.value.hasCustomSubjectCoeffs)
        assertEquals(8.0, vm.moyenneState.value.totalSubjectCoeffSum, 0.001)

        // Modification du coefficient de Maths à 6 et Physique à 2 -> (18*6 + 10*2) / 8 = 16.0
        vm.updateSubjectCoeff("maths", "6")
        vm.updateSubjectCoeff("phys", "2")
        assertTrue(vm.moyenneState.value.hasCustomSubjectCoeffs)
        assertTrue(vm.moyenneState.value.isSubjectCoeffCustomized("maths"))
        assertEquals(16.0, vm.moyenneState.value.annualResult.annualAverage!!, 0.001)
        assertEquals(16.0, vm.moyenneState.value.examResult.examAverage!!, 0.001)
        assertEquals(16.0, vm.moyenneState.value.generalAverage!!, 0.001)

        // Pas de +0.5 sur Maths -> 6.5 ; total = 8.5
        vm.stepSubjectCoeff("maths", 0.5)
        assertEquals(8.5, vm.moyenneState.value.totalSubjectCoeffSum, 0.001)

        // Valeur invalide > 20 -> signalée invalide et retombe en sécurité sur le coefficient officiel (4.0)
        vm.updateSubjectCoeff("maths", "25")
        assertTrue(vm.moyenneState.value.isSubjectCoeffInvalid("maths"))
        assertEquals(6.0, vm.moyenneState.value.totalSubjectCoeffSum, 0.001)

        // Réinitialisation des coefficients -> retour à 4 / 4 et moyenne 14.0
        vm.resetSubjectCoeffs()
        assertFalse(vm.moyenneState.value.hasCustomSubjectCoeffs)
        assertEquals(8.0, vm.moyenneState.value.totalSubjectCoeffSum, 0.001)
        assertEquals(14.0, vm.moyenneState.value.generalAverage!!, 0.001)
    }

    private class FakeDevoirDao : DevoirDao {
        private val items = MutableStateFlow<List<DevoirEntity>>(emptyList())

        override fun observeAllDevoirs(): Flow<List<DevoirEntity>> = items
        override suspend fun getById(id: String): DevoirEntity? = items.value.firstOrNull { it.id == id }
        override suspend fun getDevoirCount(): Int = items.value.size
        override suspend fun getExistingVersions(): List<DevoirVersionTuple> =
            items.value.map { DevoirVersionTuple(it.id, it.version, it.sujetUrl) }

        override suspend fun insertAll(devoirs: List<DevoirEntity>) {
            items.update { current ->
                val existingIds = current.map { it.id }.toSet()
                current + devoirs.filterNot { it.id in existingIds }
            }
        }

        override suspend fun insertIgnore(devoir: DevoirEntity) {
            items.update { current ->
                if (current.any { it.id == devoir.id }) current else current + devoir
            }
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
            items.update { list ->
                list.map { existing ->
                    if (existing.id == id) {
                        existing.copy(
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
                    } else existing
                }
            }
        }

        override suspend fun markDownloaded(
            devoirId: String,
            localSujetPath: String?,
            localCorrigePath: String?,
            downloadedBytes: Long,
            downloadedVersion: Int
        ) {
            items.update { list ->
                list.map {
                    if (it.id == devoirId) {
                        it.copy(
                            localSujetPath = localSujetPath,
                            localCorrigePath = localCorrigePath,
                            downloadedBytes = downloadedBytes,
                            downloadedVersion = downloadedVersion
                        )
                    } else it
                }
            }
        }

        override suspend fun clearDownload(devoirId: String) {
            items.update { list ->
                list.map {
                    if (it.id == devoirId) {
                        it.copy(
                            localSujetPath = null,
                            localCorrigePath = null,
                            downloadedBytes = 0L,
                            downloadedVersion = 0
                        )
                    } else it
                }
            }
        }

        override suspend fun clearDownloads(devoirIds: List<String>) {
            val idSet = devoirIds.toSet()
            items.update { list ->
                list.map {
                    if (it.id in idSet) {
                        it.copy(
                            localSujetPath = null,
                            localCorrigePath = null,
                            downloadedBytes = 0L,
                            downloadedVersion = 0
                        )
                    } else it
                }
            }
        }

        override suspend fun clearAllDownloads() {
            items.update { list ->
                list.map {
                    it.copy(
                        localSujetPath = null,
                        localCorrigePath = null,
                        downloadedBytes = 0L,
                        downloadedVersion = 0
                    )
                }
            }
        }

        override suspend fun toggleFavorite(devoirId: String) {
            items.update { list ->
                list.map { if (it.id == devoirId) it.copy(isFavorite = !it.isFavorite) else it }
            }
        }

        override suspend fun toggleCompleted(devoirId: String) {
            items.update { list ->
                list.map { if (it.id == devoirId) it.copy(isCompleted = !it.isCompleted) else it }
            }
        }
    }

    private class FakeStarredIdeaDao : StarredIdeaDao {
        private val ids = MutableStateFlow<Set<String>>(emptySet())
        override fun observeStarredIdeaIds(): Flow<List<String>> = ids.map { it.toList() }
        override suspend fun starIdea(entity: StarredIdeaEntity) {
            ids.update { it + entity.ideaId }
        }
        override suspend fun unstarIdea(ideaId: String) {
            ids.update { it - ideaId }
        }
        override suspend fun isIdeaStarred(ideaId: String): Boolean = ideaId in ids.value
    }
}
