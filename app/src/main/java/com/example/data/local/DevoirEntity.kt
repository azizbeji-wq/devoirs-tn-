package com.example.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.example.data.model.CycleLevel
import com.example.data.model.DevoirExercise
import com.example.data.model.DevoirItem
import com.example.data.model.DevoirType
import com.example.data.model.Trimester
import com.example.data.model.TunisianSection
import com.squareup.moshi.Moshi
import com.squareup.moshi.Types
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory

/**
 * Entité Room unifiée stockant un devoir :
 * - soit un devoir PDF hébergé en ligne (`sujetUrl`, `corrigeUrl`, SHA-256, chemins locaux),
 * - soit un ancien sujet JSON consultable directement en texte (`exercisesSerialized`).
 * Les colonnes `isFavorite`, `isCompleted` et les téléchargements locaux sont toujours préservés.
 */
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
    val difficulty: String,
    val chapterTagsSerialized: String,
    val source: String,
    val version: Int = 1,
    val sizeMb: Double = 0.0,
    val sujetUrl: String,
    val corrigeUrl: String? = null,
    val sujetSizeBytes: Long = 0L,
    val corrigeSizeBytes: Long = 0L,
    val sujetSha256: String? = null,
    val corrigeSha256: String? = null,
    val localSujetPath: String? = null,
    val localCorrigePath: String? = null,
    val downloadedBytes: Long = 0L,
    val downloadedVersion: Int = 0,
    val isFavorite: Boolean = false,
    val isCompleted: Boolean = false,
    val exercisesSerialized: String = ""
)

@Entity(tableName = "starred_ideas")
data class StarredIdeaEntity(
    @PrimaryKey val ideaId: String,
    val starredAt: Long = System.currentTimeMillis()
)

/** Utilitaire d'encodage/décodage des tags de chapitres et des exercices texte stockés dans Room. */
object ExerciseCodec {
    private const val TAG_SEP = "|||"

    private val moshi: Moshi by lazy {
        Moshi.Builder()
            .addLast(KotlinJsonAdapterFactory())
            .build()
    }

    private val exercisesAdapter by lazy {
        val listType = Types.newParameterizedType(List::class.java, DevoirExercise::class.java)
        moshi.adapter<List<DevoirExercise>>(listType)
    }

    fun encodeTags(tags: List<String>): String = tags.joinToString(TAG_SEP)

    fun decodeTags(raw: String): List<String> =
        if (raw.isBlank()) emptyList() else raw.split(TAG_SEP)

    fun encodeExercises(exercises: List<DevoirExercise>): String {
        if (exercises.isEmpty()) return ""
        return runCatching { exercisesAdapter.toJson(exercises) }.getOrDefault("")
    }

    fun decodeExercises(raw: String): List<DevoirExercise> {
        if (raw.isBlank()) return emptyList()
        return runCatching { exercisesAdapter.fromJson(raw).orEmpty() }.getOrDefault(emptyList())
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
    difficulty = difficulty,
    chapterTags = ExerciseCodec.decodeTags(chapterTagsSerialized),
    source = source,
    version = version,
    sizeMb = sizeMb,
    sujetUrl = sujetUrl,
    corrigeUrl = corrigeUrl,
    sujetSizeBytes = sujetSizeBytes,
    corrigeSizeBytes = corrigeSizeBytes,
    sujetSha256 = sujetSha256,
    corrigeSha256 = corrigeSha256,
    localSujetPath = localSujetPath,
    localCorrigePath = localCorrigePath,
    downloadedBytes = downloadedBytes,
    downloadedVersion = downloadedVersion,
    isFavorite = isFavorite,
    isCompleted = isCompleted,
    exercises = ExerciseCodec.decodeExercises(exercisesSerialized)
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
    difficulty = difficulty,
    chapterTagsSerialized = ExerciseCodec.encodeTags(chapterTags),
    source = source,
    version = version,
    sizeMb = sizeMb,
    sujetUrl = sujetUrl,
    corrigeUrl = corrigeUrl,
    sujetSizeBytes = sujetSizeBytes,
    corrigeSizeBytes = corrigeSizeBytes,
    sujetSha256 = sujetSha256,
    corrigeSha256 = corrigeSha256,
    localSujetPath = localSujetPath,
    localCorrigePath = localCorrigePath,
    downloadedBytes = downloadedBytes,
    downloadedVersion = downloadedVersion,
    isFavorite = isFavorite,
    isCompleted = isCompleted,
    exercisesSerialized = ExerciseCodec.encodeExercises(exercises)
)
