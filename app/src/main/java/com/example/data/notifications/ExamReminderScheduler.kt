package com.example.data.notifications

import android.content.Context
import androidx.work.Data
import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import com.example.data.local.ExamReminderEntity
import java.util.Calendar
import java.util.concurrent.TimeUnit

interface ReminderScheduler {
    fun scheduleReminder(reminder: ExamReminderEntity): List<Int>
    fun cancelReminder(reminderId: Long)
}

/**
 * Planifie jusqu'à 3 notifications locales via WorkManager : J-7, J-3 et J-1 à 09h00 heure locale.
 */
class ExamReminderScheduler(
    private val context: Context
) : ReminderScheduler {

    override fun scheduleReminder(reminder: ExamReminderEntity): List<Int> {
        ExamReminderWorker.ensureNotificationChannel(context)
        val workManager = WorkManager.getInstance(context)
        val now = System.currentTimeMillis()
        val scheduledOffsets = mutableListOf<Int>()

        for (daysBefore in REMINDER_OFFSETS_DAYS) {
            val triggerMillis = computeTriggerTimeMillis(reminder.examDateMillis, daysBefore)
            val delayMillis = triggerMillis - now
            if (delayMillis <= 0L) continue

            val inputData = Data.Builder()
                .putLong(ExamReminderWorker.KEY_REMINDER_ID, reminder.id)
                .putString(ExamReminderWorker.KEY_TITLE, reminder.title)
                .putString(ExamReminderWorker.KEY_SUBJECT, reminder.subject)
                .putString(ExamReminderWorker.KEY_EXAM_TYPE, reminder.examType)
                .putInt(ExamReminderWorker.KEY_DAYS_BEFORE, daysBefore)
                .build()

            val request = OneTimeWorkRequestBuilder<ExamReminderWorker>()
                .setInitialDelay(delayMillis, TimeUnit.MILLISECONDS)
                .setInputData(inputData)
                .addTag(reminderTag(reminder.id))
                .build()

            workManager.enqueueUniqueWork(
                uniqueWorkName(reminder.id, daysBefore),
                ExistingWorkPolicy.REPLACE,
                request
            )
            scheduledOffsets.add(daysBefore)
        }
        return scheduledOffsets
    }

    override fun cancelReminder(reminderId: Long) {
        WorkManager.getInstance(context).cancelAllWorkByTag(reminderTag(reminderId))
    }

    companion object {
        val REMINDER_OFFSETS_DAYS = listOf(7, 3, 1)

        fun computeTriggerTimeMillis(examDateMillis: Long, daysBefore: Int): Long {
            val cal = Calendar.getInstance().apply {
                timeInMillis = examDateMillis
                add(Calendar.DAY_OF_YEAR, -daysBefore)
                set(Calendar.HOUR_OF_DAY, 9)
                set(Calendar.MINUTE, 0)
                set(Calendar.SECOND, 0)
                set(Calendar.MILLISECOND, 0)
            }
            return cal.timeInMillis
        }

        fun reminderTag(reminderId: Long): String = "exam_reminder_$reminderId"

        fun uniqueWorkName(reminderId: Long, daysBefore: Int): String =
            "exam_reminder_${reminderId}_d$daysBefore"
    }
}
