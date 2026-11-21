package com.example.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.example.data.model.CycleLevel
import com.example.data.model.DevoirItem
import com.example.data.model.DevoirType
import com.example.data.model.Trimester
import com.example.data.model.TunisianSection

/**
 * Entité Room stockant les métadonnées d'un devoir PDF hébergé en ligne,
 * ses empreintes d'intégrité (taille / SHA-256), ses chemins privés téléchargés hors ligne
 * ainsi que la progression locale de l'élève (`isFavorite`, `isCompleted`).
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
    val isCompleted: Boolean = false
)

@Entity(tableName = "starred_ideas")
data class StarredIdeaEntity(
    @PrimaryKey val ideaId: String,
    val starredAt: Long = System.currentTimeMillis()
)

/** Utilitaire d'encodage/décodage des tags de chapitres stockés dans Room. */
object ExerciseCodec {
    private const val TAG_SEP = "|||"

    fun encodeTags(tags: List<String>): String = tags.joinToString(TAG_SEP)

    fun decodeTags(raw: String): List<String> =
        if (raw.isBlank()) emptyList() else raw.split(TAG_SEP)
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
    isCompleted = isCompleted
)
