package com.example.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.data.local.SubjectEntity
import com.example.data.repository.SubjectRepository
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

/**
 * État typé de l'interface pour le chargement réactif des matières depuis Room.
 */
sealed interface SubjectsUiState {
    data object Loading : SubjectsUiState
    data class Success(val subjects: List<SubjectEntity>) : SubjectsUiState
    data class Error(val message: String) : SubjectsUiState
}

/**
 * ViewModel responsable de récupérer les matières scolaires depuis Room via SubjectRepository
 * et de les exposer sous forme de StateFlow réactif pour l'affichage en LazyColumn.
 */
class SubjectsViewModel(
    private val repository: SubjectRepository
) : ViewModel() {

    val uiState: StateFlow<SubjectsUiState> = repository.allSubjects
        .map<List<SubjectEntity>, SubjectsUiState> { list ->
            SubjectsUiState.Success(list)
        }
        .catch { throwable ->
            emit(SubjectsUiState.Error(throwable.message ?: "Erreur de chargement des matières"))
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = SubjectsUiState.Loading
        )

    val subjects: StateFlow<List<SubjectEntity>> = repository.allSubjects
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    init {
        // Peuple la table Room `subjects` dès le démarrage si elle est vide
        viewModelScope.launch {
            repository.ensureDefaultSubjectsSeeded()
        }
    }

    /** Ajoute une matière personnalisée en base Room. */
    fun addCustomSubject(name: String, subtitle: String, iconKey: String = "school") {
        viewModelScope.launch {
            repository.addCustomSubject(name = name, subtitle = subtitle, iconKey = iconKey)
        }
    }

    /** Supprime une matière personnalisée de la base Room. */
    fun deleteSubject(subjectId: String) {
        viewModelScope.launch {
            repository.deleteSubject(subjectId)
        }
    }

    class Factory(
        private val repository: SubjectRepository
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            if (modelClass.isAssignableFrom(SubjectsViewModel::class.java)) {
                return SubjectsViewModel(repository) as T
            }
            throw IllegalArgumentException("Unknown ViewModel class: ${modelClass.name}")
        }
    }
}
