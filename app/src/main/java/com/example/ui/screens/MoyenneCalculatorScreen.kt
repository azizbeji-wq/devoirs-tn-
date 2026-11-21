package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
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
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Calculate
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.example.R
import com.example.data.model.SubjectCoefficient
import com.example.ui.theme.CrimsonAccent
import com.example.ui.theme.EmeraldVerified
import com.example.ui.theme.OchreGold
import com.example.ui.viewmodel.MoyenneUiState
import com.example.ui.viewmodel.SubjectGradeInput

@Composable
fun MoyenneCalculatorScreen(
    state: MoyenneUiState,
    onSelectPreset: (String) -> Unit,
    onSetTrimesterMode: (Boolean) -> Unit,
    onUpdateGrade: (subjectId: String, dc: String?, ds: String?, direct: String?) -> Unit,
    onResetGrades: () -> Unit,
    onFillSampleGrades: () -> Unit,
    modifier: Modifier = Modifier
) {
    val preset = state.selectedPreset
    val avg = state.computedAverage

    val gaugeColor = when {
        avg == null -> MaterialTheme.colorScheme.primary
        avg >= 14.0 -> EmeraldVerified
        avg >= 10.0 -> OchreGold
        else -> CrimsonAccent
    }

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .testTag("moyenne_calculator_list"),
        contentPadding = PaddingValues(bottom = 28.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // Editorial Header
        item(key = "moyenne_header") {
            Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp)) {
                Text(
                    text = stringResource(R.string.moyenne_title),
                    style = MaterialTheme.typography.headlineMedium,
                    color = MaterialTheme.colorScheme.onBackground
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = stringResource(R.string.moyenne_subtitle),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        // Section Selector Chips
        item(key = "section_presets") {
            LazyRow(
                contentPadding = PaddingValues(horizontal = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(state.presets, key = { it.id }) { item ->
                    FilterChip(
                        selected = item.id == state.selectedPresetId,
                        onClick = { onSelectPreset(item.id) },
                        modifier = Modifier.testTag("preset_chip_${item.id}"),
                        label = { Text(item.title) },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = MaterialTheme.colorScheme.primary,
                            selectedLabelColor = MaterialTheme.colorScheme.onPrimary
                        )
                    )
                }
            }
        }

        // Animated Score Summary Dashboard Card
        item(key = "score_gauge_card") {
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
                shape = MaterialTheme.shapes.large,
                color = MaterialTheme.colorScheme.surface,
                tonalElevation = 2.dp,
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
            ) {
                Column(modifier = Modifier.padding(18.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        // Circular Canvas Gauge
                        Box(
                            modifier = Modifier.size(104.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            val trackColor = MaterialTheme.colorScheme.surfaceVariant
                            val sweepFraction = ((avg ?: 0.0) / 20.0).toFloat().coerceIn(0f, 1f)
                            Canvas(modifier = Modifier.fillMaxSize()) {
                                val strokeWidth = 10.dp.toPx()
                                drawArc(
                                    color = trackColor,
                                    startAngle = 135f,
                                    sweepAngle = 270f,
                                    useCenter = false,
                                    style = Stroke(width = strokeWidth, cap = StrokeCap.Round)
                                )
                                drawArc(
                                    color = gaugeColor,
                                    startAngle = 135f,
                                    sweepAngle = 270f * sweepFraction,
                                    useCenter = false,
                                    style = Stroke(width = strokeWidth, cap = StrokeCap.Round)
                                )
                            }
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Text(
                                    text = avg?.let { "%.2f".format(it) } ?: "--",
                                    style = MaterialTheme.typography.headlineSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = gaugeColor,
                                    modifier = Modifier.testTag("computed_average_text")
                                )
                                Text(
                                    text = "/ 20",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }

                        Column(modifier = Modifier.weight(1f)) {
                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = gaugeColor.copy(alpha = 0.14f)
                            ) {
                                Text(
                                    text = state.mentionLabel,
                                    style = MaterialTheme.typography.labelLarge,
                                    color = gaugeColor,
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                                )
                            }
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = state.nextTargetHint,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            if (state.activeCoeffSum > 0.0) {
                                Spacer(modifier = Modifier.height(6.dp))
                                Text(
                                    text = "Points : %.1f · Coeff actifs : %.1f".format(
                                        state.totalWeightedPoints,
                                        state.activeCoeffSum
                                    ),
                                    style = MaterialTheme.typography.labelMedium,
                                    color = MaterialTheme.colorScheme.primary
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    // Mode Toggle (Trimestre DC+DS vs Note Directe / Bac)
                    SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
                        SegmentedButton(
                            selected = state.isTrimesterMode,
                            onClick = { onSetTrimesterMode(true) },
                            shape = SegmentedButtonDefaults.itemShape(index = 0, count = 2),
                            modifier = Modifier.testTag("mode_trimestre_btn")
                        ) {
                            Text(stringResource(R.string.mode_trimestre), maxLines = 1)
                        }
                        SegmentedButton(
                            selected = !state.isTrimesterMode,
                            onClick = { onSetTrimesterMode(false) },
                            shape = SegmentedButtonDefaults.itemShape(index = 1, count = 2),
                            modifier = Modifier.testTag("mode_direct_btn")
                        ) {
                            Text(stringResource(R.string.mode_direct), maxLines = 1)
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    // Quick Action Buttons
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        OutlinedButton(
                            onClick = onFillSampleGrades,
                            modifier = Modifier
                                .weight(1f)
                                .testTag("fill_sample_grades_btn")
                        ) {
                            Icon(
                                imageVector = Icons.Filled.AutoAwesome,
                                contentDescription = null,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(stringResource(R.string.action_simulate_sample), maxLines = 1)
                        }
                        OutlinedButton(
                            onClick = onResetGrades,
                            modifier = Modifier
                                .weight(1f)
                                .testTag("reset_grades_btn")
                        ) {
                            Icon(
                                imageVector = Icons.Filled.Refresh,
                                contentDescription = null,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(stringResource(R.string.action_reset_grades), maxLines = 1)
                        }
                    }
                }
            }
        }

        // Subject Inputs List
        if (preset != null) {
            item(key = "preset_subtitle") {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Icon(
                        imageVector = Icons.Filled.Calculate,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(18.dp)
                    )
                    Text(
                        text = "${preset.title} — ${preset.subtitle}",
                        style = MaterialTheme.typography.titleSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            items(preset.subjects, key = { "${preset.id}_${it.id}" }) { subject ->
                SubjectGradeCard(
                    subject = subject,
                    input = state.gradeInputs[subject.id] ?: SubjectGradeInput(),
                    isTrimesterMode = state.isTrimesterMode,
                    computedSubjectAvg = state.subjectAverage(subject.id),
                    onUpdateGrade = { dc, ds, direct ->
                        onUpdateGrade(subject.id, dc, ds, direct)
                    },
                    modifier = Modifier.padding(horizontal = 16.dp)
                )
            }
        }
    }
}

@Composable
private fun SubjectGradeCard(
    subject: SubjectCoefficient,
    input: SubjectGradeInput,
    isTrimesterMode: Boolean,
    computedSubjectAvg: Double?,
    onUpdateGrade: (dc: String?, ds: String?, direct: String?) -> Unit,
    modifier: Modifier = Modifier
) {
    val coeffDisplay = if (subject.coefficient % 1.0 == 0.0) {
        subject.coefficient.toInt().toString()
    } else {
        subject.coefficient.toString()
    }

    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.medium,
        color = MaterialTheme.colorScheme.surface,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
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
                    Text(
                        text = subject.name,
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Surface(
                        shape = RoundedCornerShape(6.dp),
                        color = MaterialTheme.colorScheme.primaryContainer
                    ) {
                        Text(
                            text = "Coeff $coeffDisplay",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onPrimaryContainer,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                        )
                    }
                }

                if (computedSubjectAvg != null) {
                    Text(
                        text = "%.2f / 20".format(computedSubjectAvg),
                        style = MaterialTheme.typography.titleSmall,
                        color = if (computedSubjectAvg >= 10.0) EmeraldVerified else CrimsonAccent
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            if (isTrimesterMode) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    OutlinedTextField(
                        value = input.dcText,
                        onValueChange = { onUpdateGrade(it, null, null) },
                        label = { Text(stringResource(R.string.label_dc)) },
                        placeholder = { Text("0–20") },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier
                            .weight(1f)
                            .testTag("dc_input_${subject.id}")
                    )
                    OutlinedTextField(
                        value = input.dsText,
                        onValueChange = { onUpdateGrade(null, it, null) },
                        label = { Text("${stringResource(R.string.label_ds)} (×2)") },
                        placeholder = { Text("0–20") },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier
                            .weight(1f)
                            .testTag("ds_input_${subject.id}")
                    )
                }
            } else {
                OutlinedTextField(
                    value = input.directText,
                    onValueChange = { onUpdateGrade(null, null, it) },
                    label = { Text(stringResource(R.string.label_note)) },
                    placeholder = { Text("Ex: 15.5") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("direct_input_${subject.id}")
                )
            }
        }
    }
}
