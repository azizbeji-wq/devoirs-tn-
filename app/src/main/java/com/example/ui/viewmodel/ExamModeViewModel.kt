package com.example.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.data.local.ExamAttemptEntity
import com.example.data.local.ExerciseCodec
import com.example.data.local.ExerciseScoreCodec
import com.example.data.local.ExerciseScoreEntry
import com.example.data.model.DevoirItem
import com.example.data.repository.ProgressRepository
import kotlin.math.roundToInt
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

enum class ExamModePhase {
    IDLE,
    IN_PROGRESS,
    SELF_EVALUATION,
    SAVED_SUMMARY
}

data class ExerciseEvalInput(
    val exerciseNumber: Int,
    val title: String,
    val maxPoints: Double,
    val scoreText: String = "",
    val error: String? = null
)

data class ExamModeUiState(
    val phase: ExamModePhase = ExamModePhase.IDLE,
    val devoir: DevoirItem? = null,
    val officialDurationSeconds: Int = 3600,
    val remainingSeconds: Int = 3600,
    val actualElapsedSeconds: Int = 0,
    val exerciseInputs: List<ExerciseEvalInput> = emptyList(),
    val lastSavedTotalScore: Double? = null,
    val lastSavedMaxScore: Double = 20.0,
    val isSaving: Boolean = false,
    val validationBanner: String? = null,
    val pastAttemptsForDevoir: List<ExamAttemptEntity> = emptyList()
) {
    val isSolutionsLocked: Boolean
        get() = phase == ExamModePhase.IN_PROGRESS

    val computedTotalScore: Double
        get() = exerciseInputs.sumOf { input ->
            parseScore(input.scoreText)?.coerceIn(0.0, input.maxPoints) ?: 0.0
        }.let { (it * 100.0).roundToInt() / 100.0 }

    val computedMaxScore: Double
        get() = exerciseInputs.sumOf { it.maxPoints }.takeIf { it > 0.0 } ?: 20.0

    val canSubmitEvaluation: Boolean
        get() = exerciseInputs.isNotEmpty() &&
            exerciseInputs.all { input ->
                val value = parseScore(input.scoreText)
                value != null && value in 0.0..input.maxPoints
            }

    companion object {
        fun parseScore(raw: String): Double? {
            val cleaned = raw.trim().replace(',', '.')
            if (cleaned.isEmpty()) return null
            return cleaned.toDoubleOrNull()?.takeIf { it.isFinite() }
        }
    }
}

class ExamModeViewModel(
    private val progressRepository: ProgressRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(ExamModeUiState())
    val uiState: StateFlow<ExamModeUiState> = _uiState.asStateFlow()

    private var countdownJob: Job? = null
    private var attemptsObserverJob: Job? = null

    fun bindDevoir(devoir: DevoirItem?) {
        attemptsObserverJob?.cancel()
        if (devoir == null) {
            stopExamSession()
            _uiState.value = ExamModeUiState()
            return
        }

        // Si on ouvre un autre devoir, réinitialiser l'état examen
        if (_uiState.value.devoir?.id != devoir.id) {
            countdownJob?.cancel()
            _uiState.value = ExamModeUiState(
                devoir = devoir,
                officialDurationSeconds = devoir.durationMinutes * 60,
                remainingSeconds = devoir.durationMinutes * 60
            )
        }

        attemptsObserverJob = viewModelScope.launch {
            progressRepository.observeAttemptsForDevoir(devoir.id).collect { attempts ->
                _uiState.update { it.copy(pastAttemptsForDevoir = attempts) }
            }
        }
    }

    fun startRealConditionsExam(devoir: DevoirItem) {
        countdownJob?.cancel()
        val totalSeconds = (devoir.durationMinutes.coerceAtLeast(1)) * 60
        _uiState.update { state ->
            state.copy(
                phase = ExamModePhase.IN_PROGRESS,
                devoir = devoir,
                officialDurationSeconds = totalSeconds,
                remainingSeconds = totalSeconds,
                actualElapsedSeconds = 0,
                exerciseInputs = buildInitialInputs(devoir),
                validationBanner = null
            )
        }

        countdownJob = viewModelScope.launch {
            while (_uiState.value.phase == ExamModePhase.IN_PROGRESS && _uiState.value.remainingSeconds > 0) {
                delay(1000L)
                _uiState.update { current ->
                    if (current.phase != ExamModePhase.IN_PROGRESS) return@update current
                    val nextRemaining = (current.remainingSeconds - 1).coerceAtLeast(0)
                    val nextElapsed = current.actualElapsedSeconds + 1
                    current.copy(
                        remainingSeconds = nextRemaining,
                        actualElapsedSeconds = nextElapsed
                    )
                }
            }
            if (_uiState.value.phase == ExamModePhase.IN_PROGRESS && _uiState.value.remainingSeconds == 0) {
                finishExamAndOpenEvaluation()
            }
        }
    }

    fun finishExamAndOpenEvaluation() {
        countdownJob?.cancel()
        val current = _uiState.value
        val devoir = current.devoir ?: return
        _uiState.update { state ->
            state.copy(
                phase = ExamModePhase.SELF_EVALUATION,
                exerciseInputs = state.exerciseInputs.ifEmpty { buildInitialInputs(devoir) },
                validationBanner = null
            )
        }
    }

    fun cancelExamMode() {
        countdownJob?.cancel()
        val devoir = _uiState.value.devoir
        _uiState.update { state ->
            state.copy(
                phase = ExamModePhase.IDLE,
                remainingSeconds = (devoir?.durationMinutes ?: 60) * 60,
                actualElapsedSeconds = 0,
                validationBanner = null
            )
        }
    }

    fun updateExerciseScore(exerciseNumber: Int, rawText: String) {
        if (rawText.length > 6) return
        _uiState.update { state ->
            val updated = state.exerciseInputs.map { input ->
                if (input.exerciseNumber != exerciseNumber) {
                    input
                } else {
                    val parsed = ExamModeUiState.parseScore(rawText)
                    val err = when {
                        rawText.isBlank() -> null
                        parsed == null -> "Nombre invalide"
                        parsed < 0.0 || parsed > input.maxPoints -> "Entre 0 et ${formatPoints(input.maxPoints)}"
                        else -> null
                    }
                    input.copy(scoreText = rawText, error = err)
                }
            }
            state.copy(exerciseInputs = updated, validationBanner = null)
        }
    }

    fun saveSelfEvaluation(onSaved: () -> Unit = {}) {
        val state = _uiState.value
        val devoir = state.devoir ?: return

        val validatedEntries = mutableListOf<ExerciseScoreEntry>()
        var hasError = false
        val checkedInputs = state.exerciseInputs.map { input ->
            val parsed = ExamModeUiState.parseScore(input.scoreText)
            if (parsed == null || parsed < 0.0 || parsed > input.maxPoints) {
                hasError = true
                input.copy(error = "Saisis une note entre 0 et ${formatPoints(input.maxPoints)}")
            } else {
                validatedEntries.add(
                    ExerciseScoreEntry(
                        exerciseNumber = input.exerciseNumber,
                        obtainedPoints = (parsed * 100.0).roundToInt() / 100.0,
                        maxPoints = input.maxPoints
                    )
                )
                input.copy(error = null)
            }
        }

        if (hasError) {
            _uiState.update {
                it.copy(
                    exerciseInputs = checkedInputs,
                    validationBanner = "Complète une note valide pour chaque exercice avant d'enregistrer."
                )
            }
            return
        }

        val totalScore = (validatedEntries.sumOf { it.obtainedPoints } * 100.0).roundToInt() / 100.0
        val maxScore = validatedEntries.sumOf { it.maxPoints }.takeIf { it > 0.0 } ?: 20.0
        val actualDuration = state.actualElapsedSeconds.coerceAtLeast(1)

        viewModelScope.launch {
            _uiState.update { it.copy(isSaving = true) }
            progressRepository.saveExamAttempt(
                ExamAttemptEntity(
                    devoirId = devoir.id,
                    devoirTitle = devoir.title,
                    subject = devoir.subject,
                    chapterTagsSerialized = ExerciseCodec.encodeTags(devoir.chapterTags),
                    attemptedAtMillis = System.currentTimeMillis(),
                    officialDurationSeconds = state.officialDurationSeconds,
                    actualDurationSeconds = actualDuration,
                    exerciseScoresSerialized = ExerciseScoreCodec.encode(validatedEntries),
                    totalScore = totalScore,
                    maxScore = maxScore
                )
            )
            _uiState.update {
                it.copy(
                    phase = ExamModePhase.SAVED_SUMMARY,
                    isSaving = false,
                    lastSavedTotalScore = totalScore,
                    lastSavedMaxScore = maxScore,
                    validationBanner = null
                )
            }
            onSaved()
        }
    }

    fun dismissSavedSummary() {
        val devoir = _uiState.value.devoir
        _uiState.update {
            it.copy(
                phase = ExamModePhase.IDLE,
                remainingSeconds = (devoir?.durationMinutes ?: 60) * 60,
                actualElapsedSeconds = 0
            )
        }
    }

    private fun stopExamSession() {
        countdownJob?.cancel()
    }

    private fun buildInitialInputs(devoir: DevoirItem): List<ExerciseEvalInput> = listOf(
        ExerciseEvalInput(
            exerciseNumber = 1,
            title = devoir.title,
            maxPoints = 20.0
        )
    )

    private fun formatPoints(value: Double): String =
        if (value % 1.0 == 0.0) value.toInt().toString() else "%.1f".format(value)

    override fun onCleared() {
        super.onCleared()
        countdownJob?.cancel()
        attemptsObserverJob?.cancel()
    }

    class Factory(
        private val progressRepository: ProgressRepository
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return ExamModeViewModel(progressRepository) as T
        }
    }
}
