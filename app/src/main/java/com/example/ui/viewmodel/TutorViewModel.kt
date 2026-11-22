package com.example.ui.viewmodel

import android.graphics.Bitmap
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.data.ai.PdfPageImageScaler
import com.example.data.ai.TutorAiRepository
import com.example.data.ai.TutorErrorException
import com.example.data.ai.TutorHelpMode
import com.example.data.ai.TutorLanguage
import com.example.data.local.AppPreferencesRepository
import com.example.data.local.TutorMessageEntity
import com.example.data.local.TutorQuotaAndPolicyStore
import com.example.data.model.DevoirExercise
import com.example.data.model.DevoirItem
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

private const val TAG = "TutorViewModel"

/** Portée d'affichage de l'historique Room du tuteur IA. */
enum class TutorHistoryScope {
    CURRENT_CONTEXT,
    ALL_DEVOIR
}

sealed interface TutorGenerationState {
    data object Idle : TutorGenerationState
    data class Loading(val mode: TutorHelpMode, val language: TutorLanguage) : TutorGenerationState
    data class Streaming(
        val partialText: String,
        val mode: TutorHelpMode,
        val language: TutorLanguage
    ) : TutorGenerationState

    data class Error(
        val message: String,
        val canRetry: Boolean = true
    ) : TutorGenerationState
}

data class PendingTutorRequest(
    val devoir: DevoirItem,
    val exercise: DevoirExercise,
    val mode: TutorHelpMode,
    val language: TutorLanguage,
    val userQuestion: String? = null,
    val pdfPageNumber: Int? = null,
    val pdfDocumentKindLabel: String? = null
)

data class TutorUiState(
    val isSheetOpen: Boolean = false,
    val activeDevoir: DevoirItem? = null,
    val activeExercise: DevoirExercise? = null,
    val activePdfPageNumber: Int? = null,
    val activePdfDocumentKindLabel: String? = null,
    val historyScope: TutorHistoryScope = TutorHistoryScope.CURRENT_CONTEXT,
    val selectedLanguage: TutorLanguage = TutorLanguage.FRENCH,
    val questionDraft: String = "",
    val generationState: TutorGenerationState = TutorGenerationState.Idle,
    val lastFailedRequest: PendingTutorRequest? = null,
    val aiTutorEnabled: Boolean = true,
    val dailyQuotaUsed: Int = 0,
    val dailyQuotaLimit: Int = AppPreferencesRepository.DEFAULT_DAILY_TUTOR_QUOTA
) {
    val isPdfPageMode: Boolean get() = activePdfPageNumber != null
    val dailyQuotaRemaining: Int get() = (dailyQuotaLimit - dailyQuotaUsed).coerceAtLeast(0)
    val isDailyQuotaReached: Boolean get() = dailyQuotaUsed >= dailyQuotaLimit
}

/**
 * ViewModel du tuteur IA Gemini :
 * - Gère le contexte textuel classique ET le mode multimodal « Explique cette page » depuis le lecteur PDF,
 * - Applique le contrôle parental (`aiTutorEnabled`) et le quota quotidien par appareil (20 requêtes/jour),
 * - Conserve une copie déjà redimensionnée du bitmap de la page courante pour les suites (Indice 1 / 2 / Solution / Chat),
 * - Expose l'historique Room par page courante ou pour l'ensemble du devoir.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class TutorViewModel(
    private val tutorRepository: TutorAiRepository,
    private val quotaPolicyStore: TutorQuotaAndPolicyStore? = null
) : ViewModel() {

    private val _uiState = MutableStateFlow(
        TutorUiState(
            aiTutorEnabled = quotaPolicyStore?.state?.value?.aiTutorEnabled ?: true,
            dailyQuotaUsed = quotaPolicyStore?.state?.value?.tutorDailyUsedCount ?: 0,
            dailyQuotaLimit = quotaPolicyStore?.state?.value?.tutorDailyQuotaLimit
                ?: AppPreferencesRepository.DEFAULT_DAILY_TUTOR_QUOTA
        )
    )
    val uiState: StateFlow<TutorUiState> = _uiState.asStateFlow()

    private val activeKey = MutableStateFlow<Pair<String, Int>?>(null)
    private val boundDevoirId = MutableStateFlow<String?>(null)
    private val historyScopeFlow = MutableStateFlow(TutorHistoryScope.CURRENT_CONTEXT)

    /** Copie légère déjà réduite (<= 1024 px) de la page PDF active pour les requêtes Gemini successives. */
    private var activePageBitmap: Bitmap? = null

    init {
        quotaPolicyStore?.let { store ->
            viewModelScope.launch {
                store.state.collect { prefs ->
                    _uiState.update { current ->
                        val shouldClose = !prefs.aiTutorEnabled && current.isSheetOpen
                        if (shouldClose) {
                            activeGenerationJob?.cancel()
                            releaseActivePageBitmap()
                        }
                        current.copy(
                            isSheetOpen = if (!prefs.aiTutorEnabled) false else current.isSheetOpen,
                            generationState = if (shouldClose) TutorGenerationState.Idle else current.generationState,
                            aiTutorEnabled = prefs.aiTutorEnabled,
                            dailyQuotaUsed = prefs.tutorDailyUsedCount,
                            dailyQuotaLimit = prefs.tutorDailyQuotaLimit
                        )
                    }
                }
            }
        }
    }

    val messages: StateFlow<List<TutorMessageEntity>> = combine(
        activeKey,
        historyScopeFlow
    ) { key, scope -> key to scope }
        .flatMapLatest { (key, scope) ->
            if (key == null) {
                flowOf(emptyList())
            } else if (scope == TutorHistoryScope.ALL_DEVOIR) {
                tutorRepository.observeDevoirHistory(key.first)
            } else {
                tutorRepository.observeConversation(key.first, key.second)
            }
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    /** Nombre total d'explications IA enregistrées dans Room pour le devoir ouvert. */
    val activeDevoirExplanationCount: StateFlow<Int> = boundDevoirId
        .flatMapLatest { id ->
            if (id.isNullOrBlank()) flowOf(0) else tutorRepository.observeDevoirExplanationCount(id)
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = 0
        )

    private var activeGenerationJob: Job? = null

    /** Lie le devoir ouvert afin d'afficher le compteur d'historique IA dans la fiche devoir. */
    fun bindDevoir(devoirId: String?) {
        boundDevoirId.value = devoirId
    }

    /** Ouvre le tuteur en mode texte (fiche devoir, exercice texte ou historique complet du devoir). */
    fun openTutor(
        devoir: DevoirItem,
        exercise: DevoirExercise,
        autoTriggerMode: TutorHelpMode? = null,
        initialHistoryScope: TutorHistoryScope = TutorHistoryScope.CURRENT_CONTEXT
    ) {
        quotaPolicyStore?.refreshDailyTutorQuota()
        if (!_uiState.value.aiTutorEnabled) return

        activeGenerationJob?.cancel()
        releaseActivePageBitmap()
        boundDevoirId.value = devoir.id
        activeKey.value = devoir.id to exercise.number
        historyScopeFlow.value = initialHistoryScope

        val suggestedLang = suggestLanguage(devoir)

        _uiState.update {
            it.copy(
                isSheetOpen = true,
                activeDevoir = devoir,
                activeExercise = exercise,
                activePdfPageNumber = null,
                activePdfDocumentKindLabel = null,
                historyScope = initialHistoryScope,
                selectedLanguage = suggestedLang,
                generationState = TutorGenerationState.Idle,
                lastFailedRequest = null
            )
        }

        if (autoTriggerMode != null) {
            triggerHelpMode(autoTriggerMode)
        }
    }

    /**
     * Ouvre le tuteur IA directement depuis le lecteur PDF sur l'image de la page courante
     * (« Explique cette page »), en réduisant l'image avant envoi à Gemini.
     */
    fun openTutorForPdfPage(
        devoir: DevoirItem,
        pageNumber: Int,
        documentKindLabel: String,
        pageBitmap: Bitmap,
        autoTriggerMode: TutorHelpMode? = TutorHelpMode.EXPLAIN
    ) {
        quotaPolicyStore?.refreshDailyTutorQuota()
        if (!_uiState.value.aiTutorEnabled) return

        activeGenerationJob?.cancel()
        boundDevoirId.value = devoir.id
        activeKey.value = devoir.id to pageNumber
        historyScopeFlow.value = TutorHistoryScope.CURRENT_CONTEXT

        val suggestedLang = suggestLanguage(devoir)
        val syntheticExercise = DevoirExercise(
            number = pageNumber,
            title = "$documentKindLabel · Page $pageNumber",
            points = "",
            statement = devoir.title,
            solution = ""
        )

        val willTrigger = autoTriggerMode != null && !_uiState.value.isDailyQuotaReached

        _uiState.update {
            it.copy(
                isSheetOpen = true,
                activeDevoir = devoir,
                activeExercise = syntheticExercise,
                activePdfPageNumber = pageNumber,
                activePdfDocumentKindLabel = documentKindLabel,
                historyScope = TutorHistoryScope.CURRENT_CONTEXT,
                selectedLanguage = suggestedLang,
                generationState = when {
                    autoTriggerMode != null && it.isDailyQuotaReached -> TutorGenerationState.Error(
                        message = dailyQuotaReachedMessage(suggestedLang, it.dailyQuotaLimit),
                        canRetry = false
                    )
                    willTrigger -> TutorGenerationState.Loading(autoTriggerMode!!, suggestedLang)
                    else -> TutorGenerationState.Idle
                },
                lastFailedRequest = null
            )
        }

        viewModelScope.launch {
            val prepared = withContext(Dispatchers.Default) {
                PdfPageImageScaler.prepareBitmapForGemini(pageBitmap)
            }
            replaceActivePageBitmap(prepared)
            if (willTrigger && autoTriggerMode != null) {
                triggerHelpMode(autoTriggerMode)
            }
        }
    }

    fun selectHistoryScope(scope: TutorHistoryScope) {
        historyScopeFlow.value = scope
        _uiState.update { it.copy(historyScope = scope) }
    }

    fun closeTutor() {
        activeGenerationJob?.cancel()
        releaseActivePageBitmap()
        _uiState.update {
            it.copy(
                isSheetOpen = false,
                generationState = TutorGenerationState.Idle
            )
        }
    }

    fun selectLanguage(language: TutorLanguage) {
        _uiState.update { it.copy(selectedLanguage = language) }
    }

    fun updateQuestionDraft(text: String) {
        _uiState.update { it.copy(questionDraft = text) }
    }

    fun triggerHelpMode(mode: TutorHelpMode) {
        val state = _uiState.value
        if (!state.aiTutorEnabled) return
        val devoir = state.activeDevoir ?: return
        val exercise = state.activeExercise ?: return
        if (state.historyScope != TutorHistoryScope.CURRENT_CONTEXT) {
            selectHistoryScope(TutorHistoryScope.CURRENT_CONTEXT)
        }
        executeRequest(
            request = PendingTutorRequest(
                devoir = devoir,
                exercise = exercise,
                mode = mode,
                language = state.selectedLanguage,
                pdfPageNumber = state.activePdfPageNumber,
                pdfDocumentKindLabel = state.activePdfDocumentKindLabel
            ),
            saveUserPrompt = true
        )
    }

    fun sendQuestion() {
        val state = _uiState.value
        if (!state.aiTutorEnabled) return
        val devoir = state.activeDevoir ?: return
        val exercise = state.activeExercise ?: return
        val question = state.questionDraft.trim()
        if (question.isEmpty()) return

        if (state.historyScope != TutorHistoryScope.CURRENT_CONTEXT) {
            selectHistoryScope(TutorHistoryScope.CURRENT_CONTEXT)
        }

        _uiState.update { it.copy(questionDraft = "") }
        executeRequest(
            request = PendingTutorRequest(
                devoir = devoir,
                exercise = exercise,
                mode = TutorHelpMode.CHAT,
                language = state.selectedLanguage,
                userQuestion = question,
                pdfPageNumber = state.activePdfPageNumber,
                pdfDocumentKindLabel = state.activePdfDocumentKindLabel
            ),
            saveUserPrompt = true
        )
    }

    fun retryLastRequest() {
        if (!_uiState.value.aiTutorEnabled) return
        val failed = _uiState.value.lastFailedRequest ?: return
        executeRequest(
            request = failed.copy(language = _uiState.value.selectedLanguage),
            saveUserPrompt = false
        )
    }

    fun clearHistory() {
        val state = _uiState.value
        val devoir = state.activeDevoir ?: return
        val exercise = state.activeExercise ?: return
        activeGenerationJob?.cancel()
        viewModelScope.launch {
            if (state.historyScope == TutorHistoryScope.ALL_DEVOIR) {
                tutorRepository.clearDevoirHistory(devoir.id)
            } else {
                tutorRepository.clearConversation(devoir.id, exercise.number)
            }
            _uiState.update {
                it.copy(
                    generationState = TutorGenerationState.Idle,
                    lastFailedRequest = null
                )
            }
        }
    }

    private fun executeRequest(
        request: PendingTutorRequest,
        saveUserPrompt: Boolean
    ) {
        quotaPolicyStore?.refreshDailyTutorQuota()
        val currentState = _uiState.value

        if (!currentState.aiTutorEnabled) {
            _uiState.update {
                it.copy(
                    isSheetOpen = false,
                    generationState = TutorGenerationState.Idle
                )
            }
            return
        }

        // Vérification et décompte du quota quotidien par appareil (20 requêtes/jour)
        val quotaConsumed = quotaPolicyStore?.tryConsumeTutorQuota() ?: true
        if (!quotaConsumed) {
            _uiState.update {
                it.copy(
                    generationState = TutorGenerationState.Error(
                        message = dailyQuotaReachedMessage(request.language, it.dailyQuotaLimit),
                        canRetry = false
                    ),
                    lastFailedRequest = null
                )
            }
            return
        }

        activeGenerationJob?.cancel()
        activeGenerationJob = viewModelScope.launch {
            _uiState.update {
                it.copy(
                    generationState = TutorGenerationState.Loading(request.mode, request.language),
                    lastFailedRequest = null
                )
            }

            val contextNumber = request.pdfPageNumber ?: request.exercise.number

            if (saveUserPrompt) {
                val userLabel = tutorRepository.buildUserDisplayLabel(
                    mode = request.mode,
                    language = request.language,
                    question = request.userQuestion,
                    pageNumber = request.pdfPageNumber
                )
                tutorRepository.saveMessage(
                    devoirId = request.devoir.id,
                    exerciseNumber = contextNumber,
                    role = "user",
                    mode = request.mode,
                    language = request.language,
                    contentText = userLabel
                )
            }

            var latestText = ""
            try {
                val pageBmp = activePageBitmap?.takeIf { !it.isRecycled }
                val streamFlow = if (request.pdfPageNumber != null && pageBmp != null) {
                    tutorRepository.streamPageTutorResponse(
                        devoir = request.devoir,
                        pageNumber = request.pdfPageNumber,
                        documentKindLabel = request.pdfDocumentKindLabel ?: "Sujet PDF",
                        pageBitmap = pageBmp,
                        mode = request.mode,
                        language = request.language,
                        userQuestion = request.userQuestion
                    )
                } else {
                    tutorRepository.streamTutorResponse(
                        devoir = request.devoir,
                        exercise = request.exercise,
                        mode = request.mode,
                        language = request.language,
                        userQuestion = request.userQuestion
                    )
                }

                streamFlow.collect { accumulated ->
                    latestText = accumulated
                    _uiState.update {
                        it.copy(
                            generationState = TutorGenerationState.Streaming(
                                partialText = accumulated,
                                mode = request.mode,
                                language = request.language
                            )
                        )
                    }
                }

                if (latestText.isNotBlank()) {
                    tutorRepository.saveMessage(
                        devoirId = request.devoir.id,
                        exerciseNumber = contextNumber,
                        role = "assistant",
                        mode = request.mode,
                        language = request.language,
                        contentText = latestText
                    )
                    _uiState.update {
                        it.copy(
                            generationState = TutorGenerationState.Idle,
                            lastFailedRequest = null
                        )
                    }
                } else {
                    quotaPolicyStore?.refundTutorQuota()
                    _uiState.update {
                        it.copy(
                            generationState = TutorGenerationState.Error(
                                message = if (request.language == TutorLanguage.ARABIC) {
                                    "أرجع المساعد إجابة فارغة. أعد المحاولة."
                                } else {
                                    "Réponse vide reçue du tuteur IA. Appuie sur Réessayer."
                                },
                                canRetry = true
                            ),
                            lastFailedRequest = request
                        )
                    }
                }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                // Ne pénalise pas le quota quotidien si l'appareil est hors ligne
                if (e is TutorErrorException.NoNetwork) {
                    quotaPolicyStore?.refundTutorQuota()
                }
                Log.e(TAG, "Tutor AI request failed", e)
                val message = e.message ?: if (request.language == TutorLanguage.ARABIC) {
                    "حدث خطأ غير متوقع. أعد المحاولة."
                } else {
                    "Erreur lors de l'appel au tuteur IA. Réessaie."
                }
                _uiState.update {
                    it.copy(
                        generationState = TutorGenerationState.Error(
                            message = message,
                            canRetry = !it.isDailyQuotaReached
                        ),
                        lastFailedRequest = request
                    )
                }
            }
        }
    }

    private fun dailyQuotaReachedMessage(language: TutorLanguage, limit: Int): String =
        if (language == TutorLanguage.ARABIC) {
            "لقد بلغت الحدّ اليومي ($limit طلباً) للمساعد الذكي على هذا الجهاز. سيتجدد الرصيد تلقائياً غداً، ويمكنك مواصلة مراجعة الفروض المحمّلة دون اتصال."
        } else {
            "Tu as atteint le quota quotidien de $limit requêtes au Tuteur IA sur cet appareil. Ton quota se réinitialisera automatiquement demain ; tes devoirs restent accessibles hors ligne."
        }

    private fun suggestLanguage(devoir: DevoirItem): TutorLanguage =
        if (devoir.subject.equals("Arabe", ignoreCase = true)) {
            TutorLanguage.ARABIC
        } else {
            _uiState.value.selectedLanguage
        }

    private fun replaceActivePageBitmap(newBitmap: Bitmap) {
        val old = activePageBitmap
        activePageBitmap = newBitmap
        if (old != null && old !== newBitmap && !old.isRecycled) {
            old.recycle()
        }
    }

    private fun releaseActivePageBitmap() {
        val old = activePageBitmap
        activePageBitmap = null
        if (old != null && !old.isRecycled) {
            old.recycle()
        }
    }

    override fun onCleared() {
        super.onCleared()
        activeGenerationJob?.cancel()
        releaseActivePageBitmap()
    }

    class Factory(
        private val repository: TutorAiRepository,
        private val quotaPolicyStore: TutorQuotaAndPolicyStore? = null
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            if (modelClass.isAssignableFrom(TutorViewModel::class.java)) {
                return TutorViewModel(repository, quotaPolicyStore) as T
            }
            throw IllegalArgumentException("Unknown ViewModel class")
        }
    }
}
