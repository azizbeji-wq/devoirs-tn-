package com.example.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AssignmentTurnedIn
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.PlayCircleFilled
import androidx.compose.material.icons.filled.StopCircle
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.example.R
import com.example.domain.progress.ProgressAnalytics
import com.example.ui.viewmodel.ExamModeUiState

/**
 * Carte d'invitation à lancer le mode examen en conditions réelles
 * (chronomètre officiel, corrigés verrouillés et auto-évaluation finale).
 */
@Composable
fun ExamModeLaunchCard(
    durationMinutes: Int,
    attemptsCount: Int,
    bestScoreLabel: String?,
    onStartRealConditions: () -> Unit,
    modifier: Modifier = Modifier
) {
    val emerald = adaptiveEmerald()
    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.large,
        color = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.55f),
        border = BorderStroke(1.2.dp, MaterialTheme.colorScheme.secondary.copy(alpha = 0.35f))
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
                    imageVector = Icons.Filled.AssignmentTurnedIn,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.secondary,
                    modifier = Modifier.size(22.dp)
                )
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = stringResource(R.string.exam_mode_card_title),
                        style = MaterialTheme.typography.titleSmall,
                        color = MaterialTheme.colorScheme.onSecondaryContainer
                    )
                    Text(
                        text = stringResource(
                            R.string.exam_mode_card_desc,
                            formatDurationMinutes(durationMinutes)
                        ),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSecondaryContainer.copy(alpha = 0.85f)
                    )
                }
            }

            if (attemptsCount > 0 && bestScoreLabel != null) {
                Text(
                    text = stringResource(
                        R.string.exam_mode_past_attempts,
                        attemptsCount,
                        bestScoreLabel
                    ),
                    style = MaterialTheme.typography.labelMedium,
                    color = emerald,
                    fontWeight = FontWeight.SemiBold
                )
            }

            Button(
                onClick = onStartRealConditions,
                modifier = Modifier
                    .fillMaxWidth()
                    .minimumInteractiveComponentSize()
                    .testTag("start_exam_mode_btn")
            ) {
                Icon(
                    imageVector = Icons.Filled.PlayCircleFilled,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(stringResource(R.string.exam_mode_start_btn))
            }
        }
    }
}

/**
 * Bannière affichée pendant une épreuve active : décompte restant,
 * barre de progression et boutons de fin ou d'abandon.
 */
@Composable
fun ExamModeActiveBanner(
    state: ExamModeUiState,
    onFinishExam: () -> Unit,
    onCancelExam: () -> Unit,
    modifier: Modifier = Modifier
) {
    val crimson = adaptiveCrimson()
    val progress = if (state.officialDurationSeconds > 0) {
        state.remainingSeconds.toFloat() / state.officialDurationSeconds.toFloat()
    } else 0f

    val hours = state.remainingSeconds / 3600
    val minutes = (state.remainingSeconds % 3600) / 60
    val seconds = state.remainingSeconds % 60
    val formatted = "%02d:%02d:%02d".format(hours, minutes, seconds)

    Surface(
        modifier = modifier
            .fillMaxWidth()
            .testTag("exam_mode_active_banner"),
        shape = MaterialTheme.shapes.large,
        color = crimson.copy(alpha = 0.12f),
        border = BorderStroke(1.5.dp, crimson.copy(alpha = 0.55f))
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
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Filled.Timer,
                        contentDescription = null,
                        tint = crimson,
                        modifier = Modifier.size(22.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Column {
                        Text(
                            text = stringResource(R.string.exam_mode_active_title),
                            style = MaterialTheme.typography.labelLarge,
                            color = crimson,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = formatted,
                            style = MaterialTheme.typography.headlineSmall,
                            color = MaterialTheme.colorScheme.onSurface,
                            modifier = Modifier.testTag("exam_mode_countdown")
                        )
                    }
                }

                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = crimson.copy(alpha = 0.16f)
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Lock,
                            contentDescription = null,
                            tint = crimson,
                            modifier = Modifier.size(14.dp)
                        )
                        Text(
                            text = stringResource(R.string.exam_mode_locked_short),
                            style = MaterialTheme.typography.labelSmall,
                            color = crimson,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }
            }

            LinearProgressIndicator(
                progress = { progress },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(7.dp)
                    .clip(RoundedCornerShape(4.dp)),
                color = crimson,
                trackColor = MaterialTheme.colorScheme.surface
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Button(
                    onClick = onFinishExam,
                    modifier = Modifier
                        .weight(1f)
                        .minimumInteractiveComponentSize()
                        .testTag("finish_exam_mode_btn"),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = crimson,
                        contentColor = Color.White
                    )
                ) {
                    Icon(
                        imageVector = Icons.Filled.StopCircle,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(stringResource(R.string.exam_mode_finish_btn))
                }

                TextButton(
                    onClick = onCancelExam,
                    modifier = Modifier
                        .minimumInteractiveComponentSize()
                        .testTag("cancel_exam_mode_btn")
                ) {
                    Text(stringResource(R.string.exam_mode_cancel_btn))
                }
            }
        }
    }
}

/**
 * Formulaire d'auto-évaluation exercice par exercice à l'issue de l'épreuve.
 */
@Composable
fun ExamSelfEvaluationCard(
    state: ExamModeUiState,
    onScoreChange: (exerciseNumber: Int, rawText: String) -> Unit,
    onSaveAttempt: () -> Unit,
    onCancel: () -> Unit,
    modifier: Modifier = Modifier
) {
    val emerald = adaptiveEmerald()
    Surface(
        modifier = modifier
            .fillMaxWidth()
            .testTag("exam_self_eval_card"),
        shape = MaterialTheme.shapes.large,
        color = MaterialTheme.colorScheme.surface,
        tonalElevation = 3.dp,
        border = BorderStroke(1.5.dp, emerald.copy(alpha = 0.6f))
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                text = stringResource(R.string.exam_eval_title),
                style = MaterialTheme.typography.titleMedium,
                color = emerald,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = stringResource(R.string.exam_eval_subtitle) + " " +
                    stringResource(
                        R.string.exam_eval_real_time,
                        ProgressAnalytics.formatStudyDuration(state.actualElapsedSeconds.toLong())
                    ),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)

            state.exerciseInputs.forEach { input ->
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = stringResource(R.string.exercise_short_heading, input.exerciseNumber),
                            style = MaterialTheme.typography.labelLarge,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = input.title,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1
                        )
                        if (input.error != null) {
                            Text(
                                text = input.error,
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.error
                            )
                        }
                    }

                    OutlinedTextField(
                        value = input.scoreText,
                        onValueChange = { onScoreChange(input.exerciseNumber, it) },
                        modifier = Modifier
                            .width(116.dp)
                            .testTag("eval_score_ex_${input.exerciseNumber}"),
                        singleLine = true,
                        isError = input.error != null,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        label = {
                            val maxStr = if (input.maxPoints % 1.0 == 0.0) {
                                input.maxPoints.toInt().toString()
                            } else {
                                "%.1f".format(input.maxPoints)
                            }
                            Text("/ $maxStr")
                        }
                    )
                }
            }

            if (state.validationBanner != null) {
                Text(
                    text = state.validationBanner,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.error
                )
            }

            Surface(
                shape = MaterialTheme.shapes.medium,
                color = emerald.copy(alpha = 0.12f)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 14.dp, vertical = 10.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = stringResource(R.string.exam_eval_estimated_total),
                        style = MaterialTheme.typography.labelLarge
                    )
                    Text(
                        text = "${"%.2f".format(state.computedTotalScore)} / ${"%.0f".format(state.computedMaxScore)}",
                        style = MaterialTheme.typography.titleMedium,
                        color = emerald,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                OutlinedButton(
                    onClick = onCancel,
                    modifier = Modifier
                        .weight(0.7f)
                        .minimumInteractiveComponentSize()
                ) {
                    Text(stringResource(R.string.action_cancel))
                }
                Button(
                    onClick = onSaveAttempt,
                    enabled = !state.isSaving,
                    modifier = Modifier
                        .weight(1.3f)
                        .minimumInteractiveComponentSize()
                        .testTag("save_exam_eval_btn"),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = emerald,
                        contentColor = Color.White
                    )
                ) {
                    Text(stringResource(R.string.exam_eval_save_btn))
                }
            }
        }
    }
}

/**
 * Carte de confirmation affichée après l'enregistrement d'une tentative en base Room.
 */
@Composable
fun ExamSavedSummaryCard(
    totalScore: Double,
    maxScore: Double,
    actualDurationSeconds: Int,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier
) {
    val emerald = adaptiveEmerald()
    Surface(
        modifier = modifier
            .fillMaxWidth()
            .testTag("exam_saved_summary_card"),
        shape = MaterialTheme.shapes.large,
        color = emerald.copy(alpha = 0.12f),
        border = BorderStroke(1.5.dp, emerald.copy(alpha = 0.6f))
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(
                    imageVector = Icons.Filled.CheckCircle,
                    contentDescription = null,
                    tint = emerald
                )
                Text(
                    text = stringResource(R.string.exam_eval_saved_title),
                    style = MaterialTheme.typography.titleMedium,
                    color = emerald,
                    fontWeight = FontWeight.Bold
                )
            }
            Text(
                text = stringResource(
                    R.string.exam_eval_saved_body,
                    "%.2f".format(totalScore),
                    "%.0f".format(maxScore),
                    ProgressAnalytics.formatStudyDuration(actualDurationSeconds.toLong())
                ),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurface
            )
            Spacer(modifier = Modifier.height(2.dp))
            OutlinedButton(
                onClick = onDismiss,
                modifier = Modifier.minimumInteractiveComponentSize()
            ) {
                Text(stringResource(R.string.exam_eval_back_btn))
            }
        }
    }
}
