package com.example.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface DevoirDao {
    @Query("SELECT * FROM devoirs ORDER BY id ASC")
    fun observeAllDevoirs(): Flow<List<DevoirEntity>>

    @Query("SELECT COUNT(*) FROM devoirs")
    suspend fun getDevoirCount(): Int

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertAll(devoirs: List<DevoirEntity>)

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
