package com.example.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Transaction
import kotlinx.coroutines.flow.Flow

/**
 * DAO d'accès à l'index plein texte FTS4 des devoirs PDF téléchargés.
 */
@Dao
interface DevoirPdfContentDao {

    /** Observe l'ensemble des documents PDF déjà indexés (utilisé pour l'affichage des extraits surlignés). */
    @Query("SELECT * FROM devoir_pdf_contents_fts")
    fun observeAllIndexedContents(): Flow<List<DevoirPdfContentFtsEntity>>

    /** Recherche plein texte FTS4 (titres, chapitres et contenu OCR) retournant les identifiants correspondants. */
    @Query("SELECT DISTINCT devoirId FROM devoir_pdf_contents_fts WHERE devoir_pdf_contents_fts MATCH :ftsQuery")
    fun searchDevoirIdsByFts(ftsQuery: String): Flow<List<String>>

    /** Variante suspendue utile pour les vérifications synchrones et tests unitaires. */
    @Query("SELECT DISTINCT devoirId FROM devoir_pdf_contents_fts WHERE devoir_pdf_contents_fts MATCH :ftsQuery")
    suspend fun searchDevoirIdsByFtsOnce(ftsQuery: String): List<String>

    @Query("SELECT * FROM devoir_pdf_contents_fts WHERE devoirId = :devoirId LIMIT 1")
    suspend fun getByDevoirId(devoirId: String): DevoirPdfContentFtsEntity?

    @Insert
    suspend fun insert(entry: DevoirPdfContentFtsEntity)

    @Query("DELETE FROM devoir_pdf_contents_fts WHERE devoirId = :devoirId")
    suspend fun deleteByDevoirId(devoirId: String)

    @Query("DELETE FROM devoir_pdf_contents_fts WHERE devoirId IN (:devoirIds)")
    suspend fun deleteByDevoirIds(devoirIds: List<String>)

    @Query("DELETE FROM devoir_pdf_contents_fts")
    suspend fun deleteAll()

    /** Remplace proprement l'entrée FTS d'un devoir pour éviter tout doublon lors d'une réindexation. */
    @Transaction
    suspend fun upsertByDevoirId(entry: DevoirPdfContentFtsEntity) {
        deleteByDevoirId(entry.devoirId)
        insert(entry)
    }

    companion object {
        private val TOKEN_SPLIT_REGEX = Regex("[\\p{Punct}\\s]+")
        private val RESERVED_FTS_KEYWORDS = setOf("AND", "OR", "NOT", "NEAR")

        /**
         * Transforme une requête utilisateur libre en expression FTS4 sécurisée (`mot1* mot2*`)
         * compatible avec le français, l'anglais et l'arabe, sans risque d'erreur de syntaxe SQLite.
         */
        fun buildFtsMatchQuery(rawQuery: String): String? {
            val tokens = rawQuery.trim()
                .split(TOKEN_SPLIT_REGEX)
                .map { it.trim() }
                .filter { token ->
                    token.isNotEmpty() && token.uppercase() !in RESERVED_FTS_KEYWORDS
                }
            if (tokens.isEmpty()) return null
            return tokens.joinToString(separator = " ") { "$it*" }
        }
    }
}
