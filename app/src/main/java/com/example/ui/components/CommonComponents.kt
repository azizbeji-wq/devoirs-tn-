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
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.CloudDone
import androidx.compose.material.icons.filled.CloudDownload
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.ErrorOutline
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.School
import androidx.compose.material.icons.filled.SignalCellularAlt
import androidx.compose.material.icons.filled.Wifi
import androidx.compose.material.icons.outlined.BookmarkBorder
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.minimumInteractiveComponentSize
import com.example.data.download.DevoirDownloadProgress
import com.example.data.download.DownloadTaskStatus
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.R
import com.example.data.model.DevoirItem
import com.example.data.model.DevoirType
import com.example.domain.search.DevoirSearchEngine
import com.example.ui.theme.CrimsonAccent
import com.example.ui.theme.CrimsonDark
import com.example.ui.theme.EmeraldDark
import com.example.ui.theme.EmeraldVerified
import com.example.ui.theme.LocalIsDarkTheme
import com.example.ui.theme.OchreGold
import com.example.ui.theme.OchreGoldDark
import com.example.ui.theme.SearchHighlightBgDark
import com.example.ui.theme.SearchHighlightBgLight
import com.example.ui.theme.SearchHighlightFgDark
import com.example.ui.theme.SearchHighlightFgLight
import com.example.ui.theme.SubjectAnglais
import com.example.ui.theme.SubjectAnglaisDark
import com.example.ui.theme.SubjectArabe
import com.example.ui.theme.SubjectArabeDark
import com.example.ui.theme.SubjectEco
import com.example.ui.theme.SubjectEcoDark
import com.example.ui.theme.SubjectFrancais
import com.example.ui.theme.SubjectFrancaisDark
import com.example.ui.theme.SubjectInfo
import com.example.ui.theme.SubjectInfoDark
import com.example.ui.theme.SubjectMaths
import com.example.ui.theme.SubjectMathsDark
import com.example.ui.theme.SubjectPhilo
import com.example.ui.theme.SubjectPhiloDark
import com.example.ui.theme.SubjectPhysique
import com.example.ui.theme.SubjectPhysiqueDark
import com.example.ui.theme.SubjectSvt
import com.example.ui.theme.SubjectSvtDark
import com.example.ui.theme.SubjectTechnique
import com.example.ui.theme.SubjectTechniqueDark

/** Couleur émeraude adaptée au thème clair/sombre pour garantir un contraste WCAG AA. */
@Composable
fun adaptiveEmerald(): Color = if (LocalIsDarkTheme.current) EmeraldDark else EmeraldVerified

/** Couleur rouge tunisien adaptée au thème clair/sombre. */
@Composable
fun adaptiveCrimson(): Color = if (LocalIsDarkTheme.current) CrimsonDark else CrimsonAccent

/** Couleur ocre doré adaptée au thème clair/sombre. */
@Composable
fun adaptiveOchre(): Color = if (LocalIsDarkTheme.current) OchreGoldDark else OchreGold

/**
 * Retourne la couleur d'accent associée à une matière tunisienne,
 * automatiquement éclaircie en mode sombre pour préserver la lisibilité.
 */
@Composable
fun subjectAccentColor(subject: String): Color {
    val isDark = LocalIsDarkTheme.current
    return when {
        subject.contains("Math", ignoreCase = true) -> if (isDark) SubjectMathsDark else SubjectMaths
        subject.contains("Physique", ignoreCase = true) -> if (isDark) SubjectPhysiqueDark else SubjectPhysique
        subject.contains("SVT", ignoreCase = true) -> if (isDark) SubjectSvtDark else SubjectSvt
        subject.contains("Info", ignoreCase = true) -> if (isDark) SubjectInfoDark else SubjectInfo
        subject.contains("Éco", ignoreCase = true) || subject.contains("Gestion", ignoreCase = true) ->
            if (isDark) SubjectEcoDark else SubjectEco
        subject.contains("Tech", ignoreCase = true) -> if (isDark) SubjectTechniqueDark else SubjectTechnique
        subject.contains("Philo", ignoreCase = true) -> if (isDark) SubjectPhiloDark else SubjectPhilo
        subject.contains("Français", ignoreCase = true) -> if (isDark) SubjectFrancaisDark else SubjectFrancais
        subject.contains("Arabe", ignoreCase = true) -> if (isDark) SubjectArabeDark else SubjectArabe
        subject.contains("Anglais", ignoreCase = true) -> if (isDark) SubjectAnglaisDark else SubjectAnglais
        else -> if (isDark) SubjectMathsDark else SubjectMaths
    }
}

/** Retourne la couleur d'accent du badge DC / DS / Examen adaptée au thème actif. */
@Composable
fun devoirTypeAccent(type: DevoirType): Color {
    val isDark = LocalIsDarkTheme.current
    return when (type) {
        DevoirType.CONTROLE -> if (isDark) SubjectMathsDark else SubjectMaths
        DevoirType.SYNTHESE -> adaptiveOchre()
        DevoirType.EXAMEN -> adaptiveCrimson()
        DevoirType.ALL -> if (isDark) SubjectMathsDark else SubjectMaths
    }
}

/** Retourne la couleur d'accent associée au niveau de difficulté d'un devoir. */
@Composable
fun difficultyAccentColor(difficulty: String): Color {
    val rank = remember(difficulty) { DevoirSearchEngine.difficultyRank(difficulty) }
    return when (rank) {
        1 -> adaptiveEmerald()
        2 -> MaterialTheme.colorScheme.primary
        3 -> adaptiveOchre()
        else -> adaptiveCrimson()
    }
}

/** Formate une durée en minutes en format court lisible (ex. 120 -> 2h, 90 -> 1h30). */
fun formatDurationMinutes(minutes: Int): String {
    val hours = minutes / 60
    val rem = minutes % 60
    return when {
        hours > 0 && rem > 0 -> "${hours}h${rem.toString().padStart(2, '0')}"
        hours > 0 -> "${hours}h"
        else -> "${rem} min"
    }
}

/**
 * Construit un [AnnotatedString] mettant en évidence toutes les occurrences des mots
 * de [query] trouvés dans [text], sans sensibilité à la casse ni aux accents.
 */
@Composable
fun buildHighlightedText(
    text: String,
    query: String
): AnnotatedString {
    val isDark = LocalIsDarkTheme.current
    val highlightBg = if (isDark) SearchHighlightBgDark else SearchHighlightBgLight
    val highlightFg = if (isDark) SearchHighlightFgDark else SearchHighlightFgLight

    val ranges = remember(text, query) {
        DevoirSearchEngine.findHighlightRanges(text, query)
    }

    if (ranges.isEmpty()) return AnnotatedString(text)

    return buildAnnotatedString {
        append(text)
        ranges.forEach { range ->
            addStyle(
                style = SpanStyle(
                    background = highlightBg,
                    color = highlightFg,
                    fontWeight = FontWeight.Bold
                ),
                start = range.first,
                end = (range.last + 1).coerceAtMost(text.length)
            )
        }
    }
}

/**
 * Carte d'un devoir affichée dans la bibliothèque, la sélection par matière et l'espace personnel.
 * Prend en charge la mise en évidence des termes recherchés dans le titre, les chapitres et les énoncés.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun DevoirCard(
    devoir: DevoirItem,
    onClick: () -> Unit,
    onToggleFavorite: () -> Unit,
    modifier: Modifier = Modifier,
    searchQuery: String = "",
    matchingPdfSnippet: String? = null,
    downloadProgress: DevoirDownloadProgress? = null,
    hasResumablePart: Boolean = false,
    onDownloadClick: (() -> Unit)? = null,
    onCancelDownloadClick: (() -> Unit)? = null
) {
    val accent = subjectAccentColor(devoir.subject)
    val typeAccent = devoirTypeAccent(devoir.type)
    val difficultyAccent = difficultyAccentColor(devoir.difficulty)
    val emerald = adaptiveEmerald()
    val crimson = adaptiveCrimson()

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
                emerald.copy(alpha = 0.5f)
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
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    // Badge matière
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = accent.copy(alpha = 0.14f)
                    ) {
                        Text(
                            text = localizedSubjectName(devoir.subject),
                            style = MaterialTheme.typography.labelMedium,
                            color = accent,
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }

                    // Badge DC / DS / Examen + Trimestre
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = typeAccent.copy(alpha = 0.14f)
                    ) {
                        Text(
                            text = "${devoir.type.localizedBadge()} · ${devoir.trimester.localizedShortLabel()}",
                            style = MaterialTheme.typography.labelMedium,
                            color = typeAccent,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                            maxLines = 1
                        )
                    }

                    // Indicateur explicite « Déjà fait » lorsque le devoir est terminé
                    if (devoir.isCompleted) {
                        Surface(
                            shape = CircleShape,
                            color = emerald.copy(alpha = 0.14f),
                            modifier = Modifier.testTag("completed_badge_${devoir.id}")
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(4.dp),
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Filled.CheckCircle,
                                    contentDescription = stringResource(R.string.completed_badge),
                                    tint = emerald,
                                    modifier = Modifier.size(14.dp)
                                )
                                Text(
                                    text = stringResource(R.string.card_already_done),
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.SemiBold,
                                    color = emerald,
                                    maxLines = 1
                                )
                            }
                        }
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
                        tint = if (devoir.isFavorite) crimson else MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            Spacer(modifier = Modifier.height(6.dp))

            // Titre avec mise en évidence des termes trouvés
            Text(
                text = buildHighlightedText(devoir.title, searchQuery),
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )

            Spacer(modifier = Modifier.height(6.dp))

            // Métadonnées Niveau + Section
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Icon(
                    imageVector = Icons.Filled.School,
                    contentDescription = stringResource(R.string.cd_card_school_icon),
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(15.dp)
                )
                Text(
                    text = "${devoir.level.localizedShortLabel()} · ${devoir.section.localizedLabel()}",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.primary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }

            Spacer(modifier = Modifier.height(6.dp))

            // Mention « Sujet d'entraînement » (sans nom de lycée réel) + badge de difficulté
            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Surface(
                    shape = RoundedCornerShape(6.dp),
                    color = MaterialTheme.colorScheme.tertiaryContainer
                ) {
                    Text(
                        text = stringResource(R.string.training_subject_badge),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onTertiaryContainer,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
                        maxLines = 1
                    )
                }

                if (devoir.difficulty.isNotBlank()) {
                    Surface(
                        shape = RoundedCornerShape(6.dp),
                        color = difficultyAccent.copy(alpha = 0.12f),
                        modifier = Modifier.testTag("difficulty_badge_${devoir.id}")
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp),
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Filled.SignalCellularAlt,
                                contentDescription = stringResource(R.string.cd_card_difficulty_icon),
                                tint = difficultyAccent,
                                modifier = Modifier.size(13.dp)
                            )
                            Text(
                                text = devoir.difficulty,
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Medium,
                                color = difficultyAccent,
                                maxLines = 1
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Tags de chapitres avec surlignage
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
                            text = buildHighlightedText(tag, searchQuery),
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            // Extrait plein texte issu de l'index OCR PDF (table Room FTS4) avec mots surlignés
            if (!matchingPdfSnippet.isNullOrBlank() && searchQuery.isNotBlank()) {
                Spacer(modifier = Modifier.height(8.dp))
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.35f),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.25f)),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("pdf_search_snippet_${devoir.id}")
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 7.dp),
                        verticalAlignment = Alignment.Top,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Description,
                            contentDescription = stringResource(R.string.cd_card_search_match_icon),
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier
                                .padding(top = 2.dp)
                                .size(14.dp)
                        )
                        Text(
                            text = buildHighlightedText(matchingPdfSnippet, searchQuery),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurface,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Pied de carte : durée, taille PDF et disponibilité du corrigé PDF
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Filled.Schedule,
                            contentDescription = stringResource(R.string.cd_card_duration_icon),
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
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Filled.Description,
                            contentDescription = stringResource(R.string.pdf_meta_size),
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(14.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = if (devoir.isTextOnly) {
                                stringResource(R.string.text_mode_badge)
                            } else {
                                stringResource(
                                    R.string.pdf_size_label,
                                    "%.2f".format(devoir.sizeMb)
                                )
                            },
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }

                Spacer(modifier = Modifier.width(8.dp))

                // N'affiche « Corrigé inclus » que si un corrigé PDF (corrigeUrl) ou un corrigé texte existe réellement
                Surface(
                    shape = CircleShape,
                    color = if (devoir.hasCorrige) {
                        emerald.copy(alpha = 0.14f)
                    } else {
                        MaterialTheme.colorScheme.surfaceVariant
                    },
                    modifier = Modifier.testTag("corrige_badge_${devoir.id}")
                ) {
                    Text(
                        text = stringResource(
                            if (devoir.hasCorrige) {
                                R.string.card_solution_included
                            } else {
                                R.string.pdf_subject_only_badge
                            }
                        ),
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.SemiBold,
                        color = if (devoir.hasCorrige) {
                            emerald
                        } else {
                            MaterialTheme.colorScheme.onSurfaceVariant
                        },
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                        maxLines = 1
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Indicateur clair « Consultable en texte » / « Disponible hors ligne » / « À télécharger » + actions PDF
            val activeStatus = downloadProgress?.status ?: DownloadTaskStatus.IDLE
            val isDownloading = devoir.hasPdfSource && activeStatus == DownloadTaskStatus.DOWNLOADING
            val isQueued = devoir.hasPdfSource && activeStatus == DownloadTaskStatus.QUEUED
            val isPausedResumable = devoir.hasPdfSource && (
                activeStatus == DownloadTaskStatus.PAUSED_RESUMABLE ||
                    (!devoir.isAvailableOffline && hasResumablePart && !isDownloading && !isQueued)
                )
            val isFailed = devoir.hasPdfSource && activeStatus == DownloadTaskStatus.FAILED

            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                val statusColor = when {
                    devoir.isTextOnly || devoir.isAvailableOffline -> emerald
                    isDownloading || isQueued -> MaterialTheme.colorScheme.primary
                    isFailed -> crimson
                    else -> MaterialTheme.colorScheme.onSurfaceVariant
                }
                val statusBg = when {
                    devoir.isTextOnly || devoir.isAvailableOffline -> emerald.copy(alpha = 0.12f)
                    isDownloading || isQueued -> MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.7f)
                    isFailed -> crimson.copy(alpha = 0.12f)
                    else -> MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.8f)
                }
                val statusIcon = when {
                    devoir.isTextOnly -> Icons.Filled.Description
                    devoir.isAvailableOffline -> Icons.Filled.CloudDone
                    isQueued -> Icons.Filled.Wifi
                    isFailed -> Icons.Filled.ErrorOutline
                    isPausedResumable -> Icons.Filled.PlayArrow
                    else -> Icons.Filled.CloudDownload
                }
                val statusText = when {
                    devoir.isTextOnly -> stringResource(R.string.status_text_offline)
                    devoir.isAvailableOffline -> stringResource(R.string.status_available_offline)
                    isDownloading -> stringResource(
                        R.string.status_downloading_pct,
                        downloadProgress?.progressPercent ?: 0
                    )
                    isQueued -> stringResource(R.string.status_queued_wifi)
                    isFailed -> stringResource(R.string.status_download_failed)
                    isPausedResumable -> stringResource(R.string.status_paused_resumable)
                    else -> stringResource(R.string.status_to_download)
                }

                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = statusBg,
                    modifier = Modifier.testTag("offline_status_${devoir.id}")
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 9.dp, vertical = 5.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(5.dp)
                    ) {
                        Icon(
                            imageVector = statusIcon,
                            contentDescription = stringResource(R.string.cd_download_status_icon),
                            tint = statusColor,
                            modifier = Modifier.size(14.dp)
                        )
                        Text(
                            text = statusText,
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.SemiBold,
                            color = statusColor,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }

                if ((isDownloading || isQueued) && onCancelDownloadClick != null) {
                    Surface(
                        onClick = onCancelDownloadClick,
                        shape = RoundedCornerShape(8.dp),
                        color = crimson.copy(alpha = 0.10f),
                        modifier = Modifier.testTag("card_cancel_dl_${devoir.id}")
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Filled.Close,
                                contentDescription = stringResource(R.string.cd_cancel_download_icon),
                                tint = crimson,
                                modifier = Modifier.size(14.dp)
                            )
                            Text(
                                text = stringResource(R.string.action_cancel_download),
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.SemiBold,
                                color = crimson
                            )
                        }
                    }
                } else if (devoir.hasPdfSource && !devoir.isAvailableOffline && onDownloadClick != null) {
                    Surface(
                        onClick = onDownloadClick,
                        shape = RoundedCornerShape(8.dp),
                        color = MaterialTheme.colorScheme.primaryContainer,
                        modifier = Modifier.testTag("card_download_btn_${devoir.id}")
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Icon(
                                imageVector = if (isPausedResumable) Icons.Filled.PlayArrow else Icons.Filled.CloudDownload,
                                contentDescription = stringResource(R.string.cd_download_action_icon),
                                tint = MaterialTheme.colorScheme.onPrimaryContainer,
                                modifier = Modifier.size(14.dp)
                            )
                            Text(
                                text = stringResource(
                                    when {
                                        isFailed -> R.string.action_retry_download
                                        isPausedResumable -> R.string.action_resume_download
                                        else -> R.string.action_download
                                    }
                                ),
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.SemiBold,
                                color = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                        }
                    }
                }
            }

            if (isFailed && !downloadProgress?.errorMessage.isNullOrBlank()) {
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = downloadProgress?.errorMessage.orEmpty(),
                    style = MaterialTheme.typography.labelSmall,
                    color = crimson,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.testTag("card_download_error_${devoir.id}")
                )
            }

            if (isDownloading) {
                Spacer(modifier = Modifier.height(8.dp))
                LinearProgressIndicator(
                    progress = { ((downloadProgress?.progressPercent ?: 0) / 100f).coerceIn(0f, 1f) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(5.dp)
                        .clip(RoundedCornerShape(3.dp))
                )
            }
        }
    }
}
