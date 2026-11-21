package com.example.data.repository

import com.example.data.local.DevoirDao
import com.example.data.local.StarredIdeaDao
import com.example.data.local.StarredIdeaEntity
import com.example.data.local.TunisianCurriculumSeed
import com.example.data.local.toDomain
import com.example.data.local.toEntity
import com.example.data.model.AppFeatureIdea
import com.example.data.model.DevoirItem
import com.example.data.model.SectionCoefficientPreset
import com.example.data.model.StudyMethodGuide
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class DevoirRepository(
    private val devoirDao: DevoirDao,
    private val starredIdeaDao: StarredIdeaDao
) {
    val allDevoirs: Flow<List<DevoirItem>> = devoirDao.observeAllDevoirs()
        .map { list -> list.map { it.toDomain() } }

    val starredIdeaIds: Flow<Set<String>> = starredIdeaDao.observeStarredIdeaIds()
        .map { it.toSet() }

    suspend fun ensureSeedDataLoaded() {
        devoirDao.insertAll(TunisianCurriculumSeed.devoirs.map { it.toEntity() })
    }

    suspend fun toggleFavorite(devoirId: String) {
        devoirDao.toggleFavorite(devoirId)
    }

    suspend fun toggleCompleted(devoirId: String) {
        devoirDao.toggleCompleted(devoirId)
    }

    suspend fun toggleStarIdea(ideaId: String) {
        if (starredIdeaDao.isIdeaStarred(ideaId)) {
            starredIdeaDao.unstarIdea(ideaId)
        } else {
            starredIdeaDao.starIdea(StarredIdeaEntity(ideaId = ideaId))
        }
    }

    fun getCoefficientPresets(): List<SectionCoefficientPreset> =
        TunisianCurriculumSeed.coefficientPresets

    fun getStudyMethodGuides(): List<StudyMethodGuide> =
        TunisianCurriculumSeed.studyMethodGuides

    fun getAppFeatureIdeas(): List<AppFeatureIdea> =
        TunisianCurriculumSeed.appFeatureIdeas
}
