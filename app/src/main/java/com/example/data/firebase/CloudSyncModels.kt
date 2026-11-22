package com.example.data.firebase

import androidx.annotation.Keep
import com.google.firebase.Timestamp
import com.google.firebase.firestore.DocumentId
import com.google.firebase.firestore.IgnoreExtraProperties
import com.google.firebase.firestore.ServerTimestamp

@Keep
@IgnoreExtraProperties
data class UserProfile(
    @DocumentId val id: String = "",
    val userId: String = "",
    val displayName: String = "",
    val weeklyGoalTarget: Int = 3,
    val favoriteDevoirIds: List<String> = emptyList(),
    val completedDevoirIds: List<String> = emptyList(),
    val starredMethodIds: List<String> = emptyList(),
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
)

@Keep
@IgnoreExtraProperties
data class CloudExamAttempt(
    @DocumentId val id: String = "",
    val userId: String = "",
    val devoirId: String = "",
    val devoirTitle: String = "",
    val subject: String = "",
    val totalScore: Double = 0.0,
    val maxScore: Double = 20.0,
    val actualDurationSeconds: Int = 0,
    val attemptedAtMillis: Long = 0L,
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
)

@Keep
@IgnoreExtraProperties
data class CloudExamReminder(
    @DocumentId val id: String = "",
    val userId: String = "",
    val title: String = "",
    val subject: String = "",
    val examType: String = "",
    val examDateMillis: Long = 0L,
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
)

@Keep
@IgnoreExtraProperties
data class CloudSubject(
    @DocumentId val id: String = "",
    val userId: String = "",
    val name: String = "",
    val subtitle: String = "",
    @ServerTimestamp val createdAt: Timestamp? = null,
    @ServerTimestamp val updatedAt: Timestamp? = null
)
