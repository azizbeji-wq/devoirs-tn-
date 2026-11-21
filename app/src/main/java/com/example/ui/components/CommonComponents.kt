package com.example.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.School
import androidx.compose.material.icons.outlined.BookmarkBorder
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.R
import com.example.data.model.DevoirItem
import com.example.data.model.DevoirType
import com.example.ui.theme.CrimsonAccent
import com.example.ui.theme.EmeraldVerified
import com.example.ui.theme.OchreGold
import com.example.ui.theme.SubjectAnglais
import com.example.ui.theme.SubjectArabe
import com.example.ui.theme.SubjectEco
import com.example.ui.theme.SubjectFrancais
import com.example.ui.theme.SubjectInfo
import com.example.ui.theme.SubjectMaths
import com.example.ui.theme.SubjectPhilo
import com.example.ui.theme.SubjectPhysique
import com.example.ui.theme.SubjectSvt
import com.example.ui.theme.SubjectTechnique

fun subjectAccentColor(subject: String): Color = when {
    subject.contains("Math", ignoreCase = true) -> SubjectMaths
    subject.contains("Physique", ignoreCase = true) -> SubjectPhysique
    subject.contains("SVT", ignoreCase = true) -> SubjectSvt
    subject.contains("Info", ignoreCase = true) -> SubjectInfo
    subject.contains("Éco", ignoreCase = true) || subject.contains("Gestion", ignoreCase = true) -> SubjectEco
    subject.contains("Tech", ignoreCase = true) -> SubjectTechnique
    subject.contains("Philo", ignoreCase = true) -> SubjectPhilo
    subject.contains("Français", ignoreCase = true) -> SubjectFrancais
    subject.contains("Arabe", ignoreCase = true) -> SubjectArabe
    subject.contains("Anglais", ignoreCase = true) -> SubjectAnglais
    else -> SubjectMaths
}

fun devoirTypeAccent(type: DevoirType): Color = when (type) {
    DevoirType.CONTROLE -> SubjectMaths
    DevoirType.SYNTHESE -> OchreGold
    DevoirType.EXAMEN -> CrimsonAccent
    DevoirType.ALL -> SubjectMaths
}

fun formatDurationMinutes(minutes: Int): String {
    val hours = minutes / 60
    val rem = minutes % 60
    return when {
        hours > 0 && rem > 0 -> "${hours}h${rem.toString().padStart(2, '0')}"
        hours > 0 -> "${hours}h"
        else -> "${rem} min"
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun DevoirCard(
    devoir: DevoirItem,
    onClick: () -> Unit,
    onToggleFavorite: () -> Unit,
    modifier: Modifier = Modifier
) {
    val accent = subjectAccentColor(devoir.subject)
    val typeAccent = devoirTypeAccent(devoir.type)

    Surface(
        modifier = modifier
            .fillMaxWidth()
            .testTag("devoir_card_${devoir.id}")
            .clip(MaterialTheme.shapes.medium)
            .clickable(onClick = onClick),
        shape = MaterialTheme.shapes.medium,
        color = MaterialTheme.colorScheme.surface,
        tonalElevation = 2.dp,
        border = BorderStroke(
            width = 1.dp,
            color = if (devoir.isCompleted) {
                EmeraldVerified.copy(alpha = 0.45f)
            } else {
                MaterialTheme.colorScheme.outlineVariant
            }
        )
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
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
                    // Subject pill
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = accent.copy(alpha = 0.13f)
                    ) {
                        Text(
                            text = devoir.subject,
                            style = MaterialTheme.typography.labelMedium,
                            color = accent,
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }

                    // DC / DS / Examen badge
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = typeAccent.copy(alpha = 0.13f)
                    ) {
                        Text(
                            text = "${devoir.type.badge} · ${devoir.trimester.shortLabel}",
                            style = MaterialTheme.typography.labelMedium,
                            color = typeAccent,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                            maxLines = 1
                        )
                    }

                    if (devoir.isCompleted) {
                        Icon(
                            imageVector = Icons.Filled.CheckCircle,
                            contentDescription = stringResource(R.string.completed_badge),
                            tint = EmeraldVerified,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }

                IconButton(
                    onClick = onToggleFavorite,
                    modifier = Modifier
                        .minimumInteractiveComponentSize()
                        .testTag("favorite_btn_${devoir.id}")
                ) {
                    Icon(
                        imageVector = if (devoir.isFavorite) Icons.Filled.Bookmark else Icons.Outlined.BookmarkBorder,
                        contentDescription = stringResource(
                            if (devoir.isFavorite) R.string.bookmarked_cd else R.string.bookmark_cd
                        ),
                        tint = if (devoir.isFavorite) CrimsonAccent else MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            Spacer(modifier = Modifier.height(6.dp))

            Text(
                text = devoir.title,
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )

            Spacer(modifier = Modifier.height(6.dp))

            // Level + Section + School metadata
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Icon(
                    imageVector = Icons.Filled.School,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(15.dp)
                )
                Text(
                    text = "${devoir.level.shortLabel} · ${devoir.section.label}",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.primary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }

            Spacer(modifier = Modifier.height(4.dp))

            Text(
                text = devoir.sourceSchool,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )

            Spacer(modifier = Modifier.height(10.dp))

            // Chapter tags
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                devoir.chapterTags.take(3).forEach { tag ->
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(6.dp))
                            .background(MaterialTheme.colorScheme.surfaceVariant)
                            .padding(horizontal = 8.dp, vertical = 3.dp)
                    ) {
                        Text(
                            text = tag,
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Footer meta row
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Filled.Schedule,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(14.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = formatDurationMinutes(devoir.durationMinutes),
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Text(
                        text = "${devoir.exercises.size} exercices · ${devoir.totalPoints} pts",
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                Surface(
                    shape = CircleShape,
                    color = EmeraldVerified.copy(alpha = 0.12f)
                ) {
                    Text(
                        text = "Corrigé inclus",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.SemiBold,
                        color = EmeraldVerified,
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                    )
                }
            }
        }
    }
}
