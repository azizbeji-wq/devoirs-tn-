package com.example

import com.example.data.local.ExerciseCodec
import com.example.data.local.toDomain
import com.example.data.local.toEntity
import com.example.data.model.CycleLevel
import com.example.data.model.DevoirItem
import com.example.data.model.DevoirType
import com.example.data.model.Trimester
import com.example.data.model.TunisianSection
import com.example.domain.search.DevoirSearchEngine
import com.example.ui.viewmodel.DevoirFilterState
import com.example.ui.viewmodel.DevoirSortOption
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Tests unitaires JUnit vérifiant :
 * - les filtres et tris de la bibliothèque PDF Devoirs TN,
 * - la recherche plein texte (titres, chapitres, matière, source) et la mise en évidence,
 * - la conversion bidirectionnelle DevoirEntity <-> DevoirItem (URL distantes, SHA-256 et chemins locaux).
 */
class DevoirSearchAndMapperTest {

    private val sampleDevoirs = listOf(
        DevoirItem(
            id = "math_bac_ds1",
            title = "Devoir de Synthèse N°1 — Analyse & Nombres Complexes",
            subject = "Mathématiques",
            level = CycleLevel.BAC,
            section = TunisianSection.MATHS,
            type = DevoirType.SYNTHESE,
            trimester = Trimester.T1,
            durationMinutes = 180,
            difficulty = "Bac",
            chapterTags = listOf("Suites réelles", "Nombres complexes", "Continuité"),
            source = "Sujet d'entraînement",
            version = 3,
            sizeMb = 6.4,
            sujetUrl = "https://cdn.devoirstn.example/pdfs/bac_maths_ds1_sujet.pdf",
            corrigeUrl = "https://cdn.devoirstn.example/pdfs/bac_maths_ds1_corrige.pdf",
            sujetSizeBytes = 3_800_000L,
            corrigeSizeBytes = 2_900_000L,
            sujetSha256 = "1111111111111111111111111111111111111111111111111111111111111111",
            corrigeSha256 = "2222222222222222222222222222222222222222222222222222222222222222",
            localSujetPath = "/files/pdfs/math_bac_ds1/sujet.pdf",
            localCorrigePath = "/files/pdfs/math_bac_ds1/corrige.pdf",
            downloadedBytes = 6_700_000L,
            downloadedVersion = 3,
            isFavorite = true,
            isCompleted = false
        ),
        DevoirItem(
            id = "phys_3eme_dc2",
            title = "Devoir de Contrôle N°2 — Dipôle RC & Oscillations",
            subject = "Physique-Chimie",
            level = CycleLevel.LYCEE_3,
            section = TunisianSection.SCIENCES_EXP,
            type = DevoirType.CONTROLE,
            trimester = Trimester.T2,
            durationMinutes = 60,
            difficulty = "Moyen",
            chapterTags = listOf("Condensateur", "Dipôle RC", "Acides et bases"),
            source = "Sujet d'entraînement",
            version = 2,
            sizeMb = 4.1,
            sujetUrl = "https://cdn.devoirstn.example/pdfs/lycee_3eme_physique_dc2_sujet.pdf",
            corrigeUrl = "https://cdn.devoirstn.example/pdfs/lycee_3eme_physique_dc2_corrige.pdf",
            sujetSizeBytes = 2_200_000L,
            corrigeSizeBytes = 2_100_000L,
            isFavorite = false,
            isCompleted = true
        ),
        DevoirItem(
            id = "info_9eme_ds3",
            title = "Devoir de Synthèse N°3 — Algorithmique & Tableaux",
            subject = "Informatique",
            level = CycleLevel.COLLEGE_9,
            section = TunisianSection.TRONC_COMMUN,
            type = DevoirType.SYNTHESE,
            trimester = Trimester.T3,
            durationMinutes = 90,
            difficulty = "Facile",
            chapterTags = listOf("Algorithmes", "Boucles", "Tableaux"),
            source = "Sujet d'entraînement",
            version = 1,
            sizeMb = 3.2,
            sujetUrl = "https://cdn.devoirstn.example/pdfs/college_9eme_info_ds3_sujet.pdf",
            corrigeUrl = null,
            sujetSizeBytes = 3_355_443L,
            corrigeSizeBytes = 0L,
            isFavorite = false,
            isCompleted = false
        )
    )

    @Test
    fun `filterDevoirs sans criteres retourne tous les devoirs`() {
        val result = DevoirSearchEngine.filterDevoirs(sampleDevoirs, DevoirFilterState())
        assertEquals(3, result.size)
    }

    @Test
    fun `filterDevoirs filtre par niveau et section tunisienne`() {
        val filter = DevoirFilterState(
            selectedLevel = CycleLevel.BAC,
            selectedSection = TunisianSection.MATHS
        )
        val result = DevoirSearchEngine.filterDevoirs(sampleDevoirs, filter)
        assertEquals(1, result.size)
        assertEquals("math_bac_ds1", result.first().id)
    }

    @Test
    fun `filterDevoirs filtre par type trimestre matiere et favoris`() {
        val filter = DevoirFilterState(
            selectedType = DevoirType.SYNTHESE,
            selectedTrimester = Trimester.T1,
            selectedSubject = "Mathématiques",
            favoritesOnly = true
        )
        val result = DevoirSearchEngine.filterDevoirs(sampleDevoirs, filter)
        assertEquals(1, result.size)
        assertEquals("math_bac_ds1", result.first().id)

        val emptyFilter = filter.copy(selectedSubject = "Physique-Chimie")
        assertTrue(DevoirSearchEngine.filterDevoirs(sampleDevoirs, emptyFilter).isEmpty())
    }

    @Test
    fun `recherche plein texte trouve un chapitre et ignore accents et majuscules`() {
        val byChapter = DevoirSearchEngine.filterDevoirs(
            sampleDevoirs,
            DevoirFilterState(searchQuery = "continuite")
        )
        assertEquals(1, byChapter.size)
        assertEquals("math_bac_ds1", byChapter.first().id)

        val byTitle = DevoirSearchEngine.filterDevoirs(
            sampleDevoirs,
            DevoirFilterState(searchQuery = "DIPOLE")
        )
        assertEquals(1, byTitle.size)
        assertEquals("phys_3eme_dc2", byTitle.first().id)
    }

    @Test
    fun `recherche multi-mots croise titre matiere et chapitres`() {
        val filter = DevoirFilterState(searchQuery = "tableaux boucles")
        val result = DevoirSearchEngine.filterDevoirs(sampleDevoirs, filter)
        assertEquals(1, result.size)
        assertEquals("info_9eme_ds3", result.first().id)
    }

    @Test
    fun `findHighlightRanges repere les plages exactes malgre les accents`() {
        val text = "Devoir de Synthèse — Étude de la dérivée"
        val ranges = DevoirSearchEngine.findHighlightRanges(text, "synthese derivee")
        assertEquals(2, ranges.size)
        assertEquals("Synthèse", text.substring(ranges[0]))
        assertEquals("dérivée", text.substring(ranges[1]))
    }

    @Test
    fun `findHighlightRanges fusionne les plages adjacentes et gere les requetes vides`() {
        assertTrue(DevoirSearchEngine.findHighlightRanges("Suites réelles", "   ").isEmpty())
        val text = "Mathématiques"
        val ranges = DevoirSearchEngine.findHighlightRanges(text, "math emat")
        assertEquals(1, ranges.size)
        assertEquals("Mathémat", text.substring(ranges.first()))
    }

    @Test
    fun `conversion bidirectionnelle DevoirItem vers DevoirEntity puis retour preserve URLs integrite et stockage local`() {
        val original = sampleDevoirs[0]
        val entity = original.toEntity()
        val roundTripped = entity.toDomain()

        assertEquals(original.id, roundTripped.id)
        assertEquals(original.title, roundTripped.title)
        assertEquals(original.subject, roundTripped.subject)
        assertEquals(original.level, roundTripped.level)
        assertEquals(original.section, roundTripped.section)
        assertEquals(original.type, roundTripped.type)
        assertEquals(original.trimester, roundTripped.trimester)
        assertEquals(original.durationMinutes, roundTripped.durationMinutes)
        assertEquals(original.difficulty, roundTripped.difficulty)
        assertEquals(original.chapterTags, roundTripped.chapterTags)
        assertEquals(original.source, roundTripped.source)
        assertEquals(original.version, roundTripped.version)
        assertEquals(original.sizeMb, roundTripped.sizeMb, 0.0001)
        assertEquals(original.sujetUrl, roundTripped.sujetUrl)
        assertEquals(original.corrigeUrl, roundTripped.corrigeUrl)
        assertEquals(original.sujetSizeBytes, roundTripped.sujetSizeBytes)
        assertEquals(original.corrigeSizeBytes, roundTripped.corrigeSizeBytes)
        assertEquals(original.sujetSha256, roundTripped.sujetSha256)
        assertEquals(original.corrigeSha256, roundTripped.corrigeSha256)
        assertEquals(original.localSujetPath, roundTripped.localSujetPath)
        assertEquals(original.localCorrigePath, roundTripped.localCorrigePath)
        assertEquals(original.downloadedBytes, roundTripped.downloadedBytes)
        assertEquals(original.downloadedVersion, roundTripped.downloadedVersion)
        assertTrue(roundTripped.hasCorrigePdf)
        assertTrue(roundTripped.isAvailableOffline)
        assertEquals(original.isFavorite, roundTripped.isFavorite)
        assertEquals(original.isCompleted, roundTripped.isCompleted)

        // Vérification du cas corrigeUrl = null et non téléchargé
        val noCorrigeRoundTrip = sampleDevoirs[2].toEntity().toDomain()
        assertNull(noCorrigeRoundTrip.corrigeUrl)
        assertFalse(noCorrigeRoundTrip.hasCorrigePdf)
        assertFalse(noCorrigeRoundTrip.isAvailableOffline)
    }

    @Test
    fun `ExerciseCodec encode et decode fidelement les tags avec caracteres accentues et arabes`() {
        assertTrue(ExerciseCodec.decodeTags("").isEmpty())
        val tags = listOf("الدوال العددية", "Nombres complexes", "Dipôle RL")
        val encoded = ExerciseCodec.encodeTags(tags)
        assertEquals(tags, ExerciseCodec.decodeTags(encoded))
    }

    @Test
    fun `filterDevoirs trie par plus recents duree difficulte et matiere`() {
        val byRecent = DevoirSearchEngine.filterDevoirs(
            sampleDevoirs,
            DevoirFilterState(sortOption = DevoirSortOption.RECENT)
        )
        assertEquals(listOf("math_bac_ds1", "phys_3eme_dc2", "info_9eme_ds3"), byRecent.map { it.id })

        val byDuration = DevoirSearchEngine.filterDevoirs(
            sampleDevoirs,
            DevoirFilterState(sortOption = DevoirSortOption.DURATION)
        )
        assertEquals(listOf("phys_3eme_dc2", "info_9eme_ds3", "math_bac_ds1"), byDuration.map { it.id })

        val byDifficulty = DevoirSearchEngine.filterDevoirs(
            sampleDevoirs,
            DevoirFilterState(sortOption = DevoirSortOption.DIFFICULTY)
        )
        assertEquals(listOf("info_9eme_ds3", "phys_3eme_dc2", "math_bac_ds1"), byDifficulty.map { it.id })

        val bySubject = DevoirSearchEngine.filterDevoirs(
            sampleDevoirs,
            DevoirFilterState(sortOption = DevoirSortOption.SUBJECT)
        )
        assertEquals(listOf("info_9eme_ds3", "math_bac_ds1", "phys_3eme_dc2"), bySubject.map { it.id })
    }
}
