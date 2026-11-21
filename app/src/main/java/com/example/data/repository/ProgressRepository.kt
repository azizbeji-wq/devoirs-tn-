package com.example.data.repository

import com.example.data.local.ExamAttemptDao
import com.example.data.local.ExamAttemptEntity
import com.example.data.local.ExamReminderDao
import com.example.data.local.ExamReminderEntity
import com.example.data.local.QcmResultDao
import com.example.data.local.QcmResultEntity
import com.example.data.notifications.ReminderScheduler
import kotlinx.coroutines.flow.Flow

class ProgressRepository(
    private val examAttemptDao: ExamAttemptDao,
    private val examReminderDao: ExamReminderDao,
    private val qcmResultDao: QcmResultDao,
    private val reminderScheduler: ReminderScheduler
) {
    val allAttempts: Flow<List<ExamAttemptEntity>> = examAttemptDao.observeAllAttempts()

    val allReminders: Flow<List<ExamReminderEntity>> = examReminderDao.observeAllReminders()

    val allQcmResults: Flow<List<QcmResultEntity>> = qcmResultDao.observeAllResults()

    fun observeAttemptsForDevoir(devoirId: String): Flow<List<ExamAttemptEntity>> =
        examAttemptDao.observeAttemptsForDevoir(devoirId)

    suspend fun saveExamAttempt(attempt: ExamAttemptEntity): Long =
        examAttemptDao.insertAttempt(attempt)

    suspend fun addReminderAndSchedule(
        title: String,
        subject: String,
        examType: String,
        examDateMillis: Long
    ): List<Int> {
        val insertedId = examReminderDao.insertReminder(
            ExamReminderEntity(
                title = title.trim(),
                subject = subject.trim(),
                examType = examType,
                examDateMillis = examDateMillis
            )
        )
        val savedEntity = ExamReminderEntity(
            id = insertedId,
            title = title.trim(),
            subject = subject.trim(),
            examType = examType,
            examDateMillis = examDateMillis
        )
        return reminderScheduler.scheduleReminder(savedEntity)
    }

    suspend fun deleteReminder(reminderId: Long) {
        reminderScheduler.cancelReminder(reminderId)
        examReminderDao.deleteReminder(reminderId)
    }

    suspend fun saveQcmResult(result: QcmResultEntity): Long =
        qcmResultDao.insertResult(result)
}
