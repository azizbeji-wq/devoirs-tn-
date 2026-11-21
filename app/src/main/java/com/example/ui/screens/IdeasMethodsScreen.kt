package com.example.ui.screens

import androidx.annotation.StringRes
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
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
import androidx.compose.material.icons.filled.BugReport
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material.icons.filled.MenuBook
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.TipsAndUpdates
import androidx.compose.material.icons.outlined.StarOutline
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.BuildConfig
import com.example.R
import com.example.data.model.AppFeatureIdea
import com.example.data.model.StudyMethodGuide
import com.example.ui.components.adaptiveOchre
import com.example.ui.theme.CobaltDeep

/**
 * Catégories de filtrage des fiches méthodes de révision.
 */
private enum class MethodCategoryFilter(
    val id: String,
    @StringRes val labelRes: Int
) {
    ALL("all", R.string.methods_filter_all),
    EXAMENS("examens", R.string.methods_filter_examens),
    MATIERES("matieres", R.string.methods_filter_matieres),
    ORGANISATION("organisation", R.string.methods_filter_organisation),
    RAMADAN("ramadan", R.string.methods_filter_ramadan),
    BAC("bac", R.string.methods_filter_bac)
}

/**
 * Associe chaque fiche méthode aux catégories de filtrage pertinentes
 * (Examens, Matières, Organisation, Ramadan, Bac) sans modifier les données sources.
 */
private fun StudyMethodGuide.matchesCategory(filter: MethodCategoryFilter): Boolean {
    if (filter == MethodCategoryFilter.ALL) return true
    val searchable = "$id $category $title $summary $tunisianTip".lowercase()
    return when (filter) {
        MethodCategoryFilter.ALL -> true
        MethodCategoryFilter.EXAMENS ->
            searchable.contains("ds") ||
                searchable.contains("dc") ||
                searchable.contains("concours") ||
                searchable.contains("synthèse") ||
                searchable.contains("examen")

        MethodCategoryFilter.MATIERES ->
            searchable.contains("math") ||
                searchable.contains("physique") ||
                searchable.contains("chimie") ||
                searchable.contains("philo") ||
                searchable.contains("langues") ||
                searchable.contains("svt")

        MethodCategoryFilter.ORGANISATION ->
            searchable.contains("organisation") ||
                searchable.contains("productivité") ||
                searchable.contains("stratégie") ||
                searchable.contains("rétroplanning") ||
                searchable.contains("cycles") ||
                searchable.contains("plan")

        MethodCategoryFilter.RAMADAN ->
            searchable.contains("ramadan") ||
                searchable.contains("fajr") ||
                searchable.contains("cycles 45/10")

        MethodCategoryFilter.BAC ->
            searchable.contains("bac")
    }
}

/**
 * Écran « Méthodes de révision » affichant directement les fiches méthodes sous forme de cartes
 * avec filtre par catégorie (Examens, Matières, Organisation, Ramadan, Bac), gestion des favoris
 * via la table Room existante, et section cachée réservée exclusivement au mode debug.
 */
@Composable
fun IdeasMethodsScreen(
    studyMethods: List<StudyMethodGuide>,
    appIdeas: List<AppFeatureIdea>,
    starredIdeaIds: Set<String>,
    onToggleStarIdea: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    var selectedCategoryId by rememberSaveable { mutableStateOf(MethodCategoryFilter.ALL.id) }
    var favoritesOnly by rememberSaveable { mutableStateOf(false) }
    var expandedMethodId by rememberSaveable { mutableStateOf<String?>(studyMethods.firstOrNull()?.id) }
    var debugSectionExpanded by rememberSaveable { mutableStateOf(false) }

    val selectedCategory = remember(selectedCategoryId) {
        MethodCategoryFilter.entries.find { it.id == selectedCategoryId } ?: MethodCategoryFilter.ALL
    }

    val filteredMethods = remember(studyMethods, selectedCategory, favoritesOnly, starredIdeaIds) {
        studyMethods.filter { guide ->
            val categoryOk = guide.matchesCategory(selectedCategory)
            val favoriteOk = !favoritesOnly || guide.id in starredIdeaIds
            categoryOk && favoriteOk
        }
    }

    val favoriteMethodsCount = remember(studyMethods, starredIdeaIds) {
        studyMethods.count { it.id in starredIdeaIds }
    }

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .testTag("ideas_methods_list"),
        contentPadding = PaddingValues(bottom = 28.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // Bandeau d'en-tête compact (116 dp <= 120 dp) avec dégradé sombre renforcé pour une lisibilité optimale
        item(key = "methods_banner") {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp)
                    .height(116.dp)
                    .clip(MaterialTheme.shapes.large)
            ) {
                Image(
                    painter = painterResource(id = R.drawable.img_study_ideas_1795190054925),
                    contentDescription = stringResource(R.string.cd_ideas_banner),
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize()
                )
                // Dégradé sombre renforcé sous le texte pour garantir un fort contraste WCAG
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(
                            Brush.verticalGradient(
                                colorStops = arrayOf(
                                    0.0f to Color.Black.copy(alpha = 0.28f),
                                    0.42f to CobaltDeep.copy(alpha = 0.72f),
                                    1.0f to Color(0xEB071022)
                                )
                            )
                        )
                )
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 16.dp, vertical = 14.dp),
                    verticalArrangement = Arrangement.Bottom
                ) {
                    Text(
                        text = stringResource(R.string.methods_title),
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = Color.White,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Spacer(modifier = Modifier.height(3.dp))
                    Text(
                        text = stringResource(R.string.methods_subtitle),
                        style = MaterialTheme.typography.bodySmall,
                        color = Color.White.copy(alpha = 0.94f),
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
        }

        // Filtre par catégorie (Toutes, Examens, Matières, Organisation, Ramadan, Bac) + Favoris
        item(key = "methods_category_filters") {
            val ochre = adaptiveOchre()
            LazyRow(
                contentPadding = PaddingValues(horizontal = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.testTag("methods_category_filter_row")
            ) {
                // Puce Favoris
                item(key = "filter_favorites") {
                    FilterChip(
                        selected = favoritesOnly,
                        onClick = { favoritesOnly = !favoritesOnly },
                        modifier = Modifier
                            .minimumInteractiveComponentSize()
                            .testTag("methods_filter_favorites"),
                        leadingIcon = {
                            Icon(
                                imageVector = if (favoritesOnly) Icons.Filled.Star else Icons.Outlined.StarOutline,
                                contentDescription = null,
                                modifier = Modifier.size(16.dp)
                            )
                        },
                        label = {
                            val favLabel = if (favoriteMethodsCount > 0) {
                                "${stringResource(R.string.methods_filter_favorites)} ($favoriteMethodsCount)"
                            } else {
                                stringResource(R.string.methods_filter_favorites)
                            }
                            Text(favLabel, maxLines = 1)
                        },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = ochre.copy(alpha = 0.22f),
                            selectedLabelColor = ochre,
                            selectedLeadingIconColor = ochre
                        )
                    )
                }

                // Puces des catégories demandées
                items(MethodCategoryFilter.entries, key = { it.id }) { category ->
                    val isSelected = selectedCategory == category
                    FilterChip(
                        selected = isSelected,
                        onClick = { selectedCategoryId = category.id },
                        modifier = Modifier
                            .minimumInteractiveComponentSize()
                            .testTag("methods_filter_${category.id}"),
                        label = {
                            Text(
                                text = stringResource(category.labelRes),
                                maxLines = 1
                            )
                        }
                    )
                }
            }
        }

        // Liste directe des fiches méthodes ou état vide si filtre sans résultat
        if (filteredMethods.isEmpty()) {
            item(key = "methods_empty_state") {
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
                            modifier = Modifier.size(36.dp)
                        )
                        Spacer(modifier = Modifier.height(10.dp))
                        Text(
                            text = stringResource(R.string.methods_empty_title),
                            style = MaterialTheme.typography.titleMedium,
                            textAlign = TextAlign.Center
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        TextButton(
                            onClick = {
                                selectedCategoryId = MethodCategoryFilter.ALL.id
                                favoritesOnly = false
                            },
                            modifier = Modifier
                                .minimumInteractiveComponentSize()
                                .testTag("methods_reset_filters_btn")
                        ) {
                            Text(stringResource(R.string.action_reset_filters))
                        }
                    }
                }
            }
        } else {
            items(filteredMethods, key = { it.id }) { guide ->
                val expanded = expandedMethodId == guide.id
                val isFavorite = guide.id in starredIdeaIds
                StudyMethodCard(
                    guide = guide,
                    expanded = expanded,
                    isFavorite = isFavorite,
                    onToggleExpand = {
                        expandedMethodId = if (expanded) null else guide.id
                    },
                    onToggleFavorite = { onToggleStarIdea(guide.id) },
                    modifier = Modifier.padding(horizontal = 16.dp)
                )
            }
        }

        // Section cachée réservée exclusivement au mode debug (jamais affichée en release)
        if (BuildConfig.DEBUG && appIdeas.isNotEmpty()) {
            item(key = "debug_ideas_hidden_section") {
                DebugProjectIdeasSection(
                    appIdeas = appIdeas,
                    expanded = debugSectionExpanded,
                    onToggleExpand = { debugSectionExpanded = !debugSectionExpanded },
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp)
                )
            }
        }
    }
}

/**
 * Carte d'une fiche méthode : catégorie, durée, bouton favori, titre, résumé,
 * étapes numérotées dépliables et encadré « Conseil tunisien ».
 */
@Composable
private fun StudyMethodCard(
    guide: StudyMethodGuide,
    expanded: Boolean,
    isFavorite: Boolean,
    onToggleExpand: () -> Unit,
    onToggleFavorite: () -> Unit,
    modifier: Modifier = Modifier
) {
    val ochre = adaptiveOchre()

    Surface(
        modifier = modifier
            .fillMaxWidth()
            .clip(MaterialTheme.shapes.medium)
            .clickable(onClick = onToggleExpand)
            .testTag("method_card_${guide.id}"),
        shape = MaterialTheme.shapes.medium,
        color = MaterialTheme.colorScheme.surface,
        tonalElevation = if (isFavorite) 2.dp else 1.dp,
        border = BorderStroke(
            width = if (isFavorite) 1.5.dp else 1.dp,
            color = if (isFavorite) ochre.copy(alpha = 0.7f) else MaterialTheme.colorScheme.outlineVariant
        )
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    modifier = Modifier.weight(1f),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = MaterialTheme.colorScheme.primaryContainer
                    ) {
                        Text(
                            text = guide.category,
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onPrimaryContainer,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.padding(horizontal = 9.dp, vertical = 4.dp)
                        )
                    }
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(3.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Schedule,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(14.dp)
                        )
                        Text(
                            text = guide.durationTag,
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconButton(
                        onClick = onToggleFavorite,
                        modifier = Modifier
                            .minimumInteractiveComponentSize()
                            .testTag("favorite_method_btn_${guide.id}")
                    ) {
                        Icon(
                            imageVector = if (isFavorite) Icons.Filled.Star else Icons.Outlined.StarOutline,
                            contentDescription = stringResource(
                                if (isFavorite) R.string.bookmarked_cd else R.string.bookmark_cd
                            ),
                            tint = if (isFavorite) ochre else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Icon(
                        imageVector = if (expanded) Icons.Filled.ExpandLess else Icons.Filled.ExpandMore,
                        contentDescription = stringResource(R.string.cd_expand_method),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            Spacer(modifier = Modifier.height(6.dp))

            Text(
                text = guide.title,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )

            Spacer(modifier = Modifier.height(6.dp))

            Text(
                text = guide.summary,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            // Étapes dépliables + encadré Conseil tunisien
            AnimatedVisibility(
                visible = expanded,
                enter = expandVertically() + fadeIn(),
                exit = shrinkVertically() + fadeOut()
            ) {
                Column(modifier = Modifier.padding(top = 12.dp)) {
                    guide.steps.forEachIndexed { index, step ->
                        Row(
                            modifier = Modifier.padding(vertical = 4.dp),
                            verticalAlignment = Alignment.Top
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(22.dp)
                                    .clip(CircleShape)
                                    .background(MaterialTheme.colorScheme.primary),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = "${index + 1}",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onPrimary,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                            Spacer(modifier = Modifier.width(10.dp))
                            Text(
                                text = step,
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurface,
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .background(ochre.copy(alpha = 0.14f))
                            .padding(12.dp),
                        verticalAlignment = Alignment.Top
                    ) {
                        Icon(
                            imageVector = Icons.Filled.TipsAndUpdates,
                            contentDescription = null,
                            tint = ochre,
                            modifier = Modifier
                                .padding(top = 2.dp)
                                .size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = stringResource(R.string.methods_tunisian_tip_label),
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.Bold,
                                color = ochre
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = guide.tunisianTip,
                                style = MaterialTheme.typography.bodySmall,
                                fontWeight = FontWeight.Medium,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }
                    }
                }
            }
        }
    }
}

/**
 * Section cachée réservée exclusivement aux builds DEBUG (BuildConfig.DEBUG),
 * repliée par défaut, conservant les idées produit internes sans compteur ni onglet public.
 */
@Composable
private fun DebugProjectIdeasSection(
    appIdeas: List<AppFeatureIdea>,
    expanded: Boolean,
    onToggleExpand: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier
            .fillMaxWidth()
            .clip(MaterialTheme.shapes.medium)
            .clickable(onClick = onToggleExpand)
            .testTag("debug_ideas_section"),
        shape = MaterialTheme.shapes.medium,
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f),
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
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        imageVector = Icons.Filled.BugReport,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(18.dp)
                    )
                    Text(
                        text = stringResource(R.string.debug_roadmap_section_title, appIdeas.size),
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Icon(
                    imageVector = if (expanded) Icons.Filled.ExpandLess else Icons.Filled.ExpandMore,
                    contentDescription = stringResource(R.string.cd_expand_method),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            AnimatedVisibility(
                visible = expanded,
                enter = expandVertically() + fadeIn(),
                exit = shrinkVertically() + fadeOut()
            ) {
                Column(
                    modifier = Modifier.padding(top = 10.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    appIdeas.forEach { idea ->
                        Surface(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(10.dp),
                            color = MaterialTheme.colorScheme.surface
                        ) {
                            Column(
                                modifier = Modifier.padding(12.dp),
                                verticalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Text(
                                    text = "${idea.category} · ${idea.targetAudience}",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.primary
                                )
                                Text(
                                    text = idea.title,
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.SemiBold,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Text(
                                    text = idea.description,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
