package com.example

import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.example.data.ai.PdfPageImageScaler
import com.example.data.ai.PdfTextExtractor
import com.example.data.ai.TutorAiRepository
import com.example.data.ai.TutorHelpMode
import com.example.data.ai.TutorLanguage
import com.example.data.local.DevoirPdfContentDao
import com.example.data.local.DevoirPdfContentFtsEntity
import com.example.data.local.DevoirsDatabase
import com.example.data.local.TutorMessageEntity
import com.example.data.model.CycleLevel
import com.example.data.model.DevoirItem
import com.example.data.model.DevoirType
import com.example.data.model.Trimester
import com.example.data.model.TunisianSection
import com.example.domain.search.DevoirSearchEngine
import com.example.ui.viewmodel.DevoirFilterState
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

/**
 * Tests unitaires et Robolectric validant :
 * - la table Room FTS4 (français accentué + arabe + nettoyage des opérateurs),
 * - la recherche plein texte + génération d'extrait surligné dans DevoirSearchEngine,
 * - le redimensionnement d'image avant envoi à Gemini (PdfPageImageScaler),
 * - la détection du fallback OCR Arabe (PdfTextExtractor),
 * - la persistance de l'historique des explications IA par devoir et par page dans Room.
 */
@RunWith(RobolectricTestRunner::class)
class PdfOcrFtsAndTutorTest {

    private lateinit var database: DevoirsDatabase

    @Before
    fun setUp() {
        database = Room.inMemoryDatabaseBuilder(
            ApplicationProvider.getApplicationContext(),
            DevoirsDatabase::class.java
        )
            .allowMainThreadQueries()
            .build()
    }

    @After
    fun tearDown() {
        database.close()
    }

    @Test
    fun fts4_indexesAndMatchesFrenchPdfContentWithAccents() = runTest {
        val dao = database.devoirPdfContentDao()
        dao.upsertByDevoirId(
            DevoirPdfContentFtsEntity(
                devoirId = "dev_math_bac_1",
                title = "Devoir de Synthèse N°1 — Analyse",
                chapters = "Suites réelles Limites Continuité",
                extractedText = "Exercice 1 : On considère la suite récurrente définie par u_0 = 2 et le théorème des gendarmes.",
                ocrSource = PdfTextExtractor.SOURCE_MLKIT_LATIN
            )
        )

        val query = DevoirPdfContentDao.buildFtsMatchQuery("theoreme gendarmes")
        assertNotNull(query)
        val matchedIds = dao.searchDevoirIdsByFtsOnce(query!!)
        assertEquals(listOf("dev_math_bac_1"), matchedIds)
    }

    @Test
    fun fts4_indexesAndMatchesArabicPdfContent() = runTest {
        val dao = database.devoirPdfContentDao()
        dao.upsertByDevoirId(
            DevoirPdfContentFtsEntity(
                devoirId = "dev_arabe_9eme_1",
                title = "فرض تأليفي عدد 1 — العربية",
                chapters = "شرح نص إنتاج كتابي",
                extractedText = "النص : في قلب المدينة العتيقة بتونس تتعانق الأصالة مع الحداثة.",
                ocrSource = PdfTextExtractor.SOURCE_GEMINI_ARABIC
            )
        )

        val query = DevoirPdfContentDao.buildFtsMatchQuery("العتيقة بتونس")
        assertNotNull(query)
        val matchedIds = dao.searchDevoirIdsByFtsOnce(query!!)
        assertEquals(listOf("dev_arabe_9eme_1"), matchedIds)
    }

    @Test
    fun fts4_upsertReplacesExistingRowWithoutDuplicates() = runTest {
        val dao = database.devoirPdfContentDao()
        dao.upsertByDevoirId(
            DevoirPdfContentFtsEntity(
                devoirId = "dev_phy_1",
                title = "Dipôle RC",
                chapters = "Électricité",
                extractedText = "Ancien texte condensateur",
                ocrSource = PdfTextExtractor.SOURCE_MLKIT_LATIN
            )
        )
        dao.upsertByDevoirId(
            DevoirPdfContentFtsEntity(
                devoirId = "dev_phy_1",
                title = "Dipôle RC",
                chapters = "Électricité",
                extractedText = "Nouveau texte constante de temps tau",
                ocrSource = PdfTextExtractor.SOURCE_MLKIT_LATIN
            )
        )

        val all = dao.observeAllIndexedContents().first()
        assertEquals(1, all.size)
        assertTrue(all.first().extractedText.contains("constante de temps"))
        assertTrue(dao.searchDevoirIdsByFtsOnce("Ancien*").isEmpty())
        assertEquals(listOf("dev_phy_1"), dao.searchDevoirIdsByFtsOnce("constante*"))
    }

    @Test
    fun fts4_buildFtsMatchQuery_sanitizesOperatorsAndReservedWords() {
        assertNull(DevoirPdfContentDao.buildFtsMatchQuery("   --- \"*\"   "))
        assertEquals("derivee* fonction*", DevoirPdfContentDao.buildFtsMatchQuery("dérivée: AND \"fonction\"!"))
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
    fun tutorHistory_persistsExplanationsPerPageAndAggregatesPerDevoir() = runTest {
        val dao = database.tutorMessageDao()
        dao.insertMessage(
            TutorMessageEntity(
                devoirId = "dev_bac_1",
                exerciseNumber = 1,
                role = "user",
                mode = TutorHelpMode.HINT_1.id,
                language = TutorLanguage.FRENCH.code,
                content = "Donne-moi l'Indice 1 (Page 1)"
            )
        )
        dao.insertMessage(
            TutorMessageEntity(
                devoirId = "dev_bac_1",
                exerciseNumber = 1,
                role = "assistant",
                mode = TutorHelpMode.HINT_1.id,
                language = TutorLanguage.FRENCH.code,
                content = "Commence par calculer la dérivée f'(x)."
            )
        )
        dao.insertMessage(
            TutorMessageEntity(
                devoirId = "dev_bac_1",
                exerciseNumber = 2,
                role = "assistant",
                mode = TutorHelpMode.SOLUTION.id,
                language = TutorLanguage.ARABIC.code,
                content = "الحل المفصّل للتمرين الثاني على الصفحة 2."
            )
        )

        val page1Messages = dao.observeConversation("dev_bac_1", 1).first()
        val allDevoirMessages = dao.observeDevoirHistory("dev_bac_1").first()
        val assistantCount = dao.observeDevoirExplanationCount("dev_bac_1").first()

        assertEquals(2, page1Messages.size)
        assertEquals(3, allDevoirMessages.size)
        assertEquals(2, assistantCount)

        dao.clearDevoirHistory("dev_bac_1")
        assertTrue(dao.observeDevoirHistory("dev_bac_1").first().isEmpty())
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
