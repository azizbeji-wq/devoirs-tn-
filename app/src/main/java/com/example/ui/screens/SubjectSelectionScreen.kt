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
import androidx.compose.material.icons.filled.AutoStories
import androidx.compose.material.icons.filled.Biotech
import androidx.compose.material.icons.filled.Calculate
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Code
import androidx.compose.material.icons.filled.HistoryEdu
import androidx.compose.material.icons.filled.MenuBook
import androidx.compose.material.icons.filled.PrecisionManufacturing
import androidx.compose.material.icons.filled.Public
import androidx.compose.material.icons.filled.RestartAlt
import androidx.compose.material.icons.filled.Science
import androidx.compose.material.icons.filled.Translate
import androidx.compose.material3.Button
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
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
import com.example.data.model.CycleLevel
import com.example.data.model.DevoirItem
import com.example.ui.components.DevoirCard
import com.example.ui.components.subjectAccentColor
import com.example.ui.theme.CobaltDeep
import com.example.ui.theme.CobaltPrimary

data class SubjectOptionSpec(
    val id: String,
    val name: String,
    val subtitle: String,
    val icon: ImageVector
)

private val tunisianSubjectCatalog: List<SubjectOptionSpec> = listOf(
    SubjectOptionSpec("maths", "Mathématiques", "Algèbre, Analyse, Géométrie", Icons.Filled.Calculate),
    SubjectOptionSpec("physique", "Physique-Chimie", "Circuits RC/RL, Cinétique", Icons.Filled.Science),
    SubjectOptionSpec("arabe", "Arabe", "Texte, Langue & Essai (العربية)", Icons.Filled.Translate),
    SubjectOptionSpec("svt", "SVT", "Génétique, Neuro, Géologie", Icons.Filled.Biotech),
    SubjectOptionSpec("francais", "Français", "Compréhension, Langue, Essai", Icons.Filled.AutoStories),
    SubjectOptionSpec("anglais", "Anglais", "Reading, Grammar & Writing", Icons.Filled.Public),
    SubjectOptionSpec("info", "Informatique", "Python, Algo & Bases SQL", Icons.Filled.Code),
    SubjectOptionSpec("philo", "Philosophie", "Dissertation & Concepts Bac", Icons.Filled.HistoryEdu),
    SubjectOptionSpec("eco", "Économie & Gestion", "PIB, Croissance & Finance", Icons.Filled.AccountBalance),
    SubjectOptionSpec("tech", "Technologie", "GRAFCET & Logique Séquentielle", Icons.Filled.PrecisionManufacturing)
)

@Composable
fun SubjectSelectionScreen(
    allDevoirs: List<DevoirItem>,
    selectedYear: CycleLevel,
    selectedSubject: String,
    onSelectYear: (CycleLevel) -> Unit,
    onSelectSubject: (String) -> Unit,
    onResetSelection: () -> Unit,
    onOpenDevoir: (DevoirItem) -> Unit,
    onToggleFavorite: (String) -> Unit,
    onViewInLibrary: () -> Unit,
    modifier: Modifier = Modifier
) {
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

    val subjectCountsForYear = remember(allDevoirs, selectedYear) {
        val yearScoped = if (selectedYear == CycleLevel.ALL) {
            allDevoirs
        } else {
            allDevoirs.filter { it.level == selectedYear }
        }
        tunisianSubjectCatalog.associate { spec ->
            spec.name to yearScoped.count { it.subject.equals(spec.name, ignoreCase = true) }
        }
    }

    val subjectPairs = remember { tunisianSubjectCatalog.chunked(2) }
    val hasActiveFilter = selectedYear != CycleLevel.ALL || selectedSubject != "Toutes"

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .testTag("subject_selection_screen"),
        contentPadding = PaddingValues(bottom = 28.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        // Editorial Header Card
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
                                modifier = Modifier.testTag("subject_selection_reset_btn")
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
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

        // Step 1: Academic Year Selector
        item(key = "year_section_title") {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column {
                    Text(
                        text = stringResource(R.string.step_choose_year),
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onBackground
                    )
                    Text(
                        text = selectedYear.label,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
                Text(
                    text = "${yearCounts[selectedYear] ?: 0} sujets",
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

        // Step 2: Subject Grid Header
        item(key = "subject_section_title") {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column {
                    Text(
                        text = stringResource(R.string.step_choose_subject),
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onBackground
                    )
                    Text(
                        text = if (selectedSubject == "Toutes") {
                            stringResource(R.string.all_subjects_label)
                        } else {
                            selectedSubject
                        },
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.primary
                    )
                }

                if (selectedSubject != "Toutes") {
                    TextButton(
                        onClick = { onSelectSubject("Toutes") },
                        modifier = Modifier.testTag("clear_subject_selection_btn")
                    ) {
                        Text(stringResource(R.string.all_subjects_label))
                    }
                }
            }
        }

        // 2-Column Subject Grid Rows
        items(subjectPairs, key = { pair -> pair.joinToString("_") { it.id } }) { pair ->
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                pair.forEach { spec ->
                    val isSelected = selectedSubject.equals(spec.name, ignoreCase = true)
                    val count = subjectCountsForYear[spec.name] ?: 0
                    SubjectTileCard(
                        spec = spec,
                        count = count,
                        selected = isSelected,
                        onClick = {
                            if (isSelected) onSelectSubject("Toutes") else onSelectSubject(spec.name)
                        },
                        modifier = Modifier.weight(1f)
                    )
                }
                if (pair.size == 1) {
                    Spacer(modifier = Modifier.weight(1f))
                }
            }
        }

        // Matching Study Materials Header + CTA to Library
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
                        text = "${matchingDevoirs.size} support${if (matchingDevoirs.size > 1) "s" else ""} · ${selectedYear.shortLabel} · $selectedSubject",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                FilledTonalButton(
                    onClick = onViewInLibrary,
                    modifier = Modifier.testTag("open_filtered_library_btn")
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

        // Matching Study Materials List or Friendly Empty State
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
                            modifier = Modifier.testTag("empty_subject_reset_btn")
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
            .width(104.dp)
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
                text = year.cycle,
                style = MaterialTheme.typography.labelSmall,
                color = contentColor.copy(alpha = 0.78f),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = year.shortLabel,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = contentColor,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = "$count devoir${if (count > 1) "s" else ""}",
                style = MaterialTheme.typography.labelSmall,
                color = contentColor.copy(alpha = 0.85f),
                maxLines = 1
            )
        }
    }
}

@Composable
private fun SubjectTileCard(
    spec: SubjectOptionSpec,
    count: Int,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val accent = subjectAccentColor(spec.name)
    val bgColor by animateColorAsState(
        targetValue = if (selected) accent.copy(alpha = 0.14f) else MaterialTheme.colorScheme.surface,
        label = "subject_tile_bg"
    )

    Surface(
        modifier = modifier
            .clip(MaterialTheme.shapes.medium)
            .clickable(onClick = onClick)
            .testTag("subject_card_${spec.id}"),
        shape = MaterialTheme.shapes.medium,
        color = bgColor,
        tonalElevation = if (selected) 3.dp else 1.dp,
        border = BorderStroke(
            width = if (selected) 1.8.dp else 1.dp,
            color = if (selected) accent else MaterialTheme.colorScheme.outlineVariant
        )
    ) {
        Column(
            modifier = Modifier.padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Box(
                    modifier = Modifier
                        .size(38.dp)
                        .clip(RoundedCornerShape(10.dp))
                        .background(accent.copy(alpha = 0.16f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = spec.icon,
                        contentDescription = spec.name,
                        tint = accent,
                        modifier = Modifier.size(22.dp)
                    )
                }

                if (selected) {
                    Icon(
                        imageVector = Icons.Filled.CheckCircle,
                        contentDescription = null,
                        tint = accent,
                        modifier = Modifier.size(20.dp)
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
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                        )
                    }
                }
            }

            Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(
                    text = spec.name,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = spec.subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}
