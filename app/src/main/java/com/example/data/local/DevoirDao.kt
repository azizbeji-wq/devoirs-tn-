package com.example.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

data class DevoirVersionTuple(
    val id: String,
    val version: Int
)

@Dao
interface DevoirDao {
    @Query("SELECT * FROM devoirs ORDER BY id ASC")
    fun observeAllDevoirs(): Flow<List<DevoirEntity>>

    @Query("SELECT * FROM devoirs WHERE id = :id LIMIT 1")
    suspend fun getById(id: String): DevoirEntity?

    @Query("SELECT COUNT(*) FROM devoirs")
    suspend fun getDevoirCount(): Int

    @Query("SELECT id, version FROM devoirs")
    suspend fun getExistingVersions(): List<DevoirVersionTuple>

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertAll(devoirs: List<DevoirEntity>)

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertIgnore(devoir: DevoirEntity)

    /**
     * Met à jour les métadonnées et URL distantes lorsqu'une nouvelle `version`
     * arrive depuis `catalogue.json`, tout en préservant `isFavorite`, `isCompleted`
     * et les fichiers locaux déjà téléchargés.
     */
    @Query(
        """
        UPDATE devoirs SET
            title = :title,
            subject = :subject,
            levelId = :levelId,
            sectionId = :sectionId,
            typeId = :typeId,
            trimesterId = :trimesterId,
            durationMinutes = :durationMinutes,
            difficulty = :difficulty,
            chapterTagsSerialized = :chapterTagsSerialized,
            source = :source,
            version = :version,
            sizeMb = :sizeMb,
            sujetUrl = :sujetUrl,
            corrigeUrl = :corrigeUrl,
            sujetSizeBytes = :sujetSizeBytes,
            corrigeSizeBytes = :corrigeSizeBytes,
            sujetSha256 = :sujetSha256,
            corrigeSha256 = :corrigeSha256
        WHERE id = :id
        """
    )
    suspend fun updateContentPreservingProgress(
        id: String,
        title: String,
        subject: String,
        levelId: String,
        sectionId: String,
        typeId: String,
        trimesterId: String,
        durationMinutes: Int,
        difficulty: String,
        chapterTagsSerialized: String,
        source: String,
        version: Int,
        sizeMb: Double,
        sujetUrl: String,
        corrigeUrl: String?,
        sujetSizeBytes: Long,
        corrigeSizeBytes: Long,
        sujetSha256: String?,
        corrigeSha256: String?
    )

    /** Enregistre les chemins privés et la taille réelle d'un devoir téléchargé et vérifié. */
    @Query(
        """
        UPDATE devoirs SET
            localSujetPath = :localSujetPath,
            localCorrigePath = :localCorrigePath,
            downloadedBytes = :downloadedBytes,
            downloadedVersion = :downloadedVersion
        WHERE id = :devoirId
        """
    )
    suspend fun markDownloaded(
        devoirId: String,
        localSujetPath: String?,
        localCorrigePath: String?,
        downloadedBytes: Long,
        downloadedVersion: Int
    )

    /** Supprime les références locales d'un devoir après effacement de ses fichiers PDF. */
    @Query(
        """
        UPDATE devoirs SET
            localSujetPath = NULL,
            localCorrigePath = NULL,
            downloadedBytes = 0,
            downloadedVersion = 0
        WHERE id = :devoirId
        """
    )
    suspend fun clearDownload(devoirId: String)

    /** Supprime les références locales d'une liste de devoirs (suppression d'un pack). */
    @Query(
        """
        UPDATE devoirs SET
            localSujetPath = NULL,
            localCorrigePath = NULL,
            downloadedBytes = 0,
            downloadedVersion = 0
        WHERE id IN (:devoirIds)
        """
    )
    suspend fun clearDownloads(devoirIds: List<String>)

    /** Réinitialise tous les téléchargements locaux (sans toucher favoris ni progression). */
    @Query(
        """
        UPDATE devoirs SET
            localSujetPath = NULL,
            localCorrigePath = NULL,
            downloadedBytes = 0,
            downloadedVersion = 0
        """
    )
    suspend fun clearAllDownloads()

    @Query("UPDATE devoirs SET isFavorite = NOT isFavorite WHERE id = :devoirId")
    suspend fun toggleFavorite(devoirId: String)

    @Query("UPDATE devoirs SET isCompleted = NOT isCompleted WHERE id = :devoirId")
    suspend fun toggleCompleted(devoirId: String)
}

@Dao
interface StarredIdeaDao {
    @Query("SELECT ideaId FROM starred_ideas")
    fun observeStarredIdeaIds(): Flow<List<String>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun starIdea(entity: StarredIdeaEntity)

    @Query("DELETE FROM starred_ideas WHERE ideaId = :ideaId")
    suspend fun unstarIdea(ideaId: String)

    @Query("SELECT EXISTS(SELECT 1 FROM starred_ideas WHERE ideaId = :ideaId)")
    suspend fun isIdeaStarred(ideaId: String): Boolean
}
