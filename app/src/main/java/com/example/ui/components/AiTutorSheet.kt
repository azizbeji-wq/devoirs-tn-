package com.example.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.ErrorOutline
import androidx.compose.material.icons.filled.Lightbulb
import androidx.compose.material.icons.filled.Psychology
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.School
import androidx.compose.material3.AssistChip
import androidx.compose.material3.AssistChipDefaults
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import com.example.R
import com.example.data.ai.TutorHelpMode
import com.example.data.ai.TutorLanguage
import com.example.data.local.TutorMessageEntity
import com.example.ui.viewmodel.TutorGenerationState
import com.example.ui.viewmodel.TutorHistoryScope
import com.example.ui.viewmodel.TutorUiState

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AiTutorBottomSheet(
    uiState: TutorUiState,
    messages: List<TutorMessageEntity>,
    devoirExplanationCount: Int = 0,
    onDismiss: () -> Unit,
    onSelectLanguage: (TutorLanguage) -> Unit,
    onSelectHistoryScope: (TutorHistoryScope) -> Unit = {},
    onTriggerHelpMode: (TutorHelpMode) -> Unit,
    onQuestionChange: (String) -> Unit,
    onSendQuestion: () -> Unit,
    onRetry: () -> Unit,
    onClearHistory: () -> Unit
) {
    val devoir = uiState.activeDevoir ?: return
    val exercise = uiState.activeExercise ?: return
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val listState = rememberLazyListState()

    val isBusy = uiState.generationState is TutorGenerationState.Loading ||
        uiState.generationState is TutorGenerationState.Streaming
    val isArabicInput = uiState.selectedLanguage.isRtl

    val hint1Done = messages.any { it.mode == TutorHelpMode.HINT_1.id && it.role == "assistant" }
    val hint2Done = messages.any { it.mode == TutorHelpMode.HINT_2.id && it.role == "assistant" }

    // Scroll smoothly to latest message / streaming chunk
    LaunchedEffect(messages.size, uiState.generationState) {
        val targetIndex = messages.size + if (uiState.generationState != TutorGenerationState.Idle) 1 else 0
        if (targetIndex > 0) {
            listState.animateScrollToItem(targetIndex - 1)
        }
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = MaterialTheme.colorScheme.background,
        dragHandle = null
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .fillMaxHeight(0.90f)
                .navigationBarsPadding()
                .imePadding()
                .testTag("ai_tutor_sheet")
        ) {
            // Top Sheet Bar
            Surface(
                color = MaterialTheme.colorScheme.surface,
                tonalElevation = 2.dp
            ) {
                Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.weight(1f)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(38.dp)
                                    .clip(CircleShape)
                                    .background(MaterialTheme.colorScheme.primaryContainer),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Filled.AutoAwesome,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                            Spacer(modifier = Modifier.width(10.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = stringResource(R.string.tutor_title),
                                    style = MaterialTheme.typography.titleMedium,
                                    maxLines = 1
                                )
                                val subtitleText = when {
                                    uiState.historyScope == TutorHistoryScope.ALL_DEVOIR ->
                                        "${stringResource(R.string.tutor_history_all_devoir)} · ${localizedSubjectName(devoir.subject)}"
                                    uiState.activePdfPageNumber != null ->
                                        "${stringResource(R.string.tutor_pdf_page_label, uiState.activePdfPageNumber)} · ${uiState.activePdfDocumentKindLabel ?: stringResource(R.string.pdf_badge_subject)} · ${localizedSubjectName(devoir.subject)}"
                                    else ->
                                        "${devoir.level.localizedShortLabel()} · ${localizedSubjectName(devoir.subject)}"
                                }
                                Text(
                                    text = subtitleText,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.primary,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                        }

                        Row {
                            if (messages.isNotEmpty()) {
                                IconButton(
                                    onClick = onClearHistory,
                                    enabled = !isBusy,
                                    modifier = Modifier.testTag("tutor_clear_history_btn")
                                ) {
                                    Icon(
                                        imageVector = Icons.Filled.DeleteOutline,
                                        contentDescription = stringResource(R.string.tutor_clear_history)
                                    )
                                }
                            }
                            IconButton(
                                onClick = onDismiss,
                                modifier = Modifier.testTag("tutor_close_btn")
                            ) {
                                Icon(
                                    imageVector = Icons.Filled.Close,
                                    contentDescription = stringResource(R.string.tutor_close_cd)
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    // Language Selector (Français / العربية)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            text = stringResource(R.string.tutor_language_label),
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        SingleChoiceSegmentedButtonRow(modifier = Modifier.width(210.dp)) {
                            TutorLanguage.entries.forEachIndexed { idx, lang ->
                                SegmentedButton(
                                    selected = uiState.selectedLanguage == lang,
                                    onClick = { onSelectLanguage(lang) },
                                    shape = SegmentedButtonDefaults.itemShape(idx, TutorLanguage.entries.size),
                                    modifier = Modifier.testTag("tutor_lang_${lang.code}")
                                ) {
                                    Text(lang.label, maxLines = 1)
                                }
                            }
                        }
                    }

                    // Sélecteur de portée d'historique Room (Page courante / Tout le devoir)
                    if (uiState.isPdfPageMode || devoirExplanationCount > 0) {
                        Spacer(modifier = Modifier.height(8.dp))
                        SingleChoiceSegmentedButtonRow(
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("tutor_history_scope_toggle")
                        ) {
                            SegmentedButton(
                                selected = uiState.historyScope == TutorHistoryScope.CURRENT_CONTEXT,
                                onClick = { onSelectHistoryScope(TutorHistoryScope.CURRENT_CONTEXT) },
                                shape = SegmentedButtonDefaults.itemShape(0, 2),
                                modifier = Modifier.testTag("tutor_scope_current")
                            ) {
                                Text(
                                    text = uiState.activePdfPageNumber?.let {
                                        stringResource(R.string.tutor_pdf_page_label, it)
                                    } ?: stringResource(R.string.tutor_history_current),
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                            SegmentedButton(
                                selected = uiState.historyScope == TutorHistoryScope.ALL_DEVOIR,
                                onClick = { onSelectHistoryScope(TutorHistoryScope.ALL_DEVOIR) },
                                shape = SegmentedButtonDefaults.itemShape(1, 2),
                                modifier = Modifier.testTag("tutor_scope_all_devoir")
                            ) {
                                Text(
                                    text = stringResource(R.string.tutor_history_all_count, devoirExplanationCount),
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    // Barre d'aide progressive : Indice 1 -> Indice 2 -> Solution complète + Expliquer
                    val ochre = adaptiveOchre()
                    val emerald = adaptiveEmerald()
                    Text(
                        text = stringResource(R.string.tutor_progressive_label),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    LazyRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.testTag("tutor_progressive_bar")
                    ) {
                        item {
                            ProgressiveActionChip(
                                label = stringResource(R.string.tutor_hint_1),
                                icon = if (hint1Done) Icons.Filled.CheckCircle else Icons.Filled.Lightbulb,
                                accent = ochre,
                                enabled = !isBusy,
                                testTag = "tutor_hint1_btn",
                                onClick = { onTriggerHelpMode(TutorHelpMode.HINT_1) }
                            )
                        }
                        item {
                            ProgressiveActionChip(
                                label = stringResource(R.string.tutor_hint_2),
                                icon = if (hint2Done) Icons.Filled.CheckCircle else Icons.Filled.Lightbulb,
                                accent = MaterialTheme.colorScheme.primary,
                                enabled = !isBusy,
                                testTag = "tutor_hint2_btn",
                                onClick = { onTriggerHelpMode(TutorHelpMode.HINT_2) }
                            )
                        }
                        item {
                            ProgressiveActionChip(
                                label = stringResource(R.string.tutor_full_solution),
                                icon = Icons.Filled.School,
                                accent = emerald,
                                enabled = !isBusy,
                                testTag = "tutor_solution_btn",
                                onClick = { onTriggerHelpMode(TutorHelpMode.SOLUTION) }
                            )
                        }
                        item {
                            ProgressiveActionChip(
                                label = stringResource(R.string.tutor_explain_chip),
                                icon = Icons.Filled.Psychology,
                                accent = MaterialTheme.colorScheme.tertiary,
                                enabled = !isBusy,
                                testTag = "tutor_explain_btn",
                                onClick = { onTriggerHelpMode(TutorHelpMode.EXPLAIN) }
                            )
                        }
                    }
                }
            }

            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)

            // Scrollable Conversation Thread
            LazyColumn(
                state = listState,
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .testTag("tutor_messages_list"),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                if (messages.isEmpty() && uiState.generationState == TutorGenerationState.Idle) {
                    item(key = "tutor_welcome") {
                        WelcomeTutorCard(
                            exerciseTitle = exercise.title,
                            levelLabel = devoir.level.localizedLabel(),
                            isArabic = isArabicInput
                        )
                    }
                }

                items(messages, key = { it.id }) { msg ->
                    TutorMessageBubble(
                        message = msg,
                        showPageBadge = uiState.historyScope == TutorHistoryScope.ALL_DEVOIR
                    )
                }

                // Live Loading / Streaming / Error Item
                when (val gen = uiState.generationState) {
                    is TutorGenerationState.Loading -> {
                        item(key = "tutor_loading") {
                            LoadingBubble(isArabic = gen.language.isRtl)
                        }
                    }

                    is TutorGenerationState.Streaming -> {
                        item(key = "tutor_streaming") {
                            StreamingAssistantBubble(
                                text = gen.partialText,
                                mode = gen.mode,
                                language = gen.language
                            )
                        }
                    }

                    is TutorGenerationState.Error -> {
                        item(key = "tutor_error") {
                            TutorErrorCard(
                                errorMessage = gen.message,
                                isArabic = isArabicInput,
                                onRetry = onRetry
                            )
                        }
                    }

                    TutorGenerationState.Idle -> Unit
                }
            }

            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)

            // Bottom "Pose ta question" Input Bar (RTL-aware)
            CompositionLocalProvider(
                LocalLayoutDirection provides if (isArabicInput) LayoutDirection.Rtl else LayoutDirection.Ltr
            ) {
                Surface(
                    color = MaterialTheme.colorScheme.surface,
                    tonalElevation = 3.dp
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 12.dp, vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OutlinedTextField(
                            value = uiState.questionDraft,
                            onValueChange = onQuestionChange,
                            modifier = Modifier
                                .weight(1f)
                                .testTag("tutor_question_input"),
                            placeholder = {
                                Text(
                                    text = stringResource(R.string.tutor_question_placeholder),
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            },
                            maxLines = 3,
                            enabled = !isBusy,
                            shape = RoundedCornerShape(16.dp)
                        )

                        FilledIconButton(
                            onClick = onSendQuestion,
                            enabled = !isBusy && uiState.questionDraft.isNotBlank(),
                            modifier = Modifier
                                .size(50.dp)
                                .testTag("tutor_send_btn")
                        ) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.Send,
                                contentDescription = stringResource(R.string.tutor_send_cd)
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun ProgressiveActionChip(
    label: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    accent: Color,
    enabled: Boolean,
    testTag: String,
    onClick: () -> Unit
) {
    AssistChip(
        onClick = onClick,
        enabled = enabled,
        modifier = Modifier.testTag(testTag),
        leadingIcon = {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = accent,
                modifier = Modifier.size(16.dp)
            )
        },
        label = {
            Text(
                text = label,
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.SemiBold
            )
        },
        colors = AssistChipDefaults.assistChipColors(
            containerColor = accent.copy(alpha = 0.10f),
            labelColor = MaterialTheme.colorScheme.onSurface
        ),
        border = BorderStroke(1.dp, accent.copy(alpha = 0.35f))
    )
}

@Composable
private fun WelcomeTutorCard(
    exerciseTitle: String,
    levelLabel: String,
    isArabic: Boolean
) {
    CompositionLocalProvider(
        LocalLayoutDirection provides if (isArabic) LayoutDirection.Rtl else LayoutDirection.Ltr
    ) {
        Surface(
            shape = MaterialTheme.shapes.large,
            color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.42f),
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.25f))
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Text(
                    text = stringResource(R.string.tutor_welcome_title, levelLabel),
                    style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.primary
                )
                Text(
                    text = stringResource(R.string.tutor_welcome_body, exerciseTitle),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }
        }
    }
}

@Composable
private fun TutorMessageBubble(
    message: TutorMessageEntity,
    showPageBadge: Boolean = false
) {
    val isUser = message.role == "user"
    val lang = TutorLanguage.fromCode(message.language)
    val mode = TutorHelpMode.fromId(message.mode)

    val bubbleDirection = if (lang.isRtl) LayoutDirection.Rtl else LayoutDirection.Ltr
    val alignment = if (isUser) Alignment.End else Alignment.Start
    val bubbleColor = if (isUser) {
        MaterialTheme.colorScheme.primary
    } else {
        MaterialTheme.colorScheme.surface
    }
    val textColor = if (isUser) {
        MaterialTheme.colorScheme.onPrimary
    } else {
        MaterialTheme.colorScheme.onSurface
    }

    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = alignment
    ) {
        CompositionLocalProvider(LocalLayoutDirection provides bubbleDirection) {
            Surface(
                modifier = Modifier.widthIn(max = 340.dp),
                shape = RoundedCornerShape(
                    topStart = 16.dp,
                    topEnd = 16.dp,
                    bottomStart = if (isUser) 16.dp else 4.dp,
                    bottomEnd = if (isUser) 4.dp else 16.dp
                ),
                color = bubbleColor,
                tonalElevation = if (isUser) 0.dp else 2.dp,
                border = if (isUser) null else BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    if (!isUser) {
                        val headerText = if (showPageBadge && message.exerciseNumber > 0) {
                            "${stringResource(R.string.tutor_sender_prefix, mode.label(lang))} · ${
                                stringResource(R.string.tutor_pdf_page_label, message.exerciseNumber)
                            }"
                        } else {
                            stringResource(R.string.tutor_sender_prefix, mode.label(lang))
                        }
                        Text(
                            text = headerText,
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.primary,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                    }
                    Text(
                        text = message.content,
                        style = MaterialTheme.typography.bodyMedium,
                        color = textColor
                    )
                }
            }
        }
    }
}

@Composable
private fun LoadingBubble(isArabic: Boolean) {
    CompositionLocalProvider(
        LocalLayoutDirection provides if (isArabic) LayoutDirection.Rtl else LayoutDirection.Ltr
    ) {
        Surface(
            shape = RoundedCornerShape(16.dp),
            color = MaterialTheme.colorScheme.surface,
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 14.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                CircularProgressIndicator(
                    modifier = Modifier.size(18.dp),
                    strokeWidth = 2.2.dp
                )
                Text(
                    text = stringResource(R.string.tutor_loading),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
private fun StreamingAssistantBubble(
    text: String,
    mode: TutorHelpMode,
    language: TutorLanguage
) {
    CompositionLocalProvider(
        LocalLayoutDirection provides if (language.isRtl) LayoutDirection.Rtl else LayoutDirection.Ltr
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth(0.95f)
                .testTag("tutor_streaming_bubble"),
            shape = RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp, bottomEnd = 16.dp, bottomStart = 4.dp),
            color = MaterialTheme.colorScheme.surface,
            tonalElevation = 2.dp,
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.4f))
        ) {
            Column(modifier = Modifier.padding(14.dp)) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(14.dp),
                        strokeWidth = 2.dp
                    )
                    Text(
                        text = stringResource(R.string.tutor_sender_prefix, mode.label(language)),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.Bold
                    )
                }
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = text,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }
        }
    }
}

@Composable
private fun TutorErrorCard(
    errorMessage: String,
    isArabic: Boolean,
    onRetry: () -> Unit
) {
    val crimson = adaptiveCrimson()
    CompositionLocalProvider(
        LocalLayoutDirection provides if (isArabic) LayoutDirection.Rtl else LayoutDirection.Ltr
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .testTag("tutor_error_card"),
            shape = MaterialTheme.shapes.medium,
            color = crimson.copy(alpha = 0.11f),
            border = BorderStroke(1.dp, crimson.copy(alpha = 0.45f))
        ) {
            Column(
                modifier = Modifier.padding(14.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.Top,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        imageVector = Icons.Filled.ErrorOutline,
                        contentDescription = null,
                        tint = crimson
                    )
                    Text(
                        text = errorMessage,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier.weight(1f)
                    )
                }

                FilledTonalButton(
                    onClick = onRetry,
                    modifier = Modifier.testTag("tutor_retry_btn"),
                    colors = ButtonDefaults.filledTonalButtonColors(
                        containerColor = crimson.copy(alpha = 0.18f),
                        contentColor = crimson
                    )
                ) {
                    Icon(
                        imageVector = Icons.Filled.Refresh,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(stringResource(R.string.tutor_retry))
                }
            }
        }
    }
}
