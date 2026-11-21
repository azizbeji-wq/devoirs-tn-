package com.example.data.repository

import com.example.data.local.SubjectDao
import com.example.data.local.SubjectEntity
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.withContext
import java.util.Locale

/**
 * Dépôt (Repository) d'accès aux matières scolaires stockées dans Room.
 * Abstrait SubjectDao vis-à-vis du ViewModel et garantit l'initialisation des matières tunisiennes par défaut.
 */
class SubjectRepository(
    private val subjectDao: SubjectDao,
    private val ioDispatcher: CoroutineDispatcher = Dispatchers.IO
) {
    val allSubjects: Flow<List<SubjectEntity>> = subjectDao.observeAllSubjects()

    /**
     * Insère les matières académiques de base (Mathématiques, Physique-Chimie, Arabe, etc.)
     * lors du premier lancement si la table `subjects` est encore vide.
     */
    suspend fun ensureDefaultSubjectsSeeded() = withContext(ioDispatcher) {
        if (subjectDao.getSubjectCount() == 0) {
            subjectDao.insertAll(defaultSubjects)
        }
    }

    /**
     * Ajoute une nouvelle matière personnalisée dans Room après validation du nom.
     */
    suspend fun addCustomSubject(
        name: String,
        subtitle: String,
        iconKey: String = "school"
    ) = withContext(ioDispatcher) {
        val cleanName = name.trim()
        if (cleanName.isBlank()) return@withContext

        val slug = cleanName
            .lowercase(Locale.ROOT)
            .replace(Regex("[^a-z0-9\\u0600-\\u06FF]+"), "_")
            .trim('_')
            .ifBlank { "custom" }

        val nextOrder = subjectDao.getSubjectCount() + 100
        val entity = SubjectEntity(
            id = "custom_${slug}_${System.currentTimeMillis()}",
            canonicalName = cleanName,
            subtitle = subtitle.trim().ifBlank { cleanName },
            iconKey = iconKey,
            displayOrder = nextOrder,
            isCustom = true
        )
        subjectDao.insertSubject(entity)
    }

    /** Supprime une matière par son identifiant. */
    suspend fun deleteSubject(subjectId: String) = withContext(ioDispatcher) {
        subjectDao.deleteSubjectById(subjectId)
    }

    companion object {
        /** Catalogue officiel par défaut des matières du système éducatif tunisien. */
        val defaultSubjects: List<SubjectEntity> = listOf(
            SubjectEntity("maths", "Mathématiques", "Algèbre, Analyse & Géométrie", "calculate", 1, false),
            SubjectEntity("physique", "Physique-Chimie", "Mécanique, Électricité & Chimie", "science", 2, false),
            SubjectEntity("arabe", "Arabe", "Littérature, Langue & Rédaction", "translate", 3, false),
            SubjectEntity("svt", "SVT", "Biologie, Génétique & Géologie", "biotech", 4, false),
            SubjectEntity("francais", "Français", "Étude de texte & Essai", "auto_stories", 5, false),
            SubjectEntity("anglais", "Anglais", "Reading, Language & Writing", "public", 6, false),
            SubjectEntity("info", "Informatique", "Algorithmique, Python & Bases de données", "code", 7, false),
            SubjectEntity("philo", "Philosophie", "Dissertation & Analyse de texte", "history_edu", 8, false),
            SubjectEntity("eco", "Économie & Gestion", "Économie générale & Comptabilité", "account_balance", 9, false),
            SubjectEntity("tech", "Technologie", "Systèmes automatisés & Électronique", "precision", 10, false)
        )
    }
}
