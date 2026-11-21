package com.example.ui.screens

import android.graphics.Bitmap
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.CloudDone
import androidx.compose.material.icons.filled.CloudDownload
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.ErrorOutline
import androidx.compose.material.icons.filled.Forum
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PictureAsPdf
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material.icons.filled.Verified
import androidx.compose.material.icons.filled.Wifi
import androidx.compose.material.icons.outlined.BookmarkBorder
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.R
import com.example.data.ai.TutorHelpMode
import com.example.data.ai.TutorLanguage
import com.example.data.download.DevoirDownloadProgress
import com.example.data.download.DownloadTaskStatus
import com.example.data.download.PdfStorageManager
import com.example.data.local.TutorMessageEntity
import com.example.data.model.DevoirExercise
import com.example.data.model.DevoirItem
import com.example.ui.components.AiTutorBottomSheet
import com.example.ui.components.ExamModeActiveBanner
import com.example.ui.components.ExamModeLaunchCard
import com.example.ui.components.ExamSavedSummaryCard
import com.example.ui.components.ExamSelfEvaluationCard
import com.example.ui.components.adaptiveCrimson
import com.example.ui.components.adaptiveEmerald
import com.example.ui.components.adaptiveOchre
import com.example.ui.components.buildHighlightedText
import com.example.ui.components.formatDurationMinutes
import com.example.ui.components.localizedBadge
import com.example.ui.components.localizedShortLabel
import com.example.ui.components.localizedSubjectName
import com.example.ui.components.subjectAccentColor
import com.example.ui.viewmodel.ExamModePhase
import com.example.ui.viewmodel.ExamModeUiState
import com.example.ui.viewmodel.ExamTimerState
import com.example.ui.viewmodel.TutorHistoryScope
import com.example.ui.viewmodel.TutorUiState
import java.util.Locale

/**
 * Écran de détail d'un devoir PDF hébergé en ligne :
 * - Téléchargement (sujet + corrigé) avec progression, annulation, reprise et vérification d'intégrité,
 * - Lecture hors ligne dans le lecteur PDF natif avec chronomètre flottant,
 * - Verrouillage du corrigé pendant le chronomètre ou le mode examen + confirmation utilisateur,
 * - Suppression locale du devoir pour libérer de l'espace.
 */
@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun DevoirDetailScreen(
    devoir: DevoirItem,
    downloadProgress: DevoirDownloadProgress?,
    hasResumablePart: Boolean,
    timerState: ExamTimerState,
    examModeState: ExamModeUiState,
    tutorUiState: TutorUiState,
    tutorMessages: List<TutorMessageEntity>,
    devoirExplanationCount: Int = 0,
    onBack: () -> Unit,
    onToggleFavorite: () -> Unit,
    onToggleCompleted: () -> Unit,
    onDownloadDevoir: () -> Unit,
    onCancelDownload: () -> Unit,
    onDeleteDownload: () -> Unit,
    onToggleTimer: () -> Unit,
    onResetTimer: () -> Unit,
    onStartExamMode: () -> Unit,
    onFinishExamMode: () -> Unit,
    onCancelExamMode: () -> Unit,
    onUpdateExamScore: (Int, String) -> Unit,
    onSaveExamEvaluation: () -> Unit,
    onDismissExamSummary: () -> Unit,
    onExplainExerciseWithAi: (DevoirExercise) -> Unit,
    onOpenTutorChat: (DevoirExercise) -> Unit,
    onOpenDevoirTutorHistory: (DevoirExercise) -> Unit = {},
    onExplainPdfPage: (pageNumber: Int, documentKindLabel: String, pageBitmap: Bitmap) -> Unit = { _, _, _ -> },
    onDismissTutor: () -> Unit,
    onSelectTutorLanguage: (TutorLanguage) -> Unit,
    onSelectTutorHistoryScope: (TutorHistoryScope) -> Unit = {},
    onTriggerTutorMode: (TutorHelpMode) -> Unit,
    onTutorQuestionChange: (String) -> Unit,
    onSendTutorQuestion: () -> Unit,
    onRetryTutor: () -> Unit,
    onClearTutorHistory: () -> Unit,
    modifier: Modifier = Modifier,
    searchQuery: String = ""
) {
    var activePdfKindName by rememberSaveable(devoir.id) { mutableStateOf<String?>(null) }
    val activePdfKind = activePdfKindName?.let { runCatching { PdfDocumentKind.valueOf(it) }.getOrNull() }

    var showCorrigeConfirmDialog by rememberSaveable(devoir.id) { mutableStateOf(false) }
    var corrigeConfirmed by rememberSaveable(devoir.id) { mutableStateOf(false) }

    val isExamLocked = examModeState.isSolutionsLocked
    val isTimerLocked = timerState.isRunning
    val isCorrigeLocked = isExamLocked || isTimerLocked
    val isSelfEvaluation = examModeState.phase == ExamModePhase.SELF_EVALUATION
    val subjectBadgeLabel = stringResource(R.string.pdf_badge_subject)
    val corrigeBadgeLabel = stringResource(R.string.pdf_badge_corrige)

    // Si un PDF local téléchargé est ouvert, afficher le lecteur PDF natif + chronomètre flottant + Tuteur IA page courante
    if (activePdfKind != null) {
        val localPath = when (activePdfKind) {
            PdfDocumentKind.SUJET -> devoir.localSujetPath.orEmpty()
            PdfDocumentKind.CORRIGE -> devoir.localCorrigePath ?: devoir.localSujetPath.orEmpty()
        }
        PdfViewerScreen(
            documentTitle = devoir.title,
            pdfAssetPath = localPath,
            documentKind = activePdfKind,
            timerState = timerState,
            examModeState = examModeState,
            onToggleTimer = onToggleTimer,
            onResetTimer = onResetTimer,
            onFinishExamMode = {
                activePdfKindName = null
                onFinishExamMode()
            },
            onBack = { activePdfKindName = null },
            onExplainCurrentPage = { pageNumber, kind, bitmap ->
                val kindLabel = if (kind == PdfDocumentKind.CORRIGE) corrigeBadgeLabel else subjectBadgeLabel
                onExplainPdfPage(pageNumber, kindLabel, bitmap)
            },
            modifier = modifier
        )

        if (tutorUiState.isSheetOpen) {
            AiTutorBottomSheet(
                uiState = tutorUiState,
                messages = tutorMessages,
                devoirExplanationCount = devoirExplanationCount,
                onDismiss = onDismissTutor,
                onSelectLanguage = onSelectTutorLanguage,
                onSelectHistoryScope = onSelectTutorHistoryScope,
                onTriggerHelpMode = onTriggerTutorMode,
                onQuestionChange = onTutorQuestionChange,
                onSendQuestion = onSendTutorQuestion,
                onRetry = onRetryTutor,
                onClearHistory = onClearTutorHistory
            )
        }
        return
    }

    BackHandler(onBack = onBack)

    val subjectAccent = subjectAccentColor(devoir.subject)
    val emerald = adaptiveEmerald()
    val crimson = adaptiveCrimson()
    val ochre = adaptiveOchre()
    val formattedSize = remember(devoir.sizeMb, devoir.downloadedBytes) {
        if (devoir.isAvailableOffline && devoir.downloadedBytes > 0L) {
            PdfStorageManager.formatBytes(devoir.downloadedBytes)
        } else {
            String.format(Locale.getDefault(), "%.1f Mo", devoir.sizeMb)
        }
    }

    val devoirSummaryForTutor = remember(devoir.id, devoir.title, devoir.chapterTags) {
        DevoirExercise(
            number = 1,
            title = devoir.title,
            points = "20 pts",
            statement = "Devoir PDF (${devoir.type.badge}) — Chapitres : ${devoir.chapterTags.joinToString(", ")}.",
            solution = if (devoir.hasCorrigePdf) {
                "Corrigé détaillé disponible en PDF."
            } else {
                "Sujet d'entraînement seul (corrigé non fourni)."
            }
        )
    }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            text = "${localizedSubjectName(devoir.subject)} · ${devoir.type.localizedBadge()}",
                            style = MaterialTheme.typography.titleMedium,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Text(
                            text = "${devoir.level.localizedShortLabel()} · ${devoir.section.localizedShortLabel()}",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                },
                navigationIcon = {
                    IconButton(
                        onClick = onBack,
                        modifier = Modifier
                            .minimumInteractiveComponentSize()
                            .testTag("detail_back_button")
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = stringResource(R.string.back_button_cd)
                        )
                    }
                },
                actions = {
                    IconButton(
                        onClick = onToggleFavorite,
                        modifier = Modifier
                            .minimumInteractiveComponentSize()
                            .testTag("detail_favorite_button")
                    ) {
                        Icon(
                            imageVector = if (devoir.isFavorite) Icons.Filled.Bookmark else Icons.Outlined.BookmarkBorder,
                            contentDescription = stringResource(
                                if (devoir.isFavorite) R.string.bookmarked_cd else R.string.bookmark_cd
                            ),
                            tint = if (devoir.isFavorite) crimson else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background
                )
            )
        },
        bottomBar = {
            Surface(
                tonalElevation = 4.dp,
                shadowElevation = 6.dp,
                color = MaterialTheme.colorScheme.surface
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .navigationBarsPadding()
                        .padding(horizontal = 16.dp, vertical = 12.dp),
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    if (devoir.isAvailableOffline) {
                        OutlinedButton(
                            onClick = { activePdfKindName = PdfDocumentKind.SUJET.name },
                            modifier = Modifier
                                .weight(1f)
                                .minimumInteractiveComponentSize()
                                .testTag("bottom_open_sujet_btn")
                        ) {
                            Icon(
                                imageVector = Icons.Filled.PictureAsPdf,
                                contentDescription = null,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = stringResource(R.string.pdf_open_subject_btn),
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    } else {
                        OutlinedButton(
                            onClick = onDownloadDevoir,
                            modifier = Modifier
                                .weight(1f)
                                .minimumInteractiveComponentSize()
                                .testTag("bottom_download_btn")
                        ) {
                            Icon(
                                imageVector = Icons.Filled.CloudDownload,
                                contentDescription = null,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = stringResource(
                                    if (hasResumablePart) R.string.action_resume_download else R.string.action_download
                                ),
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }

                    Button(
                        onClick = onToggleCompleted,
                        modifier = Modifier
                            .weight(1f)
                            .minimumInteractiveComponentSize()
                            .testTag("mark_completed_btn"),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (devoir.isCompleted) {
                                MaterialTheme.colorScheme.secondary
                            } else {
                                MaterialTheme.colorScheme.primary
                            }
                        )
                    ) {
                        Icon(
                            imageVector = if (devoir.isCompleted) Icons.Filled.CheckCircle else Icons.Outlined.CheckCircle,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = stringResource(
                                if (devoir.isCompleted) R.string.completed_badge else R.string.mark_completed
                            ),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
            }
        }
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .testTag("devoir_detail_content"),
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            // 1. En-tête de la fiche devoir
            item(key = "exam_header") {
                Surface(
                    shape = MaterialTheme.shapes.large,
                    color = MaterialTheme.colorScheme.surface,
                    tonalElevation = 2.dp,
                    border = BorderStroke(1.5.dp, subjectAccent.copy(alpha = 0.35f))
                ) {
                    Column(modifier = Modifier.padding(18.dp)) {
                        Text(
                            text = stringResource(R.string.official_header_republic),
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Text(
                                text = devoir.source,
                                style = MaterialTheme.typography.labelMedium,
                                color = subjectAccent,
                                fontWeight = FontWeight.SemiBold
                            )
                            Surface(
                                shape = RoundedCornerShape(6.dp),
                                color = if (devoir.isAvailableOffline) {
                                    emerald.copy(alpha = 0.14f)
                                } else {
                                    MaterialTheme.colorScheme.surfaceVariant
                                }
                            ) {
                                Text(
                                    text = stringResource(
                                        if (devoir.isAvailableOffline) {
                                            R.string.status_available_offline
                                        } else {
                                            R.string.status_to_download
                                        }
                                    ),
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.SemiBold,
                                    color = if (devoir.isAvailableOffline) {
                                        emerald
                                    } else {
                                        MaterialTheme.colorScheme.onSurfaceVariant
                                    },
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                                )
                            }
                        }
                        Spacer(modifier = Modifier.height(10.dp))
                        Text(
                            text = buildHighlightedText(devoir.title, searchQuery),
                            style = MaterialTheme.typography.headlineSmall,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                        Spacer(modifier = Modifier.height(12.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            ExamMetaColumn(
                                label = stringResource(R.string.meta_duration),
                                value = formatDurationMinutes(devoir.durationMinutes)
                            )
                            ExamMetaColumn(
                                label = stringResource(R.string.pdf_meta_size),
                                value = formattedSize
                            )
                            ExamMetaColumn(
                                label = stringResource(R.string.meta_trimester),
                                value = devoir.trimester.localizedShortLabel()
                            )
                            ExamMetaColumn(
                                label = stringResource(R.string.meta_difficulty),
                                value = devoir.difficulty
                            )
                        }

                        if (devoir.chapterTags.isNotEmpty()) {
                            Spacer(modifier = Modifier.height(12.dp))
                            FlowRow(
                                horizontalArrangement = Arrangement.spacedBy(6.dp),
                                verticalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                devoir.chapterTags.forEach { tag ->
                                    Box(
                                        modifier = Modifier
                                            .clip(RoundedCornerShape(8.dp))
                                            .background(subjectAccent.copy(alpha = 0.13f))
                                            .padding(horizontal = 10.dp, vertical = 4.dp)
                                    ) {
                                        Text(
                                            text = buildHighlightedText(tag, searchQuery),
                                            style = MaterialTheme.typography.labelSmall,
                                            color = subjectAccent
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // 2. Carte Documents PDF : téléchargement / progression / annulation / reprise + Voir le sujet & Voir le corrigé
            item(key = "pdf_documents_section") {
                PdfDocumentsActionCard(
                    devoir = devoir,
                    downloadProgress = downloadProgress,
                    hasResumablePart = hasResumablePart,
                    isExamLocked = isExamLocked,
                    isTimerLocked = isTimerLocked,
                    subjectAccent = subjectAccent,
                    emerald = emerald,
                    crimson = crimson,
                    ochre = ochre,
                    onDownload = onDownloadDevoir,
                    onCancelDownload = onCancelDownload,
                    onDeleteDownload = onDeleteDownload,
                    onOpenSujet = { activePdfKindName = PdfDocumentKind.SUJET.name },
                    onRequestOpenCorrige = {
                        if (!devoir.hasCorrigePdf || !devoir.isAvailableOffline || isCorrigeLocked) {
                            return@PdfDocumentsActionCard
                        }
                        if (isSelfEvaluation || corrigeConfirmed) {
                            activePdfKindName = PdfDocumentKind.CORRIGE.name
                        } else {
                            showCorrigeConfirmDialog = true
                        }
                    }
                )
            }

            // 3. Mode Examen en conditions réelles
            item(key = "exam_mode_section") {
                when (examModeState.phase) {
                    ExamModePhase.IDLE -> {
                        val bestAttempt = examModeState.pastAttemptsForDevoir.maxByOrNull {
                            if (it.maxScore > 0.0) it.totalScore / it.maxScore else 0.0
                        }
                        val bestLabel = bestAttempt?.let {
                            "${"%.1f".format(it.totalScore)} / ${"%.0f".format(it.maxScore)}"
                        }
                        ExamModeLaunchCard(
                            durationMinutes = devoir.durationMinutes,
                            attemptsCount = examModeState.pastAttemptsForDevoir.size,
                            bestScoreLabel = bestLabel,
                            onStartRealConditions = {
                                corrigeConfirmed = false
                                onStartExamMode()
                            }
                        )
                    }

                    ExamModePhase.IN_PROGRESS -> {
                        ExamModeActiveBanner(
                            state = examModeState,
                            onFinishExam = onFinishExamMode,
                            onCancelExam = onCancelExamMode
                        )
                    }

                    ExamModePhase.SELF_EVALUATION -> {
                        ExamSelfEvaluationCard(
                            state = examModeState,
                            onScoreChange = onUpdateExamScore,
                            onSaveAttempt = onSaveExamEvaluation,
                            onCancel = onCancelExamMode
                        )
                    }

                    ExamModePhase.SAVED_SUMMARY -> {
                        ExamSavedSummaryCard(
                            totalScore = examModeState.lastSavedTotalScore ?: 0.0,
                            maxScore = examModeState.lastSavedMaxScore,
                            actualDurationSeconds = examModeState.actualElapsedSeconds,
                            onDismiss = onDismissExamSummary
                        )
                    }
                }
            }

            // 4. Chronomètre libre
            if (examModeState.phase == ExamModePhase.IDLE) {
                item(key = "exam_timer") {
                    ExamTimerCard(
                        timerState = timerState,
                        onToggleTimer = {
                            if (!timerState.isRunning) {
                                corrigeConfirmed = false
                            }
                            onToggleTimer()
                        },
                        onResetTimer = onResetTimer
                    )
                }
            }

            // 5. Tuteur IA
            if (!isExamLocked) {
                item(key = "tutor_help_section") {
                    Surface(
                        shape = MaterialTheme.shapes.medium,
                        color = MaterialTheme.colorScheme.surface,
                        tonalElevation = 1.dp,
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
                    ) {
                        Column(
                            modifier = Modifier.padding(16.dp),
                            verticalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Text(
                                text = stringResource(R.string.tutor_title),
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                FilledTonalButton(
                                    onClick = { onExplainExerciseWithAi(devoirSummaryForTutor) },
                                    modifier = Modifier
                                        .weight(1.25f)
                                        .minimumInteractiveComponentSize()
                                        .testTag("explain_ai_devoir_btn"),
                                    contentPadding = PaddingValues(horizontal = 10.dp, vertical = 10.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Filled.AutoAwesome,
                                        contentDescription = null,
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        text = stringResource(R.string.tutor_explain_button),
                                        style = MaterialTheme.typography.labelMedium,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }

                                OutlinedButton(
                                    onClick = { onOpenTutorChat(devoirSummaryForTutor) },
                                    modifier = Modifier
                                        .weight(1f)
                                        .minimumInteractiveComponentSize()
                                        .testTag("chat_ai_devoir_btn"),
                                    contentPadding = PaddingValues(horizontal = 10.dp, vertical = 10.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Filled.Forum,
                                        contentDescription = null,
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        text = stringResource(R.string.tutor_hints_chat_button),
                                        style = MaterialTheme.typography.labelMedium,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }
                            }
                            if (devoirExplanationCount > 0) {
                                TextButton(
                                    onClick = { onOpenDevoirTutorHistory(devoirSummaryForTutor) },
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .minimumInteractiveComponentSize()
                                        .testTag("devoir_tutor_history_btn")
                                ) {
                                    Text(
                                        text = stringResource(
                                            R.string.tutor_view_saved_history_btn,
                                            devoirExplanationCount
                                        ),
                                        style = MaterialTheme.typography.labelLarge,
                                        fontWeight = FontWeight.SemiBold
                                    )
                                }
                            }
                        }
                    }
                }
            }

            item { Spacer(modifier = Modifier.height(12.dp)) }
        }
    }

    if (showCorrigeConfirmDialog) {
        AlertDialog(
            onDismissRequest = { showCorrigeConfirmDialog = false },
            icon = {
                Icon(
                    imageVector = Icons.Filled.Verified,
                    contentDescription = stringResource(R.string.cd_pdf_corrige_icon),
                    tint = emerald
                )
            },
            title = { Text(stringResource(R.string.pdf_corrige_confirm_title)) },
            text = { Text(stringResource(R.string.pdf_corrige_confirm_body)) },
            confirmButton = {
                Button(
                    onClick = {
                        showCorrigeConfirmDialog = false
                        corrigeConfirmed = true
                        activePdfKindName = PdfDocumentKind.CORRIGE.name
                    },
                    modifier = Modifier.testTag("confirm_open_corrige_btn")
                ) {
                    Text(stringResource(R.string.pdf_corrige_confirm_yes))
                }
            },
            dismissButton = {
                TextButton(
                    onClick = { showCorrigeConfirmDialog = false },
                    modifier = Modifier.testTag("cancel_open_corrige_btn")
                ) {
                    Text(stringResource(R.string.action_cancel))
                }
            }
        )
    }

    if (tutorUiState.isSheetOpen) {
        AiTutorBottomSheet(
            uiState = tutorUiState,
            messages = tutorMessages,
            devoirExplanationCount = devoirExplanationCount,
            onDismiss = onDismissTutor,
            onSelectLanguage = onSelectTutorLanguage,
            onSelectHistoryScope = onSelectTutorHistoryScope,
            onTriggerHelpMode = onTriggerTutorMode,
            onQuestionChange = onTutorQuestionChange,
            onSendQuestion = onSendTutorQuestion,
            onRetry = onRetryTutor,
            onClearHistory = onClearTutorHistory
        )
    }
}

@Composable
private fun PdfDocumentsActionCard(
    devoir: DevoirItem,
    downloadProgress: DevoirDownloadProgress?,
    hasResumablePart: Boolean,
    isExamLocked: Boolean,
    isTimerLocked: Boolean,
    subjectAccent: Color,
    emerald: Color,
    crimson: Color,
    ochre: Color,
    onDownload: () -> Unit,
    onCancelDownload: () -> Unit,
    onDeleteDownload: () -> Unit,
    onOpenSujet: () -> Unit,
    onRequestOpenCorrige: () -> Unit
) {
    val activeStatus = downloadProgress?.status ?: DownloadTaskStatus.IDLE
    val isDownloading = activeStatus == DownloadTaskStatus.DOWNLOADING
    val isQueued = activeStatus == DownloadTaskStatus.QUEUED
    val isFailed = activeStatus == DownloadTaskStatus.FAILED
    val isPausedResumable = activeStatus == DownloadTaskStatus.PAUSED_RESUMABLE ||
        (!devoir.isAvailableOffline && hasResumablePart && !isDownloading && !isQueued)

    val isCorrigeLocked = isExamLocked || isTimerLocked
    val corrigeEnabled = devoir.isAvailableOffline && devoir.hasCorrigePdf && !isCorrigeLocked

    Surface(
        shape = MaterialTheme.shapes.large,
        color = MaterialTheme.colorScheme.surface,
        tonalElevation = 2.dp,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .clip(RoundedCornerShape(10.dp))
                        .background(subjectAccent.copy(alpha = 0.14f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = if (devoir.isAvailableOffline) Icons.Filled.CloudDone else Icons.Filled.PictureAsPdf,
                        contentDescription = stringResource(R.string.cd_pdf_subject_icon),
                        tint = if (devoir.isAvailableOffline) emerald else subjectAccent,
                        modifier = Modifier.size(22.dp)
                    )
                }
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = stringResource(R.string.pdf_actions_section_title),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = stringResource(
                            if (devoir.isAvailableOffline) {
                                R.string.pdf_actions_section_subtitle
                            } else {
                                R.string.download_required_before_open
                            }
                        ),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            // Zone téléchargement si le PDF n'est pas encore hors ligne (ou mise à jour disponible)
            if (!devoir.isAvailableOffline || isDownloading || isQueued) {
                if (isDownloading || isQueued) {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Icon(
                                    imageVector = if (isQueued) Icons.Filled.Wifi else Icons.Filled.CloudDownload,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(18.dp)
                                )
                                Text(
                                    text = if (isQueued) {
                                        stringResource(R.string.status_queued_wifi)
                                    } else {
                                        stringResource(
                                            R.string.status_downloading_pct,
                                            downloadProgress?.progressPercent ?: 0
                                        )
                                    },
                                    style = MaterialTheme.typography.labelLarge,
                                    fontWeight = FontWeight.SemiBold,
                                    color = MaterialTheme.colorScheme.primary
                                )
                            }
                            if (isDownloading && (downloadProgress?.totalBytes ?: 0L) > 0L) {
                                Text(
                                    text = "${PdfStorageManager.formatBytes(downloadProgress?.bytesDownloaded ?: 0L)} / ${
                                        PdfStorageManager.formatBytes(downloadProgress?.totalBytes ?: 0L)
                                    }",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }

                        if (isDownloading) {
                            LinearProgressIndicator(
                                progress = { ((downloadProgress?.progressPercent ?: 0) / 100f).coerceIn(0f, 1f) },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(7.dp)
                                    .clip(RoundedCornerShape(4.dp))
                            )
                        } else {
                            LinearProgressIndicator(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(7.dp)
                                    .clip(RoundedCornerShape(4.dp))
                            )
                        }

                        OutlinedButton(
                            onClick = onCancelDownload,
                            modifier = Modifier
                                .fillMaxWidth()
                                .minimumInteractiveComponentSize()
                                .testTag("detail_cancel_download_btn"),
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = crimson),
                            border = BorderStroke(1.dp, crimson.copy(alpha = 0.45f))
                        ) {
                            Icon(
                                imageVector = Icons.Filled.Close,
                                contentDescription = stringResource(R.string.cd_cancel_download_icon),
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(stringResource(R.string.action_cancel_download))
                        }
                    }
                } else {
                    if (isFailed) {
                        Surface(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(10.dp),
                            color = crimson.copy(alpha = 0.10f),
                            border = BorderStroke(1.dp, crimson.copy(alpha = 0.35f))
                        ) {
                            Row(
                                modifier = Modifier.padding(10.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Filled.ErrorOutline,
                                    contentDescription = null,
                                    tint = crimson,
                                    modifier = Modifier.size(18.dp)
                                )
                                Text(
                                    text = downloadProgress?.errorMessage
                                        ?: stringResource(R.string.download_err_network),
                                    style = MaterialTheme.typography.bodySmall,
                                    color = crimson
                                )
                            }
                        }
                    }

                    Button(
                        onClick = onDownload,
                        modifier = Modifier
                            .fillMaxWidth()
                            .minimumInteractiveComponentSize()
                            .testTag("detail_download_btn"),
                        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 14.dp)
                    ) {
                        Icon(
                            imageVector = if (isPausedResumable) Icons.Filled.PlayArrow else Icons.Filled.CloudDownload,
                            contentDescription = stringResource(R.string.cd_download_action_icon),
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        val btnLabel = when {
                            isFailed -> stringResource(R.string.action_retry_download)
                            isPausedResumable -> stringResource(R.string.action_resume_download)
                            else -> stringResource(R.string.action_download)
                        }
                        Text(
                            text = "$btnLabel (${String.format(Locale.getDefault(), "%.1f Mo", devoir.sizeMb)})",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            } else {
                // Fichiers disponibles hors ligne : boutons Voir le sujet + Voir le corrigé
                if (devoir.hasNewerRemoteVersion) {
                    FilledTonalButton(
                        onClick = onDownload,
                        modifier = Modifier
                            .fillMaxWidth()
                            .minimumInteractiveComponentSize()
                    ) {
                        Icon(
                            imageVector = Icons.Filled.CloudDownload,
                            contentDescription = null,
                            tint = ochre,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(stringResource(R.string.download_update_available, devoir.version))
                    }
                }

                Button(
                    onClick = onOpenSujet,
                    modifier = Modifier
                        .fillMaxWidth()
                        .minimumInteractiveComponentSize()
                        .testTag("open_sujet_pdf_btn"),
                    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 14.dp)
                ) {
                    Icon(
                        imageVector = Icons.Filled.Description,
                        contentDescription = null,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = stringResource(R.string.pdf_open_subject_btn),
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold
                    )
                }

                OutlinedButton(
                    onClick = onRequestOpenCorrige,
                    enabled = corrigeEnabled,
                    modifier = Modifier
                        .fillMaxWidth()
                        .minimumInteractiveComponentSize()
                        .testTag("open_corrige_pdf_btn"),
                    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 14.dp),
                    border = BorderStroke(
                        width = 1.2.dp,
                        color = if (corrigeEnabled) emerald.copy(alpha = 0.7f) else MaterialTheme.colorScheme.outlineVariant
                    ),
                    colors = ButtonDefaults.outlinedButtonColors(
                        contentColor = emerald,
                        disabledContentColor = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                ) {
                    Icon(
                        imageVector = when {
                            !devoir.hasCorrigePdf -> Icons.Filled.Description
                            isCorrigeLocked -> Icons.Filled.Lock
                            else -> Icons.Filled.Verified
                        },
                        contentDescription = null,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = if (!devoir.hasCorrigePdf) {
                            stringResource(R.string.pdf_corrige_unavailable)
                        } else {
                            stringResource(R.string.pdf_open_corrige_btn)
                        },
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.SemiBold
                    )
                }

                if (devoir.hasCorrigePdf && isCorrigeLocked) {
                    Surface(
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("corrige_locked_banner"),
                        shape = RoundedCornerShape(10.dp),
                        color = crimson.copy(alpha = 0.10f),
                        border = BorderStroke(1.dp, crimson.copy(alpha = 0.35f))
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Filled.Lock,
                                contentDescription = null,
                                tint = crimson,
                                modifier = Modifier.size(16.dp)
                            )
                            Text(
                                text = stringResource(
                                    if (isExamLocked) {
                                        R.string.pdf_corrige_locked_exam
                                    } else {
                                        R.string.pdf_corrige_locked_timer
                                    }
                                ),
                                style = MaterialTheme.typography.labelMedium,
                                color = crimson
                            )
                        }
                    }
                }

                // Suppression rapide du fichier hors ligne pour libérer l'espace
                TextButton(
                    onClick = onDeleteDownload,
                    modifier = Modifier
                        .align(Alignment.End)
                        .testTag("detail_delete_download_btn"),
                    colors = ButtonDefaults.textButtonColors(contentColor = crimson)
                ) {
                    Icon(
                        imageVector = Icons.Filled.Delete,
                        contentDescription = stringResource(R.string.cd_delete_storage_icon),
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = "${stringResource(R.string.action_delete_download)} (${
                            PdfStorageManager.formatBytes(devoir.downloadedBytes)
                        })",
                        style = MaterialTheme.typography.labelMedium
                    )
                }
            }
        }
    }
}

@Composable
private fun ExamMetaColumn(label: String, value: String) {
    Column {
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Text(
            text = value,
            style = MaterialTheme.typography.titleSmall,
            color = MaterialTheme.colorScheme.onSurface
        )
    }
}

@Composable
private fun ExamTimerCard(
    timerState: ExamTimerState,
    onToggleTimer: () -> Unit,
    onResetTimer: () -> Unit
) {
    val progress = if (timerState.totalSeconds > 0) {
        timerState.remainingSeconds.toFloat() / timerState.totalSeconds.toFloat()
    } else 0f

    val hours = timerState.remainingSeconds / 3600
    val minutes = (timerState.remainingSeconds % 3600) / 60
    val seconds = timerState.remainingSeconds % 60
    val formattedTime = "%02d:%02d:%02d".format(hours, minutes, seconds)

    Surface(
        shape = MaterialTheme.shapes.medium,
        color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.45f),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.25f))
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Filled.Timer,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Column {
                        Text(
                            text = stringResource(R.string.timer_section_title),
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                        Text(
                            text = formattedTime,
                            style = MaterialTheme.typography.titleLarge,
                            color = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.testTag("exam_timer_display")
                        )
                    }
                }

                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    FilledTonalButton(
                        onClick = onToggleTimer,
                        modifier = Modifier
                            .minimumInteractiveComponentSize()
                            .testTag("timer_toggle_btn")
                    ) {
                        Icon(
                            imageVector = if (timerState.isRunning) Icons.Filled.Pause else Icons.Filled.PlayArrow,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            stringResource(
                                if (timerState.isRunning) R.string.timer_pause else R.string.timer_start
                            )
                        )
                    }
                    IconButton(
                        onClick = onResetTimer,
                        modifier = Modifier
                            .minimumInteractiveComponentSize()
                            .testTag("timer_reset_btn")
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Refresh,
                            contentDescription = stringResource(R.string.timer_reset),
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))
            LinearProgressIndicator(
                progress = { progress },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(6.dp)
                    .clip(RoundedCornerShape(3.dp)),
                color = MaterialTheme.colorScheme.primary,
                trackColor = MaterialTheme.colorScheme.surface
            )
        }
    }
}
