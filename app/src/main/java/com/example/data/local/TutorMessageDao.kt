package com.example.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface TutorMessageDao {
    @Query(
        "SELECT * FROM tutor_messages " +
            "WHERE devoirId = :devoirId AND exerciseNumber = :exerciseNumber " +
            "ORDER BY timestamp ASC, id ASC"
    )
    fun observeConversation(devoirId: String, exerciseNumber: Int): Flow<List<TutorMessageEntity>>

    @Query(
        "SELECT * FROM tutor_messages " +
            "WHERE devoirId = :devoirId AND exerciseNumber = :exerciseNumber " +
            "ORDER BY timestamp ASC, id ASC"
    )
    suspend fun getConversationSnapshot(devoirId: String, exerciseNumber: Int): List<TutorMessageEntity>

    @Query(
        "SELECT * FROM tutor_messages " +
            "WHERE devoirId = :devoirId " +
            "ORDER BY timestamp ASC, id ASC"
    )
    fun observeDevoirHistory(devoirId: String): Flow<List<TutorMessageEntity>>

    @Query(
        "SELECT COUNT(*) FROM tutor_messages " +
            "WHERE devoirId = :devoirId AND role = 'assistant'"
    )
    fun observeDevoirExplanationCount(devoirId: String): Flow<Int>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMessage(message: TutorMessageEntity): Long

    @Query("DELETE FROM tutor_messages WHERE devoirId = :devoirId AND exerciseNumber = :exerciseNumber")
    suspend fun clearConversation(devoirId: String, exerciseNumber: Int)

    @Query("DELETE FROM tutor_messages WHERE devoirId = :devoirId")
    suspend fun clearDevoirHistory(devoirId: String)
}
