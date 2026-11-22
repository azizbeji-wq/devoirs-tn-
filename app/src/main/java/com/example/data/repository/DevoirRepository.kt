package com.example.data.repository

import com.example.data.ai.PdfTextExtractor
import com.example.data.local.CatalogueFetchResult
import com.example.data.local.CatalogueOrigin
import com.example.data.local.DevoirAssetLoader
import com.example.data.local.DevoirDao
import com.example.data.local.DevoirPdfContentDao
import com.example.data.local.RemoteCatalogueService
import com.example.data.local.StarredIdeaDao
import com.example.data.local.StarredIdeaEntity
import com.example.data.local.TunisianCurriculumSeed
import com.example.data.local.toDomain
import com.example.data.local.toEntity
import com.example.data.model.AppFeatureIdea
import com.example.data.model.DevoirItem
import com.example.data.model.SectionCoefficientPreset
import com.example.data.model.StudyMethodGuide
import java.io.File
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext

/** État de synchronisation du catalogue distant / cache local. */
data class CatalogueSyncStatus(
    val isSyncing: Boolean = false,
    val catalogueVersion: Int = 0,
    val origin: CatalogueOrigin = CatalogueOrigin.LOCAL_CACHE,
    val syncedAtMillis: Long = 0L,
    val isOfflineFallback: Boolean = false,
    val errorMessage: String? = null
)

class DevoirRepository(
    private val devoirDao: DevoirDao,
    private val starredIdeaDao: StarredIdeaDao,
    private val fetchCatalogue: suspend () -> CatalogueFetchResult,
    private val pdfContentDao: DevoirPdfContentDao? = null,
    private val pdfTextExtractor: PdfTextExtractor? = null
) {
    /** Constructeur principal utilisant le service distant avec cache disque local et index FTS4. */
    constructor(
        devoirDao: DevoirDao,
        starredIdeaDao: StarredIdeaDao,
        remoteCatalogueService: RemoteCatalogueService,
        pdfContentDao: DevoirPdfContentDao? = null,
        pdfTextExtractor: PdfTextExtractor? = null
    ) : this(
        devoirDao = devoirDao,
        starredIdeaDao = starredIdeaDao,
        fetchCatalogue = { remoteCatalogueService.fetchCatalogueWithFallback() },
        pdfContentDao = pdfContentDao,
        pdfTextExtractor = pdfTextExtractor
    )

    /** Constructeur de compatibilité (tests ou chargeur local direct). */
    constructor(
        devoirDao: DevoirDao,
        starredIdeaDao: StarredIdeaDao,
        assetLoader: DevoirAssetLoader
    ) : this(
        devoirDao = devoirDao,
        starredIdeaDao = starredIdeaDao,
        fetchCatalogue = {
            val loaded = assetLoader.loadAllDevoirs()
            CatalogueFetchResult(
                catalogueVersion = loaded.maxOfOrNull { it.version } ?: 1,
                devoirs = loaded,
                origin = CatalogueOrigin.LOCAL_CACHE,
                syncedAtMillis = System.currentTimeMillis()
            )
        }
    )

    /** Constructeur léger dédié aux tests unitaires injectant une liste de devoirs. */
    constructor(
        devoirDao: DevoirDao,
        starredIdeaDao: StarredIdeaDao,
        loadAssetDevoirs: () -> List<DevoirItem>
    ) : this(
        devoirDao = devoirDao,
        starredIdeaDao = starredIdeaDao,
        fetchCatalogue = {
            val items = loadAssetDevoirs()
            CatalogueFetchResult(
                catalogueVersion = items.maxOfOrNull { it.version } ?: 1,
                devoirs = items,
                origin = CatalogueOrigin.REMOTE,
                syncedAtMillis = System.currentTimeMillis()
            )
        }
    )

    /** Flux Room des devoirs avec vérification rapide de l'existence réelle des fichiers locaux. */
    val allDevoirs: Flow<List<DevoirItem>> = devoirDao.observeAllDevoirs()
        .map { list ->
            list.map { entity ->
                val domain = entity.toDomain()
                val validSujet = domain.localSujetPath?.takeIf { File(it).exists() }
                val validCorrige = domain.localCorrigePath?.takeIf { File(it).exists() }
                if (validSujet != domain.localSujetPath || validCorrige != domain.localCorrigePath) {
                    domain.copy(
                        localSujetPath = validSujet,
                        localCorrigePath = validCorrige,
                        downloadedBytes = if (validSujet == null) 0L else domain.downloadedBytes
                    )
                } else {
                    domain
                }
            }
        }

    val starredIdeaIds: Flow<Set<String>> = starredIdeaDao.observeStarredIdeaIds()
        .map { it.toSet() }

    /** Carte réactive `devoirId -> texte OCR extrait` issue de la table Room FTS4. */
    val indexedPdfContentMap: Flow<Map<String, String>> = pdfContentDao
        ?.observeAllIndexedContents()
        ?.map { rows -> rows.associate { it.devoirId to it.extractedText } }
        ?: flowOf(emptyMap())

    /** Exécute une recherche Room FTS4 `MATCH` et retourne les identifiants de devoirs correspondants. */
    fun searchFtsDevoirIds(rawQuery: String): Flow<Set<String>> {
        val dao = pdfContentDao ?: return flowOf(emptySet())
        val ftsQuery = DevoirPdfContentDao.buildFtsMatchQuery(rawQuery) ?: return flowOf(emptySet())
        return dao.searchDevoirIdsByFts(ftsQuery)
            .map { it.toSet() }
            .catch { emit(emptySet()) }
    }

    /**
     * Déclenche une indexation OCR paresseuse si le devoir est disponible hors ligne
     * mais n'a pas encore d'entrée dans la table FTS4 (ex. téléchargé avant la mise à jour).
     */
    suspend fun ensureDevoirIndexed(devoir: DevoirItem) = withContext(Dispatchers.IO) {
        val dao = pdfContentDao ?: return@withContext
        val extractor = pdfTextExtractor ?: return@withContext
        val localSujet = devoir.localSujetPath?.let { File(it) }?.takeIf { it.exists() } ?: return@withContext
        val existing = dao.getByDevoirId(devoir.id)
        if (existing != null && existing.extractedText.isNotBlank()) return@withContext

        runCatching {
            extractor.extractAndIndexDevoir(
                devoirId = devoir.id,
                title = devoir.title,
                subject = devoir.subject,
                chapters = devoir.chapterTags,
                sujetFile = localSujet,
                corrigeFile = devoir.localCorrigePath?.let { File(it) }?.takeIf { it.exists() }
            )
        }
    }

    /**
     * Récupère `catalogue.json` en ligne (ou le dernier cache local si le réseau est absent),
     * unifié avec les anciens sujets texte sans PDF, et synchronise Room via `version` sans
     * jamais effacer `isFavorite`, `isCompleted` ni les PDF déjà téléchargés.
     */
    suspend fun syncCatalogue(): CatalogueFetchResult = withContext(Dispatchers.IO) {
        val result = fetchCatalogue()
        if (result.devoirs.isEmpty()) return@withContext result

        val existingById = devoirDao.getExistingVersions()
            .associateBy { it.id }

        for (devoir in result.devoirs) {
            val existing = existingById[devoir.id]
            val entity = devoir.toEntity()
            val upgradedFromTextToPdf = existing != null &&
                existing.sujetUrl.isBlank() &&
                entity.sujetUrl.isNotBlank()
            when {
                existing == null -> {
                    devoirDao.insertIgnore(entity)
                }

                devoir.version > existing.version || upgradedFromTextToPdf -> {
                    devoirDao.updateContentPreservingProgress(
                        id = entity.id,
                        title = entity.title,
                        subject = entity.subject,
                        levelId = entity.levelId,
                        sectionId = entity.sectionId,
                        typeId = entity.typeId,
                        trimesterId = entity.trimesterId,
                        durationMinutes = entity.durationMinutes,
                        difficulty = entity.difficulty,
                        chapterTagsSerialized = entity.chapterTagsSerialized,
                        source = entity.source,
                        version = entity.version,
                        sizeMb = entity.sizeMb,
                        sujetUrl = entity.sujetUrl,
                        corrigeUrl = entity.corrigeUrl,
                        sujetSizeBytes = entity.sujetSizeBytes,
                        corrigeSizeBytes = entity.corrigeSizeBytes,
                        sujetSha256 = entity.sujetSha256,
                        corrigeSha256 = entity.corrigeSha256,
                        exercisesSerialized = entity.exercisesSerialized
                    )
                }
            }
        }
        result
    }

    /** Alias conservé pour la compatibilité avec le démarrage et les tests existants. */
    suspend fun ensureSeedDataLoaded() {
        syncCatalogue()
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
