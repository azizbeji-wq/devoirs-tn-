package com.example.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.data.firebase.CloudExamAttempt
import com.example.data.firebase.CloudExamReminder
import com.example.data.firebase.CloudSubject
import com.example.data.firebase.CloudSyncRepository
import com.example.data.firebase.UserProfile
import com.example.data.local.ExamAttemptEntity
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

sealed interface CloudSyncUiState {
    data object Loading : CloudSyncUiState

    data class Synced(
        val profile: UserProfile?,
        val attempts: List<CloudExamAttempt>,
        val reminders: List<CloudExamReminder>,
        val subjects: List<CloudSubject>
    ) : CloudSyncUiState

    data class Error(val message: String) : CloudSyncUiState
}

/**
 * ViewModel dédié à la synchronisation temps réel Cloud Firestore de l'élève connecté.
 * Instancié uniquement après authentification Google avec `key(uid)`.
 */
class CloudSyncViewModel(
    private val cloudSyncRepository: CloudSyncRepository,
    val currentUserId: String,
    val displayName: String,
    val email: String?
) : ViewModel() {

    val uiState: StateFlow<CloudSyncUiState> = combine(
        cloudSyncRepository.observeUserProfile(),
        cloudSyncRepository.observeExamAttempts(),
        cloudSyncRepository.observeReminders(),
        cloudSyncRepository.observeCustomSubjects()
    ) { profile, attempts, reminders, subjects ->
        CloudSyncUiState.Synced(
            profile = profile,
            attempts = attempts,
            reminders = reminders,
            subjects = subjects
        ) as CloudSyncUiState
    }
        .catch { e ->
            emit(CloudSyncUiState.Error(e.message ?: "Erreur de synchronisation Cloud"))
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000L),
            initialValue = CloudSyncUiState.Loading
        )

    private val _lastSyncError = MutableStateFlow<String?>(null)
    val lastSyncError: StateFlow<String?> = _lastSyncError.asStateFlow()

    fun ensureProfileInitialized(
        weeklyGoalTarget: Int,
        favoriteDevoirIds: List<String>,
        completedDevoirIds: List<String>,
        starredMethodIds: List<String>
    ) {
        viewModelScope.launch {
            cloudSyncRepository.ensureUserProfile(
                displayName = displayName,
                weeklyGoalTarget = weeklyGoalTarget,
                favoriteDevoirIds = favoriteDevoirIds,
                completedDevoirIds = completedDevoirIds,
                starredMethodIds = starredMethodIds
            ).onFailure { error ->
                _lastSyncError.value = error.message
            }.onSuccess {
                _lastSyncError.value = null
            }
        }
    }

    fun syncStudyProfile(
        weeklyGoalTarget: Int,
        favoriteDevoirIds: List<String>,
        completedDevoirIds: List<String>,
        starredMethodIds: List<String>
    ) {
        viewModelScope.launch {
            cloudSyncRepository.updateUserStudyProfile(
                displayName = displayName,
                weeklyGoalTarget = weeklyGoalTarget,
                favoriteDevoirIds = favoriteDevoirIds,
                completedDevoirIds = completedDevoirIds,
                starredMethodIds = starredMethodIds
            ).onFailure { error ->
                _lastSyncError.value = error.message
            }.onSuccess {
                _lastSyncError.value = null
            }
        }
    }

    fun syncExamAttempt(attempt: ExamAttemptEntity) {
        viewModelScope.launch {
            val docId = "attempt_${attempt.devoirId}_${attempt.attemptedAtMillis}"
            cloudSyncRepository.saveExamAttempt(
                attemptId = docId,
                devoirId = attempt.devoirId,
                devoirTitle = attempt.devoirTitle,
                subject = attempt.subject,
                totalScore = attempt.totalScore,
                maxScore = attempt.maxScore,
                actualDurationSeconds = attempt.actualDurationSeconds,
                attemptedAtMillis = attempt.attemptedAtMillis
            ).onFailure { error ->
                _lastSyncError.value = error.message
            }
        }
    }

    fun syncReminder(
        title: String,
        subject: String,
        examType: String,
        examDateMillis: Long
    ) {
        viewModelScope.launch {
            val docId = "rem_${examDateMillis}_${title.hashCode().toUInt()}"
            cloudSyncRepository.saveReminder(
                reminderId = docId,
                title = title,
                subject = subject,
                examType = examType,
                examDateMillis = examDateMillis
            ).onFailure { error ->
                _lastSyncError.value = error.message
            }
        }
    }

    fun deleteCloudReminder(reminderId: String) {
        viewModelScope.launch {
            cloudSyncRepository.deleteReminder(reminderId).onFailure { error ->
                _lastSyncError.value = error.message
            }
        }
    }

    fun syncCustomSubject(name: String, subtitle: String) {
        viewModelScope.launch {
            val docId = "subj_${name.trim().lowercase().hashCode().toUInt()}"
            cloudSyncRepository.saveSubject(
                subjectId = docId,
                name = name,
                subtitle = subtitle
            ).onFailure { error ->
                _lastSyncError.value = error.message
            }
        }
    }

    fun deleteCloudSubject(subjectId: String) {
        viewModelScope.launch {
            cloudSyncRepository.deleteSubject(subjectId).onFailure { error ->
                _lastSyncError.value = error.message
            }
        }
    }

    fun clearSyncError() {
        _lastSyncError.value = null
    }

    class Factory(
        private val cloudSyncRepository: CloudSyncRepository,
        private val currentUserId: String,
        private val displayName: String,
        private val email: String?
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return CloudSyncViewModel(
                cloudSyncRepository = cloudSyncRepository,
                currentUserId = currentUserId,
                displayName = displayName,
                email = email
            ) as T
        }
    }
}
