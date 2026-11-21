package com.example.ui.screens

import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.AccountBalance
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.AutoStories
import androidx.compose.material.icons.filled.Biotech
import androidx.compose.material.icons.filled.Calculate
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Code
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.HistoryEdu
import androidx.compose.material.icons.filled.MenuBook
import androidx.compose.material.icons.filled.PrecisionManufacturing
import androidx.compose.material.icons.filled.Public
import androidx.compose.material.icons.filled.RestartAlt
import androidx.compose.material.icons.filled.School
import androidx.compose.material.icons.filled.Science
import androidx.compose.material.icons.filled.Translate
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilledTonalButton
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
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.R
import com.example.data.local.SubjectEntity
import com.example.data.model.CycleLevel
import com.example.data.model.DevoirItem
import com.example.ui.components.DevoirCard
import com.example.ui.components.localizedCycle
import com.example.ui.components.localizedLabel
import com.example.ui.components.localizedShortLabel
import com.example.ui.components.localizedSubjectName
import com.example.ui.components.subjectAccentColor
import com.example.ui.theme.CobaltDeep
import com.example.ui.theme.CobaltPrimary
import com.example.ui.viewmodel.SubjectsUiState

/**
 * Associe la clé d'icône persistée dans Room (`SubjectEntity.iconKey`) à son icône Material Symbols.
 */
private fun subjectIconForKey(iconKey: String): ImageVector = when (iconKey) {
    "calculate" -> Icons.Filled.Calculate
    "science" -> Icons.Filled.Science
    "translate" -> Icons.Filled.Translate
    "biotech" -> Icons.Filled.Biotech
    "auto_stories" -> Icons.Filled.AutoStories
    "public" -> Icons.Filled.Public
    "code" -> Icons.Filled.Code
    "history_edu" -> Icons.Filled.HistoryEdu
    "account_balance" -> Icons.Filled.AccountBalance
    "precision" -> Icons.Filled.PrecisionManufacturing
    else -> Icons.Filled.School
}

/**
 * Retourne le sous-titre localisé (FR / AR / EN) pour les matières standard du programme tunisien,
 * ou le sous-titre saisi par l'utilisateur pour une matière personnalisée.
 */
@Composable
private fun localizedSubjectSubtitle(subject: SubjectEntity): String = when (subject.id) {
    "maths" -> stringResource(R.string.subj_maths_sub)
    "physique" -> stringResource(R.string.subj_physique_sub)
    "arabe" -> stringResource(R.string.subj_arabe_sub)
    "svt" -> stringResource(R.string.subj_svt_sub)
    "francais" -> stringResource(R.string.subj_francais_sub)
    "anglais" -> stringResource(R.string.subj_anglais_sub)
    "info" -> stringResource(R.string.subj_info_sub)
    "philo" -> stringResource(R.string.subj_philo_sub)
    "eco" -> stringResource(R.string.subj_eco_sub)
    "tech" -> stringResource(R.string.subj_tech_sub)
    else -> subject.subtitle
}

/**
 * Écran de sélection rapide par année scolaire et matière.
 * Les matières académiques sont chargées réactivement depuis Room via SubjectsViewModel
 * et affichées dans le LazyColumn principal.
 */
@Composable
fun SubjectSelectionScreen(
    subjectsUiState: SubjectsUiState,
    allDevoirs: List<DevoirItem>,
    selectedYear: CycleLevel,
    selectedSubject: String,
    onSelectYear: (CycleLevel) -> Unit,
    onSelectSubject: (String) -> Unit,
    onAddCustomSubject: (name: String, subtitle: String) -> Unit,
    onDeleteSubject: (subjectId: String) -> Unit,
    onResetSelection: () -> Unit,
    onOpenDevoir: (DevoirItem) -> Unit,
    onToggleFavorite: (String) -> Unit,
    onViewInLibrary: () -> Unit,
    modifier: Modifier = Modifier
) {
    var showAddSubjectDialog by remember { mutableStateOf(false) }

    val subjects = when (subjectsUiState) {
        is SubjectsUiState.Success -> subjectsUiState.subjects
        else -> emptyList()
    }

    val matchingDevoirs = remember(allDevoirs, selectedYear, selectedSubject) {
        allDevoirs.filter { devoir ->
            val yearOk = selectedYear == CycleLevel.ALL || devoir.level == selectedYear
            val subjectOk = selectedSubject == "Toutes" ||
                devoir.subject.equals(selectedSubject, ignoreCase = true)
            yearOk && subjectOk
        }
    }

    val yearCounts = remember(allDevoirs) {
        CycleLevel.entries.associateWith { level ->
            if (level == CycleLevel.ALL) allDevoirs.size
            else allDevoirs.count { it.level == level }
        }
    }

    val subjectCountsForYear = remember(allDevoirs, selectedYear, subjects) {
        val yearScoped = if (selectedYear == CycleLevel.ALL) {
            allDevoirs
        } else {
            allDevoirs.filter { it.level == selectedYear }
        }
        subjects.associate { subj ->
            subj.canonicalName to yearScoped.count { it.subject.equals(subj.canonicalName, ignoreCase = true) }
        }
    }

    val hasActiveFilter = selectedYear != CycleLevel.ALL || selectedSubject != "Toutes"

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .testTag("subject_selection_screen"),
        contentPadding = PaddingValues(bottom = 28.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // Bannière éditoriale
        item(key = "selection_header") {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp)
                    .clip(MaterialTheme.shapes.large)
                    .background(
                        Brush.linearGradient(
                            colors = listOf(CobaltDeep, CobaltPrimary)
                        )
                    )
                    .padding(20.dp)
            ) {
                Column {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Surface(
                            shape = CircleShape,
                            color = Color.White.copy(alpha = 0.16f)
                        ) {
                            Text(
                                text = stringResource(R.string.subject_selector_badge),
                                style = MaterialTheme.typography.labelSmall,
                                color = Color.White,
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                            )
                        }

                        if (hasActiveFilter) {
                            Surface(
                                onClick = onResetSelection,
                                shape = CircleShape,
                                color = Color.White.copy(alpha = 0.18f),
                                modifier = Modifier
                                    .minimumInteractiveComponentSize()
                                    .testTag("subject_selection_reset_btn")
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        imageVector = Icons.Filled.RestartAlt,
                                        contentDescription = stringResource(R.string.action_reset_filters),
                                        tint = Color.White,
                                        modifier = Modifier.size(14.dp)
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text(
                                        text = stringResource(R.string.action_reset_grades),
                                        style = MaterialTheme.typography.labelSmall,
                                        color = Color.White
                                    )
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    Text(
                        text = stringResource(R.string.subject_selector_title),
                        style = MaterialTheme.typography.headlineMedium,
                        color = Color.White
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = stringResource(R.string.subject_selector_subtitle),
                        style = MaterialTheme.typography.bodySmall,
                        color = Color.White.copy(alpha = 0.88f)
                    )
                }
            }
        }

        // Étape 1 : Année scolaire
        item(key = "year_section_title") {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = stringResource(R.string.step_choose_year),
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onBackground
                    )
                    Text(
                        text = selectedYear.localizedLabel(),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
                Text(
                    text = stringResource(R.string.subjects_count_label, yearCounts[selectedYear] ?: 0),
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        item(key = "year_cards_row") {
            LazyRow(
                contentPadding = PaddingValues(horizontal = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                modifier = Modifier.testTag("academic_year_selector_row")
            ) {
                items(CycleLevel.entries, key = { it.id }) { year ->
                    val isSelected = selectedYear == year
                    val count = yearCounts[year] ?: 0
                    AcademicYearTile(
                        year = year,
                        count = count,
                        selected = isSelected,
                        onClick = { onSelectYear(year) }
                    )
                }
            }
        }

        // Étape 2 : Liste des matières chargées depuis Room
        item(key = "subject_section_title") {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = stringResource(R.string.step_choose_subject),
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onBackground
                    )
                    Text(
                        text = localizedSubjectName(selectedSubject),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.primary
                    )
                }

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    if (selectedSubject != "Toutes") {
                        TextButton(
                            onClick = { onSelectSubject("Toutes") },
                            modifier = Modifier
                                .minimumInteractiveComponentSize()
                                .testTag("clear_subject_selection_btn")
                        ) {
                            Text(stringResource(R.string.all_subjects_label))
                        }
                    }

                    FilledTonalButton(
                        onClick = { showAddSubjectDialog = true },
                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                        modifier = Modifier
                            .minimumInteractiveComponentSize()
                            .testTag("add_subject_btn")
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Add,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(stringResource(R.string.subject_add_btn))
                    }
                }
            }
        }

        when (subjectsUiState) {
            is SubjectsUiState.Loading -> {
                item(key = "subjects_loading") {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 18.dp),
                        horizontalArrangement = Arrangement.Center,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        CircularProgressIndicator(modifier = Modifier.size(22.dp), strokeWidth = 2.5.dp)
                        Spacer(modifier = Modifier.width(12.dp))
                        Text(
                            text = stringResource(R.string.subjects_loading),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            is SubjectsUiState.Error -> {
                item(key = "subjects_error") {
                    Surface(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp),
                        shape = MaterialTheme.shapes.medium,
                        color = MaterialTheme.colorScheme.errorContainer
                    ) {
                        Text(
                            text = subjectsUiState.message,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onErrorContainer,
                            modifier = Modifier.padding(14.dp)
                        )
                    }
                }
            }

            is SubjectsUiState.Success -> {
                // Chaque matière Room est rendue directement comme élément du LazyColumn
                items(
                    items = subjectsUiState.subjects,
                    key = { subject -> "room_subject_${subject.id}" }
                ) { subject ->
                    val isSelected = selectedSubject.equals(subject.canonicalName, ignoreCase = true)
                    val count = subjectCountsForYear[subject.canonicalName] ?: 0
                    SubjectRowCard(
                        subject = subject,
                        count = count,
                        selected = isSelected,
                        onClick = {
                            if (isSelected) onSelectSubject("Toutes")
                            else onSelectSubject(subject.canonicalName)
                        },
                        onDelete = if (subject.isCustom) {
                            {
                                if (isSelected) onSelectSubject("Toutes")
                                onDeleteSubject(subject.id)
                            }
                        } else null,
                        modifier = Modifier.padding(horizontal = 16.dp)
                    )
                }
            }
        }

        // En-tête des supports correspondants + accès bibliothèque
        item(key = "matching_results_header") {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = stringResource(R.string.matching_materials_title),
                        style = MaterialTheme.typography.titleLarge,
                        color = MaterialTheme.colorScheme.onBackground
                    )
                    Text(
                        text = stringResource(
                            R.string.matching_supports_summary,
                            matchingDevoirs.size,
                            selectedYear.localizedShortLabel(),
                            localizedSubjectName(selectedSubject)
                        ),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                FilledTonalButton(
                    onClick = onViewInLibrary,
                    modifier = Modifier
                        .minimumInteractiveComponentSize()
                        .testTag("open_filtered_library_btn")
                ) {
                    Text(stringResource(R.string.action_open_library))
                    Spacer(modifier = Modifier.width(4.dp))
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                        contentDescription = null,
                        modifier = Modifier.size(16.dp)
                    )
                }
            }
        }

        // Liste des supports correspondants ou état vide
        if (matchingDevoirs.isEmpty()) {
            item(key = "matching_empty") {
                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp),
                    shape = MaterialTheme.shapes.large,
                    color = MaterialTheme.colorScheme.surface,
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Icon(
                            imageVector = Icons.Filled.MenuBook,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(40.dp)
                        )
                        Spacer(modifier = Modifier.height(10.dp))
                        Text(
                            text = stringResource(R.string.subject_empty_title),
                            style = MaterialTheme.typography.titleMedium,
                            textAlign = TextAlign.Center
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = stringResource(R.string.subject_empty_subtitle),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            textAlign = TextAlign.Center
                        )
                        Spacer(modifier = Modifier.height(14.dp))
                        Button(
                            onClick = onResetSelection,
                            modifier = Modifier
                                .minimumInteractiveComponentSize()
                                .testTag("empty_subject_reset_btn")
                        ) {
                            Text(stringResource(R.string.action_reset_filters))
                        }
                    }
                }
            }
        } else {
            items(matchingDevoirs, key = { "sel_${it.id}" }) { devoir ->
                DevoirCard(
                    devoir = devoir,
                    onClick = { onOpenDevoir(devoir) },
                    onToggleFavorite = { onToggleFavorite(devoir.id) },
                    modifier = Modifier.padding(horizontal = 16.dp)
                )
            }
        }
    }

    if (showAddSubjectDialog) {
        AddSubjectDialog(
            onDismiss = { showAddSubjectDialog = false },
            onConfirm = { name, subtitle ->
                onAddCustomSubject(name, subtitle)
                showAddSubjectDialog = false
            }
        )
    }
}

@Composable
private fun AcademicYearTile(
    year: CycleLevel,
    count: Int,
    selected: Boolean,
    onClick: () -> Unit
) {
    val containerColor by animateColorAsState(
        targetValue = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surface,
        label = "year_tile_bg"
    )
    val contentColor by animateColorAsState(
        targetValue = if (selected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurface,
        label = "year_tile_fg"
    )

    Surface(
        modifier = Modifier
            .width(108.dp)
            .minimumInteractiveComponentSize()
            .clip(MaterialTheme.shapes.medium)
            .clickable(onClick = onClick)
            .testTag("year_tile_${year.id}"),
        shape = MaterialTheme.shapes.medium,
        color = containerColor,
        tonalElevation = if (selected) 4.dp else 1.dp,
        border = BorderStroke(
            width = if (selected) 1.5.dp else 1.dp,
            color = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant
        )
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(3.dp)
        ) {
            Text(
                text = year.localizedCycle(),
                style = MaterialTheme.typography.labelSmall,
                color = contentColor.copy(alpha = 0.78f),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = year.localizedShortLabel(),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = contentColor,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = stringResource(R.string.devoirs_count_short, count),
                style = MaterialTheme.typography.labelSmall,
                color = contentColor.copy(alpha = 0.85f),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

/**
 * Carte horizontale d'une matière académique issue de Room, affichée dans le LazyColumn.
 */
@Composable
private fun SubjectRowCard(
    subject: SubjectEntity,
    count: Int,
    selected: Boolean,
    onClick: () -> Unit,
    onDelete: (() -> Unit)?,
    modifier: Modifier = Modifier
) {
    val accent = subjectAccentColor(subject.canonicalName)
    val displayName = localizedSubjectName(subject.canonicalName)
    val displaySubtitle = localizedSubjectSubtitle(subject)
    val bgColor by animateColorAsState(
        targetValue = if (selected) accent.copy(alpha = 0.14f) else MaterialTheme.colorScheme.surface,
        label = "subject_row_bg"
    )

    Surface(
        modifier = modifier
            .fillMaxWidth()
            .minimumInteractiveComponentSize()
            .clip(MaterialTheme.shapes.medium)
            .clickable(onClick = onClick)
            .testTag("subject_card_${subject.id}"),
        shape = MaterialTheme.shapes.medium,
        color = bgColor,
        tonalElevation = if (selected) 3.dp else 1.dp,
        border = BorderStroke(
            width = if (selected) 1.8.dp else 1.dp,
            color = if (selected) accent else MaterialTheme.colorScheme.outlineVariant
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(42.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(accent.copy(alpha = 0.16f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = subjectIconForKey(subject.iconKey),
                    contentDescription = displayName,
                    tint = accent,
                    modifier = Modifier.size(22.dp)
                )
            }

            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(2.dp)
            ) {
                Text(
                    text = displayName,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = displaySubtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }

            if (onDelete != null) {
                IconButton(
                    onClick = onDelete,
                    modifier = Modifier
                        .minimumInteractiveComponentSize()
                        .testTag("delete_subject_${subject.id}")
                ) {
                    Icon(
                        imageVector = Icons.Filled.DeleteOutline,
                        contentDescription = stringResource(R.string.cd_delete_subject),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            if (selected) {
                Icon(
                    imageVector = Icons.Filled.CheckCircle,
                    contentDescription = null,
                    tint = accent,
                    modifier = Modifier.size(22.dp)
                )
            } else {
                Surface(
                    shape = CircleShape,
                    color = MaterialTheme.colorScheme.surfaceVariant
                ) {
                    Text(
                        text = count.toString(),
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                    )
                }
            }
        }
    }
}

/**
 * Boîte de dialogue permettant à l'élève d'ajouter une matière scolaire personnalisée dans Room.
 */
@Composable
private fun AddSubjectDialog(
    onDismiss: () -> Unit,
    onConfirm: (name: String, subtitle: String) -> Unit
) {
    var name by remember { mutableStateOf("") }
    var subtitle by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.subject_add_dialog_title)) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text(stringResource(R.string.subject_name_label)) },
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("add_subject_name_input")
                )
                OutlinedTextField(
                    value = subtitle,
                    onValueChange = { subtitle = it },
                    label = { Text(stringResource(R.string.subject_subtitle_label)) },
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("add_subject_subtitle_input")
                )
            }
        },
        confirmButton = {
            Button(
                onClick = { onConfirm(name, subtitle) },
                enabled = name.isNotBlank(),
                modifier = Modifier
                    .minimumInteractiveComponentSize()
                    .testTag("confirm_add_subject_btn")
            ) {
                Text(stringResource(R.string.action_save))
            }
        },
        dismissButton = {
            TextButton(
                onClick = onDismiss,
                modifier = Modifier.minimumInteractiveComponentSize()
            ) {
                Text(stringResource(R.string.action_cancel))
            }
        }
    )
}
