package com.example.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.data.model.AppFeatureIdea
import com.example.data.model.CycleLevel
import com.example.data.model.DevoirItem
import com.example.data.model.DevoirType
import com.example.data.model.SectionCoefficientPreset
import com.example.data.model.StudyMethodGuide
import com.example.data.model.Trimester
import com.example.data.model.TunisianSection
import com.example.data.repository.DevoirRepository
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
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

data class DevoirFilterState(
    val searchQuery: String = "",
    val selectedLevel: CycleLevel = CycleLevel.ALL,
    val selectedSection: TunisianSection = TunisianSection.ALL,
    val selectedType: DevoirType = DevoirType.ALL,
    val selectedTrimester: Trimester = Trimester.ALL,
    val selectedSubject: String = "Toutes",
    val favoritesOnly: Boolean = false
)

data class ExamTimerState(
    val devoirId: String? = null,
    val totalSeconds: Int = 3600,
    val remainingSeconds: Int = 3600,
    val isRunning: Boolean = false
)

data class SubjectGradeInput(
    val dcText: String = "",
    val dsText: String = "",
    val directText: String = ""
)

data class MoyenneUiState(
    val presets: List<SectionCoefficientPreset> = emptyList(),
    val selectedPresetId: String = "bac_maths",
    val isTrimesterMode: Boolean = true,
    val gradeInputs: Map<String, SubjectGradeInput> = emptyMap()
) {
    val selectedPreset: SectionCoefficientPreset?
        get() = presets.find { it.id == selectedPresetId } ?: presets.firstOrNull()

    fun subjectAverage(subjectId: String): Double? {
        val input = gradeInputs[subjectId] ?: return null
        return if (isTrimesterMode) {
            val dc = parseGrade(input.dcText)
            val ds = parseGrade(input.dsText)
            when {
                dc != null && ds != null -> (dc + 2.0 * ds) / 3.0
                ds != null -> ds
                dc != null -> dc
                else -> null
            }
        } else {
            parseGrade(input.directText)
        }
    }

    val computedAverage: Double?
        get() {
            val preset = selectedPreset ?: return null
            var weightedSum = 0.0
            var totalCoeff = 0.0
            for (subject in preset.subjects) {
                val avg = subjectAverage(subject.id)
                if (avg != null) {
                    weightedSum += avg * subject.coefficient
                    totalCoeff += subject.coefficient
                }
            }
            return if (totalCoeff > 0.0) weightedSum / totalCoeff else null
        }

    val totalWeightedPoints: Double
        get() {
            val preset = selectedPreset ?: return 0.0
            return preset.subjects.sumOf { subject ->
                val avg = subjectAverage(subject.id) ?: 0.0
                avg * subject.coefficient
            }
        }

    val activeCoeffSum: Double
        get() {
            val preset = selectedPreset ?: return 0.0
            return preset.subjects.sumOf { subject ->
                if (subjectAverage(subject.id) != null) subject.coefficient else 0.0
            }
        }

    val mentionLabel: String
        get() {
            val avg = computedAverage ?: return "Saisis tes notes"
            return when {
                avg >= 16.0 -> "Mention Très Bien"
                avg >= 14.0 -> "Mention Bien"
                avg >= 12.0 -> "Mention Assez Bien"
                avg >= 10.0 -> "Admis · Passable"
                avg >= 7.0 -> "Session de Contrôle"
                else -> "Efforts à renforcer"
            }
        }

    val nextTargetHint: String
        get() {
            val avg = computedAverage ?: return "Entre au moins une note pour estimer ta moyenne tunisienne."
            val nextThreshold = when {
                avg < 10.0 -> 10.0 to "l'admission (10/20)"
                avg < 12.0 -> 12.0 to "Assez Bien (12/20)"
                avg < 14.0 -> 14.0 to "Bien (14/20)"
                avg < 16.0 -> 16.0 to "Très Bien (16/20)"
                else -> null
            }
            return if (nextThreshold == null) {
                "Excellent niveau ! Tu vises les meilleures orientations universitaires."
            } else {
                val diff = nextThreshold.first - avg
                val pointsNeeded = diff * activeCoeffSum
                "Il te manque +%.2f pts de moyenne (≈ +%.1f pts pondérés) pour %s."
                    .format(diff, pointsNeeded, nextThreshold.second)
            }
        }

    companion object {
        fun parseGrade(raw: String): Double? {
            val cleaned = raw.trim().replace(',', '.')
            val value = cleaned.toDoubleOrNull() ?: return null
            return value.coerceIn(0.0, 20.0)
        }
    }
}

data class SubjectProgressStat(
    val subject: String,
    val completed: Int,
    val total: Int
)

class DevoirsViewModel(
    private val repository: DevoirRepository
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
            presets = repository.getCoefficientPresets(),
            selectedPresetId = repository.getCoefficientPresets().firstOrNull()?.id ?: "bac_maths"
        )
    )
    val moyenneState: StateFlow<MoyenneUiState> = _moyenneState.asStateFlow()

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

    val filteredDevoirs: StateFlow<List<DevoirItem>> = combine(
        allDevoirs,
        _filterState
    ) { devoirs, filter ->
        devoirs.filter { item ->
            val matchesQuery = filter.searchQuery.isBlank() ||
                item.title.contains(filter.searchQuery, ignoreCase = true) ||
                item.subject.contains(filter.searchQuery, ignoreCase = true) ||
                item.sourceSchool.contains(filter.searchQuery, ignoreCase = true) ||
                item.chapterTags.any { it.contains(filter.searchQuery, ignoreCase = true) }

            val matchesLevel = filter.selectedLevel == CycleLevel.ALL || item.level == filter.selectedLevel
            val matchesSection = filter.selectedSection == TunisianSection.ALL || item.section == filter.selectedSection
            val matchesType = filter.selectedType == DevoirType.ALL || item.type == filter.selectedType
            val matchesTrimester = filter.selectedTrimester == Trimester.ALL || item.trimester == filter.selectedTrimester
            val matchesSubject = filter.selectedSubject == "Toutes" || item.subject == filter.selectedSubject
            val matchesFavorite = !filter.favoritesOnly || item.isFavorite

            matchesQuery && matchesLevel && matchesSection && matchesType &&
                matchesTrimester && matchesSubject && matchesFavorite
        }
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
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
        viewModelScope.launch {
            repository.ensureSeedDataLoaded()
        }
    }

    fun selectTab(tab: MainTab) {
        _selectedTab.value = tab
    }

    fun openDevoir(devoir: DevoirItem) {
        _selectedDevoirId.value = devoir.id
        _revealedExercises.value = emptySet()
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

    fun resetFilters() {
        _filterState.value = DevoirFilterState()
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

    // Moyenne Calculator actions
    fun selectCoefficientPreset(presetId: String) {
        _moyenneState.update { it.copy(selectedPresetId = presetId, gradeInputs = emptyMap()) }
    }

    fun setTrimesterMode(trimesterMode: Boolean) {
        _moyenneState.update { it.copy(isTrimesterMode = trimesterMode) }
    }

    fun updateSubjectGrade(
        subjectId: String,
        dc: String? = null,
        ds: String? = null,
        direct: String? = null
    ) {
        _moyenneState.update { state ->
            val current = state.gradeInputs[subjectId] ?: SubjectGradeInput()
            val updated = current.copy(
                dcText = dc?.filter { it.isDigit() || it == '.' || it == ',' }?.take(5) ?: current.dcText,
                dsText = ds?.filter { it.isDigit() || it == '.' || it == ',' }?.take(5) ?: current.dsText,
                directText = direct?.filter { it.isDigit() || it == '.' || it == ',' }?.take(5) ?: current.directText
            )
            state.copy(gradeInputs = state.gradeInputs + (subjectId to updated))
        }
    }

    fun resetGrades() {
        _moyenneState.update { it.copy(gradeInputs = emptyMap()) }
    }

    fun fillSampleGrades() {
        val preset = _moyenneState.value.selectedPreset ?: return
        val sampleMap = preset.subjects.mapIndexed { idx, subj ->
            val dc = (13.5 + (idx % 4) * 1.25).coerceAtMost(19.0)
            val ds = (14.0 + ((idx + 1) % 4) * 1.1).coerceAtMost(19.5)
            val direct = ((dc + 2.0 * ds) / 3.0)
            subj.id to SubjectGradeInput(
                dcText = "%.1f".format(dc).replace(',', '.'),
                dsText = "%.1f".format(ds).replace(',', '.'),
                directText = "%.1f".format(direct).replace(',', '.')
            )
        }.toMap()
        _moyenneState.update { it.copy(gradeInputs = sampleMap) }
    }

    override fun onCleared() {
        super.onCleared()
        timerJob?.cancel()
    }

    class Factory(private val repository: DevoirRepository) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            if (modelClass.isAssignableFrom(DevoirsViewModel::class.java)) {
                return DevoirsViewModel(repository) as T
            }
            throw IllegalArgumentException("Unknown ViewModel class")
        }
    }
}
