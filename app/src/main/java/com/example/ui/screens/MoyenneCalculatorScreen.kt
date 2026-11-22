package com.example.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
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
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.ErrorOutline
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material.icons.filled.Verified
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.PrimaryTabRow
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Slider
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.R
import com.example.domain.bac.BacCalculator
import com.example.domain.bac.BacMention
import com.example.domain.bac.BacSectionConfig
import com.example.domain.bac.BacSession
import com.example.domain.bac.BacSubjectConfig
import com.example.domain.bac.GradeValidation
import com.example.domain.bac.ReverseSimulationResult
import com.example.domain.bac.TrimesterGradeInput
import com.example.domain.bac.TrimesterKey
import com.example.ui.components.adaptiveCrimson
import com.example.ui.components.adaptiveEmerald
import com.example.ui.components.adaptiveOchre
import com.example.ui.components.localizedLabel
import com.example.ui.viewmodel.BacCalcTab
import com.example.ui.viewmodel.MoyenneUiState
import kotlin.math.roundToInt

/**
 * Écran Calculateur du Bac Tunisien :
 * - Résumé fixe en haut pendant le défilement (sticky header) avec jauge circulaire animée.
 * - Menu déroulant compact pour la section, un seul niveau d'onglets (Annuelle, Examen, FG, Simulateur),
 *   et sélecteur segmenté pour les trimestres.
 * - Panneau « Coefficients » affichant la pondération, le barème indicatif, la source et la date de mise à jour.
 */
@OptIn(ExperimentalFoundationApi::class, ExperimentalMaterial3Api::class)
@Composable
fun MoyenneCalculatorScreen(
    state: MoyenneUiState,
    onSelectSection: (String) -> Unit,
    onSelectCalcTab: (BacCalcTab) -> Unit,
    onSelectTrimester: (TrimesterKey) -> Unit,
    onUpdateAnnualPercent: (Double) -> Unit,
    onResetWeights: () -> Unit,
    onUpdateAnnualGrade: (subjectId: String, trimester: TrimesterKey, dc: String?, ds: String?) -> Unit,
    onUpdateExamGrade: (subjectId: String, value: String) -> Unit,
    onUpdateSubjectCoeff: (subjectId: String, value: String) -> Unit = { _, _ -> },
    onStepSubjectCoeff: (subjectId: String, delta: Double) -> Unit = { _, _ -> },
    onResetSubjectCoeffs: () -> Unit = {},
    onUpdateCustomFgCoeff: (subjectId: String, value: String) -> Unit,
    onUpdateFgMultiplier: (String) -> Unit,
    onResetFgCoeffs: () -> Unit,
    onUpdateSimulatorTarget: (String) -> Unit,
    onUpdateSimulatorCustomAnnual: (String) -> Unit,
    onResetGrades: () -> Unit,
    onFillSampleGrades: () -> Unit,
    modifier: Modifier = Modifier
) {
    var showWeightConfig by rememberSaveable { mutableStateOf(false) }
    val section = state.selectedSection
    val effectiveSection = state.effectiveSection ?: section

    val generalAvg = state.generalAverage
    val annualAvg = state.annualResult.annualAverage
    val examAvg = state.examResult.examAverage
    val fgScore = state.fgResult.score

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .testTag("bac_calculator_screen"),
        contentPadding = PaddingValues(bottom = 28.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // En-tête éditorial + sélecteur compact de section (menu déroulant)
        item(key = "bac_header") {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Column {
                    Text(
                        text = stringResource(R.string.bac_calc_title),
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onBackground
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = stringResource(
                            R.string.bac_calc_subtitle,
                            state.annualPercent.roundToInt(),
                            state.examPercent.roundToInt()
                        ),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                // Menu déroulant compact pour choisir la section du bac
                SectionDropdownSelector(
                    sections = state.sections,
                    selectedSection = section,
                    onSelectSection = onSelectSection
                )
            }
        }

        // Barre de résumé fixe (sticky header) : MG + jauge animée + mention + annuelle + examen + FG
        stickyHeader(key = "bac_sticky_summary") {
            StickyBacSummaryBar(
                generalAvg = generalAvg,
                annualAvg = annualAvg,
                examAvg = examAvg,
                fgScore = fgScore,
                mention = state.mention,
                session = state.session,
                annualPercent = state.annualPercent,
                examPercent = state.examPercent
            )
        }

        // Barre d'actions rapides : Exemple, Réinitialiser, Coefficients (libellé explicite) + réglages
        item(key = "bac_actions_and_settings") {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedButton(
                        onClick = onFillSampleGrades,
                        modifier = Modifier
                            .weight(1f)
                            .minimumInteractiveComponentSize()
                            .testTag("bac_fill_sample_btn"),
                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 8.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Filled.AutoAwesome,
                            contentDescription = stringResource(R.string.cd_bac_sample_icon),
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = stringResource(R.string.bac_action_sample),
                            style = MaterialTheme.typography.labelMedium,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }

                    OutlinedButton(
                        onClick = onResetGrades,
                        modifier = Modifier
                            .weight(1f)
                            .minimumInteractiveComponentSize()
                            .testTag("bac_reset_grades_btn"),
                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 8.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Refresh,
                            contentDescription = stringResource(R.string.cd_bac_reset_icon),
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = stringResource(R.string.action_reset_grades),
                            style = MaterialTheme.typography.labelMedium,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }

                    if (showWeightConfig) {
                        FilledTonalButton(
                            onClick = { showWeightConfig = false },
                            modifier = Modifier
                                .weight(1.15f)
                                .minimumInteractiveComponentSize()
                                .testTag("bac_toggle_weights_btn"),
                            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 8.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Filled.Tune,
                                contentDescription = stringResource(R.string.cd_bac_coefficients_icon),
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = stringResource(R.string.bac_action_coefficients),
                                style = MaterialTheme.typography.labelMedium,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    } else {
                        OutlinedButton(
                            onClick = { showWeightConfig = true },
                            modifier = Modifier
                                .weight(1.15f)
                                .minimumInteractiveComponentSize()
                                .testTag("bac_toggle_weights_btn"),
                            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 8.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Filled.Tune,
                                contentDescription = stringResource(R.string.cd_bac_coefficients_icon),
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = stringResource(R.string.bac_action_coefficients),
                                style = MaterialTheme.typography.labelMedium,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                }

                // Panneau des réglages : pondérations + source + date de mise à jour + note indicative
                AnimatedVisibility(visible = showWeightConfig) {
                    Surface(
                        shape = MaterialTheme.shapes.medium,
                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.55f),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("bac_coefficients_panel")
                    ) {
                        Column(
                            modifier = Modifier.padding(14.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = stringResource(R.string.bac_weights_title),
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.SemiBold
                                )
                                TextButton(onClick = onResetWeights) {
                                    Text("25% / 75%")
                                }
                            }
                            Text(
                                text = stringResource(
                                    R.string.bac_weights_detail,
                                    state.annualPercent.roundToInt(),
                                    state.examPercent.roundToInt()
                                ),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.primary,
                                fontWeight = FontWeight.SemiBold
                            )
                            Slider(
                                value = state.annualPercent.toFloat(),
                                onValueChange = { onUpdateAnnualPercent((it / 5f).roundToInt() * 5.0) },
                                valueRange = 0f..50f,
                                steps = 9,
                                modifier = Modifier.testTag("bac_weights_slider")
                            )

                            if (section != null && effectiveSection != null) {
                                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)

                                SubjectCoefficientsConfigSection(
                                    section = section,
                                    effectiveSection = effectiveSection,
                                    state = state,
                                    onUpdateSubjectCoeff = onUpdateSubjectCoeff,
                                    onStepSubjectCoeff = onStepSubjectCoeff,
                                    onResetSubjectCoeffs = onResetSubjectCoeffs
                                )
                            }

                            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)

                            // Note indicative + métadonnées issues de assets/bac_config.json
                            Row(
                                verticalAlignment = Alignment.Top,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Filled.Info,
                                    contentDescription = stringResource(R.string.cd_bac_info_icon),
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier
                                        .padding(top = 2.dp)
                                        .size(16.dp)
                                )
                                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                    Text(
                                        text = stringResource(R.string.bac_disclaimer_note),
                                        style = MaterialTheme.typography.bodySmall,
                                        fontWeight = FontWeight.Medium,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                    Text(
                                        text = stringResource(R.string.bac_config_source_label, state.config.source),
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        modifier = Modifier.testTag("bac_config_source_text")
                                    )
                                    Text(
                                        text = stringResource(R.string.bac_config_updated_label, state.config.updatedAt),
                                        style = MaterialTheme.typography.labelSmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        modifier = Modifier.testTag("bac_config_updated_text")
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }

        // Carte de résultat officiel final avec compteur numérique et barre de progression animés
        item(key = "bac_official_result_progress") {
            OfficialResultProgressCard(
                generalAvg = generalAvg,
                annualAvg = annualAvg,
                examAvg = examAvg,
                mention = state.mention,
                session = state.session,
                totalCoeffSum = state.totalSubjectCoeffSum,
                hasCustomCoeffs = state.hasCustomSubjectCoeffs,
                modifier = Modifier.padding(horizontal = 16.dp)
            )
        }

        // Un seul niveau d'onglets : Annuelle | Examen | FG | Simulateur
        item(key = "bac_mode_tabs") {
            PrimaryTabRow(
                selectedTabIndex = state.selectedCalcTab.ordinal,
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("bac_subtabs_row"),
                containerColor = MaterialTheme.colorScheme.surface,
                contentColor = MaterialTheme.colorScheme.primary
            ) {
                BacCalcTab.entries.forEach { tab ->
                    val isSelected = state.selectedCalcTab == tab
                    Tab(
                        selected = isSelected,
                        onClick = { onSelectCalcTab(tab) },
                        modifier = Modifier
                            .minimumInteractiveComponentSize()
                            .testTag("bac_tab_${tab.name.lowercase()}"),
                        selectedContentColor = MaterialTheme.colorScheme.primary,
                        unselectedContentColor = MaterialTheme.colorScheme.onSurfaceVariant,
                        text = {
                            Text(
                                text = tab.localizedLabel(),
                                style = MaterialTheme.typography.labelLarge,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                maxLines = 1,
                                softWrap = false,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    )
                }
            }
        }

        // Contenu de l'onglet actif
        if (section != null && effectiveSection != null) {
            when (state.selectedCalcTab) {
                BacCalcTab.ANNUELLE -> {
                    item(key = "trimester_selector") {
                        TrimesterBarCard(
                            selectedTrimester = state.selectedTrimester,
                            trimesterAverages = state.annualResult.trimesterAverages,
                            annualAverage = annualAvg,
                            onSelectTrimester = onSelectTrimester,
                            modifier = Modifier.padding(horizontal = 16.dp)
                        )
                    }

                    items(effectiveSection.subjects, key = { "ann_${section.id}_${state.selectedTrimester.name}_${it.id}" }) { subject ->
                        val officialSubject = section.subjects.find { it.id == subject.id } ?: subject
                        val triInput = state.annualInputs[subject.id]?.forTrimester(state.selectedTrimester)
                            ?: TrimesterGradeInput()
                        val subjAvg = state.annualResult.subjectTrimesterAverages[state.selectedTrimester]?.get(subject.id)
                        val coeffText = state.customSubjectCoeffTexts[subject.id] ?: formatCoeff(officialSubject.annualCoeff)
                        AnnualSubjectCard(
                            subject = subject,
                            officialCoeff = officialSubject.annualCoeff,
                            coeffText = coeffText,
                            isCoeffInvalid = state.isSubjectCoeffInvalid(subject.id),
                            isCoeffCustomized = state.isSubjectCoeffCustomized(subject.id),
                            input = triInput,
                            subjectAverage = subjAvg,
                            onUpdate = { dc, ds -> onUpdateAnnualGrade(subject.id, state.selectedTrimester, dc, ds) },
                            onUpdateCoeff = { onUpdateSubjectCoeff(subject.id, it) },
                            modifier = Modifier.padding(horizontal = 16.dp)
                        )
                    }
                }

                BacCalcTab.EXAMEN -> {
                    item(key = "exam_info_card") {
                        Surface(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp),
                            shape = MaterialTheme.shapes.medium,
                            color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.4f),
                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.25f))
                        ) {
                            Row(
                                modifier = Modifier.padding(14.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = stringResource(R.string.bac_exam_header, section.shortTitle),
                                        style = MaterialTheme.typography.titleSmall,
                                        color = MaterialTheme.colorScheme.onPrimaryContainer
                                    )
                                    Text(
                                        text = stringResource(
                                            R.string.bac_active_coeffs,
                                            formatCoeff(state.examResult.activeCoeffSum)
                                        ),
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                                Text(
                                    text = examAvg?.let { "%.2f / 20".format(it) } ?: "-- / 20",
                                    style = MaterialTheme.typography.titleLarge,
                                    color = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.testTag("bac_exam_avg_header")
                                )
                            }
                        }
                    }

                    items(effectiveSection.subjects, key = { "exam_${section.id}_${it.id}" }) { subject ->
                        val officialSubject = section.subjects.find { it.id == subject.id } ?: subject
                        val coeffText = state.customSubjectCoeffTexts[subject.id] ?: formatCoeff(officialSubject.examCoeff)
                        ExamSubjectCard(
                            subject = subject,
                            officialCoeff = officialSubject.examCoeff,
                            coeffText = coeffText,
                            isCoeffInvalid = state.isSubjectCoeffInvalid(subject.id),
                            isCoeffCustomized = state.isSubjectCoeffCustomized(subject.id),
                            gradeText = state.examGradeTexts[subject.id] ?: "",
                            onGradeChange = { onUpdateExamGrade(subject.id, it) },
                            onUpdateCoeff = { onUpdateSubjectCoeff(subject.id, it) },
                            modifier = Modifier.padding(horizontal = 16.dp)
                        )
                    }
                }

                BacCalcTab.FG -> {
                    item(key = "fg_panel") {
                        FormuleGlobaleSection(
                            section = effectiveSection,
                            state = state,
                            onUpdateFgMultiplier = onUpdateFgMultiplier,
                            onUpdateCustomFgCoeff = onUpdateCustomFgCoeff,
                            onResetFgCoeffs = onResetFgCoeffs,
                            modifier = Modifier.padding(horizontal = 16.dp)
                        )
                    }
                }

                BacCalcTab.SIMULATEUR -> {
                    item(key = "reverse_sim_panel") {
                        ReverseSimulatorSection(
                            state = state,
                            onUpdateTarget = onUpdateSimulatorTarget,
                            onUpdateCustomAnnual = onUpdateSimulatorCustomAnnual,
                            modifier = Modifier.padding(horizontal = 16.dp)
                        )
                    }
                }
            }
        }

        // Note de bas de page (barème indicatif)
        item(key = "official_notice") {
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
                shape = MaterialTheme.shapes.medium,
                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.55f)
            ) {
                Row(
                    modifier = Modifier.padding(12.dp),
                    verticalAlignment = Alignment.Top,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        imageVector = Icons.Filled.Info,
                        contentDescription = stringResource(R.string.cd_bac_info_icon),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(18.dp)
                    )
                    Text(
                        text = stringResource(R.string.bac_disclaimer_note),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}

/**
 * Menu déroulant compact pour sélectionner la section du Baccalauréat Tunisien.
 */
@Composable
private fun SectionDropdownSelector(
    sections: List<BacSectionConfig>,
    selectedSection: BacSectionConfig?,
    onSelectSection: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    var expanded by rememberSaveable { mutableStateOf(false) }

    Box(modifier = modifier.fillMaxWidth()) {
        Surface(
            shape = RoundedCornerShape(12.dp),
            color = MaterialTheme.colorScheme.surface,
            border = BorderStroke(
                width = 1.dp,
                color = if (expanded) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant
            ),
            modifier = Modifier
                .fillMaxWidth()
                .minimumInteractiveComponentSize()
                .clip(RoundedCornerShape(12.dp))
                .clickable { expanded = !expanded }
                .testTag("bac_section_dropdown")
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 14.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    Text(
                        text = stringResource(R.string.bac_section_dropdown_label),
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        text = selectedSection?.title.orEmpty(),
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurface,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
                Icon(
                    imageVector = if (expanded) Icons.Filled.KeyboardArrowUp else Icons.Filled.KeyboardArrowDown,
                    contentDescription = stringResource(R.string.cd_bac_section_dropdown),
                    tint = MaterialTheme.colorScheme.primary
                )
            }
        }

        DropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false },
            modifier = Modifier.fillMaxWidth(0.9f)
        ) {
            sections.forEach { sec ->
                val isSelected = sec.id == selectedSection?.id
                DropdownMenuItem(
                    modifier = Modifier.testTag("bac_section_${sec.id}"),
                    text = {
                        Column {
                            Text(
                                text = sec.title,
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
                            )
                            if (sec.description.isNotBlank()) {
                                Text(
                                    text = sec.description,
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                        }
                    },
                    trailingIcon = if (isSelected) {
                        {
                            Icon(
                                imageVector = Icons.Filled.Check,
                                contentDescription = sec.title,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    } else null,
                    onClick = {
                        onSelectSection(sec.id)
                        expanded = false
                    }
                )
            }
        }
    }
}

/**
 * Barre compacte de résumé fixe (sticky header) restant toujours visible au défilement :
 * jauge circulaire animée, statut unique (sans doublon), mention, annuelle, examen et FG.
 */
@Composable
private fun StickyBacSummaryBar(
    generalAvg: Double?,
    annualAvg: Double?,
    examAvg: Double?,
    fgScore: Double?,
    mention: BacMention,
    session: BacSession,
    annualPercent: Double,
    examPercent: Double
) {
    val emerald = adaptiveEmerald()
    val crimson = adaptiveCrimson()
    val ochre = adaptiveOchre()
    val primary = MaterialTheme.colorScheme.primary

    val gaugeValue = generalAvg ?: examAvg ?: annualAvg
    val gaugeLabel = when {
        generalAvg != null -> stringResource(R.string.bac_mg_short)
        examAvg != null -> stringResource(R.string.bac_exam_short)
        annualAvg != null -> stringResource(R.string.bac_annual_short)
        else -> "/ 20"
    }

    val targetColor = when {
        gaugeValue == null -> primary
        gaugeValue >= 14.0 -> emerald
        gaugeValue >= 12.0 -> primary
        gaugeValue >= 10.0 -> ochre
        else -> crimson
    }
    val animatedArcColor by animateColorAsState(
        targetValue = targetColor,
        animationSpec = tween(durationMillis = 380, easing = FastOutSlowInEasing),
        label = "bac_gauge_color"
    )
    val targetSweepFraction = ((gaugeValue ?: 0.0) / 20.0).toFloat().coerceIn(0f, 1f)
    val animatedSweepFraction by animateFloatAsState(
        targetValue = targetSweepFraction,
        animationSpec = tween(durationMillis = 650, easing = FastOutSlowInEasing),
        label = "bac_gauge_sweep"
    )
    val animatedGaugeNumber by animateFloatAsState(
        targetValue = (gaugeValue ?: 0.0).toFloat().coerceIn(0f, 20f),
        animationSpec = tween(durationMillis = 650, easing = FastOutSlowInEasing),
        label = "bac_gauge_number"
    )

    val sessionColor = when (session) {
        BacSession.PRINCIPALE -> emerald
        BacSession.CONTROLE -> ochre
        BacSession.REFUSE -> crimson
        BacSession.INCONNU -> MaterialTheme.colorScheme.onSurfaceVariant
    }

    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("bac_sticky_summary_bar"),
        color = MaterialTheme.colorScheme.surface,
        tonalElevation = 4.dp,
        shadowElevation = 2.dp
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                // Jauge circulaire visible (piste claire contrastée + arc & compteur animés)
                AnimatedBacGauge(
                    valueText = if (gaugeValue != null) "%.2f".format(animatedGaugeNumber) else "--",
                    subLabel = gaugeLabel,
                    sweepFraction = animatedSweepFraction,
                    arcColor = animatedArcColor,
                    hasValue = gaugeValue != null
                )

                // Colonne droite : statut unique + mention (si calculée) + KPI Annuelle / Examen / FG
                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = sessionColor.copy(alpha = 0.14f)
                        ) {
                            Text(
                                text = session.localizedLabel(),
                                style = MaterialTheme.typography.labelSmall,
                                color = sessionColor,
                                fontWeight = FontWeight.Bold,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                modifier = Modifier
                                    .padding(horizontal = 8.dp, vertical = 3.dp)
                                    .testTag("bac_session_badge")
                            )
                        }

                        // Affiche la mention uniquement lorsqu'elle est connue (supprime tout doublon « Notes incomplètes »)
                        if (mention != BacMention.INCONNU) {
                            Text(
                                text = mention.localizedLabel(),
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.Bold,
                                color = animatedArcColor,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                modifier = Modifier.testTag("bac_mention_text")
                            )
                        }
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        CompactKpiPill(
                            label = "${stringResource(R.string.bac_kpi_annual_short)} ${annualPercent.roundToInt()}%",
                            value = annualAvg?.let { "%.2f".format(it) } ?: "--",
                            accent = primary,
                            modifier = Modifier.weight(1f)
                        )
                        CompactKpiPill(
                            label = "${stringResource(R.string.bac_kpi_exam_short)} ${examPercent.roundToInt()}%",
                            value = examAvg?.let { "%.2f".format(it) } ?: "--",
                            accent = ochre,
                            modifier = Modifier.weight(1f)
                        )
                        CompactKpiPill(
                            label = stringResource(R.string.bac_kpi_fg_short),
                            value = fgScore?.let { "%.1f".format(it) } ?: "--",
                            accent = emerald,
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
            }
            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.7f))
        }
    }
}

/**
 * Jauge circulaire animée avec piste claire contrastée et arc proprement inséré dans les bornes du Canvas.
 */
@Composable
private fun AnimatedBacGauge(
    valueText: String,
    subLabel: String,
    sweepFraction: Float,
    arcColor: Color,
    hasValue: Boolean
) {
    val trackColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.14f)
    val trackHighlightColor = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.55f)

    Box(
        modifier = Modifier
            .size(76.dp)
            .testTag("bac_animated_gauge"),
        contentAlignment = Alignment.Center
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val strokePx = 7.5.dp.toPx()
            val inset = strokePx / 2f
            val arcSize = Size(size.width - strokePx, size.height - strokePx)
            val topLeft = Offset(inset, inset)

            // Piste claire et bien visible (270°)
            drawArc(
                color = trackColor,
                startAngle = 135f,
                sweepAngle = 270f,
                useCenter = false,
                topLeft = topLeft,
                size = arcSize,
                style = Stroke(width = strokePx, cap = StrokeCap.Round)
            )
            // Liseré subtil sur la piste pour améliorer la visibilité en thème clair et sombre
            drawArc(
                color = trackHighlightColor,
                startAngle = 135f,
                sweepAngle = 270f,
                useCenter = false,
                topLeft = topLeft,
                size = arcSize,
                style = Stroke(width = 1.dp.toPx(), cap = StrokeCap.Round)
            )

            // Arc actif animé et contrasté
            if (hasValue) {
                val activeSweep = (270f * sweepFraction).coerceAtLeast(6f)
                drawArc(
                    color = arcColor,
                    startAngle = 135f,
                    sweepAngle = activeSweep,
                    useCenter = false,
                    topLeft = topLeft,
                    size = arcSize,
                    style = Stroke(width = strokePx, cap = StrokeCap.Round)
                )
            }
        }

        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                text = valueText,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.ExtraBold,
                color = if (hasValue) arcColor else MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                modifier = Modifier.testTag("bac_general_average_text")
            )
            Text(
                text = subLabel,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1
            )
        }
    }
}

@Composable
private fun CompactKpiPill(
    label: String,
    value: String,
    accent: Color,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(10.dp),
        color = accent.copy(alpha = 0.10f),
        border = BorderStroke(1.dp, accent.copy(alpha = 0.24f))
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 6.dp, vertical = 5.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = value,
                style = MaterialTheme.typography.labelLarge,
                fontWeight = FontWeight.Bold,
                color = accent,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
private fun TrimesterBarCard(
    selectedTrimester: TrimesterKey,
    trimesterAverages: Map<TrimesterKey, Double?>,
    annualAverage: Double?,
    onSelectTrimester: (TrimesterKey) -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.medium,
        color = MaterialTheme.colorScheme.surface,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
    ) {
        Column(
            modifier = Modifier.padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = stringResource(R.string.bac_annual_formula),
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.weight(1f)
                )
                Text(
                    text = annualAverage?.let { "%.2f / 20".format(it) } ?: "--",
                    style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.primary
                )
            }

            SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
                TrimesterKey.entries.forEachIndexed { idx, tri ->
                    val triAvg = trimesterAverages[tri]
                    SegmentedButton(
                        selected = selectedTrimester == tri,
                        onClick = { onSelectTrimester(tri) },
                        shape = SegmentedButtonDefaults.itemShape(idx, TrimesterKey.entries.size),
                        modifier = Modifier.testTag("trimester_btn_${tri.name}")
                    ) {
                        val suffix = triAvg?.let { " · %.1f".format(it) } ?: ""
                        Text("${tri.shortLabel}$suffix", maxLines = 1)
                    }
                }
            }
        }
    }
}

@Composable
private fun AnnualSubjectCard(
    subject: BacSubjectConfig,
    officialCoeff: Double,
    coeffText: String,
    isCoeffInvalid: Boolean,
    isCoeffCustomized: Boolean,
    input: TrimesterGradeInput,
    subjectAverage: Double?,
    onUpdate: (dc: String?, ds: String?) -> Unit,
    onUpdateCoeff: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val dcValidation = BacCalculator.validateGrade(input.dcText)
    val dsValidation = BacCalculator.validateGrade(input.dsText)

    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.medium,
        color = MaterialTheme.colorScheme.surface,
        border = BorderStroke(
            width = 1.dp,
            color = if (isCoeffCustomized) {
                MaterialTheme.colorScheme.tertiary.copy(alpha = 0.45f)
            } else {
                MaterialTheme.colorScheme.outlineVariant
            }
        )
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
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
                    Text(
                        text = subject.name,
                        style = MaterialTheme.typography.titleMedium,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Surface(
                        shape = RoundedCornerShape(6.dp),
                        color = if (isCoeffCustomized) {
                            MaterialTheme.colorScheme.tertiaryContainer
                        } else {
                            MaterialTheme.colorScheme.primaryContainer
                        }
                    ) {
                        Text(
                            text = stringResource(R.string.bac_coeff_badge, formatCoeff(subject.annualCoeff)),
                            style = MaterialTheme.typography.labelSmall,
                            color = if (isCoeffCustomized) {
                                MaterialTheme.colorScheme.onTertiaryContainer
                            } else {
                                MaterialTheme.colorScheme.onPrimaryContainer
                            },
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                        )
                    }
                }
                if (subjectAverage != null) {
                    Text(
                        text = "%.2f".format(subjectAverage),
                        style = MaterialTheme.typography.titleSmall,
                        color = if (subjectAverage >= 10.0) adaptiveEmerald() else adaptiveCrimson()
                    )
                }
            }

            if (isCoeffCustomized) {
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = stringResource(R.string.bac_subject_coeff_default_hint, formatCoeff(officialCoeff)),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.tertiary
                )
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Libellés courts et permanents au-dessus de chaque champ : « DC », « DS » et « Coeff »
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.Top
            ) {
                ValidatedGradeField(
                    value = input.dcText,
                    onValueChange = { onUpdate(it, null) },
                    label = stringResource(R.string.label_dc_short),
                    validation = dcValidation,
                    fieldTag = "annual_dc_${subject.id}",
                    modifier = Modifier.weight(1f)
                )
                ValidatedGradeField(
                    value = input.dsText,
                    onValueChange = { onUpdate(null, it) },
                    label = stringResource(R.string.label_ds_short),
                    validation = dsValidation,
                    fieldTag = "annual_ds_${subject.id}",
                    modifier = Modifier.weight(1f)
                )
                InlineSubjectCoeffField(
                    subjectId = "annual_${subject.id}",
                    value = coeffText,
                    isInvalid = isCoeffInvalid,
                    isCustomized = isCoeffCustomized,
                    onValueChange = onUpdateCoeff,
                    modifier = Modifier.width(74.dp)
                )
            }
        }
    }
}

@Composable
private fun ExamSubjectCard(
    subject: BacSubjectConfig,
    officialCoeff: Double,
    coeffText: String,
    isCoeffInvalid: Boolean,
    isCoeffCustomized: Boolean,
    gradeText: String,
    onGradeChange: (String) -> Unit,
    onUpdateCoeff: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val validation = BacCalculator.validateGrade(gradeText)
    val parsed = (validation as? GradeValidation.Valid)?.value
    val emerald = adaptiveEmerald()

    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.medium,
        color = MaterialTheme.colorScheme.surface,
        border = BorderStroke(
            width = 1.dp,
            color = if (isCoeffCustomized) {
                MaterialTheme.colorScheme.tertiary.copy(alpha = 0.45f)
            } else {
                MaterialTheme.colorScheme.outlineVariant
            }
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
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
                    Text(
                        text = subject.name,
                        style = MaterialTheme.typography.titleMedium,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Surface(
                        shape = RoundedCornerShape(6.dp),
                        color = if (isCoeffCustomized) {
                            MaterialTheme.colorScheme.tertiaryContainer
                        } else {
                            MaterialTheme.colorScheme.primaryContainer
                        }
                    ) {
                        Text(
                            text = stringResource(R.string.bac_coeff_badge, formatCoeff(subject.examCoeff)),
                            style = MaterialTheme.typography.labelSmall,
                            color = if (isCoeffCustomized) {
                                MaterialTheme.colorScheme.onTertiaryContainer
                            } else {
                                MaterialTheme.colorScheme.onPrimaryContainer
                            },
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                        )
                    }
                }
                if (parsed != null) {
                    Text(
                        text = "= %.2f pts".format(parsed * subject.examCoeff),
                        style = MaterialTheme.typography.labelMedium,
                        color = emerald,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }

            if (isCoeffCustomized) {
                Text(
                    text = stringResource(R.string.bac_subject_coeff_default_hint, formatCoeff(officialCoeff)),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.tertiary
                )
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                verticalAlignment = Alignment.Top
            ) {
                ValidatedGradeField(
                    value = gradeText,
                    onValueChange = onGradeChange,
                    label = stringResource(R.string.bac_exam_field_label),
                    validation = validation,
                    fieldTag = "exam_grade_${subject.id}",
                    modifier = Modifier.weight(1f)
                )
                InlineSubjectCoeffField(
                    subjectId = "exam_${subject.id}",
                    value = coeffText,
                    isInvalid = isCoeffInvalid,
                    isCustomized = isCoeffCustomized,
                    onValueChange = onUpdateCoeff,
                    modifier = Modifier.width(84.dp)
                )
            }
        }
    }
}

@Composable
private fun FormuleGlobaleSection(
    section: BacSectionConfig,
    state: MoyenneUiState,
    onUpdateFgMultiplier: (String) -> Unit,
    onUpdateCustomFgCoeff: (String, String) -> Unit,
    onResetFgCoeffs: () -> Unit,
    modifier: Modifier = Modifier
) {
    val fg = state.fgResult
    val emerald = adaptiveEmerald()

    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // Carte score FG
        Surface(
            shape = MaterialTheme.shapes.large,
            color = emerald.copy(alpha = 0.11f),
            border = BorderStroke(1.5.dp, emerald.copy(alpha = 0.35f))
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = stringResource(R.string.bac_fg_title),
                            style = MaterialTheme.typography.titleMedium,
                            color = emerald
                        )
                        Text(
                            text = stringResource(R.string.bac_fg_formula, formatCoeff(state.effectiveFgMultiplier)),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Text(
                        text = fg.score?.let { "%.2f".format(it) } ?: "--",
                        style = MaterialTheme.typography.headlineMedium,
                        fontWeight = FontWeight.Bold,
                        color = emerald,
                        modifier = Modifier.testTag("bac_fg_score_text")
                    )
                }

                if (fg.score != null) {
                    Spacer(modifier = Modifier.height(10.dp))
                    HorizontalDivider(color = emerald.copy(alpha = 0.25f))
                    Spacer(modifier = Modifier.height(8.dp))
                    fg.breakdown.forEach { term ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 2.dp),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = "${term.label} (×${formatCoeff(term.coeff)})",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = if (term.grade != null) {
                                    "%.2f → +%.2f".format(term.grade, term.contribution)
                                } else {
                                    stringResource(R.string.bac_fg_not_entered)
                                },
                                style = MaterialTheme.typography.labelMedium,
                                color = if (term.grade != null) emerald else MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                } else {
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = stringResource(R.string.bac_fg_empty_hint),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }

        // Carte coefficients FG modifiables
        Surface(
            shape = MaterialTheme.shapes.medium,
            color = MaterialTheme.colorScheme.surface,
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = stringResource(R.string.bac_fg_editable_title, section.shortTitle),
                        style = MaterialTheme.typography.titleSmall,
                        modifier = Modifier.weight(1f)
                    )
                    TextButton(
                        onClick = onResetFgCoeffs,
                        modifier = Modifier.testTag("bac_reset_fg_btn")
                    ) {
                        Text(stringResource(R.string.bac_fg_restore))
                    }
                }

                // Multiplicateur de la Moyenne Générale
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = stringResource(R.string.bac_fg_multiplier_label),
                        style = MaterialTheme.typography.bodyMedium,
                        modifier = Modifier.weight(1f)
                    )
                    Column(modifier = Modifier.width(88.dp)) {
                        Text(
                            text = stringResource(R.string.bac_fg_coeff_short),
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        OutlinedTextField(
                            value = state.fgGeneralMultiplierText,
                            onValueChange = onUpdateFgMultiplier,
                            singleLine = true,
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("fg_multiplier_input")
                        )
                    }
                }

                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)

                section.subjects.forEach { subject ->
                    val currentText = state.customFgCoeffTexts[subject.id] ?: formatCoeff(subject.fgCoeff)
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = subject.name,
                                style = MaterialTheme.typography.bodyMedium
                            )
                            val examNote = state.examGradeTexts[subject.id]?.let { BacCalculator.parseGrade(it) }
                            Text(
                                text = examNote?.let {
                                    stringResource(R.string.bac_fg_exam_grade_val, "%.2f".format(it))
                                } ?: stringResource(R.string.bac_fg_exam_grade_missing),
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Column(modifier = Modifier.width(88.dp)) {
                            Text(
                                text = stringResource(R.string.bac_fg_coeff_short),
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            OutlinedTextField(
                                value = currentText,
                                onValueChange = { onUpdateCustomFgCoeff(subject.id, it) },
                                singleLine = true,
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                                shape = RoundedCornerShape(10.dp),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .testTag("fg_coeff_${subject.id}")
                            )
                        }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun ReverseSimulatorSection(
    state: MoyenneUiState,
    onUpdateTarget: (String) -> Unit,
    onUpdateCustomAnnual: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val targetValidation = BacCalculator.validateGrade(state.simulatorTargetText)
    val customAnnualValidation = BacCalculator.validateGrade(state.simulatorCustomAnnualText)
    val simResult = state.reverseSimulation
    val emerald = adaptiveEmerald()
    val ochre = adaptiveOchre()
    val crimson = adaptiveCrimson()

    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.large,
        color = MaterialTheme.colorScheme.surface,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                text = stringResource(R.string.bac_simulator_title),
                style = MaterialTheme.typography.titleMedium
            )
            Text(
                text = stringResource(
                    R.string.bac_simulator_desc,
                    state.examPercent.roundToInt(),
                    state.annualPercent.roundToInt()
                ),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            // Objectifs rapides
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                listOf(
                    "10" to R.string.bac_sim_preset_10,
                    "12" to R.string.bac_sim_preset_12,
                    "14" to R.string.bac_sim_preset_14,
                    "16" to R.string.bac_sim_preset_16,
                    "18" to R.string.bac_sim_preset_18
                ).forEach { (value, labelRes) ->
                    FilterChip(
                        selected = state.simulatorTargetText.replace(',', '.') == value,
                        onClick = { onUpdateTarget(value) },
                        modifier = Modifier.testTag("sim_preset_$value"),
                        label = { Text(stringResource(labelRes)) }
                    )
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                ValidatedGradeField(
                    value = state.simulatorTargetText,
                    onValueChange = onUpdateTarget,
                    label = stringResource(R.string.bac_sim_target_label),
                    validation = targetValidation,
                    fieldTag = "sim_target_input",
                    modifier = Modifier.weight(1f)
                )

                ValidatedGradeField(
                    value = state.simulatorCustomAnnualText,
                    onValueChange = onUpdateCustomAnnual,
                    label = state.annualResult.annualAverage?.let {
                        stringResource(R.string.bac_sim_annual_prefilled, "%.2f".format(it))
                    } ?: stringResource(R.string.bac_sim_annual_label),
                    validation = customAnnualValidation,
                    fieldTag = "sim_annual_input",
                    modifier = Modifier.weight(1f)
                )
            }

            // Résultat du simulateur inversé
            when (simResult) {
                is ReverseSimulationResult.Possible -> {
                    val accent = if (simResult.requiredExamAverage <= 14.0) emerald else ochre
                    Surface(
                        shape = RoundedCornerShape(14.dp),
                        color = accent.copy(alpha = 0.12f),
                        border = BorderStroke(1.dp, accent.copy(alpha = 0.4f)),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("sim_result_possible")
                    ) {
                        Column(
                            modifier = Modifier.padding(14.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.Filled.Verified,
                                    contentDescription = stringResource(R.string.cd_bac_sim_ok),
                                    tint = accent
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = stringResource(R.string.bac_sim_possible_title),
                                    style = MaterialTheme.typography.titleSmall,
                                    color = accent
                                )
                            }
                            Text(
                                text = stringResource(
                                    R.string.bac_sim_possible_body,
                                    "%.2f".format(simResult.requiredExamAverage)
                                ),
                                style = MaterialTheme.typography.titleMedium,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            LinearProgressIndicator(
                                progress = { (simResult.requiredExamAverage / 20.0).toFloat().coerceIn(0f, 1f) },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(7.dp)
                                    .clip(RoundedCornerShape(4.dp)),
                                color = accent,
                                trackColor = MaterialTheme.colorScheme.surface
                            )
                            Text(
                                text = stringResource(
                                    R.string.bac_sim_max_reachable,
                                    "%.2f".format(simResult.maxReachableGeneral)
                                ),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }

                is ReverseSimulationResult.AlreadyAchieved -> {
                    Surface(
                        shape = RoundedCornerShape(14.dp),
                        color = emerald.copy(alpha = 0.12f),
                        border = BorderStroke(1.dp, emerald.copy(alpha = 0.4f)),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("sim_result_achieved")
                    ) {
                        Row(
                            modifier = Modifier.padding(14.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Filled.CheckCircle,
                                contentDescription = stringResource(R.string.cd_bac_sim_ok),
                                tint = emerald
                            )
                            Text(
                                text = stringResource(
                                    R.string.bac_sim_achieved_body,
                                    "%.2f".format(simResult.maxReachableGeneral)
                                ),
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }
                    }
                }

                is ReverseSimulationResult.Impossible -> {
                    Surface(
                        shape = RoundedCornerShape(14.dp),
                        color = crimson.copy(alpha = 0.12f),
                        border = BorderStroke(1.5.dp, crimson.copy(alpha = 0.5f)),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("sim_result_impossible")
                    ) {
                        Row(
                            modifier = Modifier.padding(14.dp),
                            verticalAlignment = Alignment.Top,
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Filled.ErrorOutline,
                                contentDescription = stringResource(R.string.cd_bac_sim_error),
                                tint = crimson
                            )
                            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                Text(
                                    text = stringResource(R.string.bac_sim_impossible_title),
                                    style = MaterialTheme.typography.titleSmall,
                                    color = crimson
                                )
                                Text(
                                    text = simResult.message,
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                            }
                        }
                    }
                }

                ReverseSimulationResult.MissingAnnualAverage -> {
                    Surface(
                        shape = RoundedCornerShape(14.dp),
                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.65f),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = stringResource(R.string.bac_sim_missing_annual),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(14.dp)
                        )
                    }
                }

                is ReverseSimulationResult.InvalidTarget -> {
                    Surface(
                        shape = RoundedCornerShape(14.dp),
                        color = crimson.copy(alpha = 0.10f),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = simResult.message,
                            style = MaterialTheme.typography.bodySmall,
                            color = crimson,
                            modifier = Modifier.padding(14.dp)
                        )
                    }
                }
            }
        }
    }
}

/**
 * Champ de saisie de note avec label permanent au-dessus du champ, clavier numérique décimal,
 * acceptation de la virgule ou du point et validation 0 à 20 via [BacCalculator].
 */
@Composable
private fun ValidatedGradeField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    validation: GradeValidation,
    fieldTag: String,
    modifier: Modifier = Modifier
) {
    val isError = validation is GradeValidation.Invalid
    Column(
        modifier = modifier,
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelMedium,
            fontWeight = FontWeight.SemiBold,
            color = if (isError) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurfaceVariant,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
        OutlinedTextField(
            value = value,
            onValueChange = { raw ->
                // Autorise la saisie fluide de chiffres, virgule ou point (validation complète 0..20 assurée par BacCalculator)
                val sanitized = raw.filter { it.isDigit() || it == '.' || it == ',' || it == '-' }
                if (sanitized.length <= 6) {
                    onValueChange(sanitized)
                }
            },
            placeholder = {
                Text(
                    text = "0–20",
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                )
            },
            isError = isError,
            supportingText = if (validation is GradeValidation.Invalid) {
                { Text(validation.reason) }
            } else {
                null
            },
            singleLine = true,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
            shape = RoundedCornerShape(12.dp),
            modifier = Modifier
                .fillMaxWidth()
                .testTag(fieldTag)
        )
    }
}

private fun formatCoeff(value: Double): String =
    if (value % 1.0 == 0.0) value.toInt().toString() else "%.1f".format(value)
