package com.example.ui.components

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AddAlert
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material.icons.filled.NotificationsOff
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.minimumInteractiveComponentSize
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import com.example.R
import com.example.data.local.ExamReminderEntity
import com.example.data.model.DevoirType
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

/**
 * Section de gestion des rappels locaux DC / DS (notifications J-7, J-3, J-1 via WorkManager).
 */
@Composable
fun RemindersSection(
    reminders: List<ExamReminderEntity>,
    feedbackMessage: String?,
    onAddReminder: (title: String, subject: String, examType: String, examDateMillis: Long) -> Unit,
    onDeleteReminder: (Long) -> Unit,
    onDismissFeedback: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val currentLocale = LocalConfiguration.current.locales.get(0) ?: Locale.getDefault()
    val emerald = adaptiveEmerald()
    var showAddDialog by remember { mutableStateOf(false) }

    val initialGranted = remember(context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.POST_NOTIFICATIONS
            ) == PackageManager.PERMISSION_GRANTED
        } else {
            true
        }
    }
    var hasNotificationPermission by remember { mutableStateOf(initialGranted) }

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { granted ->
        hasNotificationPermission = granted
    }

    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = stringResource(R.string.reminders_title),
                style = MaterialTheme.typography.titleLarge,
                color = MaterialTheme.colorScheme.onBackground,
                modifier = Modifier.weight(1f)
            )
            Button(
                onClick = {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU && !hasNotificationPermission) {
                        permissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                    }
                    showAddDialog = true
                },
                modifier = Modifier
                    .minimumInteractiveComponentSize()
                    .testTag("add_reminder_btn")
            ) {
                Icon(
                    imageVector = Icons.Filled.AddAlert,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(stringResource(R.string.reminders_add_btn))
            }
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU && !hasNotificationPermission) {
            Surface(
                shape = MaterialTheme.shapes.medium,
                color = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.55f),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.error.copy(alpha = 0.35f))
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Icon(
                        imageVector = Icons.Filled.NotificationsOff,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.error
                    )
                    Text(
                        text = stringResource(R.string.reminders_notif_permission_hint),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onErrorContainer,
                        modifier = Modifier.weight(1f)
                    )
                    TextButton(
                        onClick = {
                            permissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                        },
                        modifier = Modifier
                            .minimumInteractiveComponentSize()
                            .testTag("allow_notifications_btn")
                    ) {
                        Text(stringResource(R.string.reminders_allow_notif_btn))
                    }
                }
            }
        }

        if (feedbackMessage != null) {
            Surface(
                shape = MaterialTheme.shapes.medium,
                color = emerald.copy(alpha = 0.12f),
                border = BorderStroke(1.dp, emerald.copy(alpha = 0.35f))
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = feedbackMessage,
                        style = MaterialTheme.typography.bodySmall,
                        color = emerald,
                        modifier = Modifier.weight(1f)
                    )
                    TextButton(
                        onClick = onDismissFeedback,
                        modifier = Modifier.minimumInteractiveComponentSize()
                    ) {
                        Text(stringResource(R.string.action_ok))
                    }
                }
            }
        }

        if (reminders.isEmpty()) {
            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = MaterialTheme.shapes.medium,
                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.55f),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
            ) {
                Text(
                    text = stringResource(R.string.reminders_empty),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(16.dp)
                )
            }
        } else {
            val dateFmt = remember(currentLocale) {
                SimpleDateFormat("EEEE dd MMM yyyy", currentLocale)
            }
            reminders.forEach { reminder ->
                ReminderItemCard(
                    reminder = reminder,
                    formattedDate = dateFmt.format(Date(reminder.examDateMillis)),
                    onDelete = { onDeleteReminder(reminder.id) }
                )
            }
        }
    }

    if (showAddDialog) {
        AddExamReminderDialog(
            locale = currentLocale,
            onDismiss = { showAddDialog = false },
            onConfirm = { title, subject, examType, dateMillis ->
                onAddReminder(title, subject, examType, dateMillis)
                showAddDialog = false
            }
        )
    }
}

@Composable
private fun ReminderItemCard(
    reminder: ExamReminderEntity,
    formattedDate: String,
    onDelete: () -> Unit
) {
    val accent = subjectAccentColor(reminder.subject)
    val emerald = adaptiveEmerald()
    val crimson = adaptiveCrimson()
    val daysRemaining = computeDaysUntil(reminder.examDateMillis)
    val badgeText = when {
        daysRemaining < 0 -> stringResource(R.string.reminder_past)
        daysRemaining == 0 -> stringResource(R.string.reminder_today)
        daysRemaining == 1 -> stringResource(R.string.reminder_tomorrow)
        else -> stringResource(R.string.reminder_days_left, daysRemaining)
    }

    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("reminder_card_${reminder.id}"),
        shape = MaterialTheme.shapes.medium,
        color = MaterialTheme.colorScheme.surface,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Surface(
                shape = RoundedCornerShape(10.dp),
                color = if (daysRemaining in 0..3) crimson.copy(alpha = 0.14f) else accent.copy(alpha = 0.14f)
            ) {
                Text(
                    text = badgeText,
                    style = MaterialTheme.typography.labelLarge,
                    color = if (daysRemaining in 0..3) crimson else accent,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp)
                )
            }

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "${reminder.examType} · ${reminder.title}",
                    style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = "${localizedSubjectName(reminder.subject)} · $formattedDate",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(4.dp))
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Icon(
                        imageVector = Icons.Filled.NotificationsActive,
                        contentDescription = null,
                        tint = emerald,
                        modifier = Modifier.size(13.dp)
                    )
                    Text(
                        text = stringResource(R.string.reminder_alerts_schedule),
                        style = MaterialTheme.typography.labelSmall,
                        color = emerald
                    )
                }
            }

            IconButton(
                onClick = onDelete,
                modifier = Modifier
                    .minimumInteractiveComponentSize()
                    .testTag("delete_reminder_${reminder.id}")
            ) {
                Icon(
                    imageVector = Icons.Filled.DeleteOutline,
                    contentDescription = stringResource(R.string.cd_delete_reminder),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
private fun AddExamReminderDialog(
    locale: Locale,
    onDismiss: () -> Unit,
    onConfirm: (title: String, subject: String, examType: String, examDateMillis: Long) -> Unit
) {
    val defaultDate = remember {
        Calendar.getInstance().apply {
            add(Calendar.DAY_OF_YEAR, 7)
            set(Calendar.HOUR_OF_DAY, 12)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }.timeInMillis
    }

    val subjects = listOf(
        "Mathématiques",
        "Physique-Chimie",
        "SVT",
        "Informatique",
        "Français",
        "Anglais",
        "Arabe",
        "Philosophie",
        "Économie & Gestion"
    )
    // Valeurs canoniques enregistrées + libellés localisés à l'affichage
    val examTypes = listOf(
        "DC" to DevoirType.CONTROLE.localizedBadge(),
        "DS" to DevoirType.SYNTHESE.localizedBadge(),
        "Examen" to DevoirType.EXAMEN.localizedBadge()
    )

    var title by remember { mutableStateOf("") }
    var selectedSubject by remember { mutableStateOf(subjects.first()) }
    var selectedType by remember { mutableStateOf("DS") }
    var selectedDateMillis by remember { mutableLongStateOf(defaultDate) }
    var showDatePicker by remember { mutableStateOf(false) }

    val dateFmt = remember(locale) { SimpleDateFormat("dd/MM/yyyy", locale) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.reminder_dialog_title)) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                OutlinedTextField(
                    value = title,
                    onValueChange = { title = it },
                    label = { Text(stringResource(R.string.reminder_title_hint)) },
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("reminder_title_input")
                )

                Text(
                    text = stringResource(R.string.filter_type_label),
                    style = MaterialTheme.typography.labelMedium
                )
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    examTypes.forEach { (canonical, displayLabel) ->
                        FilterChip(
                            selected = selectedType == canonical,
                            onClick = { selectedType = canonical },
                            label = { Text(displayLabel) }
                        )
                    }
                }

                Text(
                    text = stringResource(R.string.filter_subject_label),
                    style = MaterialTheme.typography.labelMedium
                )
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    subjects.take(6).forEach { subj ->
                        FilterChip(
                            selected = selectedSubject == subj,
                            onClick = { selectedSubject = subj },
                            label = {
                                Text(
                                    text = localizedSubjectName(subj),
                                    style = MaterialTheme.typography.labelSmall
                                )
                            }
                        )
                    }
                }

                OutlinedButton(
                    onClick = { showDatePicker = true },
                    modifier = Modifier
                        .fillMaxWidth()
                        .minimumInteractiveComponentSize()
                        .testTag("reminder_pick_date_btn")
                ) {
                    Icon(
                        imageVector = Icons.Filled.CalendarMonth,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        stringResource(
                            R.string.reminder_pick_date,
                            dateFmt.format(Date(selectedDateMillis))
                        )
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val effectiveTitle = title.trim().ifBlank { "$selectedType — $selectedSubject" }
                    onConfirm(effectiveTitle, selectedSubject, selectedType, selectedDateMillis)
                },
                modifier = Modifier
                    .minimumInteractiveComponentSize()
                    .testTag("confirm_add_reminder_btn")
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

    if (showDatePicker) {
        val pickerState = rememberDatePickerState(initialSelectedDateMillis = selectedDateMillis)
        DatePickerDialog(
            onDismissRequest = { showDatePicker = false },
            confirmButton = {
                TextButton(
                    onClick = {
                        pickerState.selectedDateMillis?.let { selectedDateMillis = it }
                        showDatePicker = false
                    }
                ) { Text(stringResource(R.string.action_confirm)) }
            },
            dismissButton = {
                TextButton(onClick = { showDatePicker = false }) {
                    Text(stringResource(R.string.action_cancel))
                }
            }
        ) {
            DatePicker(state = pickerState)
        }
    }
}

private fun computeDaysUntil(targetMillis: Long): Int {
    val startOfToday = Calendar.getInstance().apply {
        set(Calendar.HOUR_OF_DAY, 0)
        set(Calendar.MINUTE, 0)
        set(Calendar.SECOND, 0)
        set(Calendar.MILLISECOND, 0)
    }.timeInMillis

    val startOfTarget = Calendar.getInstance().apply {
        timeInMillis = targetMillis
        set(Calendar.HOUR_OF_DAY, 0)
        set(Calendar.MINUTE, 0)
        set(Calendar.SECOND, 0)
        set(Calendar.MILLISECOND, 0)
    }.timeInMillis

    return ((startOfTarget - startOfToday) / 86_400_000L).toInt()
}
