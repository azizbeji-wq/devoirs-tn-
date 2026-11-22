package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccessTime
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material.icons.filled.Flag
import androidx.compose.material.icons.filled.Grade
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.LibraryBooks
import androidx.compose.material.icons.filled.LocalFireDepartment
import androidx.compose.material.icons.filled.MenuBook
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material.icons.filled.Quiz
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material.icons.filled.School
import androidx.compose.material.icons.filled.SdStorage
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.TrendingUp
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SecondaryScrollableTabRow
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.R
import com.example.data.download.DevoirDownloadProgress
import com.example.data.download.DownloadPack
import com.example.data.local.AppLanguage
import com.example.data.local.AppPreferencesState
import com.example.data.local.ExamAttemptEntity
import com.example.data.local.ExerciseScoreCodec
import com.example.data.local.FlashQcmBank
import com.example.data.local.FlashQcmChapter
import com.example.data.local.ThemeMode
import com.example.data.model.DevoirItem
import com.example.data.repository.CatalogueSyncStatus
import com.example.domain.progress.ProgressAnalytics
import com.example.domain.progress.WeakChapterInsight
import com.example.ui.components.DevoirCard
import com.example.ui.components.FlashQcmBottomSheet
import com.example.ui.components.FlashQcmChapterCard
import com.example.ui.components.GradeEvolutionCard
import com.example.ui.components.RemindersSection
import com.example.ui.components.SettingsPrivacySection
import com.example.ui.components.adaptiveCrimson
import com.example.ui.components.adaptiveEmerald
import com.example.ui.components.localizedSubjectName
import com.example.ui.components.subjectAccentColor
import com.example.ui.viewmodel.ActiveQcmSessionState
import com.example.ui.viewmodel.ChapterQcmStats
import com.example.ui.viewmodel.CloudSyncUiState
import com.example.ui.viewmodel.ProgressDashboardUiState
import com.example.ui.viewmodel.SubjectProgressStat
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

private enum class SpaceSectionTab(
    val id: String,
    val labelRes: Int,
    val icon: ImageVector,
    val contentDescriptionRes: Int
) {
    PROGRESSION(
        id = "progression",
        labelRes = R.string.space_tab_progression,
        icon = Icons.Filled.TrendingUp,
        contentDescriptionRes = R.string.cd_tab_progression
    ),
    QCM(
        id = "qcm",
        labelRes = R.string.space_tab_qcm,
        icon = Icons.Filled.Quiz,
        contentDescriptionRes = R.string.cd_tab_qcm
    ),
    RAPPELS(
        id = "rappels",
        labelRes = R.string.space_tab_rappels,
        icon = Icons.Filled.NotificationsActive,
        contentDescriptionRes = R.string.cd_tab_rappels
    ),
    BIBLIO(
        id = "biblio",
        labelRes = R.string.space_tab_biblio,
        icon = Icons.Filled.Bookmark,
        contentDescriptionRes = R.string.cd_tab_favoris
    ),
    STOCKAGE(
        id = "stockage",
        labelRes = R.string.space_tab_storage,
        icon = Icons.Filled.SdStorage,
        contentDescriptionRes = R.string.cd_tab_storage
    ),
    PARAMETRES(
        id = "parametres",
        labelRes = R.string.space_tab_settings,
        icon = Icons.Filled.Settings,
        contentDescriptionRes = R.string.cd_tab_settings
    )
}

/**
 * Écran Mon Espace : progression, objectif hebdomadaire, QCM Flash, rappels DC/DS,
 * favoris et paramètres (langue / thème / confidentialité).
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MySpaceScreen(
    preferences: AppPreferencesState,
    onSelectLanguage: (AppLanguage) -> Unit,
    onSelectThemeMode: (ThemeMode) -> Unit,
    onUpdateWeeklyGoal: (Int) -> Unit,
    onOpenLibrary: () -> Unit,
    allDevoirs: List<DevoirItem>,
    subjectStats: List<SubjectProgressStat>,
    progressState: ProgressDashboardUiState,
    qcmChapters: List<FlashQcmChapter>,
    qcmStatsByChapterId: Map<String, ChapterQcmStats>,
    activeQcmSession: ActiveQcmSessionState,
    onOpenDevoir: (DevoirItem) -> Unit,
    onToggleFavorite: (String) -> Unit,
    onSelectChartSubject: (String) -> Unit,
    onAddReminder: (title: String, subject: String, examType: String, examDateMillis: Long) -> Unit,
    onDeleteReminder: (Long) -> Unit,
    onDismissReminderFeedback: () -> Unit,
    onStartQcmChapter: (FlashQcmChapter) -> Unit,
    onStartQcmByChapterName: (String) -> Boolean,
    onSelectQcmOption: (Int) -> Unit,
    onNextQcmQuestion: () -> Unit,
    onCloseQcm: () -> Unit,
    modifier: Modifier = Modifier,
    downloadStates: Map<String, DevoirDownloadProgress> = emptyMap(),
    totalUsedBytes: Long = 0L,
    catalogueSyncStatus: CatalogueSyncStatus = CatalogueSyncStatus(),
    hasResumablePart: (String) -> Boolean = { false },
    onToggleAllowMobileData: (Boolean) -> Unit = {},
    onRefreshCatalogue: () -> Unit = {},
    onDownloadDevoir: (DevoirItem) -> Unit = {},
    onCancelDownload: (String) -> Unit = {},
    onDownloadPack: (DownloadPack, String) -> Unit = { _, _ -> },
    onCancelPack: (String) -> Unit = {},
    onDeletePack: (DownloadPack) -> Unit = {},
    onDeleteDevoirDownload: (String) -> Unit = {},
    onDeleteAllDownloads: () -> Unit = {},
    onToggleAiTutorEnabled: (Boolean) -> Unit = {},
    openStorageSignal: Int = 0,
    userDisplayName: String? = null,
    userEmail: String? = null,
    cloudSyncState: CloudSyncUiState = CloudSyncUiState.Loading,
    onSignOut: (() -> Unit)? = null,
    isDeletingAccount: Boolean = false,
    deleteAccountError: String? = null,
    onDeleteAccountAndData: (() -> Unit)? = null,
    onDismissDeleteAccountError: () -> Unit = {},
    onReviewParentalConsent: (() -> Unit)? = null
) {
    val favoriteDevoirs = allDevoirs.filter { it.isFavorite }
    val completedDevoirs = allDevoirs.filter { it.isCompleted }
    var selectedSection by rememberSaveable { mutableStateOf(SpaceSectionTab.PROGRESSION) }
    var showWeeklyGoalDialog by rememberSaveable { mutableStateOf(false) }

    LaunchedEffect(openStorageSignal) {
        if (openStorageSignal > 0) {
            selectedSection = SpaceSectionTab.STOCKAGE
        }
    }

    val emerald = adaptiveEmerald()
    val crimson = adaptiveCrimson()

    // Détermine si l'élève possède déjà des données d'activité ou de progression
    val hasProgressData = progressState.totalAttemptsCount > 0 ||
        progressState.totalQcmCount > 0 ||
        completedDevoirs.isNotEmpty()
    val weeklyCompletedCount = maxOf(progressState.weeklyAttemptsCount, completedDevoirs.size)

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .testTag("my_space_list"),
        contentPadding = PaddingValues(bottom = 28.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        // En-tête avec accès rapide aux paramètres (langue / thème / confidentialité)
        item(key = "space_header") {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = stringResource(R.string.suivi_title),
                        style = MaterialTheme.typography.headlineMedium,
                        color = MaterialTheme.colorScheme.onBackground
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = stringResource(R.string.suivi_subtitle),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                IconButton(
                    onClick = { selectedSection = SpaceSectionTab.PARAMETRES },
                    modifier = Modifier
                        .minimumInteractiveComponentSize()
                        .testTag("quick_settings_btn")
                ) {
                    Icon(
                        imageVector = Icons.Filled.Settings,
                        contentDescription = stringResource(R.string.cd_open_settings),
                        tint = if (selectedSection == SpaceSectionTab.PARAMETRES) {
                            MaterialTheme.colorScheme.primary
                        } else {
                            MaterialTheme.colorScheme.onSurfaceVariant
                        }
                    )
                }
            }
        }

        // Barre d'onglets défilante avec indicateur pour rendre visibles toutes les sections
        item(key = "space_section_tabs") {
            SecondaryScrollableTabRow(
                selectedTabIndex = selectedSection.ordinal,
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("space_scrollable_tabs"),
                edgePadding = 12.dp,
                containerColor = MaterialTheme.colorScheme.surface,
                contentColor = MaterialTheme.colorScheme.primary
            ) {
                SpaceSectionTab.entries.forEach { tab ->
                    val isSelected = selectedSection == tab
                    Tab(
                        selected = isSelected,
                        onClick = { selectedSection = tab },
                        modifier = Modifier
                            .minimumInteractiveComponentSize()
                            .testTag("space_tab_${tab.id}"),
                        selectedContentColor = MaterialTheme.colorScheme.primary,
                        unselectedContentColor = MaterialTheme.colorScheme.onSurfaceVariant,
                        text = {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Icon(
                                    imageVector = tab.icon,
                                    contentDescription = stringResource(tab.contentDescriptionRes),
                                    modifier = Modifier.size(18.dp)
                                )
                                Text(
                                    text = stringResource(tab.labelRes),
                                    style = MaterialTheme.typography.labelLarge,
                                    maxLines = 1,
                                    softWrap = false,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                        }
                    )
                }
            }
        }

        when (selectedSection) {
            SpaceSectionTab.PROGRESSION -> {
                // 1. État vide motivant si aucune activité n'est encore enregistrée
                if (!hasProgressData) {
                    item(key = "motivating_empty_state") {
                        MotivatingEmptyProgressCard(
                            onStartDevoir = onOpenLibrary,
                            onStartQcm = { selectedSection = SpaceSectionTab.QCM },
                            modifier = Modifier.padding(horizontal = 16.dp)
                        )
                    }
                }

                // 2. Carte « Objectif de la semaine » modifiable avec barre de progression
                item(key = "weekly_goal_card") {
                    WeeklyGoalCard(
                        completedCount = weeklyCompletedCount,
                        targetCount = preferences.weeklyGoalTarget,
                        onEditClick = { showWeeklyGoalDialog = true },
                        modifier = Modifier.padding(horizontal = 16.dp)
                    )
                }

                // 3. KPI Série + Temps travaillé + Moyenne essais (message motivant si à zéro)
                item(key = "progress_kpis") {
                    val streakDays = progressState.streak.currentStreakDays
                    val studySeconds = progressState.totalStudySeconds
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        StatKpiCard(
                            icon = Icons.Filled.LocalFireDepartment,
                            iconContentDescription = stringResource(R.string.cd_streak_icon),
                            value = if (streakDays == 0) {
                                stringResource(R.string.stat_start_today)
                            } else {
                                stringResource(R.string.streak_days_value, streakDays)
                            },
                            label = stringResource(R.string.streak_label),
                            accent = if (streakDays == 0) MaterialTheme.colorScheme.primary else crimson,
                            isEncouragement = streakDays == 0,
                            modifier = Modifier
                                .weight(1f)
                                .testTag("kpi_streak")
                        )
                        StatKpiCard(
                            icon = Icons.Filled.AccessTime,
                            iconContentDescription = stringResource(R.string.cd_study_time_icon),
                            value = if (studySeconds == 0L) {
                                stringResource(R.string.stat_start_today)
                            } else {
                                ProgressAnalytics.formatStudyDuration(studySeconds)
                            },
                            label = stringResource(R.string.study_time_label),
                            accent = MaterialTheme.colorScheme.primary,
                            isEncouragement = studySeconds == 0L,
                            modifier = Modifier
                                .weight(1f)
                                .testTag("kpi_study_time")
                        )
                        StatKpiCard(
                            icon = Icons.Filled.Grade,
                            iconContentDescription = stringResource(R.string.cd_avg_grade_icon),
                            value = progressState.averageGradeOn20?.let { "%.1f".format(it) } ?: "—",
                            label = stringResource(R.string.avg_grade_label),
                            accent = emerald,
                            modifier = Modifier
                                .weight(1f)
                                .testTag("kpi_avg_grade")
                        )
                    }
                }

                // 4. Graphique Canvas d'évolution des notes (affiché dès qu'il y a des données)
                if (hasProgressData) {
                    item(key = "grade_evolution_chart") {
                        GradeEvolutionCard(
                            subjects = progressState.availableChartSubjects,
                            selectedSubject = progressState.selectedChartSubject,
                            points = progressState.chartPoints,
                            onSelectSubject = onSelectChartSubject,
                            modifier = Modifier.padding(horizontal = 16.dp)
                        )
                    }
                }

                // 5. Chapitres à renforcer (icône neutre, couleur d'alerte uniquement si chapitre faible)
                item(key = "weak_chapters_card") {
                    WeakChaptersCard(
                        weakChapters = progressState.weakChapters,
                        onPracticeChapter = { chapterName ->
                            if (!onStartQcmByChapterName(chapterName)) {
                                selectedSection = SpaceSectionTab.QCM
                            }
                        },
                        modifier = Modifier.padding(horizontal = 16.dp)
                    )
                }

                // 6. Historique des tentatives en conditions réelles
                if (progressState.recentAttempts.isNotEmpty()) {
                    item(key = "recent_attempts_title") {
                        Row(
                            modifier = Modifier.padding(horizontal = 16.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Filled.History,
                                contentDescription = stringResource(R.string.cd_history_icon),
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(20.dp)
                            )
                            Text(
                                text = stringResource(R.string.recent_attempts_title),
                                style = MaterialTheme.typography.titleLarge
                            )
                        }
                    }

                    items(progressState.recentAttempts, key = { "attempt_${it.id}" }) { attempt ->
                        ExamAttemptHistoryCard(
                            attempt = attempt,
                            locale = preferences.language.toLocale(),
                            modifier = Modifier.padding(horizontal = 16.dp)
                        )
                    }
                }
            }

            SpaceSectionTab.QCM -> {
                item(key = "qcm_header") {
                    Text(
                        text = stringResource(R.string.qcm_section_title),
                        style = MaterialTheme.typography.titleLarge,
                        modifier = Modifier.padding(horizontal = 16.dp)
                    )
                }

                items(qcmChapters, key = { it.id }) { chapter ->
                    FlashQcmChapterCard(
                        chapter = chapter,
                        stats = qcmStatsByChapterId[chapter.id],
                        onStart = { onStartQcmChapter(chapter) },
                        modifier = Modifier.padding(horizontal = 16.dp)
                    )
                }
            }

            SpaceSectionTab.RAPPELS -> {
                item(key = "reminders_section") {
                    RemindersSection(
                        reminders = progressState.reminders,
                        feedbackMessage = progressState.lastReminderFeedback,
                        onAddReminder = onAddReminder,
                        onDeleteReminder = onDeleteReminder,
                        onDismissFeedback = onDismissReminderFeedback,
                        modifier = Modifier.padding(horizontal = 16.dp)
                    )
                }
            }

            SpaceSectionTab.BIBLIO -> {
                // KPI Favoris & Devoirs terminés
                item(key = "kpi_row") {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        StatKpiCard(
                            icon = Icons.Filled.CheckCircle,
                            iconContentDescription = stringResource(R.string.cd_completed_icon),
                            value = completedDevoirs.size.toString(),
                            label = stringResource(R.string.stat_completed),
                            accent = emerald,
                            modifier = Modifier.weight(1f)
                        )
                        StatKpiCard(
                            icon = Icons.Filled.Bookmark,
                            iconContentDescription = stringResource(R.string.cd_favorites_icon),
                            value = favoriteDevoirs.size.toString(),
                            label = stringResource(R.string.stat_favorites),
                            accent = crimson,
                            modifier = Modifier.weight(1f)
                        )
                        StatKpiCard(
                            icon = Icons.Filled.LibraryBooks,
                            iconContentDescription = stringResource(R.string.cd_total_devoirs_icon),
                            value = allDevoirs.size.toString(),
                            label = stringResource(R.string.stat_total),
                            accent = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.weight(1f)
                        )
                    }
                }

                // Progression par matière
                if (subjectStats.isNotEmpty()) {
                    item(key = "subject_progress_card") {
                        Surface(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp),
                            shape = MaterialTheme.shapes.large,
                            color = MaterialTheme.colorScheme.surface,
                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
                        ) {
                            Column(
                                modifier = Modifier.padding(16.dp),
                                verticalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                Text(
                                    text = stringResource(R.string.section_subject_progress),
                                    style = MaterialTheme.typography.titleMedium
                                )
                                subjectStats.forEach { stat ->
                                    val ratio = if (stat.total > 0) stat.completed.toFloat() / stat.total else 0f
                                    val barColor = subjectAccentColor(stat.subject)
                                    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceBetween
                                        ) {
                                            Text(
                                                text = localizedSubjectName(stat.subject),
                                                style = MaterialTheme.typography.bodyMedium,
                                                fontWeight = FontWeight.Medium
                                            )
                                            Text(
                                                text = "${stat.completed}/${stat.total}",
                                                style = MaterialTheme.typography.labelMedium,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                        }
                                        LinearProgressIndicator(
                                            progress = { ratio },
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .height(7.dp)
                                                .clip(RoundedCornerShape(4.dp)),
                                            color = barColor,
                                            trackColor = MaterialTheme.colorScheme.surfaceVariant
                                        )
                                    }
                                }
                            }
                        }
                    }
                }

                // Favoris
                item(key = "favorites_title") {
                    Text(
                        text = stringResource(R.string.section_favorites),
                        style = MaterialTheme.typography.titleLarge,
                        modifier = Modifier.padding(horizontal = 16.dp)
                    )
                }

                if (favoriteDevoirs.isEmpty()) {
                    item(key = "empty_favorites") {
                        EmptyHintBox(
                            text = stringResource(R.string.empty_favorites),
                            modifier = Modifier.padding(horizontal = 16.dp)
                        )
                    }
                } else {
                    items(favoriteDevoirs, key = { "fav_${it.id}" }) { devoir ->
                        DevoirCard(
                            devoir = devoir,
                            downloadProgress = downloadStates[devoir.id],
                            hasResumablePart = hasResumablePart(devoir.id),
                            onDownloadClick = { onDownloadDevoir(devoir) },
                            onCancelDownloadClick = { onCancelDownload(devoir.id) },
                            onClick = { onOpenDevoir(devoir) },
                            onToggleFavorite = { onToggleFavorite(devoir.id) },
                            modifier = Modifier.padding(horizontal = 16.dp)
                        )
                    }
                }

                // Devoirs terminés
                item(key = "completed_title") {
                    Text(
                        text = stringResource(R.string.section_completed),
                        style = MaterialTheme.typography.titleLarge,
                        modifier = Modifier.padding(horizontal = 16.dp)
                    )
                }

                if (completedDevoirs.isEmpty()) {
                    item(key = "empty_completed") {
                        EmptyHintBox(
                            text = stringResource(R.string.empty_completed),
                            modifier = Modifier.padding(horizontal = 16.dp)
                        )
                    }
                } else {
                    items(completedDevoirs, key = { "done_${it.id}" }) { devoir ->
                        DevoirCard(
                            devoir = devoir,
                            downloadProgress = downloadStates[devoir.id],
                            hasResumablePart = hasResumablePart(devoir.id),
                            onDownloadClick = { onDownloadDevoir(devoir) },
                            onCancelDownloadClick = { onCancelDownload(devoir.id) },
                            onClick = { onOpenDevoir(devoir) },
                            onToggleFavorite = { onToggleFavorite(devoir.id) },
                            modifier = Modifier.padding(horizontal = 16.dp)
                        )
                    }
                }
            }

            SpaceSectionTab.STOCKAGE -> {
                item(key = "storage_management_panel") {
                    StorageManagementSection(
                        allDevoirs = allDevoirs,
                        downloadStates = downloadStates,
                        totalUsedBytes = totalUsedBytes,
                        allowMobileData = preferences.allowMobileDataDownloads,
                        catalogueSyncStatus = catalogueSyncStatus,
                        onToggleAllowMobileData = onToggleAllowMobileData,
                        onRefreshCatalogue = onRefreshCatalogue,
                        onDownloadPack = onDownloadPack,
                        onCancelPack = onCancelPack,
                        onDeletePack = onDeletePack,
                        onDeleteDevoir = onDeleteDevoirDownload,
                        onDeleteAll = onDeleteAllDownloads,
                        onOpenDevoir = onOpenDevoir,
                        modifier = Modifier.padding(horizontal = 16.dp)
                    )
                }
            }

            SpaceSectionTab.PARAMETRES -> {
                item(key = "settings_privacy_panel") {
                    SettingsPrivacySection(
                        preferences = preferences,
                        onSelectLanguage = onSelectLanguage,
                        onSelectThemeMode = onSelectThemeMode,
                        onToggleAiTutorEnabled = onToggleAiTutorEnabled,
                        userDisplayName = userDisplayName,
                        userEmail = userEmail,
                        cloudSyncState = cloudSyncState,
                        onSignOut = onSignOut,
                        isDeletingAccount = isDeletingAccount,
                        deleteAccountError = deleteAccountError,
                        onDeleteAccountAndData = onDeleteAccountAndData,
                        onDismissDeleteAccountError = onDismissDeleteAccountError,
                        onReviewParentalConsent = onReviewParentalConsent,
                        modifier = Modifier.padding(horizontal = 16.dp)
                    )
                }
            }
        }
    }

    if (showWeeklyGoalDialog) {
        EditWeeklyGoalDialog(
            currentGoal = preferences.weeklyGoalTarget,
            onDismiss = { showWeeklyGoalDialog = false },
            onSave = { newGoal ->
                onUpdateWeeklyGoal(newGoal)
                showWeeklyGoalDialog = false
            }
        )
    }

    if (activeQcmSession.isOpen) {
        FlashQcmBottomSheet(
            session = activeQcmSession,
            onSelectOption = onSelectQcmOption,
            onNextOrFinish = onNextQcmQuestion,
            onDismiss = onCloseQcm
        )
    }
}

/**
 * Carte d'accueil motivante affichée lorsque l'élève n'a pas encore de données de progression.
 * Propose deux actions directes : démarrer un devoir ou lancer un QCM flash.
 */
@Composable
private fun MotivatingEmptyProgressCard(
    onStartDevoir: () -> Unit,
    onStartQcm: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier
            .fillMaxWidth()
            .testTag("motivating_empty_card"),
        shape = MaterialTheme.shapes.large,
        color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.55f),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.28f))
    ) {
        Column(
            modifier = Modifier.padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Surface(
                    shape = RoundedCornerShape(14.dp),
                    color = MaterialTheme.colorScheme.primary.copy(alpha = 0.14f)
                ) {
                    Icon(
                        imageVector = Icons.Filled.EmojiEvents,
                        contentDescription = stringResource(R.string.cd_welcome_trophy),
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier
                            .padding(10.dp)
                            .size(28.dp)
                    )
                }
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = stringResource(R.string.space_empty_welcome_title),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = stringResource(R.string.space_empty_welcome_body),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Button(
                    onClick = onStartDevoir,
                    modifier = Modifier
                        .fillMaxWidth()
                        .minimumInteractiveComponentSize()
                        .testTag("empty_start_devoir_btn")
                ) {
                    Icon(
                        imageVector = Icons.Filled.MenuBook,
                        contentDescription = stringResource(R.string.cd_start_devoir_icon),
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(text = stringResource(R.string.space_empty_start_devoir_btn))
                }

                FilledTonalButton(
                    onClick = onStartQcm,
                    modifier = Modifier
                        .fillMaxWidth()
                        .minimumInteractiveComponentSize()
                        .testTag("empty_start_qcm_btn")
                ) {
                    Icon(
                        imageVector = Icons.Filled.Quiz,
                        contentDescription = stringResource(R.string.cd_start_qcm_icon),
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(text = stringResource(R.string.space_empty_start_qcm_btn))
                }
            }
        }
    }
}

/**
 * Carte « Objectif de la semaine » avec barre de progression et bouton d'édition.
 */
@Composable
private fun WeeklyGoalCard(
    completedCount: Int,
    targetCount: Int,
    onEditClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val safeTarget = targetCount.coerceAtLeast(1)
    val progress = (completedCount.toFloat() / safeTarget.toFloat()).coerceIn(0f, 1f)
    val isAchieved = completedCount >= safeTarget
    val remaining = (safeTarget - completedCount).coerceAtLeast(0)
    val emerald = adaptiveEmerald()
    val accent = if (isAchieved) emerald else MaterialTheme.colorScheme.primary

    Surface(
        modifier = modifier
            .fillMaxWidth()
            .testTag("weekly_goal_card"),
        shape = MaterialTheme.shapes.large,
        color = MaterialTheme.colorScheme.surface,
        tonalElevation = 1.dp,
        border = BorderStroke(
            width = 1.dp,
            color = if (isAchieved) emerald.copy(alpha = 0.45f) else MaterialTheme.colorScheme.outlineVariant
        )
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(
                        imageVector = Icons.Filled.Flag,
                        contentDescription = stringResource(R.string.cd_weekly_goal_icon),
                        tint = accent,
                        modifier = Modifier.size(20.dp)
                    )
                    Text(
                        text = stringResource(R.string.weekly_goal_title),
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    Surface(
                        shape = RoundedCornerShape(10.dp),
                        color = accent.copy(alpha = 0.14f)
                    ) {
                        Text(
                            text = stringResource(R.string.weekly_goal_progress, completedCount, safeTarget),
                            style = MaterialTheme.typography.labelLarge,
                            color = accent,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                        )
                    }
                    IconButton(
                        onClick = onEditClick,
                        modifier = Modifier
                            .minimumInteractiveComponentSize()
                            .testTag("edit_weekly_goal_btn")
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Edit,
                            contentDescription = stringResource(R.string.cd_edit_weekly_goal),
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
            }

            LinearProgressIndicator(
                progress = { progress },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(10.dp)
                    .clip(RoundedCornerShape(6.dp))
                    .testTag("weekly_goal_progress_bar"),
                color = accent,
                trackColor = MaterialTheme.colorScheme.surfaceVariant
            )

            Text(
                text = if (isAchieved) {
                    stringResource(R.string.weekly_goal_achieved)
                } else {
                    stringResource(R.string.weekly_goal_remaining, remaining)
                },
                style = MaterialTheme.typography.bodySmall,
                color = if (isAchieved) emerald else MaterialTheme.colorScheme.onSurfaceVariant,
                fontWeight = if (isAchieved) FontWeight.SemiBold else FontWeight.Normal
            )
        }
    }
}

/**
 * Boîte de dialogue permettant de modifier l'objectif hebdomadaire (de 1 à 20 devoirs).
 */
@Composable
private fun EditWeeklyGoalDialog(
    currentGoal: Int,
    onDismiss: () -> Unit,
    onSave: (Int) -> Unit
) {
    var draftGoal by remember(currentGoal) { mutableIntStateOf(currentGoal.coerceIn(1, 20)) }
    val presets = listOf(2, 3, 5, 7)

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = stringResource(R.string.weekly_goal_dialog_title),
                style = MaterialTheme.typography.titleLarge
            )
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
                Text(
                    text = stringResource(R.string.weekly_goal_dialog_subtitle),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                // Sélecteur pas-à-pas (- / +)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    FilledTonalIconButton(
                        onClick = { if (draftGoal > 1) draftGoal -= 1 },
                        enabled = draftGoal > 1,
                        modifier = Modifier
                            .minimumInteractiveComponentSize()
                            .testTag("decrease_weekly_goal_btn")
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Remove,
                            contentDescription = stringResource(R.string.cd_decrease_goal)
                        )
                    }

                    Text(
                        text = stringResource(R.string.weekly_goal_unit, draftGoal),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.padding(horizontal = 16.dp)
                    )

                    FilledTonalIconButton(
                        onClick = { if (draftGoal < 20) draftGoal += 1 },
                        enabled = draftGoal < 20,
                        modifier = Modifier
                            .minimumInteractiveComponentSize()
                            .testTag("increase_weekly_goal_btn")
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Add,
                            contentDescription = stringResource(R.string.cd_increase_goal)
                        )
                    }
                }

                // Raccourcis rapides
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.CenterHorizontally)
                ) {
                    presets.forEach { preset ->
                        FilterChip(
                            selected = draftGoal == preset,
                            onClick = { draftGoal = preset },
                            modifier = Modifier.testTag("weekly_goal_preset_$preset"),
                            label = {
                                Text(text = preset.toString())
                            }
                        )
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = { onSave(draftGoal) },
                modifier = Modifier.testTag("save_weekly_goal_btn")
            ) {
                Text(text = stringResource(R.string.action_save))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(text = stringResource(R.string.action_cancel))
            }
        }
    )
}

/**
 * Carte « Chapitres à renforcer » : utilise une icône neutre et n'applique
 * la couleur d'alerte que lorsqu'au moins un chapitre faible est réellement détecté.
 */
@Composable
private fun WeakChaptersCard(
    weakChapters: List<WeakChapterInsight>,
    onPracticeChapter: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val hasWeakChapters = weakChapters.isNotEmpty()
    val crimson = adaptiveCrimson()
    val headerIconTint = if (hasWeakChapters) {
        crimson
    } else {
        MaterialTheme.colorScheme.onSurfaceVariant
    }

    Surface(
        modifier = modifier
            .fillMaxWidth()
            .testTag("weak_chapters_card"),
        shape = MaterialTheme.shapes.large,
        color = MaterialTheme.colorScheme.surface,
        border = BorderStroke(
            width = 1.dp,
            color = if (hasWeakChapters) crimson.copy(alpha = 0.35f) else MaterialTheme.colorScheme.outlineVariant
        )
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(
                    imageVector = Icons.Filled.School,
                    contentDescription = stringResource(R.string.cd_weak_chapters_icon),
                    tint = headerIconTint,
                    modifier = Modifier.size(20.dp)
                )
                Text(
                    text = stringResource(R.string.weak_chapters_title),
                    style = MaterialTheme.typography.titleMedium,
                    color = if (hasWeakChapters) crimson else MaterialTheme.colorScheme.onSurface
                )
            }

            if (!hasWeakChapters) {
                Text(
                    text = stringResource(R.string.weak_chapters_empty),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            } else {
                weakChapters.take(6).forEach { item ->
                    val hasQcm = FlashQcmBank.findByChapterName(item.chapter) != null
                    Surface(
                        shape = MaterialTheme.shapes.medium,
                        color = crimson.copy(alpha = 0.08f),
                        border = BorderStroke(1.dp, crimson.copy(alpha = 0.25f))
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 12.dp, vertical = 10.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = item.chapter,
                                    style = MaterialTheme.typography.titleSmall,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Text(
                                    text = stringResource(
                                        R.string.weak_chapter_meta,
                                        localizedSubjectName(item.subject),
                                        "%.1f".format(item.averageOn20),
                                        item.evaluationsCount,
                                        item.severityLabel
                                    ),
                                    style = MaterialTheme.typography.bodySmall,
                                    color = crimson
                                )
                            }
                            if (hasQcm) {
                                TextButton(
                                    onClick = { onPracticeChapter(item.chapter) },
                                    modifier = Modifier.minimumInteractiveComponentSize()
                                ) {
                                    Text(stringResource(R.string.qcm_short_btn))
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun ExamAttemptHistoryCard(
    attempt: ExamAttemptEntity,
    locale: Locale,
    modifier: Modifier = Modifier
) {
    val dateFmt = remember(locale) { SimpleDateFormat("dd/MM/yyyy · HH:mm", locale) }
    val scoreOn20 = ProgressAnalytics.normalizeTo20(attempt.totalScore, attempt.maxScore)
    val exScores = remember(attempt.exerciseScoresSerialized) {
        ExerciseScoreCodec.decode(attempt.exerciseScoresSerialized)
    }
    val emerald = adaptiveEmerald()
    val crimson = adaptiveCrimson()

    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.medium,
        color = MaterialTheme.colorScheme.surface,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
    ) {
        Column(
            modifier = Modifier.padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = localizedSubjectName(attempt.subject),
                    style = MaterialTheme.typography.labelLarge,
                    color = subjectAccentColor(attempt.subject)
                )
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = if (scoreOn20 >= 10.0) emerald.copy(alpha = 0.14f) else crimson.copy(alpha = 0.14f)
                ) {
                    Text(
                        text = "${"%.1f".format(attempt.totalScore)} / ${"%.0f".format(attempt.maxScore)}",
                        style = MaterialTheme.typography.labelLarge,
                        color = if (scoreOn20 >= 10.0) emerald else crimson,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                    )
                }
            }
            Text(
                text = attempt.devoirTitle,
                style = MaterialTheme.typography.titleSmall,
                color = MaterialTheme.colorScheme.onSurface
            )
            Text(
                text = stringResource(
                    R.string.attempt_meta_duration,
                    dateFmt.format(Date(attempt.attemptedAtMillis)),
                    ProgressAnalytics.formatStudyDuration(attempt.actualDurationSeconds.toLong())
                ),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            if (exScores.isNotEmpty()) {
                val breakdown = exScores.joinToString("  ·  ") {
                    "Ex.${it.exerciseNumber}: ${"%.1f".format(it.obtainedPoints)}/${"%.0f".format(it.maxPoints)}"
                }
                Text(
                    text = breakdown,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
private fun StatKpiCard(
    icon: ImageVector,
    iconContentDescription: String,
    value: String,
    label: String,
    accent: Color,
    isEncouragement: Boolean = false,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier,
        shape = MaterialTheme.shapes.medium,
        color = MaterialTheme.colorScheme.surface,
        tonalElevation = 1.dp,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 12.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(
                imageVector = icon,
                contentDescription = iconContentDescription,
                tint = accent,
                modifier = Modifier.size(22.dp)
            )
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = value,
                style = if (isEncouragement) {
                    MaterialTheme.typography.labelMedium
                } else {
                    MaterialTheme.typography.titleLarge
                },
                color = if (isEncouragement) accent else MaterialTheme.colorScheme.onSurface,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center,
                maxLines = if (isEncouragement) 2 else 1,
                overflow = TextOverflow.Ellipsis
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
private fun EmptyHintBox(text: String, modifier: Modifier = Modifier) {
    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.medium,
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.55f),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(16.dp)
        )
    }
}
