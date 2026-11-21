package com.example.data.local

import androidx.room.Entity
import androidx.room.Fts4
import androidx.room.FtsOptions

/**
 * Table virtuelle Room FTS4 stockant le texte extrait par OCR (ML Kit Latin + fallback Gemini Arabe)
 * pour chaque devoir PDF téléchargé.
 *
 * Le tokenizer `unicode61` normalise automatiquement les accents latins et gère les caractères arabes,
 * permettant une recherche plein texte rapide dans les titres, chapitres et contenus PDF.
 */
@Fts4(tokenizer = FtsOptions.TOKENIZER_UNICODE61)
@Entity(tableName = "devoir_pdf_contents_fts")
data class DevoirPdfContentFtsEntity(
    val devoirId: String,
    val title: String,
    val chapters: String,
    val extractedText: String,
    val ocrSource: String, // "MLKIT_LATIN", "GEMINI_ARABIC", "MIXED"
    val indexedAtMillis: Long = System.currentTimeMillis()
)
