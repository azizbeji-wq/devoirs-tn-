package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.LibraryBooks
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.R
import com.example.data.model.DevoirItem
import com.example.ui.components.DevoirCard
import com.example.ui.components.subjectAccentColor
import com.example.ui.theme.CrimsonAccent
import com.example.ui.theme.EmeraldVerified
import com.example.ui.viewmodel.SubjectProgressStat

@Composable
fun MySpaceScreen(
    allDevoirs: List<DevoirItem>,
    subjectStats: List<SubjectProgressStat>,
    onOpenDevoir: (DevoirItem) -> Unit,
    onToggleFavorite: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val favoriteDevoirs = allDevoirs.filter { it.isFavorite }
    val completedDevoirs = allDevoirs.filter { it.isCompleted }

    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .testTag("my_space_list"),
        contentPadding = PaddingValues(bottom = 28.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        item(key = "space_header") {
            Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp)) {
                Text(
                    text = stringResource(R.string.suivi_title),
                    style = MaterialTheme.typography.headlineMedium,
                    color = MaterialTheme.colorScheme.onBackground
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = stringResource(R.string.suivi_subtitle),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        // KPI Stat Row
        item(key = "kpi_row") {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                StatKpiCard(
                    icon = Icons.Filled.CheckCircle,
                    value = completedDevoirs.size.toString(),
                    label = stringResource(R.string.stat_completed),
                    accent = EmeraldVerified,
                    modifier = Modifier.weight(1f)
                )
                StatKpiCard(
                    icon = Icons.Filled.Bookmark,
                    value = favoriteDevoirs.size.toString(),
                    label = stringResource(R.string.stat_favorites),
                    accent = CrimsonAccent,
                    modifier = Modifier.weight(1f)
                )
                StatKpiCard(
                    icon = Icons.Filled.LibraryBooks,
                    value = allDevoirs.size.toString(),
                    label = stringResource(R.string.stat_total),
                    accent = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.weight(1f)
                )
            }
        }

        // Progress by Subject Card
        if (subjectStats.isNotEmpty()) {
            item(key = "subject_progress_card") {
                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp),
                    shape = MaterialTheme.shapes.large,
                    color = MaterialTheme.colorScheme.surface,
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Text(
                            text = "Progression par matière",
                            style = MaterialTheme.typography.titleMedium
                        )
                        subjectStats.forEach { stat ->
                            val ratio = if (stat.total > 0) stat.completed.toFloat() / stat.total else 0f
                            val barColor = subjectAccentColor(stat.subject)
                            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text(
                                        text = stat.subject,
                                        style = MaterialTheme.typography.bodyMedium,
                                        fontWeight = FontWeight.Medium
                                    )
                                    Text(
                                        text = "${stat.completed}/${stat.total}",
                                        style = MaterialTheme.typography.labelMedium,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                                LinearProgressIndicator(
                                    progress = { ratio },
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(7.dp)
                                        .clip(RoundedCornerShape(4.dp)),
                                    color = barColor,
                                    trackColor = MaterialTheme.colorScheme.surfaceVariant
                                )
                            }
                        }
                    }
                }
            }
        }

        // Favorites Section
        item(key = "favorites_title") {
            Text(
                text = stringResource(R.string.section_favorites),
                style = MaterialTheme.typography.titleLarge,
                modifier = Modifier.padding(horizontal = 16.dp)
            )
        }

        if (favoriteDevoirs.isEmpty()) {
            item(key = "empty_favorites") {
                EmptyHintBox(
                    text = stringResource(R.string.empty_favorites),
                    modifier = Modifier.padding(horizontal = 16.dp)
                )
            }
        } else {
            items(favoriteDevoirs, key = { "fav_${it.id}" }) { devoir ->
                DevoirCard(
                    devoir = devoir,
                    onClick = { onOpenDevoir(devoir) },
                    onToggleFavorite = { onToggleFavorite(devoir.id) },
                    modifier = Modifier.padding(horizontal = 16.dp)
                )
            }
        }

        // Completed Section
        item(key = "completed_title") {
            Text(
                text = stringResource(R.string.section_completed),
                style = MaterialTheme.typography.titleLarge,
                modifier = Modifier.padding(horizontal = 16.dp)
            )
        }

        if (completedDevoirs.isEmpty()) {
            item(key = "empty_completed") {
                EmptyHintBox(
                    text = stringResource(R.string.empty_completed),
                    modifier = Modifier.padding(horizontal = 16.dp)
                )
            }
        } else {
            items(completedDevoirs, key = { "done_${it.id}" }) { devoir ->
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
private fun StatKpiCard(
    icon: ImageVector,
    value: String,
    label: String,
    accent: Color,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier,
        shape = MaterialTheme.shapes.medium,
        color = MaterialTheme.colorScheme.surface,
        tonalElevation = 1.dp,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
    ) {
        Column(
            modifier = Modifier.padding(14.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = accent,
                modifier = Modifier.size(22.dp)
            )
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = value,
                style = MaterialTheme.typography.headlineSmall,
                color = MaterialTheme.colorScheme.onSurface
            )
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun EmptyHintBox(text: String, modifier: Modifier = Modifier) {
    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.medium,
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.55f),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(16.dp)
        )
    }
}
