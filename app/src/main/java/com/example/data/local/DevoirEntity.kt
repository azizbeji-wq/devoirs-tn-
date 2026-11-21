package com.example.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.example.data.model.CycleLevel
import com.example.data.model.DevoirExercise
import com.example.data.model.DevoirItem
import com.example.data.model.DevoirType
import com.example.data.model.Trimester
import com.example.data.model.TunisianSection

@Entity(tableName = "devoirs")
data class DevoirEntity(
    @PrimaryKey val id: String,
    val title: String,
    val subject: String,
    val levelId: String,
    val sectionId: String,
    val typeId: String,
    val trimesterId: String,
    val durationMinutes: Int,
    val totalPoints: Int,
    val difficulty: String,
    val sourceSchool: String,
    val chapterTagsSerialized: String,
    val exercisesSerialized: String,
    val isFavorite: Boolean = false,
    val isCompleted: Boolean = false
)

@Entity(tableName = "starred_ideas")
data class StarredIdeaEntity(
    @PrimaryKey val ideaId: String,
    val starredAt: Long = System.currentTimeMillis()
)

object ExerciseCodec {
    private const val EXERCISE_SEP = "<<<EX>>>"
    private const val FIELD_SEP = "<<<FLD>>>"
    private const val TAG_SEP = "|||"

    fun encodeTags(tags: List<String>): String = tags.joinToString(TAG_SEP)

    fun decodeTags(raw: String): List<String> =
        if (raw.isBlank()) emptyList() else raw.split(TAG_SEP)

    fun encodeExercises(exercises: List<DevoirExercise>): String =
        exercises.joinToString(EXERCISE_SEP) { ex ->
            listOf(
                ex.number.toString(),
                ex.title,
                ex.points,
                ex.statement,
                ex.solution
            ).joinToString(FIELD_SEP)
        }

    fun decodeExercises(raw: String): List<DevoirExercise> {
        if (raw.isBlank()) return emptyList()
        return raw.split(EXERCISE_SEP).mapNotNull { chunk ->
            val parts = chunk.split(FIELD_SEP)
            if (parts.size < 5) return@mapNotNull null
            DevoirExercise(
                number = parts[0].toIntOrNull() ?: 1,
                title = parts[1],
                points = parts[2],
                statement = parts[3],
                solution = parts[4]
            )
        }
    }
}

fun DevoirEntity.toDomain(): DevoirItem = DevoirItem(
    id = id,
    title = title,
    subject = subject,
    level = CycleLevel.fromId(levelId),
    section = TunisianSection.fromId(sectionId),
    type = DevoirType.fromId(typeId),
    trimester = Trimester.fromId(trimesterId),
    durationMinutes = durationMinutes,
    totalPoints = totalPoints,
    difficulty = difficulty,
    sourceSchool = sourceSchool,
    chapterTags = ExerciseCodec.decodeTags(chapterTagsSerialized),
    exercises = ExerciseCodec.decodeExercises(exercisesSerialized),
    isFavorite = isFavorite,
    isCompleted = isCompleted
)

fun DevoirItem.toEntity(): DevoirEntity = DevoirEntity(
    id = id,
    title = title,
    subject = subject,
    levelId = level.id,
    sectionId = section.id,
    typeId = type.id,
    trimesterId = trimester.id,
    durationMinutes = durationMinutes,
    totalPoints = totalPoints,
    difficulty = difficulty,
    sourceSchool = sourceSchool,
    chapterTagsSerialized = ExerciseCodec.encodeTags(chapterTags),
    exercisesSerialized = ExerciseCodec.encodeExercises(exercises),
    isFavorite = isFavorite,
    isCompleted = isCompleted
)
