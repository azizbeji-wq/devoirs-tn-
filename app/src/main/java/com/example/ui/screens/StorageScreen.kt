package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.CloudDone
import androidx.compose.material.icons.filled.CloudDownload
import androidx.compose.material.icons.filled.CloudOff
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.DeleteSweep
import androidx.compose.material.icons.filled.FolderZip
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.SdStorage
import androidx.compose.material.icons.filled.SignalCellularAlt
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material.icons.filled.Wifi
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
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
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.R
import com.example.data.download.DevoirDownloadProgress
import com.example.data.download.DownloadPack
import com.example.data.download.DownloadTaskStatus
import com.example.data.download.PdfDownloadCoordinator
import com.example.data.download.PdfStorageManager
import com.example.data.model.CycleLevel
import com.example.data.model.DevoirItem
import com.example.data.model.Trimester
import com.example.data.repository.CatalogueSyncStatus
import com.example.ui.components.adaptiveCrimson
import com.example.ui.components.adaptiveEmerald
import com.example.ui.components.adaptiveOchre
import com.example.ui.components.localizedLabel
import com.example.ui.components.localizedShortLabel
import com.example.ui.components.localizedSubjectName
import com.example.ui.viewmodel.PendingDownloadConfirmation
import java.util.Locale

/**
 * Contenu de l'écran « Stockage » (intégré dans Mon Espace et accessible en raccourci) :
 * - Espace total utilisé dans le dossier privé de l'application,
 * - État du catalogue distant / cache local hors ligne + actualisation manuelle,
 * - Préférence réseau (Wi-Fi uniquement par défaut, option données mobiles + règle > 20 Mo),
 * - Packs téléchargeables en un clic (dont « Télécharger tout le trimestre 1 — 9ème année »),
 * - Suppression par devoir, par pack ou totale.
 */
@Composable
fun StorageManagementSection(
    allDevoirs: List<DevoirItem>,
    downloadStates: Map<String, DevoirDownloadProgress>,
    totalUsedBytes: Long,
    allowMobileData: Boolean,
    catalogueSyncStatus: CatalogueSyncStatus,
    onToggleAllowMobileData: (Boolean) -> Unit,
    onRefreshCatalogue: () -> Unit,
    onDownloadPack: (DownloadPack, String) -> Unit,
    onCancelPack: (String) -> Unit,
    onDeletePack: (DownloadPack) -> Unit,
    onDeleteDevoir: (String) -> Unit,
    onDeleteAll: () -> Unit,
    onOpenDevoir: (DevoirItem) -> Unit,
    modifier: Modifier = Modifier
) {
    val emerald = adaptiveEmerald()
    val crimson = adaptiveCrimson()
    val ochre = adaptiveOchre()

    val packs = remember(allDevoirs) { PdfDownloadCoordinator.buildPacks(allDevoirs) }
    val downloadedDevoirs = remember(allDevoirs) {
        allDevoirs.filter { it.hasPdfSource && it.isAvailableOffline }
            .sortedByDescending { it.downloadedBytes }
    }

    var showDeleteAllDialog by rememberSaveable { mutableStateOf(false) }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp)
            .testTag("storage_management_section"),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        // 1. Carte résumé espace utilisé & statut du catalogue en ligne / cache local
        Surface(
            shape = MaterialTheme.shapes.large,
            color = MaterialTheme.colorScheme.surface,
            tonalElevation = 2.dp,
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
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
                        Box(
                            modifier = Modifier
                                .size(44.dp)
                                .clip(RoundedCornerShape(12.dp))
                                .background(MaterialTheme.colorScheme.primaryContainer),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Filled.SdStorage,
                                contentDescription = stringResource(R.string.cd_tab_storage),
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(24.dp)
                            )
                        }
                        Column {
                            Text(
                                text = stringResource(R.string.storage_used_label),
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Text(
                                text = PdfStorageManager.formatBytes(totalUsedBytes),
                                style = MaterialTheme.typography.headlineSmall,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface,
                                modifier = Modifier.testTag("storage_used_value")
                            )
                            Text(
                                text = stringResource(R.string.storage_offline_count, downloadedDevoirs.size),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    if (downloadedDevoirs.isNotEmpty() || totalUsedBytes > 0L) {
                        OutlinedButton(
                            onClick = { showDeleteAllDialog = true },
                            modifier = Modifier
                                .minimumInteractiveComponentSize()
                                .testTag("storage_delete_all_btn"),
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = crimson),
                            border = BorderStroke(1.dp, crimson.copy(alpha = 0.45f)),
                            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Filled.DeleteSweep,
                                contentDescription = stringResource(R.string.cd_delete_storage_icon),
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = stringResource(R.string.action_delete_all_downloads),
                                style = MaterialTheme.typography.labelMedium
                            )
                        }
                    }
                }

                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)

                // Statut catalogue distant / cache hors ligne + bouton d'actualisation
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
                        Icon(
                            imageVector = if (catalogueSyncStatus.isOfflineFallback) {
                                Icons.Filled.CloudOff
                            } else {
                                Icons.Filled.CloudDone
                            },
                            contentDescription = null,
                            tint = if (catalogueSyncStatus.isOfflineFallback) ochre else emerald,
                            modifier = Modifier.size(18.dp)
                        )
                        Text(
                            text = if (catalogueSyncStatus.isOfflineFallback) {
                                stringResource(
                                    R.string.storage_catalogue_status_offline,
                                    catalogueSyncStatus.catalogueVersion.coerceAtLeast(1)
                                )
                            } else {
                                stringResource(
                                    R.string.storage_catalogue_status_online,
                                    catalogueSyncStatus.catalogueVersion.coerceAtLeast(1)
                                )
                            },
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }

                    IconButton(
                        onClick = onRefreshCatalogue,
                        enabled = !catalogueSyncStatus.isSyncing,
                        modifier = Modifier
                            .minimumInteractiveComponentSize()
                            .testTag("storage_refresh_catalogue_btn")
                    ) {
                        if (catalogueSyncStatus.isSyncing) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(18.dp),
                                strokeWidth = 2.dp
                            )
                        } else {
                            Icon(
                                imageVector = Icons.Filled.Refresh,
                                contentDescription = stringResource(R.string.cd_refresh_catalogue),
                                tint = MaterialTheme.colorScheme.primary
                            )
                        }
                    }
                }

                // Message explicite si l'URL du catalogue configurée dans BuildConfig est injoignable
                if (catalogueSyncStatus.isOfflineFallback && !catalogueSyncStatus.errorMessage.isNullOrBlank()) {
                    Surface(
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("catalogue_unreachable_banner"),
                        shape = RoundedCornerShape(10.dp),
                        color = ochre.copy(alpha = 0.12f),
                        border = BorderStroke(1.dp, ochre.copy(alpha = 0.35f))
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
                            verticalAlignment = Alignment.Top,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Filled.Warning,
                                contentDescription = null,
                                tint = ochre,
                                modifier = Modifier
                                    .padding(top = 1.dp)
                                    .size(18.dp)
                            )
                            Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                                Text(
                                    text = stringResource(R.string.catalogue_unreachable_banner_title),
                                    style = MaterialTheme.typography.labelMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = ochre
                                )
                                Text(
                                    text = catalogueSyncStatus.errorMessage,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                }
            }
        }

        // 2. Carte préférences réseau (Wi-Fi uniquement par défaut + option données mobiles)
        Surface(
            shape = MaterialTheme.shapes.large,
            color = MaterialTheme.colorScheme.surface,
            tonalElevation = 1.dp,
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Icon(
                    imageVector = if (allowMobileData) Icons.Filled.SignalCellularAlt else Icons.Filled.Wifi,
                    contentDescription = null,
                    tint = if (allowMobileData) ochre else MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(24.dp)
                )
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = stringResource(R.string.storage_allow_mobile_label),
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = stringResource(R.string.storage_allow_mobile_subtitle),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Switch(
                    checked = allowMobileData,
                    onCheckedChange = onToggleAllowMobileData,
                    modifier = Modifier.testTag("allow_mobile_data_switch")
                )
            }
        }

        // 3. Packs téléchargeables en un clic (Trimestre 1 — 9ème année en tête)
        Text(
            text = stringResource(R.string.storage_packs_section_title),
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onBackground
        )

        packs.forEach { pack ->
            val isFeatured9emeT1 = pack.level == CycleLevel.COLLEGE_9 && pack.trimester == Trimester.T1
            val packDisplayTitle = if (isFeatured9emeT1) {
                stringResource(R.string.download_pack_9eme_t1_cta)
            } else {
                stringResource(
                    R.string.download_pack_title,
                    pack.trimester.localizedLabel(),
                    pack.level.localizedShortLabel()
                )
            }
            val activePackTasks = pack.devoirs.mapNotNull { downloadStates[it.id] }
                .filter { it.status == DownloadTaskStatus.DOWNLOADING || it.status == DownloadTaskStatus.QUEUED }
            val isPackDownloading = activePackTasks.isNotEmpty()
            val packProgressFraction = if (pack.totalCount > 0) {
                val sumPct = pack.devoirs.sumOf { item ->
                    when {
                        item.isAvailableOffline -> 100
                        else -> downloadStates[item.id]?.progressPercent ?: 0
                    }
                }
                (sumPct / (pack.totalCount * 100f)).coerceIn(0f, 1f)
            } else 0f

            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("storage_pack_card_${pack.packId}"),
                shape = MaterialTheme.shapes.medium,
                color = if (isFeatured9emeT1) {
                    MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.35f)
                } else {
                    MaterialTheme.colorScheme.surface
                },
                tonalElevation = if (isFeatured9emeT1) 2.dp else 1.dp,
                border = BorderStroke(
                    width = if (isFeatured9emeT1) 1.5.dp else 1.dp,
                    color = if (isFeatured9emeT1) {
                        MaterialTheme.colorScheme.primary.copy(alpha = 0.45f)
                    } else {
                        MaterialTheme.colorScheme.outlineVariant
                    }
                )
            ) {
                Column(
                    modifier = Modifier.padding(14.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Filled.FolderZip,
                            contentDescription = null,
                            tint = if (pack.isFullyDownloaded) emerald else MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(22.dp)
                        )
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = packDisplayTitle,
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = stringResource(
                                    R.string.download_pack_progress_summary,
                                    pack.downloadedCount,
                                    pack.totalCount,
                                    String.format(Locale.getDefault(), "%.1f", pack.totalSizeMb)
                                ),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    if (isPackDownloading || pack.downloadedCount > 0) {
                        LinearProgressIndicator(
                            progress = { packProgressFraction },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(6.dp)
                                .clip(RoundedCornerShape(3.dp)),
                            color = if (pack.isFullyDownloaded) emerald else MaterialTheme.colorScheme.primary
                        )
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        when {
                            isPackDownloading -> {
                                OutlinedButton(
                                    onClick = { onCancelPack(pack.packId) },
                                    modifier = Modifier
                                        .weight(1f)
                                        .minimumInteractiveComponentSize()
                                        .testTag("cancel_pack_btn_${pack.packId}")
                                ) {
                                    Icon(
                                        imageVector = Icons.Filled.Close,
                                        contentDescription = stringResource(R.string.cd_cancel_download_icon),
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(stringResource(R.string.action_cancel_download))
                                }
                            }

                            pack.isFullyDownloaded -> {
                                Surface(
                                    shape = CircleShape,
                                    color = emerald.copy(alpha = 0.14f),
                                    modifier = Modifier.weight(1f)
                                ) {
                                    Row(
                                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.Center
                                    ) {
                                        Icon(
                                            imageVector = Icons.Filled.CheckCircle,
                                            contentDescription = null,
                                            tint = emerald,
                                            modifier = Modifier.size(16.dp)
                                        )
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text(
                                            text = stringResource(R.string.download_pack_complete),
                                            style = MaterialTheme.typography.labelMedium,
                                            fontWeight = FontWeight.SemiBold,
                                            color = emerald
                                        )
                                    }
                                }
                            }

                            else -> {
                                Button(
                                    onClick = { onDownloadPack(pack, packDisplayTitle) },
                                    modifier = Modifier
                                        .weight(1f)
                                        .minimumInteractiveComponentSize()
                                        .testTag("download_pack_btn_${pack.packId}")
                                ) {
                                    Icon(
                                        imageVector = Icons.Filled.CloudDownload,
                                        contentDescription = stringResource(R.string.cd_download_action_icon),
                                        modifier = Modifier.size(18.dp)
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        text = if (isFeatured9emeT1) {
                                            stringResource(R.string.action_download) +
                                                " (${String.format(Locale.getDefault(), "%.1f", pack.remainingSizeMb)} Mo)"
                                        } else {
                                            "${stringResource(R.string.action_download)} (${pack.missingDevoirs.size})"
                                        },
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }
                            }
                        }

                        if (pack.downloadedCount > 0) {
                            OutlinedButton(
                                onClick = { onDeletePack(pack) },
                                modifier = Modifier
                                    .minimumInteractiveComponentSize()
                                    .testTag("delete_pack_btn_${pack.packId}"),
                                colors = ButtonDefaults.outlinedButtonColors(contentColor = crimson),
                                border = BorderStroke(1.dp, crimson.copy(alpha = 0.45f)),
                                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Filled.Delete,
                                    contentDescription = stringResource(R.string.cd_delete_storage_icon),
                                    modifier = Modifier.size(16.dp)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = stringResource(R.string.action_delete_pack),
                                    style = MaterialTheme.typography.labelMedium
                                )
                            }
                        }
                    }
                }
            }
        }

        // 4. Liste des devoirs téléchargés individuellement (suppression par devoir)
        Text(
            text = stringResource(R.string.storage_downloaded_section_title),
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onBackground
        )

        if (downloadedDevoirs.isEmpty()) {
            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = MaterialTheme.shapes.medium,
                color = MaterialTheme.colorScheme.surface,
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        imageVector = Icons.Filled.CloudOff,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(36.dp)
                    )
                    Text(
                        text = stringResource(R.string.storage_empty_title),
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        textAlign = TextAlign.Center
                    )
                    Text(
                        text = stringResource(R.string.storage_empty_body),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Center
                    )
                }
            }
        } else {
            downloadedDevoirs.forEach { devoir ->
                Surface(
                    onClick = { onOpenDevoir(devoir) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("storage_devoir_row_${devoir.id}"),
                    shape = MaterialTheme.shapes.medium,
                    color = MaterialTheme.colorScheme.surface,
                    tonalElevation = 1.dp,
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 14.dp, vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Filled.CloudDone,
                            contentDescription = stringResource(R.string.cd_download_status_icon),
                            tint = emerald,
                            modifier = Modifier.size(20.dp)
                        )
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = devoir.title,
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.SemiBold,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            Text(
                                text = "${localizedSubjectName(devoir.subject)} · ${devoir.level.localizedShortLabel()} · ${
                                    PdfStorageManager.formatBytes(devoir.downloadedBytes)
                                }",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        IconButton(
                            onClick = { onDeleteDevoir(devoir.id) },
                            modifier = Modifier
                                .minimumInteractiveComponentSize()
                                .testTag("delete_devoir_btn_${devoir.id}")
                        ) {
                            Icon(
                                imageVector = Icons.Filled.Delete,
                                contentDescription = stringResource(R.string.cd_delete_storage_icon),
                                tint = crimson
                            )
                        }
                    }
                }
            }
        }
    }

    if (showDeleteAllDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteAllDialog = false },
            icon = {
                Icon(
                    imageVector = Icons.Filled.DeleteSweep,
                    contentDescription = null,
                    tint = crimson
                )
            },
            title = { Text(stringResource(R.string.storage_delete_all_confirm_title)) },
            text = { Text(stringResource(R.string.storage_delete_all_confirm_body)) },
            confirmButton = {
                Button(
                    onClick = {
                        showDeleteAllDialog = false
                        onDeleteAll()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = crimson),
                    modifier = Modifier.testTag("confirm_delete_all_storage_btn")
                ) {
                    Text(stringResource(R.string.action_delete_all_downloads))
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteAllDialog = false }) {
                    Text(stringResource(R.string.action_cancel))
                }
            }
        )
    }
}

/**
 * Dialogues globaux de confirmation réseau :
 * 1. Wi-Fi requis par défaut (quand l'élève lance un téléchargement sur données mobiles),
 * 2. Avertissement obligatoire avant tout téléchargement > 20 Mo en données mobiles.
 */
@Composable
fun DownloadConfirmationDialogs(
    pending: PendingDownloadConfirmation?,
    onConfirmMobileData: (enableMobilePreference: Boolean) -> Unit,
    onQueueForWifiOnly: () -> Unit,
    onDismiss: () -> Unit
) {
    if (pending == null) return
    val ochre = adaptiveOchre()

    when (pending) {
        is PendingDownloadConfirmation.WifiOnlyBlocked -> {
            AlertDialog(
                onDismissRequest = onDismiss,
                icon = {
                    Icon(
                        imageVector = Icons.Filled.Wifi,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary
                    )
                },
                title = { Text(stringResource(R.string.wifi_only_dialog_title)) },
                text = {
                    Text(
                        stringResource(
                            R.string.wifi_only_dialog_body,
                            pending.target.label,
                            String.format(Locale.getDefault(), "%.1f", pending.target.estimatedSizeMb)
                        )
                    )
                },
                confirmButton = {
                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Button(
                            onClick = onQueueForWifiOnly,
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("wifi_wait_confirm_btn")
                        ) {
                            Text(stringResource(R.string.wifi_only_wait_btn))
                        }
                        FilledTonalButton(
                            onClick = { onConfirmMobileData(true) },
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("wifi_allow_mobile_btn")
                        ) {
                            Text(stringResource(R.string.wifi_only_allow_mobile_btn))
                        }
                    }
                },
                dismissButton = {
                    TextButton(onClick = onDismiss) {
                        Text(stringResource(R.string.action_cancel))
                    }
                }
            )
        }

        is PendingDownloadConfirmation.LargeMobileWarning -> {
            AlertDialog(
                onDismissRequest = onDismiss,
                icon = {
                    Icon(
                        imageVector = Icons.Filled.Warning,
                        contentDescription = null,
                        tint = ochre
                    )
                },
                title = { Text(stringResource(R.string.mobile_warning_title)) },
                text = {
                    Text(
                        stringResource(
                            R.string.mobile_warning_body,
                            pending.target.label,
                            String.format(Locale.getDefault(), "%.1f", pending.sizeMb)
                        )
                    )
                },
                confirmButton = {
                    Button(
                        onClick = { onConfirmMobileData(false) },
                        modifier = Modifier.testTag("mobile_warning_confirm_btn")
                    ) {
                        Text(stringResource(R.string.mobile_warning_confirm))
                    }
                },
                dismissButton = {
                    TextButton(onClick = onDismiss) {
                        Text(stringResource(R.string.action_cancel))
                    }
                }
            )
        }
    }
}
