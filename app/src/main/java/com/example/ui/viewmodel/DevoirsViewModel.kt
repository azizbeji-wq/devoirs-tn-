package com.example.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.data.model.AppFeatureIdea
import com.example.data.model.CycleLevel
import com.example.data.model.DevoirItem
import com.example.data.model.DevoirType
import com.example.data.model.StudyMethodGuide
import com.example.data.model.Trimester
import com.example.data.model.TunisianSection
import com.example.data.local.CatalogueOrigin
import com.example.data.repository.CatalogueSyncStatus
import com.example.data.repository.DevoirRepository
import com.example.domain.bac.AnnualCalculationResult
import com.example.domain.bac.BacCalculator
import com.example.domain.bac.BacConfig
import com.example.domain.bac.BacMention
import com.example.domain.bac.BacSectionConfig
import com.example.domain.bac.BacSession
import com.example.domain.bac.ExamCalculationResult
import com.example.domain.bac.FormuleGlobaleResult
import com.example.domain.bac.ReverseSimulationResult
import com.example.domain.bac.SubjectAnnualInputs
import com.example.domain.bac.TrimesterGradeInput
import com.example.domain.bac.TrimesterKey
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

enum class MainTab(val id: String) {
    MATIERES("matieres"),
    DEVOIRS("devoirs"),
    MOYENNE("moyenne"),
    IDEES("idees"),
    SUIVI("suivi")
}

/** Options de tri disponibles dans la bibliothèque des devoirs. */
enum class DevoirSortOption(val id: String) {
    RECENT("recent"),
    DURATION("duration"),
    DIFFICULTY("difficulty"),
    SUBJECT("subject")
}

data class DevoirFilterState(
    val searchQuery: String = "",
    val selectedLevel: CycleLevel = CycleLevel.ALL,
    val selectedSection: TunisianSection = TunisianSection.ALL,
    val selectedType: DevoirType = DevoirType.ALL,
    val selectedTrimester: Trimester = Trimester.ALL,
    val selectedSubject: String = "Toutes",
    val favoritesOnly: Boolean = false,
    val sortOption: DevoirSortOption = DevoirSortOption.RECENT
)

data class ExamTimerState(
    val devoirId: String? = null,
    val totalSeconds: Int = 3600,
    val remainingSeconds: Int = 3600,
    val isRunning: Boolean = false
)

enum class BacCalcTab(val label: String) {
    ANNUELLE("Annuelle"),
    EXAMEN("Examen"),
    FG("Formule FG"),
    SIMULATEUR("Simulateur")
}

data class MoyenneUiState(
    val config: BacConfig = BacConfig(),
    val selectedSectionId: String = "bac_maths",
    val selectedCalcTab: BacCalcTab = BacCalcTab.ANNUELLE,
    val selectedTrimester: TrimesterKey = TrimesterKey.T1,
    val annualPercent: Double = 25.0,
    val examPercent: Double = 75.0,
    val annualInputs: Map<String, SubjectAnnualInputs> = emptyMap(),
    val examGradeTexts: Map<String, String> = emptyMap(),
    val customSubjectCoeffTexts: Map<String, String> = emptyMap(),
    val customFgCoeffTexts: Map<String, String> = emptyMap(),
    val fgGeneralMultiplierText: String = "4",
    val simulatorTargetText: String = "14",
    val simulatorCustomAnnualText: String = ""
) {
    val sections: List<BacSectionConfig>
        get() = config.sections

    val selectedSection: BacSectionConfig?
        get() = config.sections.find { it.id == selectedSectionId } ?: config.sections.firstOrNull()

    /**
     * Section active avec les coefficients personnalisés par matière appliqués de manière sûre.
     * Les valeurs hors bornes [0, 20] ou invalides retombent sur le coefficient officiel.
     */
    val effectiveSection: BacSectionConfig?
        get() {
            val base = selectedSection ?: return null
            if (customSubjectCoeffTexts.isEmpty()) return base
            return base.copy(
                subjects = base.subjects.map { subject ->
                    val raw = customSubjectCoeffTexts[subject.id]
                    val parsed = raw?.let { BacCalculator.parseCoefficient(it) }
                        ?.takeIf { it in 0.0..20.0 }
                    if (parsed != null) {
                        subject.copy(annualCoeff = parsed, examCoeff = parsed)
                    } else {
                        subject
                    }
                }
            )
        }

    fun isSubjectCoeffInvalid(subjectId: String): Boolean {
        val raw = customSubjectCoeffTexts[subjectId]?.trim() ?: return false
        if (raw.isEmpty()) return false
        val parsed = BacCalculator.parseCoefficient(raw) ?: return true
        return parsed !in 0.0..20.0
    }

    fun isSubjectCoeffCustomized(subjectId: String): Boolean {
        val raw = customSubjectCoeffTexts[subjectId]?.trim() ?: return false
        if (raw.isEmpty()) return false
        val official = selectedSection?.subjects?.find { it.id == subjectId } ?: return false
        val parsed = BacCalculator.parseCoefficient(raw) ?: return true
        return kotlin.math.abs(parsed - official.annualCoeff) > 0.001 ||
            kotlin.math.abs(parsed - official.examCoeff) > 0.001
    }

    val hasCustomSubjectCoeffs: Boolean
        get() = selectedSection?.subjects?.any { isSubjectCoeffCustomized(it.id) } == true

    val totalSubjectCoeffSum: Double
        get() = effectiveSection?.subjects?.sumOf { it.examCoeff } ?: 0.0

    val effectiveWeights
        get() = config.weights.copy(
            annualPercent = annualPercent,
            examPercent = examPercent
        )

    val annualResult: AnnualCalculationResult
        get() {
            val section = effectiveSection ?: return AnnualCalculationResult(emptyMap(), emptyMap(), null)
            return BacCalculator.computeAnnualAverage(section, annualInputs, effectiveWeights)
        }

    val examResult: ExamCalculationResult
        get() {
            val section = effectiveSection ?: return ExamCalculationResult(null, 0.0, 0.0)
            return BacCalculator.computeExamAverage(section, examGradeTexts)
        }

    val generalAverage: Double?
        get() = BacCalculator.computeGeneralAverage(
            annualAverage = annualResult.annualAverage,
            examAverage = examResult.examAverage,
            annualPercent = annualPercent,
            examPercent = examPercent
        )

    val mention: BacMention
        get() = BacCalculator.determineMention(generalAverage, config.mentions)

    val session: BacSession
        get() = BacCalculator.determineSession(generalAverage, config.sessions)

    val parsedCustomFgCoeffs: Map<String, Double>
        get() = customFgCoeffTexts.mapNotNull { (id, text) ->
            BacCalculator.parseCoefficient(text)?.let { id to it }
        }.toMap()

    val effectiveFgMultiplier: Double
        get() = BacCalculator.parseCoefficient(fgGeneralMultiplierText) ?: config.fgGeneralMultiplier

    val fgResult: FormuleGlobaleResult
        get() {
            val section = effectiveSection ?: return FormuleGlobaleResult(null, 0.0, 0.0, emptyList())
            return BacCalculator.computeFormuleGlobale(
                generalAverage = generalAverage,
                section = section,
                examGradeTexts = examGradeTexts,
                customFgCoeffs = parsedCustomFgCoeffs,
                fgGeneralMultiplier = effectiveFgMultiplier
            )
        }

    val simulatorEffectiveAnnual: Double?
        get() = if (simulatorCustomAnnualText.isNotBlank()) {
            BacCalculator.parseGrade(simulatorCustomAnnualText)
        } else {
            annualResult.annualAverage
        }

    val reverseSimulation: ReverseSimulationResult
        get() = BacCalculator.simulateRequiredExamAverage(
            targetGeneralAverage = BacCalculator.parseGrade(simulatorTargetText),
            annualAverage = simulatorEffectiveAnnual,
            annualPercent = annualPercent,
            examPercent = examPercent
        )
}

data class SubjectProgressStat(
    val subject: String,
    val completed: Int,
    val total: Int
)

class DevoirsViewModel(
    private val repository: DevoirRepository,
    private val bacConfig: BacConfig
) : ViewModel() {

    private val _selectedTab = MutableStateFlow(MainTab.MATIERES)
    val selectedTab: StateFlow<MainTab> = _selectedTab.asStateFlow()

    private val _filterState = MutableStateFlow(DevoirFilterState())
    val filterState: StateFlow<DevoirFilterState> = _filterState.asStateFlow()

    private val _selectedDevoirId = MutableStateFlow<String?>(null)
    val selectedDevoirId: StateFlow<String?> = _selectedDevoirId.asStateFlow()

    private val _revealedExercises = MutableStateFlow<Set<Int>>(emptySet())
    val revealedExercises: StateFlow<Set<Int>> = _revealedExercises.asStateFlow()

    private val _timerState = MutableStateFlow(ExamTimerState())
    val timerState: StateFlow<ExamTimerState> = _timerState.asStateFlow()

    private val _moyenneState = MutableStateFlow(
        MoyenneUiState(
            config = bacConfig,
            selectedSectionId = bacConfig.sections.firstOrNull()?.id ?: "bac_maths",
            annualPercent = bacConfig.weights.annualPercent,
            examPercent = bacConfig.weights.examPercent,
            fgGeneralMultiplierText = formatCoeffInput(bacConfig.fgGeneralMultiplier)
        )
    )
    val moyenneState: StateFlow<MoyenneUiState> = _moyenneState.asStateFlow()

    private val _catalogueSyncStatus = MutableStateFlow(CatalogueSyncStatus(isSyncing = true))
    val catalogueSyncStatus: StateFlow<CatalogueSyncStatus> = _catalogueSyncStatus.asStateFlow()

    val studyMethodGuides: List<StudyMethodGuide> = repository.getStudyMethodGuides()
    val appFeatureIdeas: List<AppFeatureIdea> = repository.getAppFeatureIdeas()

    val allDevoirs: StateFlow<List<DevoirItem>> = repository.allDevoirs
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    val starredIdeaIds: StateFlow<Set<String>> = repository.starredIdeaIds
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptySet()
        )

    val availableSubjects: StateFlow<List<String>> = allDevoirs
        .combine(_filterState) { list, _ ->
            listOf("Toutes") + list.map { it.subject }.distinct().sorted()
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = listOf("Toutes")
        )

    /** Flux Room FTS4 des identifiants correspondant à la requête active. */
    @OptIn(ExperimentalCoroutinesApi::class)
    private val ftsMatchedIds: StateFlow<Set<String>> = _filterState
        .map { it.searchQuery }
        .distinctUntilChanged()
        .flatMapLatest { query -> repository.searchFtsDevoirIds(query) }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptySet()
        )

    /** Contenu OCR indexé par `devoirId` pour le filtrage et les extraits surlignés. */
    private val indexedPdfContentMap: StateFlow<Map<String, String>> = repository.indexedPdfContentMap
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyMap()
        )

    // Filtrage réactif délégué au moteur pur DevoirSearchEngine (titres, chapitres et contenu OCR FTS4)
    val filteredDevoirs: StateFlow<List<DevoirItem>> = combine(
        allDevoirs,
        _filterState,
        indexedPdfContentMap,
        ftsMatchedIds
    ) { devoirs, filter, contentMap, matchedFtsIds ->
        com.example.domain.search.DevoirSearchEngine.filterDevoirs(
            devoirs = devoirs,
            filter = filter,
            indexedContentByDevoirId = contentMap,
            ftsMatchedIds = matchedFtsIds
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )

    /** Extraits pertinents par `devoirId` (OCR PDF ou énoncés des sujets texte) pour la recherche. */
    val searchSnippetsByDevoirId: StateFlow<Map<String, String>> = combine(
        _filterState,
        allDevoirs,
        indexedPdfContentMap
    ) { filter, devoirs, contentMap ->
        val query = filter.searchQuery.trim()
        if (query.isEmpty()) {
            emptyMap()
        } else {
            val result = LinkedHashMap<String, String>()
            contentMap.forEach { (id, text) ->
                com.example.domain.search.DevoirSearchEngine.extractMatchingSnippet(text, query)
                    ?.let { snippet -> result[id] = snippet }
            }
            devoirs.forEach { devoir ->
                if (devoir.id !in result && devoir.exercises.isNotEmpty()) {
                    val combinedText = devoir.exercises.joinToString(" ") { "${it.title} ${it.statement}" }
                    com.example.domain.search.DevoirSearchEngine.extractMatchingSnippet(combinedText, query)
                        ?.let { snippet -> result[devoir.id] = snippet }
                }
            }
            result
        }
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyMap()
    )

    val selectedDevoir: StateFlow<DevoirItem?> = combine(
        allDevoirs,
        _selectedDevoirId
    ) { devoirs, selectedId ->
        if (selectedId == null) null else devoirs.find { it.id == selectedId }
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = null
    )

    val subjectProgressStats: StateFlow<List<SubjectProgressStat>> = allDevoirs
        .combine(_selectedTab) { devoirs, _ ->
            devoirs.groupBy { it.subject }
                .map { (subject, items) ->
                    SubjectProgressStat(
                        subject = subject,
                        completed = items.count { it.isCompleted },
                        total = items.size
                    )
                }
                .sortedByDescending { it.total }
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    private var timerJob: Job? = null

    init {
        refreshCatalogue()
    }

    /** Synchronise le catalogue distant (avec bascule transparente sur le cache local si hors ligne). */
    fun refreshCatalogue(onSynced: ((version: Int, syncedAtMillis: Long) -> Unit)? = null) {
        viewModelScope.launch {
            _catalogueSyncStatus.update { it.copy(isSyncing = true) }
            val result = repository.syncCatalogue()
            _catalogueSyncStatus.value = CatalogueSyncStatus(
                isSyncing = false,
                catalogueVersion = result.catalogueVersion,
                origin = result.origin,
                syncedAtMillis = result.syncedAtMillis,
                isOfflineFallback = result.origin != CatalogueOrigin.REMOTE,
                errorMessage = result.errorMessage
            )
            if (result.catalogueVersion > 0) {
                onSynced?.invoke(result.catalogueVersion, result.syncedAtMillis)
            }
        }
    }

    fun selectTab(tab: MainTab) {
        _selectedTab.value = tab
    }

    fun openDevoir(devoir: DevoirItem) {
        _selectedDevoirId.value = devoir.id
        _revealedExercises.value = emptySet()
        if (devoir.isAvailableOffline) {
            viewModelScope.launch {
                repository.ensureDevoirIndexed(devoir)
            }
        }
        if (_timerState.value.devoirId != devoir.id) {
            timerJob?.cancel()
            val secs = devoir.durationMinutes * 60
            _timerState.value = ExamTimerState(
                devoirId = devoir.id,
                totalSeconds = secs,
                remainingSeconds = secs,
                isRunning = false
            )
        }
    }

    fun closeDevoirDetail() {
        timerJob?.cancel()
        _timerState.update { it.copy(isRunning = false) }
        _selectedDevoirId.value = null
    }

    fun toggleExerciseSolution(exerciseNumber: Int) {
        _revealedExercises.update { current ->
            if (exerciseNumber in current) current - exerciseNumber else current + exerciseNumber
        }
    }

    fun toggleAllSolutions(exerciseNumbers: List<Int>) {
        _revealedExercises.update { current ->
            if (current.size >= exerciseNumbers.size) emptySet() else exerciseNumbers.toSet()
        }
    }

    fun toggleTimer() {
        val current = _timerState.value
        if (current.isRunning) {
            timerJob?.cancel()
            _timerState.update { it.copy(isRunning = false) }
        } else {
            if (current.remainingSeconds <= 0) {
                _timerState.update { it.copy(remainingSeconds = it.totalSeconds) }
            }
            _timerState.update { it.copy(isRunning = true) }
            timerJob?.cancel()
            timerJob = viewModelScope.launch {
                while (_timerState.value.isRunning && _timerState.value.remainingSeconds > 0) {
                    delay(1000L)
                    _timerState.update { state ->
                        val next = (state.remainingSeconds - 1).coerceAtLeast(0)
                        state.copy(remainingSeconds = next, isRunning = next > 0)
                    }
                }
            }
        }
    }

    fun resetTimer() {
        timerJob?.cancel()
        _timerState.update { state ->
            state.copy(remainingSeconds = state.totalSeconds, isRunning = false)
        }
    }

    // Filter updates
    fun updateSearchQuery(query: String) {
        _filterState.update { it.copy(searchQuery = query) }
    }

    fun selectLevel(level: CycleLevel) {
        _filterState.update { it.copy(selectedLevel = level) }
    }

    fun selectSection(section: TunisianSection) {
        _filterState.update { it.copy(selectedSection = section) }
    }

    fun selectType(type: DevoirType) {
        _filterState.update { it.copy(selectedType = type) }
    }

    fun selectTrimester(trimester: Trimester) {
        _filterState.update { it.copy(selectedTrimester = trimester) }
    }

    fun selectSubject(subject: String) {
        _filterState.update { it.copy(selectedSubject = subject) }
    }

    fun toggleFavoritesOnlyFilter() {
        _filterState.update { it.copy(favoritesOnly = !it.favoritesOnly) }
    }

    fun selectSortOption(option: DevoirSortOption) {
        _filterState.update { it.copy(sortOption = option) }
    }

    fun resetFilters() {
        _filterState.update { DevoirFilterState(sortOption = it.sortOption) }
    }

    // Persistence mutations
    fun toggleFavorite(devoirId: String) {
        viewModelScope.launch {
            repository.toggleFavorite(devoirId)
        }
    }

    fun toggleCompleted(devoirId: String) {
        viewModelScope.launch {
            repository.toggleCompleted(devoirId)
        }
    }

    fun toggleStarIdea(ideaId: String) {
        viewModelScope.launch {
            repository.toggleStarIdea(ideaId)
        }
    }

    // Tunisian Bac Calculator actions
    fun selectBacSection(sectionId: String) {
        _moyenneState.update { state ->
            state.copy(
                selectedSectionId = sectionId,
                annualInputs = emptyMap(),
                examGradeTexts = emptyMap(),
                customSubjectCoeffTexts = emptyMap(),
                customFgCoeffTexts = emptyMap()
            )
        }
    }

    fun selectBacCalcTab(tab: BacCalcTab) {
        _moyenneState.update { it.copy(selectedCalcTab = tab) }
    }

    fun selectBacTrimester(trimester: TrimesterKey) {
        _moyenneState.update { it.copy(selectedTrimester = trimester) }
    }

    fun updateAnnualPercent(newAnnualPercent: Double) {
        val clamped = newAnnualPercent.coerceIn(0.0, 100.0)
        _moyenneState.update { state ->
            state.copy(
                annualPercent = clamped,
                examPercent = (100.0 - clamped).coerceIn(0.0, 100.0)
            )
        }
    }

    fun resetWeightsToDefault() {
        _moyenneState.update { state ->
            state.copy(
                annualPercent = state.config.weights.annualPercent,
                examPercent = state.config.weights.examPercent
            )
        }
    }

    fun updateAnnualGrade(
        subjectId: String,
        trimester: TrimesterKey,
        dc: String? = null,
        ds: String? = null
    ) {
        _moyenneState.update { state ->
            val subjectInputs = state.annualInputs[subjectId] ?: SubjectAnnualInputs()
            val currentTri = subjectInputs.forTrimester(trimester)
            val updatedTri = currentTri.copy(
                dcText = dc?.take(6) ?: currentTri.dcText,
                dsText = ds?.take(6) ?: currentTri.dsText
            )
            state.copy(
                annualInputs = state.annualInputs + (subjectId to subjectInputs.updateTrimester(trimester, updatedTri))
            )
        }
    }

    fun updateExamGrade(subjectId: String, value: String) {
        _moyenneState.update { state ->
            state.copy(examGradeTexts = state.examGradeTexts + (subjectId to value.take(6)))
        }
    }

    fun updateSubjectCoeff(subjectId: String, value: String) {
        val sanitized = value.filter { it.isDigit() || it == '.' || it == ',' }.take(5)
        _moyenneState.update { state ->
            state.copy(customSubjectCoeffTexts = state.customSubjectCoeffTexts + (subjectId to sanitized))
        }
    }

    fun stepSubjectCoeff(subjectId: String, delta: Double) {
        _moyenneState.update { state ->
            val currentCoeff = state.effectiveSection?.subjects?.find { it.id == subjectId }?.examCoeff
                ?: return@update state
            val next = ((currentCoeff + delta) * 2.0).let { kotlin.math.round(it) / 2.0 }.coerceIn(0.0, 20.0)
            state.copy(
                customSubjectCoeffTexts = state.customSubjectCoeffTexts + (subjectId to formatCoeffInput(next))
            )
        }
    }

    fun resetSubjectCoeffs() {
        _moyenneState.update { it.copy(customSubjectCoeffTexts = emptyMap()) }
    }

    fun updateCustomFgCoeff(subjectId: String, value: String) {
        _moyenneState.update { state ->
            state.copy(customFgCoeffTexts = state.customFgCoeffTexts + (subjectId to value.take(5)))
        }
    }

    fun updateFgGeneralMultiplier(value: String) {
        _moyenneState.update { it.copy(fgGeneralMultiplierText = value.take(5)) }
    }

    fun resetFgCoeffs() {
        _moyenneState.update { state ->
            state.copy(
                customFgCoeffTexts = emptyMap(),
                fgGeneralMultiplierText = formatCoeffInput(state.config.fgGeneralMultiplier)
            )
        }
    }

    fun updateSimulatorTarget(value: String) {
        _moyenneState.update { it.copy(simulatorTargetText = value.take(6)) }
    }

    fun updateSimulatorCustomAnnual(value: String) {
        _moyenneState.update { it.copy(simulatorCustomAnnualText = value.take(6)) }
    }

    fun resetBacGrades() {
        _moyenneState.update { state ->
            state.copy(
                annualInputs = emptyMap(),
                examGradeTexts = emptyMap(),
                simulatorCustomAnnualText = ""
            )
        }
    }

    fun fillBacSampleGrades() {
        val section = _moyenneState.value.selectedSection ?: return
        val annualMap = mutableMapOf<String, SubjectAnnualInputs>()
        val examMap = mutableMapOf<String, String>()

        section.subjects.forEachIndexed { idx, subject ->
            val base = (12.5 + (idx % 5) * 1.15).coerceAtMost(18.5)
            val t1Dc = (base - 0.5).coerceIn(8.0, 19.5)
            val t1Ds = (base + 0.25).coerceIn(8.0, 19.5)
            val t2Dc = (base + 0.2).coerceIn(8.0, 19.5)
            val t2Ds = (base + 0.75).coerceIn(8.0, 19.5)
            val t3Dc = (base + 0.4).coerceIn(8.0, 19.5)
            val t3Ds = (base + 1.0).coerceIn(8.0, 19.5)
            val exam = (base + 0.85).coerceIn(8.0, 19.75)

            annualMap[subject.id] = SubjectAnnualInputs(
                t1 = TrimesterGradeInput(formatGradeInput(t1Dc), formatGradeInput(t1Ds)),
                t2 = TrimesterGradeInput(formatGradeInput(t2Dc), formatGradeInput(t2Ds)),
                t3 = TrimesterGradeInput(formatGradeInput(t3Dc), formatGradeInput(t3Ds))
            )
            examMap[subject.id] = formatGradeInput(exam)
        }

        _moyenneState.update { state ->
            state.copy(
                annualInputs = annualMap,
                examGradeTexts = examMap,
                simulatorCustomAnnualText = ""
            )
        }
    }

    override fun onCleared() {
        super.onCleared()
        timerJob?.cancel()
    }

    companion object {
        private fun formatGradeInput(value: Double): String =
            "%.2f".format(value).replace(',', '.').trimEnd('0').trimEnd('.')

        private fun formatCoeffInput(value: Double): String =
            if (value % 1.0 == 0.0) value.toInt().toString() else value.toString()
    }

    class Factory(
        private val repository: DevoirRepository,
        private val bacConfig: BacConfig
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            if (modelClass.isAssignableFrom(DevoirsViewModel::class.java)) {
                return DevoirsViewModel(repository, bacConfig) as T
            }
            throw IllegalArgumentException("Unknown ViewModel class")
        }
    }
}
