package com.example.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.data.local.ExamAttemptEntity
import com.example.data.local.ExamReminderEntity
import com.example.data.repository.ProgressRepository
import com.example.domain.progress.GradePoint
import com.example.domain.progress.ProgressAnalytics
import com.example.domain.progress.StudyStreakSummary
import com.example.domain.progress.WeakChapterInsight
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class ProgressDashboardUiState(
    val streak: StudyStreakSummary = StudyStreakSummary(0, 0, 0),
    val totalStudySeconds: Long = 0L,
    val totalAttemptsCount: Int = 0,
    val weeklyAttemptsCount: Int = 0,
    val totalQcmCount: Int = 0,
    val averageGradeOn20: Double? = null,
    val availableChartSubjects: List<String> = emptyList(),
    val selectedChartSubject: String? = null,
    val chartPoints: List<GradePoint> = emptyList(),
    val weakChapters: List<WeakChapterInsight> = emptyList(),
    val recentAttempts: List<ExamAttemptEntity> = emptyList(),
    val reminders: List<ExamReminderEntity> = emptyList(),
    val lastReminderFeedback: String? = null
)

class ProgressTrackingViewModel(
    private val progressRepository: ProgressRepository
) : ViewModel() {

    private val _selectedChartSubject = MutableStateFlow<String?>(null)
    private val _lastReminderFeedback = MutableStateFlow<String?>(null)

    val uiState: StateFlow<ProgressDashboardUiState> = combine(
        progressRepository.allAttempts,
        progressRepository.allQcmResults,
        progressRepository.allReminders,
        _selectedChartSubject,
        _lastReminderFeedback
    ) { attempts, qcmResults, reminders, selectedSubject, feedback ->
        val evolutionBySubject = ProgressAnalytics.buildGradeEvolutionBySubject(attempts)
        val subjects = evolutionBySubject.keys.sorted()
        val effectiveSubject = when {
            selectedSubject != null && selectedSubject in subjects -> selectedSubject
            else -> subjects.firstOrNull()
        }
        val points = effectiveSubject?.let { evolutionBySubject[it].orEmpty() }.orEmpty()

        val activityTimestamps = attempts.map { it.attemptedAtMillis } +
            qcmResults.map { it.completedAtMillis }

        val sevenDaysAgoMillis = System.currentTimeMillis() - 7L * 24L * 60L * 60L * 1000L
        val weeklyAttempts = attempts.count { it.attemptedAtMillis >= sevenDaysAgoMillis }

        ProgressDashboardUiState(
            streak = ProgressAnalytics.computeStudyStreak(activityTimestamps),
            totalStudySeconds = ProgressAnalytics.computeTotalStudySeconds(attempts, qcmResults),
            totalAttemptsCount = attempts.size,
            weeklyAttemptsCount = weeklyAttempts,
            totalQcmCount = qcmResults.size,
            averageGradeOn20 = ProgressAnalytics.computeAverageGradeOn20(attempts),
            availableChartSubjects = subjects,
            selectedChartSubject = effectiveSubject,
            chartPoints = points,
            weakChapters = ProgressAnalytics.detectWeakChapters(attempts, qcmResults),
            recentAttempts = attempts.take(10),
            reminders = reminders,
            lastReminderFeedback = feedback
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = ProgressDashboardUiState()
    )

    fun selectChartSubject(subject: String) {
        _selectedChartSubject.value = subject
    }

    fun addExamReminder(
        title: String,
        subject: String,
        examType: String,
        examDateMillis: Long
    ) {
        if (title.isBlank() || subject.isBlank()) return
        viewModelScope.launch {
            val scheduledDays = progressRepository.addReminderAndSchedule(
                title = title,
                subject = subject,
                examType = examType,
                examDateMillis = examDateMillis
            )
            _lastReminderFeedback.value = if (scheduledDays.isNotEmpty()) {
                val labels = scheduledDays.joinToString(", ") { "J-$it" }
                "Rappel enregistré · notifications planifiées ($labels)"
            } else {
                "Rappel enregistré (date trop proche pour J-7/J-3/J-1)"
            }
        }
    }

    fun deleteReminder(reminderId: Long) {
        viewModelScope.launch {
            progressRepository.deleteReminder(reminderId)
            _lastReminderFeedback.value = "Rappel supprimé"
        }
    }

    fun clearReminderFeedback() {
        _lastReminderFeedback.value = null
    }

    class Factory(
        private val progressRepository: ProgressRepository
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return ProgressTrackingViewModel(progressRepository) as T
        }
    }
}
