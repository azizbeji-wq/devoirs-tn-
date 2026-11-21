package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.Crossfade
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.Calculate
import androidx.compose.material.icons.filled.GridView
import androidx.compose.material.icons.filled.Lightbulb
import androidx.compose.material.icons.filled.MenuBook
import androidx.compose.material.icons.outlined.BookmarkBorder
import androidx.compose.material.icons.outlined.Calculate
import androidx.compose.material.icons.outlined.GridView
import androidx.compose.material.icons.outlined.Lightbulb
import androidx.compose.material.icons.outlined.MenuBook
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationRail
import androidx.compose.material3.NavigationRailItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.data.local.DevoirsDatabase
import com.example.data.repository.DevoirRepository
import com.example.ui.screens.DevoirDetailScreen
import com.example.ui.screens.DevoirsLibraryScreen
import com.example.ui.screens.IdeasMethodsScreen
import com.example.ui.screens.MoyenneCalculatorScreen
import com.example.ui.screens.MySpaceScreen
import com.example.ui.screens.SubjectSelectionScreen
import com.example.ui.theme.DevoirsTNTheme
import com.example.ui.viewmodel.DevoirsViewModel
import com.example.ui.viewmodel.MainTab

class MainActivity : ComponentActivity() {

    private val repository by lazy {
        val db = DevoirsDatabase.getInstance(applicationContext)
        DevoirRepository(db.devoirDao(), db.starredIdeaDao())
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            DevoirsTNTheme {
                val viewModel: DevoirsViewModel = viewModel(
                    factory = DevoirsViewModel.Factory(repository)
                )
                DevoirsTNApp(viewModel = viewModel)
            }
        }
    }
}

private data class NavItemSpec(
    val tab: MainTab,
    val labelRes: Int,
    val selectedIcon: ImageVector,
    val unselectedIcon: ImageVector
)

private val navItems = listOf(
    NavItemSpec(
        tab = MainTab.MATIERES,
        labelRes = R.string.nav_matieres,
        selectedIcon = Icons.Filled.GridView,
        unselectedIcon = Icons.Outlined.GridView
    ),
    NavItemSpec(
        tab = MainTab.DEVOIRS,
        labelRes = R.string.nav_devoirs,
        selectedIcon = Icons.Filled.MenuBook,
        unselectedIcon = Icons.Outlined.MenuBook
    ),
    NavItemSpec(
        tab = MainTab.MOYENNE,
        labelRes = R.string.nav_moyenne,
        selectedIcon = Icons.Filled.Calculate,
        unselectedIcon = Icons.Outlined.Calculate
    ),
    NavItemSpec(
        tab = MainTab.IDEES,
        labelRes = R.string.nav_idees,
        selectedIcon = Icons.Filled.Lightbulb,
        unselectedIcon = Icons.Outlined.Lightbulb
    ),
    NavItemSpec(
        tab = MainTab.SUIVI,
        labelRes = R.string.nav_suivi,
        selectedIcon = Icons.Filled.Bookmark,
        unselectedIcon = Icons.Outlined.BookmarkBorder
    )
)

@Composable
fun DevoirsTNApp(viewModel: DevoirsViewModel) {
    val selectedTab by viewModel.selectedTab.collectAsStateWithLifecycle()
    val allDevoirs by viewModel.allDevoirs.collectAsStateWithLifecycle()
    val filteredDevoirs by viewModel.filteredDevoirs.collectAsStateWithLifecycle()
    val filterState by viewModel.filterState.collectAsStateWithLifecycle()
    val availableSubjects by viewModel.availableSubjects.collectAsStateWithLifecycle()
    val selectedDevoir by viewModel.selectedDevoir.collectAsStateWithLifecycle()
    val timerState by viewModel.timerState.collectAsStateWithLifecycle()
    val revealedExercises by viewModel.revealedExercises.collectAsStateWithLifecycle()
    val moyenneState by viewModel.moyenneState.collectAsStateWithLifecycle()
    val starredIdeaIds by viewModel.starredIdeaIds.collectAsStateWithLifecycle()
    val subjectStats by viewModel.subjectProgressStats.collectAsStateWithLifecycle()

    Surface(
        modifier = Modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.background
    ) {
        val activeDevoir = selectedDevoir
        if (activeDevoir != null) {
            DevoirDetailScreen(
                devoir = activeDevoir,
                timerState = timerState,
                revealedExercises = revealedExercises,
                onBack = viewModel::closeDevoirDetail,
                onToggleFavorite = { viewModel.toggleFavorite(activeDevoir.id) },
                onToggleCompleted = { viewModel.toggleCompleted(activeDevoir.id) },
                onToggleTimer = viewModel::toggleTimer,
                onResetTimer = viewModel::resetTimer,
                onToggleExerciseSolution = viewModel::toggleExerciseSolution,
                onToggleAllSolutions = {
                    viewModel.toggleAllSolutions(activeDevoir.exercises.map { it.number })
                }
            )
        } else {
            BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
                val useNavigationRail = maxWidth >= 600.dp

                if (useNavigationRail) {
                    Row(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(MaterialTheme.colorScheme.background)
                    ) {
                        NavigationRail(
                            modifier = Modifier
                                .fillMaxHeight()
                                .statusBarsPadding(),
                            containerColor = MaterialTheme.colorScheme.surface
                        ) {
                            navItems.forEach { item ->
                                val isSelected = selectedTab == item.tab
                                NavigationRailItem(
                                    selected = isSelected,
                                    onClick = { viewModel.selectTab(item.tab) },
                                    modifier = Modifier.testTag("nav_item_${item.tab.id}"),
                                    icon = {
                                        Icon(
                                            imageVector = if (isSelected) item.selectedIcon else item.unselectedIcon,
                                            contentDescription = stringResource(item.labelRes)
                                        )
                                    },
                                    label = { Text(stringResource(item.labelRes)) }
                                )
                            }
                        }

                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .fillMaxHeight()
                                .statusBarsPadding(),
                            contentAlignment = Alignment.TopCenter
                        ) {
                            TabContentContainer(
                                selectedTab = selectedTab,
                                modifier = Modifier.widthIn(max = 820.dp),
                                allDevoirs = allDevoirs,
                                filteredDevoirs = filteredDevoirs,
                                filterState = filterState,
                                availableSubjects = availableSubjects,
                                moyenneState = moyenneState,
                                starredIdeaIds = starredIdeaIds,
                                subjectStats = subjectStats,
                                viewModel = viewModel
                            )
                        }
                    }
                } else {
                    Scaffold(
                        modifier = Modifier.fillMaxSize(),
                        contentWindowInsets = WindowInsets.safeDrawing,
                        bottomBar = {
                            NavigationBar(
                                containerColor = MaterialTheme.colorScheme.surface,
                                tonalElevation = 4.dp
                            ) {
                                navItems.forEach { item ->
                                    val isSelected = selectedTab == item.tab
                                    NavigationBarItem(
                                        selected = isSelected,
                                        onClick = { viewModel.selectTab(item.tab) },
                                        modifier = Modifier.testTag("nav_item_${item.tab.id}"),
                                        icon = {
                                            Icon(
                                                imageVector = if (isSelected) item.selectedIcon else item.unselectedIcon,
                                                contentDescription = stringResource(item.labelRes)
                                            )
                                        },
                                        label = {
                                            Text(
                                                text = stringResource(item.labelRes),
                                                maxLines = 1
                                            )
                                        }
                                    )
                                }
                            }
                        }
                    ) { innerPadding ->
                        TabContentContainer(
                            selectedTab = selectedTab,
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(innerPadding),
                            allDevoirs = allDevoirs,
                            filteredDevoirs = filteredDevoirs,
                            filterState = filterState,
                            availableSubjects = availableSubjects,
                            moyenneState = moyenneState,
                            starredIdeaIds = starredIdeaIds,
                            subjectStats = subjectStats,
                            viewModel = viewModel
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun TabContentContainer(
    selectedTab: MainTab,
    allDevoirs: List<com.example.data.model.DevoirItem>,
    filteredDevoirs: List<com.example.data.model.DevoirItem>,
    filterState: com.example.ui.viewmodel.DevoirFilterState,
    availableSubjects: List<String>,
    moyenneState: com.example.ui.viewmodel.MoyenneUiState,
    starredIdeaIds: Set<String>,
    subjectStats: List<com.example.ui.viewmodel.SubjectProgressStat>,
    viewModel: DevoirsViewModel,
    modifier: Modifier = Modifier
) {
    Crossfade(
        targetState = selectedTab,
        modifier = modifier,
        label = "main_tab_crossfade"
    ) { tab ->
        when (tab) {
            MainTab.MATIERES -> SubjectSelectionScreen(
                allDevoirs = allDevoirs,
                selectedYear = filterState.selectedLevel,
                selectedSubject = filterState.selectedSubject,
                onSelectYear = viewModel::selectLevel,
                onSelectSubject = viewModel::selectSubject,
                onResetSelection = viewModel::resetFilters,
                onOpenDevoir = viewModel::openDevoir,
                onToggleFavorite = viewModel::toggleFavorite,
                onViewInLibrary = { viewModel.selectTab(MainTab.DEVOIRS) }
            )

            MainTab.DEVOIRS -> DevoirsLibraryScreen(
                devoirs = filteredDevoirs,
                totalCount = allDevoirs.size,
                filterState = filterState,
                availableSubjects = availableSubjects,
                onSearchChange = viewModel::updateSearchQuery,
                onSelectLevel = viewModel::selectLevel,
                onSelectSection = viewModel::selectSection,
                onSelectType = viewModel::selectType,
                onSelectTrimester = viewModel::selectTrimester,
                onSelectSubject = viewModel::selectSubject,
                onToggleFavoritesOnly = viewModel::toggleFavoritesOnlyFilter,
                onResetFilters = viewModel::resetFilters,
                onOpenDevoir = viewModel::openDevoir,
                onToggleFavorite = viewModel::toggleFavorite
            )

            MainTab.MOYENNE -> MoyenneCalculatorScreen(
                state = moyenneState,
                onSelectPreset = viewModel::selectCoefficientPreset,
                onSetTrimesterMode = viewModel::setTrimesterMode,
                onUpdateGrade = viewModel::updateSubjectGrade,
                onResetGrades = viewModel::resetGrades,
                onFillSampleGrades = viewModel::fillSampleGrades
            )

            MainTab.IDEES -> IdeasMethodsScreen(
                studyMethods = viewModel.studyMethodGuides,
                appIdeas = viewModel.appFeatureIdeas,
                starredIdeaIds = starredIdeaIds,
                onToggleStarIdea = viewModel::toggleStarIdea
            )

            MainTab.SUIVI -> MySpaceScreen(
                allDevoirs = allDevoirs,
                subjectStats = subjectStats,
                onOpenDevoir = viewModel::openDevoir,
                onToggleFavorite = viewModel::toggleFavorite
            )
        }
    }
}
