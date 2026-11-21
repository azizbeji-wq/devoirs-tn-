package com.example.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface ExamAttemptDao {
    @Query("SELECT * FROM exam_attempts ORDER BY attemptedAtMillis DESC")
    fun observeAllAttempts(): Flow<List<ExamAttemptEntity>>

    @Query("SELECT * FROM exam_attempts WHERE devoirId = :devoirId ORDER BY attemptedAtMillis DESC")
    fun observeAttemptsForDevoir(devoirId: String): Flow<List<ExamAttemptEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAttempt(attempt: ExamAttemptEntity): Long

    @Query("DELETE FROM exam_attempts WHERE id = :attemptId")
    suspend fun deleteAttempt(attemptId: Long)
}

@Dao
interface ExamReminderDao {
    @Query("SELECT * FROM exam_reminders ORDER BY examDateMillis ASC")
    fun observeAllReminders(): Flow<List<ExamReminderEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertReminder(reminder: ExamReminderEntity): Long

    @Query("DELETE FROM exam_reminders WHERE id = :reminderId")
    suspend fun deleteReminder(reminderId: Long)
}

@Dao
interface QcmResultDao {
    @Query("SELECT * FROM qcm_results ORDER BY completedAtMillis DESC")
    fun observeAllResults(): Flow<List<QcmResultEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertResult(result: QcmResultEntity): Long
}
