package com.example.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
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
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Sort
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.CloudDownload
import androidx.compose.material.icons.filled.FilterList
import androidx.compose.material.icons.filled.FolderZip
import androidx.compose.material.icons.filled.MenuBook
import androidx.compose.material.icons.filled.SdStorage
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material3.Button
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.InputChip
import androidx.compose.material3.InputChipDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import com.example.R
import com.example.data.download.DevoirDownloadProgress
import com.example.data.download.DownloadPack
import com.example.data.download.PdfDownloadCoordinator
import com.example.data.model.CycleLevel
import com.example.data.model.DevoirItem
import com.example.data.model.DevoirType
import com.example.data.model.Trimester
import com.example.data.model.TunisianSection
import com.example.ui.components.DevoirCard
import com.example.ui.components.adaptiveEmerald
import com.example.ui.components.localizedBadge
import com.example.ui.components.localizedLabel
import com.example.ui.components.localizedShortLabel
import com.example.ui.components.localizedSubjectName
import com.example.ui.theme.CobaltDeep
import com.example.ui.theme.CrimsonAccent
import com.example.ui.viewmodel.DevoirFilterState
import com.example.ui.viewmodel.DevoirSortOption
import java.util.Locale

/** Représente une puce de filtre actif affichée sous la barre de recherche. */
private data class ActiveFilterChipItem(
    val key: String,
    val label: String,
    val onRemove: () -> Unit
)

/** Retourne le libellé localisé d'une option de tri. */
@Composable
private fun DevoirSortOption.localizedLabel(): String = when (this) {
    DevoirSortOption.RECENT -> stringResource(R.string.sort_recent)
    DevoirSortOption.DURATION -> stringResource(R.string.sort_duration)
    DevoirSortOption.DIFFICULTY -> stringResource(R.string.sort_difficulty)
    DevoirSortOption.SUBJECT -> stringResource(R.string.sort_subject)
}

/**
 * Écran Bibliothèque des Devoirs :
 * - Bannière compacte (<= 120 dp) qui se masque naturellement au défilement,
 * - Barre de filtres sticky (recherche, niveaux avec dégradé de bord, puces actives + Tout effacer),
 * - Tri multicritères (plus récents, durée, difficulté, matière),
 * - Compteur au vrai pluriel et mention discrète « Sujets d'entraînement, non officiels ».
 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun DevoirsLibraryScreen(
    devoirs: List<DevoirItem>,
    totalCount: Int,
    filterState: DevoirFilterState,
    availableSubjects: List<String>,
    onSearchChange: (String) -> Unit,
    onSelectLevel: (CycleLevel) -> Unit,
    onSelectSection: (TunisianSection) -> Unit,
    onSelectType: (DevoirType) -> Unit,
    onSelectTrimester: (Trimester) -> Unit,
    onSelectSubject: (String) -> Unit,
    onToggleFavoritesOnly: () -> Unit,
    onSelectSortOption: (DevoirSortOption) -> Unit = {},
    onResetFilters: () -> Unit,
    onOpenDevoir: (DevoirItem) -> Unit,
    onToggleFavorite: (String) -> Unit,
    modifier: Modifier = Modifier,
    allDevoirs: List<DevoirItem> = devoirs,
    searchSnippetsByDevoirId: Map<String, String> = emptyMap(),
    downloadStates: Map<String, DevoirDownloadProgress> = emptyMap(),
    hasResumablePart: (String) -> Boolean = { false },
    onDownloadDevoir: (DevoirItem) -> Unit = {},
    onCancelDownload: (String) -> Unit = {},
    onDownloadPack: (DownloadPack, String) -> Unit = { _, _ -> },
    onOpenStorage: () -> Unit = {}
) {
    val featuredPack = remember(allDevoirs) {
        PdfDownloadCoordinator.buildPacks(allDevoirs).firstOrNull {
            it.level == CycleLevel.COLLEGE_9 && it.trimester == Trimester.T1
        }
    }
    var showAdvancedFilters by rememberSaveable { mutableStateOf(false) }
    var sortMenuExpanded by remember { mutableStateOf(false) }

    val listState = rememberLazyListState()
    val isStickyPinned by remember {
        derivedStateOf { listState.firstVisibleItemIndex >= 1 }
    }

    val activeFilterCount = listOf(
        filterState.selectedLevel != CycleLevel.ALL,
        filterState.selectedSection != TunisianSection.ALL,
        filterState.selectedType != DevoirType.ALL,
        filterState.selectedTrimester != Trimester.ALL,
        filterState.selectedSubject != "Toutes",
        filterState.favoritesOnly
    ).count { it }

    // Construction des puces de filtres actifs (visibles sous la barre de recherche)
    val activeFilterChips = buildList {
        if (filterState.searchQuery.isNotBlank()) {
            add(
                ActiveFilterChipItem(
                    key = "query",
                    label = "« ${filterState.searchQuery.trim()} »",
                    onRemove = { onSearchChange("") }
                )
            )
        }
        if (filterState.selectedLevel != CycleLevel.ALL) {
            add(
                ActiveFilterChipItem(
                    key = "level",
                    label = filterState.selectedLevel.localizedShortLabel(),
                    onRemove = { onSelectLevel(CycleLevel.ALL) }
                )
            )
        }
        if (filterState.selectedSection != TunisianSection.ALL) {
            add(
                ActiveFilterChipItem(
                    key = "section",
                    label = filterState.selectedSection.localizedShortLabel(),
                    onRemove = { onSelectSection(TunisianSection.ALL) }
                )
            )
        }
        if (filterState.selectedType != DevoirType.ALL) {
            add(
                ActiveFilterChipItem(
                    key = "type",
                    label = filterState.selectedType.localizedBadge(),
                    onRemove = { onSelectType(DevoirType.ALL) }
                )
            )
        }
        if (filterState.selectedTrimester != Trimester.ALL) {
            add(
                ActiveFilterChipItem(
                    key = "trimester",
                    label = filterState.selectedTrimester.localizedShortLabel(),
                    onRemove = { onSelectTrimester(Trimester.ALL) }
                )
            )
        }
        if (filterState.selectedSubject != "Toutes") {
            add(
                ActiveFilterChipItem(
                    key = "subject",
                    label = localizedSubjectName(filterState.selectedSubject),
                    onRemove = { onSelectSubject("Toutes") }
                )
            )
        }
        if (filterState.favoritesOnly) {
            add(
                ActiveFilterChipItem(
                    key = "favorites",
                    label = stringResource(R.string.filter_favorites_chip),
                    onRemove = onToggleFavoritesOnly
                )
            )
        }
    }

    LazyColumn(
        state = listState,
        modifier = modifier
            .fillMaxSize()
            .testTag("devoirs_library_list"),
        contentPadding = PaddingValues(bottom = 24.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // 1. En-tête compact (116 dp <= 120 dp) qui disparaît au défilement
        item(key = "hero_banner") {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 6.dp)
                    .height(116.dp)
                    .clip(MaterialTheme.shapes.large)
                    .testTag("library_hero_banner")
            ) {
                Image(
                    painter = painterResource(id = R.drawable.img_hero_devoirs_1795190058528),
                    contentDescription = stringResource(R.string.cd_hero_banner),
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize()
                )
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(
                            Brush.verticalGradient(
                                colors = listOf(
                                    CobaltDeep.copy(alpha = 0.42f),
                                    CobaltDeep.copy(alpha = 0.78f),
                                    CobaltDeep.copy(alpha = 0.95f)
                                )
                            )
                        )
                )
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 16.dp, vertical = 12.dp),
                    verticalArrangement = Arrangement.SpaceBetween
                ) {
                    Surface(
                        shape = CircleShape,
                        color = CrimsonAccent.copy(alpha = 0.92f)
                    ) {
                        Text(
                            text = stringResource(R.string.hero_badge_regime),
                            style = MaterialTheme.typography.labelSmall,
                            color = Color.White,
                            fontWeight = FontWeight.Bold,
                            maxLines = 1,
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 3.dp)
                        )
                    }

                    Column {
                        Text(
                            text = stringResource(R.string.hero_title),
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                            color = Color.White,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = stringResource(R.string.hero_subtitle),
                            style = MaterialTheme.typography.bodySmall,
                            color = Color.White.copy(alpha = 0.90f),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
            }
        }

        // 2. Barre de filtres sticky : recherche + niveaux avec dégradé de bord + puces actives
        stickyHeader(key = "sticky_filter_bar") {
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("sticky_filter_bar"),
                color = MaterialTheme.colorScheme.background,
                shadowElevation = if (isStickyPinned) 4.dp else 0.dp
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 6.dp, bottom = 6.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    // Barre de recherche plein texte + bouton filtres avancés
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        OutlinedTextField(
                            value = filterState.searchQuery,
                            onValueChange = onSearchChange,
                            modifier = Modifier
                                .weight(1f)
                                .testTag("devoirs_search_input"),
                            placeholder = {
                                Text(
                                    text = stringResource(R.string.search_placeholder),
                                    style = MaterialTheme.typography.bodyMedium,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            },
                            leadingIcon = {
                                Icon(
                                    imageVector = Icons.Filled.Search,
                                    contentDescription = stringResource(R.string.cd_search_icon),
                                    tint = MaterialTheme.colorScheme.primary
                                )
                            },
                            trailingIcon = {
                                if (filterState.searchQuery.isNotEmpty()) {
                                    IconButton(
                                        onClick = { onSearchChange("") },
                                        modifier = Modifier
                                            .minimumInteractiveComponentSize()
                                            .testTag("clear_search_btn")
                                    ) {
                                        Icon(
                                            imageVector = Icons.Filled.Close,
                                            contentDescription = stringResource(R.string.cd_clear_search)
                                        )
                                    }
                                }
                            },
                            singleLine = true,
                            shape = RoundedCornerShape(16.dp),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedContainerColor = MaterialTheme.colorScheme.surface,
                                unfocusedContainerColor = MaterialTheme.colorScheme.surface,
                                focusedBorderColor = MaterialTheme.colorScheme.primary,
                                unfocusedBorderColor = MaterialTheme.colorScheme.outlineVariant
                            )
                        )

                        Surface(
                            onClick = { showAdvancedFilters = !showAdvancedFilters },
                            modifier = Modifier
                                .size(54.dp)
                                .testTag("toggle_advanced_filters_btn"),
                            shape = RoundedCornerShape(16.dp),
                            color = if (showAdvancedFilters || activeFilterCount > 0) {
                                MaterialTheme.colorScheme.primary
                            } else {
                                MaterialTheme.colorScheme.surface
                            },
                            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    imageVector = Icons.Filled.Tune,
                                    contentDescription = stringResource(R.string.cd_toggle_filters),
                                    tint = if (showAdvancedFilters || activeFilterCount > 0) {
                                        MaterialTheme.colorScheme.onPrimary
                                    } else {
                                        MaterialTheme.colorScheme.onSurface
                                    }
                                )
                            }
                        }
                    }

                    // Rangée horizontale des niveaux scolaires avec dégradé de bord (compatible RTL)
                    LevelChipsRowWithEdgeFade(
                        selectedLevel = filterState.selectedLevel,
                        onSelectLevel = onSelectLevel,
                        fadeColor = MaterialTheme.colorScheme.background
                    )

                    // Puces de filtres actifs visibles sous la recherche + bouton « Tout effacer »
                    AnimatedVisibility(
                        visible = activeFilterChips.isNotEmpty(),
                        enter = expandVertically() + fadeIn(),
                        exit = shrinkVertically() + fadeOut()
                    ) {
                        LazyRow(
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("active_filters_row"),
                            contentPadding = PaddingValues(horizontal = 16.dp),
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            items(activeFilterChips, key = { it.key }) { chip ->
                                InputChip(
                                    selected = true,
                                    onClick = chip.onRemove,
                                    modifier = Modifier
                                        .minimumInteractiveComponentSize()
                                        .testTag("active_filter_chip_${chip.key}"),
                                    label = {
                                        Text(
                                            text = chip.label,
                                            style = MaterialTheme.typography.labelMedium,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                    },
                                    trailingIcon = {
                                        Icon(
                                            imageVector = Icons.Filled.Close,
                                            contentDescription = stringResource(
                                                R.string.cd_remove_filter,
                                                chip.label
                                            ),
                                            modifier = Modifier.size(16.dp)
                                        )
                                    },
                                    colors = InputChipDefaults.inputChipColors(
                                        selectedContainerColor = MaterialTheme.colorScheme.secondaryContainer,
                                        selectedLabelColor = MaterialTheme.colorScheme.onSecondaryContainer,
                                        selectedTrailingIconColor = MaterialTheme.colorScheme.onSecondaryContainer
                                    )
                                )
                            }

                            item(key = "clear_all_btn") {
                                TextButton(
                                    onClick = onResetFilters,
                                    modifier = Modifier
                                        .minimumInteractiveComponentSize()
                                        .testTag("clear_all_filters_btn")
                                ) {
                                    Text(
                                        text = stringResource(R.string.action_clear_all),
                                        style = MaterialTheme.typography.labelLarge,
                                        fontWeight = FontWeight.SemiBold,
                                        color = MaterialTheme.colorScheme.primary
                                    )
                                }
                            }
                        }
                    }

                    if (isStickyPinned) {
                        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                    }
                }
            }
        }

        // 3. Panneau repliable de filtres avancés (Type, Trimestre, Section, Matière)
        item(key = "advanced_filters") {
            AnimatedVisibility(
                visible = showAdvancedFilters,
                enter = expandVertically() + fadeIn(),
                exit = shrinkVertically() + fadeOut()
            ) {
                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp),
                    shape = MaterialTheme.shapes.medium,
                    color = MaterialTheme.colorScheme.surface,
                    tonalElevation = 2.dp,
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
                ) {
                    Column(
                        modifier = Modifier.padding(14.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.Filled.FilterList,
                                    contentDescription = stringResource(R.string.cd_filter_panel_icon),
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = stringResource(R.string.filter_panel_title),
                                    style = MaterialTheme.typography.titleSmall
                                )
                            }
                            if (activeFilterCount > 0) {
                                TextButton(
                                    onClick = onResetFilters,
                                    modifier = Modifier.minimumInteractiveComponentSize()
                                ) {
                                    Text(stringResource(R.string.action_clear_all))
                                }
                            }
                        }

                        // Type d'épreuve
                        Text(
                            text = stringResource(R.string.filter_type_label),
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            items(DevoirType.entries) { type ->
                                FilterChip(
                                    selected = filterState.selectedType == type,
                                    onClick = { onSelectType(type) },
                                    modifier = Modifier.minimumInteractiveComponentSize(),
                                    label = { Text(type.localizedLabel()) }
                                )
                            }
                        }

                        // Trimestre
                        Text(
                            text = stringResource(R.string.filter_trimester_label),
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            items(Trimester.entries) { trimester ->
                                FilterChip(
                                    selected = filterState.selectedTrimester == trimester,
                                    onClick = { onSelectTrimester(trimester) },
                                    modifier = Modifier.minimumInteractiveComponentSize(),
                                    label = { Text(trimester.localizedLabel()) }
                                )
                            }
                        }

                        // Section
                        Text(
                            text = stringResource(R.string.filter_section_label),
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            items(TunisianSection.entries) { section ->
                                FilterChip(
                                    selected = filterState.selectedSection == section,
                                    onClick = { onSelectSection(section) },
                                    modifier = Modifier.minimumInteractiveComponentSize(),
                                    label = { Text(section.localizedShortLabel()) }
                                )
                            }
                        }

                        // Matière
                        Text(
                            text = stringResource(R.string.filter_subject_label),
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            items(availableSubjects) { subject ->
                                FilterChip(
                                    selected = filterState.selectedSubject == subject,
                                    onClick = { onSelectSubject(subject) },
                                    modifier = Modifier.minimumInteractiveComponentSize(),
                                    label = { Text(localizedSubjectName(subject)) }
                                )
                            }
                        }
                    }
                }
            }
        }

        // 4. En-tête des résultats : vrai pluriel, mention non officielle, tri et favoris
        item(key = "results_header") {
            val resultsCountText = if (devoirs.size == 1) {
                pluralStringResource(R.plurals.devoirs_count_plural, devoirs.size, devoirs.size)
            } else {
                pluralStringResource(
                    R.plurals.results_count_of_total_plural,
                    devoirs.size,
                    devoirs.size,
                    totalCount
                )
            }

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = resultsCountText,
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.onSurface,
                            modifier = Modifier.testTag("results_count_text")
                        )
                        Text(
                            text = stringResource(R.string.library_unofficial_disclaimer),
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.testTag("library_unofficial_disclaimer")
                        )
                    }
                }

                // Ligne compacte Tri + Favoris
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    // Sélecteur de tri (Plus récents, Durée, Difficulté, Matière)
                    Box {
                        FilterChip(
                            selected = filterState.sortOption != DevoirSortOption.RECENT,
                            onClick = { sortMenuExpanded = true },
                            modifier = Modifier
                                .minimumInteractiveComponentSize()
                                .testTag("sort_dropdown_btn"),
                            leadingIcon = {
                                Icon(
                                    imageVector = Icons.AutoMirrored.Filled.Sort,
                                    contentDescription = stringResource(R.string.cd_sort_menu),
                                    modifier = Modifier.size(16.dp)
                                )
                            },
                            trailingIcon = {
                                Icon(
                                    imageVector = Icons.Filled.ArrowDropDown,
                                    contentDescription = null,
                                    modifier = Modifier.size(18.dp)
                                )
                            },
                            label = {
                                Text(
                                    text = "${stringResource(R.string.sort_label)} : ${filterState.sortOption.localizedLabel()}",
                                    style = MaterialTheme.typography.labelMedium,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                        )

                        DropdownMenu(
                            expanded = sortMenuExpanded,
                            onDismissRequest = { sortMenuExpanded = false }
                        ) {
                            DevoirSortOption.entries.forEach { option ->
                                val isSelected = filterState.sortOption == option
                                DropdownMenuItem(
                                    text = {
                                        Text(
                                            text = option.localizedLabel(),
                                            fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal
                                        )
                                    },
                                    trailingIcon = if (isSelected) {
                                        {
                                            Icon(
                                                imageVector = Icons.Filled.Check,
                                                contentDescription = null,
                                                tint = MaterialTheme.colorScheme.primary,
                                                modifier = Modifier.size(18.dp)
                                            )
                                        }
                                    } else {
                                        null
                                    },
                                    onClick = {
                                        onSelectSortOption(option)
                                        sortMenuExpanded = false
                                    },
                                    modifier = Modifier.testTag("sort_option_${option.id}")
                                )
                            }
                        }
                    }

                    FilterChip(
                        selected = filterState.favoritesOnly,
                        onClick = onToggleFavoritesOnly,
                        modifier = Modifier
                            .minimumInteractiveComponentSize()
                            .testTag("favorites_only_chip"),
                        leadingIcon = {
                            Icon(
                                imageVector = Icons.Filled.Bookmark,
                                contentDescription = stringResource(R.string.cd_favorites_chip_icon),
                                modifier = Modifier.size(16.dp)
                            )
                        },
                        label = { Text(stringResource(R.string.filter_favorites_chip)) }
                    )

                    FilterChip(
                        selected = false,
                        onClick = onOpenStorage,
                        modifier = Modifier
                            .minimumInteractiveComponentSize()
                            .testTag("library_storage_shortcut_chip"),
                        leadingIcon = {
                            Icon(
                                imageVector = Icons.Filled.SdStorage,
                                contentDescription = stringResource(R.string.cd_tab_storage),
                                modifier = Modifier.size(16.dp)
                            )
                        },
                        label = { Text(stringResource(R.string.space_tab_storage)) }
                    )
                }
            }
        }

        // 4 bis. Bannière rapide Pack 1-clic « Télécharger tout le trimestre 1 — 9ème année »
        if (featuredPack != null) {
            item(key = "featured_pack_9eme_t1") {
                val emerald = adaptiveEmerald()
                val packTitle = stringResource(R.string.download_pack_9eme_t1_cta)
                val activeProgresses = featuredPack.devoirs.mapNotNull { downloadStates[it.id] }
                    .filter { it.isActive }
                val isPackBusy = activeProgresses.isNotEmpty()
                val avgPct = if (activeProgresses.isEmpty()) {
                    0
                } else {
                    activeProgresses.sumOf { it.progressPercent } / activeProgresses.size
                }
                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp)
                        .testTag("featured_pack_card"),
                    shape = MaterialTheme.shapes.large,
                    color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.42f),
                    border = BorderStroke(
                        1.dp,
                        if (featuredPack.isFullyDownloaded) {
                            emerald.copy(alpha = 0.45f)
                        } else {
                            MaterialTheme.colorScheme.primary.copy(alpha = 0.28f)
                        }
                    )
                ) {
                    Column(
                        modifier = Modifier.padding(14.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Filled.FolderZip,
                                contentDescription = stringResource(R.string.cd_download_action_icon),
                                tint = if (featuredPack.isFullyDownloaded) emerald else MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(22.dp)
                            )
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = packTitle,
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Text(
                                    text = stringResource(
                                        R.string.download_pack_progress_summary,
                                        featuredPack.downloadedCount,
                                        featuredPack.totalCount,
                                        String.format(Locale.getDefault(), "%.1f", featuredPack.totalSizeMb)
                                    ),
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            if (featuredPack.isFullyDownloaded) {
                                Text(
                                    text = stringResource(R.string.download_pack_complete),
                                    style = MaterialTheme.typography.labelLarge,
                                    color = emerald,
                                    fontWeight = FontWeight.SemiBold,
                                    modifier = Modifier.weight(1f)
                                )
                            } else {
                                FilledTonalButton(
                                    onClick = { onDownloadPack(featuredPack, packTitle) },
                                    enabled = !isPackBusy,
                                    modifier = Modifier
                                        .weight(1f)
                                        .minimumInteractiveComponentSize()
                                        .testTag("featured_pack_download_btn")
                                ) {
                                    Icon(
                                        imageVector = Icons.Filled.CloudDownload,
                                        contentDescription = stringResource(R.string.cd_download_action_icon),
                                        modifier = Modifier.size(18.dp)
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        text = if (isPackBusy) {
                                            stringResource(R.string.status_downloading_pct, avgPct)
                                        } else {
                                            stringResource(R.string.action_download)
                                        },
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }
                            }

                            TextButton(
                                onClick = onOpenStorage,
                                modifier = Modifier
                                    .minimumInteractiveComponentSize()
                                    .testTag("featured_pack_manage_storage_btn")
                            ) {
                                Text(stringResource(R.string.space_tab_storage))
                            }
                        }
                    }
                }
            }
        }

        // 5. Cartes de devoirs avec mise en évidence de la recherche ou état vide
        if (devoirs.isEmpty()) {
            item(key = "empty_state") {
                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 12.dp),
                    shape = MaterialTheme.shapes.large,
                    color = MaterialTheme.colorScheme.surface,
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(28.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Icon(
                            imageVector = Icons.Filled.MenuBook,
                            contentDescription = stringResource(R.string.cd_empty_library_icon),
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(48.dp)
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        Text(
                            text = stringResource(R.string.empty_devoirs_title),
                            style = MaterialTheme.typography.titleMedium,
                            textAlign = TextAlign.Center
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = stringResource(R.string.empty_devoirs_subtitle),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            textAlign = TextAlign.Center
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Button(
                            onClick = onResetFilters,
                            modifier = Modifier
                                .minimumInteractiveComponentSize()
                                .testTag("reset_filters_empty_btn")
                        ) {
                            Text(stringResource(R.string.action_clear_all))
                        }
                    }
                }
            }
        } else {
            items(devoirs, key = { it.id }) { devoir ->
                DevoirCard(
                    devoir = devoir,
                    searchQuery = filterState.searchQuery,
                    matchingPdfSnippet = searchSnippetsByDevoirId[devoir.id],
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
}

/**
 * Rangée horizontale des niveaux scolaires avec dégradé fondu aux bords
 * pour signaler visuellement le défilement horizontal (compatible LTR et RTL).
 */
@Composable
private fun LevelChipsRowWithEdgeFade(
    selectedLevel: CycleLevel,
    onSelectLevel: (CycleLevel) -> Unit,
    fadeColor: Color
) {
    val levelRowState = rememberLazyListState()
    val showStartFade by remember { derivedStateOf { levelRowState.canScrollBackward } }
    val showEndFade by remember { derivedStateOf { levelRowState.canScrollForward } }
    val isRtl = LocalLayoutDirection.current == LayoutDirection.Rtl

    Box(modifier = Modifier.fillMaxWidth()) {
        LazyRow(
            state = levelRowState,
            contentPadding = PaddingValues(horizontal = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(CycleLevel.entries) { level ->
                val selected = selectedLevel == level
                FilterChip(
                    selected = selected,
                    onClick = { onSelectLevel(level) },
                    modifier = Modifier
                        .minimumInteractiveComponentSize()
                        .testTag("level_chip_${level.id}"),
                    label = {
                        Text(
                            text = level.localizedShortLabel(),
                            style = MaterialTheme.typography.labelLarge
                        )
                    },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = MaterialTheme.colorScheme.primary,
                        selectedLabelColor = MaterialTheme.colorScheme.onPrimary
                    )
                )
            }
        }

        // Dégradé de bord au début de la rangée (quand on peut revenir en arrière)
        if (showStartFade) {
            val startColors = if (isRtl) {
                listOf(Color.Transparent, fadeColor)
            } else {
                listOf(fadeColor, Color.Transparent)
            }
            Box(
                modifier = Modifier
                    .align(Alignment.CenterStart)
                    .width(28.dp)
                    .height(48.dp)
                    .background(Brush.horizontalGradient(startColors))
            )
        }

        // Dégradé de bord à la fin de la rangée (indique qu'il y a d'autres niveaux)
        if (showEndFade) {
            val endColors = if (isRtl) {
                listOf(fadeColor, Color.Transparent)
            } else {
                listOf(Color.Transparent, fadeColor)
            }
            Box(
                modifier = Modifier
                    .align(Alignment.CenterEnd)
                    .width(28.dp)
                    .height(48.dp)
                    .background(Brush.horizontalGradient(endColors))
            )
        }
    }
}
