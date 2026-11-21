package com.example.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.Cancel
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.R
import com.example.data.local.FlashQcmChapter
import com.example.ui.viewmodel.ActiveQcmSessionState
import com.example.ui.viewmodel.ChapterQcmStats

/**
 * Carte présentant un chapitre de QCM Flash avec son meilleur et dernier score.
 */
@Composable
fun FlashQcmChapterCard(
    chapter: FlashQcmChapter,
    stats: ChapterQcmStats?,
    onStart: () -> Unit,
    modifier: Modifier = Modifier
) {
    val accent = subjectAccentColor(chapter.subject)
    val emerald = adaptiveEmerald()
    val crimson = adaptiveCrimson()

    Surface(
        modifier = modifier
            .fillMaxWidth()
            .testTag("qcm_chapter_card_${chapter.id}"),
        shape = MaterialTheme.shapes.medium,
        color = MaterialTheme.colorScheme.surface,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Surface(
                shape = RoundedCornerShape(12.dp),
                color = accent.copy(alpha = 0.14f)
            ) {
                Icon(
                    imageVector = Icons.Filled.Bolt,
                    contentDescription = null,
                    tint = accent,
                    modifier = Modifier.padding(10.dp)
                )
            }

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = chapter.chapterName,
                    style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = "${localizedSubjectName(chapter.subject)} · ${chapter.levelLabel}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                if (stats != null) {
                    Spacer(modifier = Modifier.height(3.dp))
                    Text(
                        text = stringResource(
                            R.string.qcm_stats_line,
                            stats.bestScore,
                            stats.totalQuestions,
                            stats.lastScore
                        ),
                        style = MaterialTheme.typography.labelSmall,
                        color = if (stats.lastScore >= 3) emerald else crimson,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }

            Button(
                onClick = onStart,
                modifier = Modifier
                    .minimumInteractiveComponentSize()
                    .testTag("start_qcm_${chapter.id}")
            ) {
                Text(stringResource(R.string.qcm_start_btn))
            }
        }
    }
}

/**
 * Feuille modale interactive pour jouer une session QCM Flash (5 questions)
 * avec correction immédiate et sauvegarde dans le suivi.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FlashQcmBottomSheet(
    session: ActiveQcmSessionState,
    onSelectOption: (Int) -> Unit,
    onNextOrFinish: () -> Unit,
    onDismiss: () -> Unit
) {
    val chapter = session.chapter ?: return
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val emerald = adaptiveEmerald()
    val crimson = adaptiveCrimson()

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = stringResource(R.string.qcm_sheet_title, chapter.chapterName),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "${localizedSubjectName(chapter.subject)} · ${chapter.levelLabel}",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                IconButton(
                    onClick = onDismiss,
                    modifier = Modifier.minimumInteractiveComponentSize()
                ) {
                    Icon(
                        imageVector = Icons.Filled.Close,
                        contentDescription = stringResource(R.string.cd_close_dialog)
                    )
                }
            }

            if (session.isCompleted) {
                val total = chapter.questions.size
                val ratio = if (total > 0) session.correctCount.toFloat() / total else 0f
                val resultColor = if (ratio >= 0.6f) emerald else crimson

                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("qcm_final_summary"),
                    shape = MaterialTheme.shapes.large,
                    color = resultColor.copy(alpha = 0.12f),
                    border = BorderStroke(1.5.dp, resultColor.copy(alpha = 0.5f))
                ) {
                    Column(
                        modifier = Modifier.padding(20.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Filled.EmojiEvents,
                            contentDescription = null,
                            tint = resultColor,
                            modifier = Modifier.size(40.dp)
                        )
                        Text(
                            text = stringResource(R.string.qcm_score_final, session.correctCount, total),
                            style = MaterialTheme.typography.headlineMedium,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = when {
                                session.correctCount == total -> stringResource(R.string.qcm_feedback_perfect)
                                session.correctCount >= 3 -> stringResource(R.string.qcm_feedback_good)
                                else -> stringResource(R.string.qcm_feedback_improve)
                            },
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        if (session.isSaved) {
                            Text(
                                text = stringResource(R.string.qcm_result_saved),
                                style = MaterialTheme.typography.labelMedium,
                                color = emerald
                            )
                        }
                    }
                }

                Button(
                    onClick = onDismiss,
                    modifier = Modifier
                        .fillMaxWidth()
                        .minimumInteractiveComponentSize()
                        .testTag("qcm_close_result_btn")
                ) {
                    Text(stringResource(R.string.qcm_close_btn))
                }
            } else {
                val question = chapter.questions[session.currentIndex]
                val total = chapter.questions.size
                val progress = (session.currentIndex + 1).toFloat() / total.toFloat()

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = stringResource(R.string.qcm_question_counter, session.currentIndex + 1, total),
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Text(
                        text = stringResource(R.string.qcm_current_score, session.correctCount),
                        style = MaterialTheme.typography.labelLarge,
                        color = emerald
                    )
                }

                LinearProgressIndicator(
                    progress = { progress },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(6.dp)
                        .clip(RoundedCornerShape(3.dp))
                )

                Text(
                    text = question.prompt,
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurface
                )

                question.options.forEachIndexed { idx, option ->
                    val isSelected = session.selectedOptionIndex == idx
                    val isCorrect = idx == question.correctIndex
                    val borderColor = when {
                        !session.isAnswerSubmitted -> MaterialTheme.colorScheme.outlineVariant
                        isCorrect -> emerald
                        isSelected && !isCorrect -> crimson
                        else -> MaterialTheme.colorScheme.outlineVariant
                    }
                    val bgColor = when {
                        !session.isAnswerSubmitted -> MaterialTheme.colorScheme.surface
                        isCorrect -> emerald.copy(alpha = 0.14f)
                        isSelected && !isCorrect -> crimson.copy(alpha = 0.12f)
                        else -> MaterialTheme.colorScheme.surface
                    }

                    Surface(
                        modifier = Modifier
                            .fillMaxWidth()
                            .minimumInteractiveComponentSize()
                            .clip(MaterialTheme.shapes.medium)
                            .clickable(enabled = !session.isAnswerSubmitted) { onSelectOption(idx) }
                            .testTag("qcm_option_$idx"),
                        shape = MaterialTheme.shapes.medium,
                        color = bgColor,
                        border = BorderStroke(1.2.dp, borderColor)
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 14.dp, vertical = 12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = option,
                                style = MaterialTheme.typography.bodyLarge,
                                modifier = Modifier.weight(1f)
                            )
                            if (session.isAnswerSubmitted && (isCorrect || isSelected)) {
                                Spacer(modifier = Modifier.width(8.dp))
                                Icon(
                                    imageVector = if (isCorrect) Icons.Filled.CheckCircle else Icons.Filled.Cancel,
                                    contentDescription = null,
                                    tint = if (isCorrect) emerald else crimson,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                        }
                    }
                }

                if (session.isAnswerSubmitted) {
                    Surface(
                        shape = MaterialTheme.shapes.medium,
                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.65f)
                    ) {
                        Text(
                            text = stringResource(R.string.qcm_explanation_prefix, question.explanation),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurface,
                            modifier = Modifier.padding(12.dp)
                        )
                    }

                    val isLast = session.currentIndex == total - 1
                    Button(
                        onClick = onNextOrFinish,
                        modifier = Modifier
                            .fillMaxWidth()
                            .minimumInteractiveComponentSize()
                            .testTag("qcm_next_btn")
                    ) {
                        Text(
                            stringResource(if (isLast) R.string.qcm_finish_btn else R.string.qcm_next_btn)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))
        }
    }
}
