package com.example

import com.example.data.ai.PdfPageImageScaler
import com.example.data.ai.PdfTextExtractor
import com.example.data.ai.TutorAiRepository
import com.example.data.local.DevoirPdfContentDao
import com.example.data.model.CycleLevel
import com.example.data.model.DevoirItem
import com.example.data.model.DevoirType
import com.example.data.model.Trimester
import com.example.data.model.TunisianSection
import com.example.domain.search.DevoirSearchEngine
import com.example.ui.viewmodel.DevoirFilterState
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Tests unitaires JVM purs (sans émulateur ni accès réseau) validant :
 * - la construction sécurisée des requêtes FTS4 (`DevoirPdfContentDao.buildFtsMatchQuery`),
 * - la recherche plein texte + génération d'extrait surligné (`DevoirSearchEngine`),
 * - le redimensionnement d'image avant envoi à Gemini (`PdfPageImageScaler`),
 * - la détection du fallback OCR Arabe (`PdfTextExtractor.shouldUseArabicFallback`),
 * - la conformité du prompt système tunisien (`TutorAiRepository.SYSTEM_PROMPT`).
 */
class PdfOcrFtsAndTutorUnitTest {

    @Test
    fun fts4_buildFtsMatchQuery_sanitizesOperatorsAndReservedWords() {
        assertNull(DevoirPdfContentDao.buildFtsMatchQuery("   --- \"*\"   "))
        assertEquals("dérivée* fonction*", DevoirPdfContentDao.buildFtsMatchQuery("dérivée: AND \"fonction\"!"))
        assertEquals("العتيقة* بتونس*", DevoirPdfContentDao.buildFtsMatchQuery("العتيقة بتونس"))
    }

    @Test
    fun searchEngine_matchesIndexedPdfContentAndExtractsSnippet() {
        val devoir = DevoirItem(
            id = "dev_svt_1",
            title = "Devoir de Contrôle N°2",
            subject = "SVT",
            level = CycleLevel.BAC,
            section = TunisianSection.SCIENCES_EXP,
            type = DevoirType.CONTROLE,
            trimester = Trimester.T2,
            durationMinutes = 120,
            difficulty = "Bac",
            chapterTags = listOf("Génétique"),
            source = "Sujet d'entraînement",
            version = 1,
            sizeMb = 1.2,
            sujetUrl = "https://example.com/svt.pdf"
        )
        val ocrContent = "Partie II : L'étude du brassage intrachromosomique montre un crossing-over entre deux gènes liés."
        val filtered = DevoirSearchEngine.filterDevoirs(
            devoirs = listOf(devoir),
            filter = DevoirFilterState(searchQuery = "crossing over"),
            indexedContentByDevoirId = mapOf(devoir.id to ocrContent)
        )

        assertEquals(1, filtered.size)
        val snippet = DevoirSearchEngine.extractMatchingSnippet(ocrContent, "crossing")
        assertNotNull(snippet)
        assertTrue(snippet!!.contains("crossing-over"))
    }

    @Test
    fun searchEngine_matchesFtsMatchedIdsEvenWithoutInlineMap() {
        val devoir = DevoirItem(
            id = "dev_arabe_1",
            title = "فرض تأليفي عدد 1",
            subject = "Arabe",
            level = CycleLevel.COLLEGE_9,
            section = TunisianSection.LETTRES,
            type = DevoirType.SYNTHESE,
            trimester = Trimester.T1,
            durationMinutes = 90,
            difficulty = "Standard",
            chapterTags = listOf("شرح نص"),
            source = "Sujet d'entraînement",
            version = 1,
            sizeMb = 0.8,
            sujetUrl = "https://example.com/ar.pdf"
        )
        val filtered = DevoirSearchEngine.filterDevoirs(
            devoirs = listOf(devoir),
            filter = DevoirFilterState(searchQuery = "العتيقة"),
            ftsMatchedIds = setOf("dev_arabe_1")
        )
        assertEquals(1, filtered.size)
    }

    @Test
    fun imageScaler_downscalesLargePortraitPagePreservingAspectRatio() {
        val (w, h) = PdfPageImageScaler.computeScaledDimensions(
            width = 2480,
            height = 3508,
            maxDimensionPx = 1024
        )
        assertEquals(1024, h)
        assertTrue("Largeur attendue autour de 724 px (reçu $w)", w in 720..728)
    }

    @Test
    fun imageScaler_keepsSmallImageUnchanged() {
        val (w, h) = PdfPageImageScaler.computeScaledDimensions(
            width = 600,
            height = 800,
            maxDimensionPx = 1024
        )
        assertEquals(600, w)
        assertEquals(800, h)
    }

    @Test
    fun arabicFallback_triggersForArabicSubjectOrSparseLatinText() {
        assertTrue(PdfTextExtractor.shouldUseArabicFallback("Arabe", "Texte long mais matière arabe officiellement"))
        assertTrue(PdfTextExtractor.shouldUseArabicFallback("Mathématiques", "Ex 1"))
        assertFalse(
            PdfTextExtractor.shouldUseArabicFallback(
                "Mathématiques",
                "Exercice 1 : Soit f la fonction définie sur R par f(x) = x^2 - 3x + 2."
            )
        )
    }

    @Test
    fun tutorSystemPrompt_containsOfficialTunisianProgressiveGuidelines() {
        val prompt = TutorAiRepository.SYSTEM_PROMPT
        assertTrue(
            prompt.contains(
                "Tu es un tuteur bienveillant pour élèves tunisiens, tu suis les programmes officiels, " +
                    "tu expliques étape par étape, tu proposes d'abord des indices avant la solution."
            )
        )
    }
}
