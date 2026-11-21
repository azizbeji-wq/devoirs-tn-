package com.example.data.local

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "exam_attempts",
    indices = [
        Index(value = ["devoirId"]),
        Index(value = ["subject"]),
        Index(value = ["attemptedAtMillis"])
    ]
)
data class ExamAttemptEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val devoirId: String,
    val devoirTitle: String,
    val subject: String,
    val chapterTagsSerialized: String,
    val attemptedAtMillis: Long,
    val officialDurationSeconds: Int,
    val actualDurationSeconds: Int,
    val exerciseScoresSerialized: String,
    val totalScore: Double,
    val maxScore: Double
)

@Entity(
    tableName = "exam_reminders",
    indices = [Index(value = ["examDateMillis"])]
)
data class ExamReminderEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val title: String,
    val subject: String,
    val examType: String,
    val examDateMillis: Long,
    val createdAtMillis: Long = System.currentTimeMillis()
)

@Entity(
    tableName = "qcm_results",
    indices = [
        Index(value = ["chapterId"]),
        Index(value = ["completedAtMillis"])
    ]
)
data class QcmResultEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val chapterId: String,
    val chapterName: String,
    val subject: String,
    val score: Int,
    val totalQuestions: Int,
    val durationSeconds: Int,
    val completedAtMillis: Long
)

data class ExerciseScoreEntry(
    val exerciseNumber: Int,
    val obtainedPoints: Double,
    val maxPoints: Double
)

object ExerciseScoreCodec {
    private const val ENTRY_SEP = "|"
    private const val PART_SEP = ":"
    private const val RATIO_SEP = "/"
    private val NUMBER_REGEX = Regex("""\d+(?:[.,]\d+)?""")

    fun encode(entries: List<ExerciseScoreEntry>): String =
        entries.joinToString(ENTRY_SEP) { entry ->
            "${entry.exerciseNumber}$PART_SEP${entry.obtainedPoints}$RATIO_SEP${entry.maxPoints}"
        }

    fun decode(raw: String): List<ExerciseScoreEntry> {
        if (raw.isBlank()) return emptyList()
        return raw.split(ENTRY_SEP).mapNotNull { token ->
            val parts = token.split(PART_SEP)
            if (parts.size != 2) return@mapNotNull null
            val number = parts[0].toIntOrNull() ?: return@mapNotNull null
            val ratio = parts[1].split(RATIO_SEP)
            if (ratio.size != 2) return@mapNotNull null
            val obtained = ratio[0].toDoubleOrNull() ?: return@mapNotNull null
            val max = ratio[1].toDoubleOrNull() ?: return@mapNotNull null
            ExerciseScoreEntry(number, obtained, max)
        }
    }

    /**
     * Extrait la valeur numérique du barème d'un exercice (ex: "5 pts" -> 5.0, "7,5 pts" -> 7.5).
     */
    fun parseMaxPoints(pointsLabel: String, fallback: Double = 5.0): Double {
        val match = NUMBER_REGEX.find(pointsLabel)?.value?.replace(',', '.')
        return match?.toDoubleOrNull()?.takeIf { it > 0.0 } ?: fallback
    }
}
