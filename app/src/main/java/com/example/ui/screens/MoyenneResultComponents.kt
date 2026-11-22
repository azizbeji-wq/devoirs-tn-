package com.example.ui.screens

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
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
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material.icons.filled.RestartAlt
import androidx.compose.material.icons.filled.TrendingUp
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.R
import com.example.domain.bac.BacMention
import com.example.domain.bac.BacSectionConfig
import com.example.domain.bac.BacSession
import com.example.ui.components.adaptiveCrimson
import com.example.ui.components.adaptiveEmerald
import com.example.ui.components.adaptiveOchre
import com.example.ui.components.localizedLabel
import com.example.ui.viewmodel.MoyenneUiState
import java.util.Locale

/**
 * Carte de résultat final officiel avec compteur numérique animé, barre de progression
 * graduée (0 à 20) et paliers de mention (10 · 12 · 14 · 16) animés en temps réel.
 */
@Composable
internal fun OfficialResultProgressCard(
    generalAvg: Double?,
    annualAvg: Double?,
    examAvg: Double?,
    mention: BacMention,
    session: BacSession,
    totalCoeffSum: Double,
    hasCustomCoeffs: Boolean,
    modifier: Modifier = Modifier
) {
    val emerald = adaptiveEmerald()
    val ochre = adaptiveOchre()
    val crimson = adaptiveCrimson()
    val primary = MaterialTheme.colorScheme.primary

    val displayAvg = generalAvg ?: examAvg ?: annualAvg
    val isOfficialGeneral = generalAvg != null

    val targetAccent = when {
        displayAvg == null -> primary
        displayAvg >= 16.0 -> emerald
        displayAvg >= 14.0 -> emerald
        displayAvg >= 12.0 -> primary
        displayAvg >= 10.0 -> ochre
        else -> crimson
    }

    val animatedAccent by animateColorAsState(
        targetValue = targetAccent,
        animationSpec = tween(durationMillis = 420, easing = FastOutSlowInEasing),
        label = "official_result_accent"
    )

    val animatedScore by animateFloatAsState(
        targetValue = (displayAvg ?: 0.0).toFloat().coerceIn(0f, 20f),
        animationSpec = tween(durationMillis = 700, easing = FastOutSlowInEasing),
        label = "official_result_score_counter"
    )

    val animatedProgress by animateFloatAsState(
        targetValue = ((displayAvg ?: 0.0) / 20.0).toFloat().coerceIn(0f, 1f),
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioLowBouncy,
            stiffness = Spring.StiffnessLow
        ),
        label = "official_result_progress_bar"
    )

    Surface(
        modifier = modifier
            .fillMaxWidth()
            .testTag("bac_official_result_card"),
        shape = MaterialTheme.shapes.large,
        color = MaterialTheme.colorScheme.surface,
        tonalElevation = 2.dp,
        border = BorderStroke(
            width = if (displayAvg != null) 1.5.dp else 1.dp,
            color = if (displayAvg != null) {
                animatedAccent.copy(alpha = 0.45f)
            } else {
                MaterialTheme.colorScheme.outlineVariant
            }
        )
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // En-tête : titre + statut barème (officiel / personnalisé) + somme des coefficients
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    Surface(
                        shape = RoundedCornerShape(10.dp),
                        color = animatedAccent.copy(alpha = 0.14f)
                    ) {
                        Icon(
                            imageVector = if (displayAvg != null && displayAvg >= 10.0) {
                                Icons.Filled.EmojiEvents
                            } else {
                                Icons.Filled.TrendingUp
                            },
                            contentDescription = stringResource(R.string.bac_result_card_title),
                            tint = animatedAccent,
                            modifier = Modifier
                                .padding(7.dp)
                                .size(18.dp)
                        )
                    }
                    Column {
                        Text(
                            text = if (isOfficialGeneral) {
                                stringResource(R.string.bac_result_card_title)
                            } else {
                                stringResource(R.string.bac_result_card_preview_title)
                            },
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Text(
                            text = stringResource(
                                R.string.bac_subject_coeffs_total,
                                formatDisplayCoeff(totalCoeffSum)
                            ),
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.testTag("bac_total_coeffs_text")
                        )
                    }
                }

                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = if (hasCustomCoeffs) {
                        MaterialTheme.colorScheme.tertiaryContainer
                    } else {
                        MaterialTheme.colorScheme.secondaryContainer
                    }
                ) {
                    Text(
                        text = if (hasCustomCoeffs) {
                            stringResource(R.string.bac_subject_coeffs_custom_badge)
                        } else {
                            stringResource(R.string.bac_subject_coeffs_official_badge)
                        },
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.SemiBold,
                        color = if (hasCustomCoeffs) {
                            MaterialTheme.colorScheme.onTertiaryContainer
                        } else {
                            MaterialTheme.colorScheme.onSecondaryContainer
                        },
                        modifier = Modifier
                            .padding(horizontal = 8.dp, vertical = 4.dp)
                            .testTag("bac_coeffs_mode_badge")
                    )
                }
            }

            // Score animé + mention / session
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Bottom
            ) {
                Row(
                    verticalAlignment = Alignment.Bottom,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Text(
                        text = if (displayAvg != null) {
                            String.format(Locale.US, "%.2f", animatedScore)
                        } else {
                            "--.--"
                        },
                        style = MaterialTheme.typography.displaySmall,
                        fontWeight = FontWeight.ExtraBold,
                        color = if (displayAvg != null) animatedAccent else MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.testTag("bac_official_animated_score")
                    )
                    Text(
                        text = "/ 20",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(bottom = 5.dp)
                    )
                }

                Column(horizontalAlignment = Alignment.End) {
                    if (isOfficialGeneral && mention != BacMention.INCONNU) {
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = animatedAccent.copy(alpha = 0.14f)
                        ) {
                            Text(
                                text = mention.localizedLabel(),
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.Bold,
                                color = animatedAccent,
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                            )
                        }
                    } else if (displayAvg != null) {
                        Text(
                            text = if (examAvg != null) {
                                stringResource(R.string.bac_exam_short)
                            } else {
                                stringResource(R.string.bac_annual_short)
                            },
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.SemiBold,
                            color = animatedAccent
                        )
                    }
                    if (isOfficialGeneral) {
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = session.localizedLabel(),
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            // Barre de progression animée 0..20 avec paliers 10, 12, 14, 16
            AnimatedMentionProgressBar(
                progressFraction = animatedProgress,
                accentColor = animatedAccent,
                activeScore = displayAvg,
                modifier = Modifier.fillMaxWidth()
            )

            // Message contextuel : points restants pour le palier suivant ou consigne
            val guidanceText = when {
                displayAvg == null -> stringResource(R.string.bac_result_waiting_hint)
                !isOfficialGeneral && annualAvg != null -> stringResource(R.string.bac_result_annual_only_hint)
                !isOfficialGeneral && examAvg != null -> stringResource(R.string.bac_result_exam_only_hint)
                displayAvg >= 16.0 -> stringResource(R.string.bac_result_top_mention)
                else -> {
                    val (nextThreshold, nextLabelRes) = when {
                        displayAvg < 10.0 -> 10 to R.string.bac_mention_p
                        displayAvg < 12.0 -> 12 to R.string.bac_mention_ab
                        displayAvg < 14.0 -> 14 to R.string.bac_mention_b
                        else -> 16 to R.string.bac_mention_tb
                    }
                    val delta = (nextThreshold - displayAvg).coerceAtLeast(0.01)
                    stringResource(
                        R.string.bac_result_next_mention,
                        String.format(Locale.US, "%.2f", delta),
                        stringResource(nextLabelRes),
                        nextThreshold
                    )
                }
            }

            Text(
                text = guidanceText,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.testTag("bac_official_progress_hint")
            )
        }
    }
}

@Composable
private fun AnimatedMentionProgressBar(
    progressFraction: Float,
    accentColor: Color,
    activeScore: Double?,
    modifier: Modifier = Modifier
) {
    val trackColor = MaterialTheme.colorScheme.surfaceVariant
    val tickColor = MaterialTheme.colorScheme.surface
    val outlineColor = MaterialTheme.colorScheme.outlineVariant
    val secondaryAccent = MaterialTheme.colorScheme.primary

    val milestones = listOf(
        10 to stringResource(R.string.bac_milestone_pass),
        12 to stringResource(R.string.bac_milestone_ab),
        14 to stringResource(R.string.bac_milestone_b),
        16 to stringResource(R.string.bac_milestone_tb)
    )

    Column(
        modifier = modifier.testTag("bac_animated_progress_bar"),
        verticalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        Canvas(
            modifier = Modifier
                .fillMaxWidth()
                .height(16.dp)
        ) {
            val barHeight = 10.dp.toPx()
            val barTop = (size.height - barHeight) / 2f
            val corner = CornerRadius(barHeight / 2f, barHeight / 2f)

            // Piste complète
            drawRoundRect(
                color = trackColor,
                topLeft = Offset(0f, barTop),
                size = Size(size.width, barHeight),
                cornerRadius = corner
            )

            // Remplissage animé avec dégradé subtil
            val fillWidth = (size.width * progressFraction).coerceIn(0f, size.width)
            if (fillWidth > 1f) {
                drawRoundRect(
                    brush = Brush.horizontalGradient(
                        colors = listOf(
                            secondaryAccent.copy(alpha = 0.85f),
                            accentColor
                        ),
                        startX = 0f,
                        endX = fillWidth.coerceAtLeast(1f)
                    ),
                    topLeft = Offset(0f, barTop),
                    size = Size(fillWidth, barHeight),
                    cornerRadius = corner
                )
            }

            // Repères officiels à 10, 12, 14, 16 / 20
            milestones.forEach { (grade, _) ->
                val x = size.width * (grade / 20f)
                drawLine(
                    color = if (progressFraction >= grade / 20f) tickColor else outlineColor,
                    start = Offset(x, barTop + 1.5.dp.toPx()),
                    end = Offset(x, barTop + barHeight - 1.5.dp.toPx()),
                    strokeWidth = 2.dp.toPx()
                )
            }

            // Curseur circulaire lumineux en bout de barre
            if (activeScore != null && fillWidth > 0f) {
                val centerX = fillWidth.coerceIn(barHeight / 2f, size.width - barHeight / 2f)
                val centerY = size.height / 2f
                drawCircle(
                    color = accentColor.copy(alpha = 0.28f),
                    radius = 8.dp.toPx(),
                    center = Offset(centerX, centerY)
                )
                drawCircle(
                    color = accentColor,
                    radius = 5.dp.toPx(),
                    center = Offset(centerX, centerY)
                )
                drawCircle(
                    color = tickColor,
                    radius = 2.dp.toPx(),
                    center = Offset(centerX, centerY)
                )
            }
        }

        // Légende des seuils officiels alignée sous la barre
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "0",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            milestones.forEach { (grade, label) ->
                val reached = activeScore != null && activeScore >= grade
                Text(
                    text = label,
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = if (reached) FontWeight.Bold else FontWeight.Normal,
                    color = if (reached) accentColor else MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Text(
                text = "20",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

/**
 * Section de définition et personnalisation des coefficients par matière dans le panneau « Coefficients ».
 */
@Composable
internal fun SubjectCoefficientsConfigSection(
    section: BacSectionConfig,
    effectiveSection: BacSectionConfig,
    state: MoyenneUiState,
    onUpdateSubjectCoeff: (subjectId: String, value: String) -> Unit,
    onStepSubjectCoeff: (subjectId: String, delta: Double) -> Unit,
    onResetSubjectCoeffs: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .testTag("bac_subject_coeffs_editor"),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = stringResource(R.string.bac_subject_coeffs_title, section.shortTitle),
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = stringResource(
                        R.string.bac_subject_coeffs_total,
                        formatDisplayCoeff(state.totalSubjectCoeffSum)
                    ),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.Medium
                )
            }
            if (state.hasCustomSubjectCoeffs) {
                TextButton(
                    onClick = onResetSubjectCoeffs,
                    modifier = Modifier
                        .minimumInteractiveComponentSize()
                        .testTag("bac_reset_subject_coeffs_btn")
                ) {
                    Icon(
                        imageVector = Icons.Filled.RestartAlt,
                        contentDescription = stringResource(R.string.bac_subject_coeffs_reset),
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = stringResource(R.string.bac_subject_coeffs_reset),
                        style = MaterialTheme.typography.labelMedium
                    )
                }
            }
        }

        section.subjects.forEachIndexed { index, officialSubject ->
            val effectiveSubject = effectiveSection.subjects.getOrNull(index) ?: officialSubject
            val displayText = state.customSubjectCoeffTexts[officialSubject.id]
                ?: formatDisplayCoeff(officialSubject.examCoeff)
            val isInvalid = state.isSubjectCoeffInvalid(officialSubject.id)
            val isCustomized = state.isSubjectCoeffCustomized(officialSubject.id)

            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = officialSubject.name,
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = if (isCustomized) FontWeight.SemiBold else FontWeight.Normal,
                        color = MaterialTheme.colorScheme.onSurface,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    if (isCustomized || isInvalid) {
                        Text(
                            text = if (isInvalid) {
                                stringResource(R.string.bac_subject_coeff_invalid)
                            } else {
                                stringResource(
                                    R.string.bac_subject_coeff_default_hint,
                                    formatDisplayCoeff(officialSubject.examCoeff)
                                )
                            },
                            style = MaterialTheme.typography.labelSmall,
                            color = if (isInvalid) {
                                MaterialTheme.colorScheme.error
                            } else {
                                MaterialTheme.colorScheme.tertiary
                            }
                        )
                    }
                }

                // Bouton -0.5 accessible (48dp touch target)
                IconButton(
                    onClick = { onStepSubjectCoeff(officialSubject.id, -0.5) },
                    enabled = effectiveSubject.examCoeff > 0.0,
                    modifier = Modifier
                        .minimumInteractiveComponentSize()
                        .testTag("panel_coeff_dec_${officialSubject.id}")
                ) {
                    Icon(
                        imageVector = Icons.Filled.Remove,
                        contentDescription = stringResource(
                            R.string.cd_bac_coeff_decrease,
                            officialSubject.name
                        ),
                        modifier = Modifier.size(18.dp)
                    )
                }

                OutlinedTextField(
                    value = displayText,
                    onValueChange = { onUpdateSubjectCoeff(officialSubject.id, it) },
                    singleLine = true,
                    isError = isInvalid,
                    textStyle = MaterialTheme.typography.bodyMedium.copy(
                        textAlign = TextAlign.Center,
                        fontWeight = FontWeight.SemiBold
                    ),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier
                        .width(68.dp)
                        .testTag("panel_coeff_input_${officialSubject.id}")
                )

                // Bouton +0.5 accessible (48dp touch target)
                IconButton(
                    onClick = { onStepSubjectCoeff(officialSubject.id, 0.5) },
                    enabled = effectiveSubject.examCoeff < 20.0,
                    modifier = Modifier
                        .minimumInteractiveComponentSize()
                        .testTag("panel_coeff_inc_${officialSubject.id}")
                ) {
                    Icon(
                        imageVector = Icons.Filled.Add,
                        contentDescription = stringResource(
                            R.string.cd_bac_coeff_increase,
                            officialSubject.name
                        ),
                        modifier = Modifier.size(18.dp)
                    )
                }
            }

            if (index < section.subjects.lastIndex) {
                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.45f))
            }
        }
    }
}

/**
 * Champ compact de coefficient directement intégré sur chaque carte matière (Annuelle & Examen).
 */
@Composable
internal fun InlineSubjectCoeffField(
    subjectId: String,
    value: String,
    isInvalid: Boolean,
    isCustomized: Boolean,
    onValueChange: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Text(
            text = stringResource(R.string.bac_coeff_input_label),
            style = MaterialTheme.typography.labelMedium,
            fontWeight = FontWeight.SemiBold,
            color = when {
                isInvalid -> MaterialTheme.colorScheme.error
                isCustomized -> MaterialTheme.colorScheme.tertiary
                else -> MaterialTheme.colorScheme.onSurfaceVariant
            },
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
        OutlinedTextField(
            value = value,
            onValueChange = { raw ->
                val sanitized = raw.filter { it.isDigit() || it == '.' || it == ',' }
                if (sanitized.length <= 5) {
                    onValueChange(sanitized)
                }
            },
            singleLine = true,
            isError = isInvalid,
            textStyle = MaterialTheme.typography.bodyMedium.copy(
                textAlign = TextAlign.Center,
                fontWeight = FontWeight.Bold
            ),
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
            shape = RoundedCornerShape(12.dp),
            modifier = Modifier
                .fillMaxWidth()
                .testTag("coeff_input_$subjectId")
        )
    }
}

internal fun formatDisplayCoeff(value: Double): String =
    if (value % 1.0 == 0.0) value.toInt().toString() else String.format(Locale.US, "%.1f", value)
