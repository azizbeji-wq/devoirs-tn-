package com.example

import com.example.data.local.SubjectDao
import com.example.data.local.SubjectEntity
import com.example.data.repository.SubjectRepository
import com.example.ui.viewmodel.SubjectsUiState
import com.example.ui.viewmodel.SubjectsViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

/**
 * Tests unitaires de SubjectRepository et SubjectsViewModel avec DAO en mémoire et coroutines test.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class SubjectsViewModelTest {

    private val testDispatcher = StandardTestDispatcher()

    @Before
    fun setup() {
        Dispatchers.setMain(testDispatcher)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `initialisation peuple les matieres academiques par defaut dont Maths Physique et Arabe`() = runTest {
        val dao = FakeSubjectDao()
        val vm = SubjectsViewModel(SubjectRepository(dao, testDispatcher))

        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { vm.uiState.collect() }
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { vm.subjects.collect() }
        advanceUntilIdle()

        val state = vm.uiState.value
        assertTrue(state is SubjectsUiState.Success)
        val subjects = (state as SubjectsUiState.Success).subjects
        assertEquals(SubjectRepository.defaultSubjects.size, subjects.size)
        val names = subjects.map { it.canonicalName }
        assertTrue("Mathématiques" in names)
        assertTrue("Physique-Chimie" in names)
        assertTrue("Arabe" in names)
    }

    @Test
    fun `addCustomSubject et deleteSubject mettent a jour le flux reactivement`() = runTest {
        val dao = FakeSubjectDao()
        val vm = SubjectsViewModel(SubjectRepository(dao, testDispatcher))

        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { vm.subjects.collect() }
        advanceUntilIdle()

        val initialSize = vm.subjects.value.size
        vm.addCustomSubject("Histoire-Géographie", "Monde contemporain")
        advanceUntilIdle()

        assertEquals(initialSize + 1, vm.subjects.value.size)
        val added = vm.subjects.value.first { it.canonicalName == "Histoire-Géographie" }
        assertTrue(added.isCustom)

        vm.deleteSubject(added.id)
        advanceUntilIdle()

        assertEquals(initialSize, vm.subjects.value.size)
        assertFalse(vm.subjects.value.any { it.id == added.id })
    }

    @Test
    fun `addCustomSubject ignore un nom vide ou blanc`() = runTest {
        val dao = FakeSubjectDao()
        val vm = SubjectsViewModel(SubjectRepository(dao, testDispatcher))

        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { vm.subjects.collect() }
        advanceUntilIdle()

        val initialSize = vm.subjects.value.size
        vm.addCustomSubject("   ", "Sous-titre")
        advanceUntilIdle()

        assertEquals(initialSize, vm.subjects.value.size)
    }

    /** Implémentation en mémoire de SubjectDao pour les tests unitaires. */
    private class FakeSubjectDao : SubjectDao {
        private val rows = MutableStateFlow<List<SubjectEntity>>(emptyList())

        override fun observeAllSubjects(): Flow<List<SubjectEntity>> =
            rows.map { list -> list.sortedWith(compareBy<SubjectEntity> { it.displayOrder }.thenBy { it.canonicalName }) }

        override suspend fun getSubjectCount(): Int = rows.value.size

        override suspend fun insertAll(subjects: List<SubjectEntity>) {
            rows.update { current ->
                val existingIds = current.map { it.id }.toSet()
                current + subjects.filterNot { it.id in existingIds }
            }
        }

        override suspend fun insertSubject(subject: SubjectEntity) {
            rows.update { current ->
                current.filterNot { it.id == subject.id } + subject
            }
        }

        override suspend fun deleteSubjectById(subjectId: String) {
            rows.update { current -> current.filterNot { it.id == subjectId } }
        }
    }
}
