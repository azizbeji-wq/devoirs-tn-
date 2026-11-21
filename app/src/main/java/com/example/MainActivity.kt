package com.example

import android.Manifest
import android.content.pm.PackageManager
import android.content.res.Configuration
import android.graphics.Color as AndroidColor
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import androidx.compose.animation.Crossfade
import androidx.compose.foundation.background
import androidx.compose.foundation.isSystemInDarkTheme
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
import androidx.compose.material.icons.filled.AutoStories
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.Calculate
import androidx.compose.material.icons.filled.GridView
import androidx.compose.material.icons.filled.MenuBook
import androidx.compose.material.icons.outlined.AutoStories
import androidx.compose.material.icons.outlined.BookmarkBorder
import androidx.compose.material.icons.outlined.Calculate
import androidx.compose.material.icons.outlined.GridView
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
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.data.ai.PdfTextExtractor
import com.example.data.ai.TutorAiRepository
import com.example.data.ai.TutorHelpMode
import com.example.data.ai.configureAppCheck
import com.example.data.download.PdfDownloadCoordinator
import com.example.data.local.AppLanguage
import com.example.data.local.AppPreferencesRepository
import com.example.data.local.AppPreferencesState
import com.example.data.local.BacConfigLoader
import com.example.data.local.DevoirsDatabase
import com.example.data.local.RemoteCatalogueService
import com.example.data.local.ThemeMode
import com.example.data.notifications.ExamReminderScheduler
import com.example.data.repository.DevoirRepository
import com.example.data.repository.ProgressRepository
import com.example.data.repository.SubjectRepository
import com.example.ui.screens.DevoirDetailScreen
import com.example.ui.screens.DevoirsLibraryScreen
import com.example.ui.screens.DownloadConfirmationDialogs
import com.example.ui.screens.IdeasMethodsScreen
import com.example.ui.screens.MoyenneCalculatorScreen
import com.example.ui.screens.MySpaceScreen
import com.example.ui.screens.SubjectSelectionScreen
import com.example.ui.theme.DevoirsTNTheme
import com.example.ui.viewmodel.DevoirsViewModel
import com.example.ui.viewmodel.ExamModeViewModel
import com.example.ui.viewmodel.FlashQcmViewModel
import com.example.ui.viewmodel.MainTab
import com.example.ui.viewmodel.ProgressTrackingViewModel
import com.example.ui.viewmodel.StorageViewModel
import com.example.ui.viewmodel.SubjectsUiState
import com.example.ui.viewmodel.SubjectsViewModel
import com.example.ui.viewmodel.TutorHistoryScope
import com.example.ui.viewmodel.TutorViewModel

/**
 * Activité principale de Devoirs TN.
 * Gère l'initialisation Firebase App Check, la langue dynamique (FR / AR RTL / EN),
 * le mode sombre (Système / Clair / Sombre) et la navigation adaptative mobile / tablette.
 */
class MainActivity : ComponentActivity() {

    private val database by lazy {
        DevoirsDatabase.getInstance(applicationContext)
    }

    private val appPreferences by lazy {
        AppPreferencesRepository(applicationContext)
    }

    private val remoteCatalogueService by lazy {
        RemoteCatalogueService(applicationContext)
    }

    private val pdfTextExtractor by lazy {
        PdfTextExtractor(
            appContext = applicationContext,
            contentDao = database.devoirPdfContentDao()
        )
    }

    private val pdfDownloadCoordinator by lazy {
        PdfDownloadCoordinator(
            context = applicationContext,
            devoirDao = database.devoirDao(),
            pdfContentDao = database.devoirPdfContentDao()
        )
    }

    private val repository by lazy {
        DevoirRepository(
            devoirDao = database.devoirDao(),
            starredIdeaDao = database.starredIdeaDao(),
            remoteCatalogueService = remoteCatalogueService,
            pdfContentDao = database.devoirPdfContentDao(),
            pdfTextExtractor = pdfTextExtractor
        )
    }

    private val tutorRepository by lazy {
        TutorAiRepository(applicationContext, database.tutorMessageDao())
    }

    private val progressRepository by lazy {
        ProgressRepository(
            examAttemptDao = database.examAttemptDao(),
            examReminderDao = database.examReminderDao(),
            qcmResultDao = database.qcmResultDao(),
            reminderScheduler = ExamReminderScheduler(applicationContext)
        )
    }

    private val subjectRepository by lazy {
        SubjectRepository(database.subjectDao())
    }

    private val bacConfig by lazy {
        BacConfigLoader.load(applicationContext)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // ÉTAPE 1 : Configurer Firebase App Check avant l'UI, les ViewModels ou tout appel Firebase
        configureAppCheck(this, intent)

        enableEdgeToEdge()
        setContent {
            val preferences by appPreferences.state.collectAsStateWithLifecycle()
            val systemDark = isSystemInDarkTheme()
            val useDarkTheme = when (preferences.themeMode) {
                ThemeMode.SYSTEM -> systemDark
                ThemeMode.LIGHT -> false
                ThemeMode.DARK -> true
            }

            // Synchronise dynamiquement la couleur des icônes des barres système avec le thème actif
            DisposableEffect(useDarkTheme) {
                enableEdgeToEdge(
                    statusBarStyle = if (useDarkTheme) {
                        SystemBarStyle.dark(AndroidColor.TRANSPARENT)
                    } else {
                        SystemBarStyle.light(AndroidColor.TRANSPARENT, AndroidColor.TRANSPARENT)
                    },
                    navigationBarStyle = if (useDarkTheme) {
                        SystemBarStyle.dark(AndroidColor.TRANSPARENT)
                    } else {
                        SystemBarStyle.light(AndroidColor.TRANSPARENT, AndroidColor.TRANSPARENT)
                    }
                )
                onDispose {}
            }

            // Construit un Context et une Configuration localisés à chaud pour FR / AR / EN
            val baseContext = LocalContext.current
            val baseConfiguration = LocalConfiguration.current
            val localizedContext = remember(preferences.language, baseConfiguration) {
                val locale = preferences.language.toLocale()
                val config = Configuration(baseConfiguration).apply {
                    setLocale(locale)
                    setLayoutDirection(locale)
                }
                baseContext.createConfigurationContext(config)
            }
            val layoutDirection = if (preferences.language.isRtl) {
                LayoutDirection.Rtl
            } else {
                LayoutDirection.Ltr
            }

            CompositionLocalProvider(
                LocalContext provides localizedContext,
                LocalConfiguration provides localizedContext.resources.configuration,
                LocalLayoutDirection provides layoutDirection
            ) {
                DevoirsTNTheme(darkTheme = useDarkTheme) {
                    val viewModel: DevoirsViewModel = viewModel(
                        factory = DevoirsViewModel.Factory(repository, bacConfig)
                    )
                    val tutorViewModel: TutorViewModel = viewModel(
                        factory = TutorViewModel.Factory(tutorRepository)
                    )
                    val examModeViewModel: ExamModeViewModel = viewModel(
                        factory = ExamModeViewModel.Factory(progressRepository)
                    )
                    val progressViewModel: ProgressTrackingViewModel = viewModel(
                        factory = ProgressTrackingViewModel.Factory(progressRepository)
                    )
                    val flashQcmViewModel: FlashQcmViewModel = viewModel(
                        factory = FlashQcmViewModel.Factory(progressRepository)
                    )
                    val subjectsViewModel: SubjectsViewModel = viewModel(
                        factory = SubjectsViewModel.Factory(subjectRepository)
                    )
                    val storageViewModel: StorageViewModel = viewModel(
                        factory = StorageViewModel.Factory(pdfDownloadCoordinator)
                    )
                    DevoirsTNApp(
                        preferences = preferences,
                        onSelectLanguage = appPreferences::setLanguage,
                        onSelectThemeMode = appPreferences::setThemeMode,
                        onUpdateWeeklyGoal = appPreferences::setWeeklyGoalTarget,
                        onToggleAllowMobileData = appPreferences::setAllowMobileDataDownloads,
                        onRecordCatalogueSync = appPreferences::recordCatalogueSync,
                        viewModel = viewModel,
                        subjectsViewModel = subjectsViewModel,
                        tutorViewModel = tutorViewModel,
                        examModeViewModel = examModeViewModel,
                        progressViewModel = progressViewModel,
                        flashQcmViewModel = flashQcmViewModel,
                        storageViewModel = storageViewModel
                    )
                }
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
        labelRes = R.string.nav_methodes,
        selectedIcon = Icons.Filled.AutoStories,
        unselectedIcon = Icons.Outlined.AutoStories
    ),
    NavItemSpec(
        tab = MainTab.SUIVI,
        labelRes = R.string.nav_suivi,
        selectedIcon = Icons.Filled.Bookmark,
        unselectedIcon = Icons.Outlined.BookmarkBorder
    )
)

@Composable
fun DevoirsTNApp(
    preferences: AppPreferencesState,
    onSelectLanguage: (AppLanguage) -> Unit,
    onSelectThemeMode: (ThemeMode) -> Unit,
    onUpdateWeeklyGoal: (Int) -> Unit,
    onToggleAllowMobileData: (Boolean) -> Unit,
    onRecordCatalogueSync: (Int, Long) -> Unit,
    viewModel: DevoirsViewModel,
    subjectsViewModel: SubjectsViewModel,
    tutorViewModel: TutorViewModel,
    examModeViewModel: ExamModeViewModel,
    progressViewModel: ProgressTrackingViewModel,
    flashQcmViewModel: FlashQcmViewModel,
    storageViewModel: StorageViewModel
) {
    val context = LocalContext.current
    val selectedTab by viewModel.selectedTab.collectAsStateWithLifecycle()
    val allDevoirs by viewModel.allDevoirs.collectAsStateWithLifecycle()
    val filteredDevoirs by viewModel.filteredDevoirs.collectAsStateWithLifecycle()
    val searchSnippetsByDevoirId by viewModel.searchSnippetsByDevoirId.collectAsStateWithLifecycle()
    val filterState by viewModel.filterState.collectAsStateWithLifecycle()
    val availableSubjects by viewModel.availableSubjects.collectAsStateWithLifecycle()
    val catalogueSyncStatus by viewModel.catalogueSyncStatus.collectAsStateWithLifecycle()
    val subjectsUiState by subjectsViewModel.uiState.collectAsStateWithLifecycle()
    val selectedDevoir by viewModel.selectedDevoir.collectAsStateWithLifecycle()
    val timerState by viewModel.timerState.collectAsStateWithLifecycle()
    val moyenneState by viewModel.moyenneState.collectAsStateWithLifecycle()
    val starredIdeaIds by viewModel.starredIdeaIds.collectAsStateWithLifecycle()
    val subjectStats by viewModel.subjectProgressStats.collectAsStateWithLifecycle()
    val tutorUiState by tutorViewModel.uiState.collectAsStateWithLifecycle()
    val tutorMessages by tutorViewModel.messages.collectAsStateWithLifecycle()
    val activeDevoirExplanationCount by tutorViewModel.activeDevoirExplanationCount.collectAsStateWithLifecycle()
    val examModeState by examModeViewModel.uiState.collectAsStateWithLifecycle()
    val progressState by progressViewModel.uiState.collectAsStateWithLifecycle()
    val qcmStatsByChapterId by flashQcmViewModel.statsByChapterId.collectAsStateWithLifecycle()
    val activeQcmSession by flashQcmViewModel.session.collectAsStateWithLifecycle()
    val downloadStates by storageViewModel.downloadStates.collectAsStateWithLifecycle()
    val totalUsedBytes by storageViewModel.totalUsedBytes.collectAsStateWithLifecycle()
    val pendingConfirmation by storageViewModel.pendingConfirmation.collectAsStateWithLifecycle()

    var openStorageSignal by remember { mutableIntStateOf(0) }

    // Demande non bloquante de la permission POST_NOTIFICATIONS sur Android 13+ pour les notifications WorkManager
    val notificationPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { /* Le téléchargement continue dans tous les cas, même sans notification */ }

    val ensureNotificationPermissionRequested = {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            val granted = ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.POST_NOTIFICATIONS
            ) == PackageManager.PERMISSION_GRANTED
            if (!granted) {
                notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
            }
        }
    }

    LaunchedEffect(selectedDevoir?.id) {
        examModeViewModel.bindDevoir(selectedDevoir)
        tutorViewModel.bindDevoir(selectedDevoir?.id)
    }

    // Met à jour l'espace disque utilisé dès qu'un téléchargement se termine ou qu'un devoir change
    LaunchedEffect(allDevoirs, downloadStates) {
        storageViewModel.refreshDiskUsage()
    }

    // Enregistre la version et la date du catalogue synchronisé dans les préférences locales
    LaunchedEffect(catalogueSyncStatus.catalogueVersion, catalogueSyncStatus.syncedAtMillis) {
        if (catalogueSyncStatus.catalogueVersion > 0 && catalogueSyncStatus.syncedAtMillis > 0L) {
            onRecordCatalogueSync(catalogueSyncStatus.catalogueVersion, catalogueSyncStatus.syncedAtMillis)
        }
    }

    Surface(
        modifier = Modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.background
    ) {
        val activeDevoir = selectedDevoir
        if (activeDevoir != null) {
            DevoirDetailScreen(
                devoir = activeDevoir,
                downloadProgress = downloadStates[activeDevoir.id],
                hasResumablePart = storageViewModel.hasResumablePart(activeDevoir.id),
                searchQuery = filterState.searchQuery,
                timerState = timerState,
                examModeState = examModeState,
                tutorUiState = tutorUiState,
                tutorMessages = tutorMessages,
                devoirExplanationCount = activeDevoirExplanationCount,
                onBack = {
                    examModeViewModel.cancelExamMode()
                    tutorViewModel.closeTutor()
                    viewModel.closeDevoirDetail()
                },
                onToggleFavorite = { viewModel.toggleFavorite(activeDevoir.id) },
                onToggleCompleted = { viewModel.toggleCompleted(activeDevoir.id) },
                onDownloadDevoir = {
                    ensureNotificationPermissionRequested()
                    storageViewModel.requestDownloadDevoir(
                        devoir = activeDevoir,
                        allowMobileData = preferences.allowMobileDataDownloads
                    )
                },
                onCancelDownload = { storageViewModel.cancelDevoirDownload(activeDevoir.id) },
                onDeleteDownload = { storageViewModel.deleteDevoirDownload(activeDevoir.id) },
                onToggleTimer = viewModel::toggleTimer,
                onResetTimer = viewModel::resetTimer,
                onStartExamMode = { examModeViewModel.startRealConditionsExam(activeDevoir) },
                onFinishExamMode = examModeViewModel::finishExamAndOpenEvaluation,
                onCancelExamMode = examModeViewModel::cancelExamMode,
                onUpdateExamScore = examModeViewModel::updateExerciseScore,
                onSaveExamEvaluation = {
                    examModeViewModel.saveSelfEvaluation {
                        if (!activeDevoir.isCompleted) {
                            viewModel.toggleCompleted(activeDevoir.id)
                        }
                    }
                },
                onDismissExamSummary = examModeViewModel::dismissSavedSummary,
                onExplainExerciseWithAi = { exercise ->
                    tutorViewModel.openTutor(
                        devoir = activeDevoir,
                        exercise = exercise,
                        autoTriggerMode = TutorHelpMode.EXPLAIN
                    )
                },
                onOpenTutorChat = { exercise ->
                    tutorViewModel.openTutor(
                        devoir = activeDevoir,
                        exercise = exercise,
                        autoTriggerMode = null
                    )
                },
                onOpenDevoirTutorHistory = { exercise ->
                    tutorViewModel.openTutor(
                        devoir = activeDevoir,
                        exercise = exercise,
                        autoTriggerMode = null,
                        initialHistoryScope = TutorHistoryScope.ALL_DEVOIR
                    )
                },
                onExplainPdfPage = { pageNumber, kindLabel, pageBitmap ->
                    tutorViewModel.openTutorForPdfPage(
                        devoir = activeDevoir,
                        pageNumber = pageNumber,
                        documentKindLabel = kindLabel,
                        pageBitmap = pageBitmap,
                        autoTriggerMode = TutorHelpMode.EXPLAIN
                    )
                },
                onDismissTutor = tutorViewModel::closeTutor,
                onSelectTutorLanguage = tutorViewModel::selectLanguage,
                onSelectTutorHistoryScope = tutorViewModel::selectHistoryScope,
                onTriggerTutorMode = tutorViewModel::triggerHelpMode,
                onTutorQuestionChange = tutorViewModel::updateQuestionDraft,
                onSendTutorQuestion = tutorViewModel::sendQuestion,
                onRetryTutor = tutorViewModel::retryLastRequest,
                onClearTutorHistory = tutorViewModel::clearHistory
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
                                    label = {
                                        Text(
                                            text = stringResource(item.labelRes),
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                    }
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
                                preferences = preferences,
                                onSelectLanguage = onSelectLanguage,
                                onSelectThemeMode = onSelectThemeMode,
                                onUpdateWeeklyGoal = onUpdateWeeklyGoal,
                                onToggleAllowMobileData = onToggleAllowMobileData,
                                onEnsureNotificationPermission = ensureNotificationPermissionRequested,
                                onOpenStorageShortcut = {
                                    openStorageSignal += 1
                                    viewModel.selectTab(MainTab.SUIVI)
                                },
                                openStorageSignal = openStorageSignal,
                                modifier = Modifier.widthIn(max = 820.dp),
                                allDevoirs = allDevoirs,
                                filteredDevoirs = filteredDevoirs,
                                searchSnippetsByDevoirId = searchSnippetsByDevoirId,
                                filterState = filterState,
                                availableSubjects = availableSubjects,
                                catalogueSyncStatus = catalogueSyncStatus,
                                subjectsUiState = subjectsUiState,
                                moyenneState = moyenneState,
                                starredIdeaIds = starredIdeaIds,
                                subjectStats = subjectStats,
                                progressState = progressState,
                                qcmStatsByChapterId = qcmStatsByChapterId,
                                activeQcmSession = activeQcmSession,
                                downloadStates = downloadStates,
                                totalUsedBytes = totalUsedBytes,
                                viewModel = viewModel,
                                subjectsViewModel = subjectsViewModel,
                                progressViewModel = progressViewModel,
                                flashQcmViewModel = flashQcmViewModel,
                                storageViewModel = storageViewModel
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
                                                maxLines = 1,
                                                overflow = TextOverflow.Ellipsis
                                            )
                                        }
                                    )
                                }
                            }
                        }
                    ) { innerPadding ->
                        TabContentContainer(
                            selectedTab = selectedTab,
                            preferences = preferences,
                            onSelectLanguage = onSelectLanguage,
                            onSelectThemeMode = onSelectThemeMode,
                            onUpdateWeeklyGoal = onUpdateWeeklyGoal,
                            onToggleAllowMobileData = onToggleAllowMobileData,
                            onEnsureNotificationPermission = ensureNotificationPermissionRequested,
                            onOpenStorageShortcut = {
                                openStorageSignal += 1
                                viewModel.selectTab(MainTab.SUIVI)
                            },
                            openStorageSignal = openStorageSignal,
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(innerPadding),
                            allDevoirs = allDevoirs,
                            filteredDevoirs = filteredDevoirs,
                            searchSnippetsByDevoirId = searchSnippetsByDevoirId,
                            filterState = filterState,
                            availableSubjects = availableSubjects,
                            catalogueSyncStatus = catalogueSyncStatus,
                            subjectsUiState = subjectsUiState,
                            moyenneState = moyenneState,
                            starredIdeaIds = starredIdeaIds,
                            subjectStats = subjectStats,
                            progressState = progressState,
                            qcmStatsByChapterId = qcmStatsByChapterId,
                            activeQcmSession = activeQcmSession,
                            downloadStates = downloadStates,
                            totalUsedBytes = totalUsedBytes,
                            viewModel = viewModel,
                            subjectsViewModel = subjectsViewModel,
                            progressViewModel = progressViewModel,
                            flashQcmViewModel = flashQcmViewModel,
                            storageViewModel = storageViewModel
                        )
                    }
                }
            }
        }

        // Dialogues globaux : Wi-Fi requis par défaut et avertissement > 20 Mo en données mobiles
        DownloadConfirmationDialogs(
            pending = pendingConfirmation,
            onConfirmMobileData = { enablePreference ->
                storageViewModel.confirmMobileDataDownload(
                    alsoEnableMobileDataPreference = enablePreference,
                    onEnableMobileData = { onToggleAllowMobileData(true) }
                )
            },
            onQueueForWifiOnly = storageViewModel::queueForWifiOnly,
            onDismiss = storageViewModel::dismissPendingConfirmation
        )
    }
}

@Composable
private fun TabContentContainer(
    selectedTab: MainTab,
    preferences: AppPreferencesState,
    onSelectLanguage: (AppLanguage) -> Unit,
    onSelectThemeMode: (ThemeMode) -> Unit,
    onUpdateWeeklyGoal: (Int) -> Unit,
    onToggleAllowMobileData: (Boolean) -> Unit,
    onEnsureNotificationPermission: () -> Unit,
    onOpenStorageShortcut: () -> Unit,
    openStorageSignal: Int,
    allDevoirs: List<com.example.data.model.DevoirItem>,
    filteredDevoirs: List<com.example.data.model.DevoirItem>,
    searchSnippetsByDevoirId: Map<String, String>,
    filterState: com.example.ui.viewmodel.DevoirFilterState,
    availableSubjects: List<String>,
    catalogueSyncStatus: com.example.data.repository.CatalogueSyncStatus,
    subjectsUiState: SubjectsUiState,
    moyenneState: com.example.ui.viewmodel.MoyenneUiState,
    starredIdeaIds: Set<String>,
    subjectStats: List<com.example.ui.viewmodel.SubjectProgressStat>,
    progressState: com.example.ui.viewmodel.ProgressDashboardUiState,
    qcmStatsByChapterId: Map<String, com.example.ui.viewmodel.ChapterQcmStats>,
    activeQcmSession: com.example.ui.viewmodel.ActiveQcmSessionState,
    downloadStates: Map<String, com.example.data.download.DevoirDownloadProgress>,
    totalUsedBytes: Long,
    viewModel: DevoirsViewModel,
    subjectsViewModel: SubjectsViewModel,
    progressViewModel: ProgressTrackingViewModel,
    flashQcmViewModel: FlashQcmViewModel,
    storageViewModel: StorageViewModel,
    modifier: Modifier = Modifier
) {
    Crossfade(
        targetState = selectedTab,
        modifier = modifier,
        label = "main_tab_crossfade"
    ) { tab ->
        when (tab) {
            MainTab.MATIERES -> SubjectSelectionScreen(
                subjectsUiState = subjectsUiState,
                allDevoirs = allDevoirs,
                selectedYear = filterState.selectedLevel,
                selectedSubject = filterState.selectedSubject,
                onSelectYear = viewModel::selectLevel,
                onSelectSubject = viewModel::selectSubject,
                onAddCustomSubject = { name, subtitle ->
                    subjectsViewModel.addCustomSubject(name, subtitle)
                },
                onDeleteSubject = subjectsViewModel::deleteSubject,
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
                onSelectSortOption = viewModel::selectSortOption,
                onResetFilters = viewModel::resetFilters,
                onOpenDevoir = viewModel::openDevoir,
                onToggleFavorite = viewModel::toggleFavorite,
                allDevoirs = allDevoirs,
                searchSnippetsByDevoirId = searchSnippetsByDevoirId,
                downloadStates = downloadStates,
                hasResumablePart = storageViewModel::hasResumablePart,
                onDownloadDevoir = { devoir ->
                    onEnsureNotificationPermission()
                    storageViewModel.requestDownloadDevoir(
                        devoir = devoir,
                        allowMobileData = preferences.allowMobileDataDownloads
                    )
                },
                onCancelDownload = storageViewModel::cancelDevoirDownload,
                onDownloadPack = { pack, title ->
                    onEnsureNotificationPermission()
                    storageViewModel.requestDownloadPack(
                        pack = pack,
                        displayTitle = title,
                        allowMobileData = preferences.allowMobileDataDownloads
                    )
                },
                onOpenStorage = onOpenStorageShortcut
            )

            MainTab.MOYENNE -> MoyenneCalculatorScreen(
                state = moyenneState,
                onSelectSection = viewModel::selectBacSection,
                onSelectCalcTab = viewModel::selectBacCalcTab,
                onSelectTrimester = viewModel::selectBacTrimester,
                onUpdateAnnualPercent = viewModel::updateAnnualPercent,
                onResetWeights = viewModel::resetWeightsToDefault,
                onUpdateAnnualGrade = viewModel::updateAnnualGrade,
                onUpdateExamGrade = viewModel::updateExamGrade,
                onUpdateCustomFgCoeff = viewModel::updateCustomFgCoeff,
                onUpdateFgMultiplier = viewModel::updateFgGeneralMultiplier,
                onResetFgCoeffs = viewModel::resetFgCoeffs,
                onUpdateSimulatorTarget = viewModel::updateSimulatorTarget,
                onUpdateSimulatorCustomAnnual = viewModel::updateSimulatorCustomAnnual,
                onResetGrades = viewModel::resetBacGrades,
                onFillSampleGrades = viewModel::fillBacSampleGrades
            )

            MainTab.IDEES -> IdeasMethodsScreen(
                studyMethods = viewModel.studyMethodGuides,
                appIdeas = viewModel.appFeatureIdeas,
                starredIdeaIds = starredIdeaIds,
                onToggleStarIdea = viewModel::toggleStarIdea
            )

            MainTab.SUIVI -> MySpaceScreen(
                preferences = preferences,
                onSelectLanguage = onSelectLanguage,
                onSelectThemeMode = onSelectThemeMode,
                onUpdateWeeklyGoal = onUpdateWeeklyGoal,
                onOpenLibrary = { viewModel.selectTab(MainTab.DEVOIRS) },
                allDevoirs = allDevoirs,
                subjectStats = subjectStats,
                progressState = progressState,
                qcmChapters = flashQcmViewModel.chapters,
                qcmStatsByChapterId = qcmStatsByChapterId,
                activeQcmSession = activeQcmSession,
                onOpenDevoir = viewModel::openDevoir,
                onToggleFavorite = viewModel::toggleFavorite,
                onSelectChartSubject = progressViewModel::selectChartSubject,
                onAddReminder = progressViewModel::addExamReminder,
                onDeleteReminder = progressViewModel::deleteReminder,
                onDismissReminderFeedback = progressViewModel::clearReminderFeedback,
                onStartQcmChapter = flashQcmViewModel::startChapterQuiz,
                onStartQcmByChapterName = flashQcmViewModel::startChapterByName,
                onSelectQcmOption = flashQcmViewModel::selectOption,
                onNextQcmQuestion = flashQcmViewModel::nextQuestionOrFinish,
                onCloseQcm = flashQcmViewModel::closeQuiz,
                downloadStates = downloadStates,
                totalUsedBytes = totalUsedBytes,
                catalogueSyncStatus = catalogueSyncStatus,
                hasResumablePart = storageViewModel::hasResumablePart,
                onToggleAllowMobileData = onToggleAllowMobileData,
                onRefreshCatalogue = { viewModel.refreshCatalogue() },
                onDownloadDevoir = { devoir ->
                    onEnsureNotificationPermission()
                    storageViewModel.requestDownloadDevoir(
                        devoir = devoir,
                        allowMobileData = preferences.allowMobileDataDownloads
                    )
                },
                onCancelDownload = storageViewModel::cancelDevoirDownload,
                onDownloadPack = { pack, title ->
                    onEnsureNotificationPermission()
                    storageViewModel.requestDownloadPack(
                        pack = pack,
                        displayTitle = title,
                        allowMobileData = preferences.allowMobileDataDownloads
                    )
                },
                onCancelPack = storageViewModel::cancelPackDownload,
                onDeletePack = storageViewModel::deletePackDownloads,
                onDeleteDevoirDownload = storageViewModel::deleteDevoirDownload,
                onDeleteAllDownloads = storageViewModel::deleteAllDownloads,
                openStorageSignal = openStorageSignal
            )
        }
    }
}
