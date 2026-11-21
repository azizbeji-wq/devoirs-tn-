package com.example.data.local

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "tutor_messages",
    indices = [Index(value = ["devoirId", "exerciseNumber"], name = "index_tutor_messages_devoir_ex")]
)
data class TutorMessageEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0L,
    val devoirId: String,
    val exerciseNumber: Int,
    val role: String, // "user" or "assistant"
    val mode: String, // "EXPLAIN", "HINT_1", "HINT_2", "SOLUTION", "CHAT"
    val language: String, // "FR" or "AR"
    val content: String,
    val timestamp: Long = System.currentTimeMillis()
)
