package com.example.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.data.local.FlashQcmBank
import com.example.data.local.FlashQcmChapter
import com.example.data.local.QcmResultEntity
import com.example.data.repository.ProgressRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class ChapterQcmStats(
    val bestScore: Int,
    val lastScore: Int,
    val totalQuestions: Int,
    val attemptsCount: Int
)

data class ActiveQcmSessionState(
    val chapter: FlashQcmChapter? = null,
    val currentIndex: Int = 0,
    val selectedOptionIndex: Int? = null,
    val isAnswerSubmitted: Boolean = false,
    val correctCount: Int = 0,
    val startedAtMillis: Long = 0L,
    val isCompleted: Boolean = false,
    val isSaved: Boolean = false
) {
    val isOpen: Boolean get() = chapter != null
}

class FlashQcmViewModel(
    private val progressRepository: ProgressRepository
) : ViewModel() {

    val chapters: List<FlashQcmChapter> = FlashQcmBank.chapters

    val statsByChapterId: StateFlow<Map<String, ChapterQcmStats>> = progressRepository.allQcmResults
        .map { results ->
            results.groupBy { it.chapterId }.mapValues { (_, list) ->
                val sorted = list.sortedByDescending { it.completedAtMillis }
                ChapterQcmStats(
                    bestScore = list.maxOf { it.score },
                    lastScore = sorted.first().score,
                    totalQuestions = sorted.first().totalQuestions,
                    attemptsCount = list.size
                )
            }
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyMap()
        )

    private val _session = MutableStateFlow(ActiveQcmSessionState())
    val session: StateFlow<ActiveQcmSessionState> = _session.asStateFlow()

    fun startChapterQuiz(chapter: FlashQcmChapter) {
        _session.value = ActiveQcmSessionState(
            chapter = chapter,
            currentIndex = 0,
            selectedOptionIndex = null,
            isAnswerSubmitted = false,
            correctCount = 0,
            startedAtMillis = System.currentTimeMillis(),
            isCompleted = false,
            isSaved = false
        )
    }

    fun startChapterByName(chapterName: String): Boolean {
        val match = FlashQcmBank.findByChapterName(chapterName) ?: return false
        startChapterQuiz(match)
        return true
    }

    fun selectOption(optionIndex: Int) {
        val current = _session.value
        val chapter = current.chapter ?: return
        if (current.isAnswerSubmitted || current.isCompleted) return
        val question = chapter.questions.getOrNull(current.currentIndex) ?: return

        val isCorrect = optionIndex == question.correctIndex
        _session.update {
            it.copy(
                selectedOptionIndex = optionIndex,
                isAnswerSubmitted = true,
                correctCount = if (isCorrect) it.correctCount + 1 else it.correctCount
            )
        }
    }

    fun nextQuestionOrFinish() {
        val current = _session.value
        val chapter = current.chapter ?: return
        if (!current.isAnswerSubmitted) return

        val nextIdx = current.currentIndex + 1
        if (nextIdx < chapter.questions.size) {
            _session.update {
                it.copy(
                    currentIndex = nextIdx,
                    selectedOptionIndex = null,
                    isAnswerSubmitted = false
                )
            }
        } else {
            val elapsedSec = ((System.currentTimeMillis() - current.startedAtMillis) / 1000L)
                .toInt()
                .coerceAtLeast(5)
            _session.update { it.copy(isCompleted = true) }
            viewModelScope.launch {
                progressRepository.saveQcmResult(
                    QcmResultEntity(
                        chapterId = chapter.id,
                        chapterName = chapter.chapterName,
                        subject = chapter.subject,
                        score = current.correctCount,
                        totalQuestions = chapter.questions.size,
                        durationSeconds = elapsedSec,
                        completedAtMillis = System.currentTimeMillis()
                    )
                )
                _session.update { it.copy(isSaved = true) }
            }
        }
    }

    fun closeQuiz() {
        _session.value = ActiveQcmSessionState()
    }

    class Factory(
        private val progressRepository: ProgressRepository
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return FlashQcmViewModel(progressRepository) as T
        }
    }
}
