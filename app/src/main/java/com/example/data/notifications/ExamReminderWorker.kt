package com.example.data.notifications

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.example.MainActivity
import com.example.R

class ExamReminderWorker(
    appContext: Context,
    params: WorkerParameters
) : CoroutineWorker(appContext, params) {

    override suspend fun doWork(): Result {
        val reminderId = inputData.getLong(KEY_REMINDER_ID, 0L)
        val title = inputData.getString(KEY_TITLE).orEmpty().ifBlank { "Épreuve à venir" }
        val subject = inputData.getString(KEY_SUBJECT).orEmpty().ifBlank { "Révision" }
        val examType = inputData.getString(KEY_EXAM_TYPE).orEmpty().ifBlank { "DS" }
        val daysBefore = inputData.getInt(KEY_DAYS_BEFORE, 1)

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            val granted = ContextCompat.checkSelfPermission(
                applicationContext,
                Manifest.permission.POST_NOTIFICATIONS
            ) == PackageManager.PERMISSION_GRANTED
            if (!granted) {
                return Result.success()
            }
        }

        ensureNotificationChannel(applicationContext)

        val openIntent = Intent(applicationContext, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        val pendingIntent = PendingIntent.getActivity(
            applicationContext,
            (reminderId * 10 + daysBefore).toInt(),
            openIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val dayLabel = if (daysBefore == 1) "demain (J-1)" else "dans $daysBefore jours (J-$daysBefore)"
        val notifTitle = "Rappel $examType · $subject ($dayLabel)"
        val notifBody = when (daysBefore) {
            7 -> "$title approche ! Planifie tes fiches et un sujet d'entraînement cette semaine."
            3 -> "$title dans 3 jours : passe un devoir en conditions réelles pour tester ton niveau."
            else -> "$title demain ! Fais un QCM flash rapide et repose-toi bien ce soir."
        }

        val notification = NotificationCompat.Builder(applicationContext, CHANNEL_ID)
            .setSmallIcon(R.mipmap.ic_launcher)
            .setContentTitle(notifTitle)
            .setContentText(notifBody)
            .setStyle(NotificationCompat.BigTextStyle().bigText(notifBody))
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)
            .build()

        val notificationId = ((reminderId % 100_000L) * 10L + daysBefore).toInt()
        NotificationManagerCompat.from(applicationContext).notify(notificationId, notification)

        return Result.success()
    }

    companion object {
        const val CHANNEL_ID = "devoirs_tn_exam_reminders"
        const val KEY_REMINDER_ID = "reminder_id"
        const val KEY_TITLE = "title"
        const val KEY_SUBJECT = "subject"
        const val KEY_EXAM_TYPE = "exam_type"
        const val KEY_DAYS_BEFORE = "days_before"

        fun ensureNotificationChannel(context: Context) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
                val channel = NotificationChannel(
                    CHANNEL_ID,
                    "Rappels DC / DS — Devoirs TN",
                    NotificationManager.IMPORTANCE_DEFAULT
                ).apply {
                    description = "Notifications locales J-7, J-3 et J-1 avant vos devoirs de contrôle et de synthèse."
                }
                manager.createNotificationChannel(channel)
            }
        }
    }
}
