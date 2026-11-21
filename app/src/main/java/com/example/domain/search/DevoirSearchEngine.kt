package com.example.domain.search

import com.example.data.model.CycleLevel
import com.example.data.model.DevoirItem
import com.example.data.model.DevoirType
import com.example.data.model.Trimester
import com.example.data.model.TunisianSection
import com.example.ui.viewmodel.DevoirFilterState
import com.example.ui.viewmodel.DevoirSortOption
import java.text.Normalizer
import java.util.Locale

/**
 * Moteur pur de filtrage, tri et recherche plein texte pour la bibliothèque PDF Devoirs TN.
 * Sans dépendance Android pour être directement testable en JUnit.
 *
 * Recherche dans :
 * - le titre du devoir,
 * - la matière,
 * - les chapitres (tags),
 * - le texte extrait des PDF (index Room FTS4 via ML Kit + fallback Gemini Arabe),
 * - la source et le niveau de difficulté.
 */
object DevoirSearchEngine {

    /**
     * Filtre puis trie une liste de devoirs selon les critères actifs, la requête plein texte
     * (incluant les correspondances FTS4 et le contenu OCR des PDF téléchargés) et le tri choisi.
     */
    fun filterDevoirs(
        devoirs: List<DevoirItem>,
        filter: DevoirFilterState,
        indexedContentByDevoirId: Map<String, String> = emptyMap(),
        ftsMatchedIds: Set<String> = emptySet()
    ): List<DevoirItem> {
        val normalizedTokens = tokenizeQuery(filter.searchQuery)

        val filtered = devoirs.filter { item ->
            val matchesQuery = normalizedTokens.isEmpty() ||
                item.id in ftsMatchedIds ||
                matchesAllTokens(item, normalizedTokens, indexedContentByDevoirId[item.id])
            val matchesLevel = filter.selectedLevel == CycleLevel.ALL || item.level == filter.selectedLevel
            val matchesSection = filter.selectedSection == TunisianSection.ALL || item.section == filter.selectedSection
            val matchesType = filter.selectedType == DevoirType.ALL || item.type == filter.selectedType
            val matchesTrimester = filter.selectedTrimester == Trimester.ALL || item.trimester == filter.selectedTrimester
            val matchesSubject = filter.selectedSubject == "Toutes" ||
                item.subject.equals(filter.selectedSubject, ignoreCase = true)
            val matchesFavorite = !filter.favoritesOnly || item.isFavorite

            matchesQuery && matchesLevel && matchesSection && matchesType &&
                matchesTrimester && matchesSubject && matchesFavorite
        }

        return sortDevoirs(filtered, filter.sortOption)
    }

    /**
     * Trie une liste de devoirs selon l'option choisie (plus récents, durée, difficulté, matière).
     */
    fun sortDevoirs(
        devoirs: List<DevoirItem>,
        sortOption: DevoirSortOption
    ): List<DevoirItem> = when (sortOption) {
        DevoirSortOption.RECENT -> devoirs.sortedWith(
            compareByDescending<DevoirItem> { it.version }
                .thenBy { normalizeForSearch(it.subject) }
                .thenBy { normalizeForSearch(it.title) }
        )
        DevoirSortOption.DURATION -> devoirs.sortedWith(
            compareBy<DevoirItem> { it.durationMinutes }
                .thenBy { normalizeForSearch(it.title) }
        )
        DevoirSortOption.DIFFICULTY -> devoirs.sortedWith(
            compareBy<DevoirItem> { difficultyRank(it.difficulty) }
                .thenBy { it.durationMinutes }
                .thenBy { normalizeForSearch(it.title) }
        )
        DevoirSortOption.SUBJECT -> devoirs.sortedWith(
            compareBy<DevoirItem> { normalizeForSearch(it.subject) }
                .thenBy { it.level.ordinal }
                .thenBy { normalizeForSearch(it.title) }
        )
    }

    /**
     * Associe un rang numérique croissant au niveau de difficulté d'un devoir
     * (du plus accessible au plus exigeant).
     */
    fun difficultyRank(difficulty: String): Int {
        val norm = normalizeForSearch(difficulty)
        return when {
            norm.contains("facile") || norm.contains("fondamental") || norm.contains("base") -> 1
            norm.contains("moyen") || norm.contains("standard") || norm.contains("intermediaire") -> 2
            norm.contains("concours") || norm.contains("pilote") || norm.contains("difficile") || norm.contains("avance") -> 3
            norm.contains("bac") || norm.contains("examen") -> 4
            norm.contains("superieur") || norm.contains("prepa") -> 5
            else -> 2
        }
    }

    /**
     * Vérifie que tous les mots-clés de la requête sont présents dans les métadonnées du devoir
     * ou dans le contenu texte extrait du PDF ([extractedPdfText]).
     */
    fun matchesAllTokens(
        devoir: DevoirItem,
        normalizedTokens: List<String>,
        extractedPdfText: String? = null
    ): Boolean {
        if (normalizedTokens.isEmpty()) return true
        val searchableBlob = buildString {
            append(normalizeForSearch(devoir.title))
            append(' ')
            append(normalizeForSearch(devoir.subject))
            append(' ')
            append(normalizeForSearch(devoir.source))
            append(' ')
            append(normalizeForSearch(devoir.difficulty))
            append(' ')
            devoir.chapterTags.forEach { tag ->
                append(normalizeForSearch(tag))
                append(' ')
            }
            if (!extractedPdfText.isNullOrBlank()) {
                append(normalizeForSearch(extractedPdfText))
                append(' ')
            }
            if (devoir.hasCorrigePdf) {
                append("corrige solution pdf ")
            }
        }
        return normalizedTokens.all { token -> searchableBlob.contains(token) }
    }

    /**
     * Extrait un court passage pertinent autour du premier mot-clé trouvé dans le contenu OCR d'un PDF,
     * afin de l'afficher avec surlignage dans la carte du devoir.
     */
    fun extractMatchingSnippet(
        extractedText: String,
        query: String,
        maxChars: Int = 140
    ): String? {
        if (extractedText.isBlank() || query.isBlank()) return null
        val cleaned = extractedText.replace(Regex("\\s+"), " ").trim()
        val ranges = findHighlightRanges(cleaned, query)
        if (ranges.isEmpty()) return null

        val firstMatch = ranges.first()
        val halfWindow = (maxChars / 2).coerceAtLeast(30)
        val rawStart = (firstMatch.first - halfWindow).coerceAtLeast(0)
        val rawEnd = (rawStart + maxChars).coerceAtMost(cleaned.length)

        // Ajuste les bornes aux espaces voisins pour ne pas couper un mot en deux
        val start = if (rawStart > 0) {
            cleaned.indexOf(' ', rawStart).takeIf { it in rawStart until firstMatch.first }?.plus(1) ?: rawStart
        } else {
            0
        }
        val end = if (rawEnd < cleaned.length) {
            cleaned.lastIndexOf(' ', rawEnd).takeIf { it > firstMatch.last } ?: rawEnd
        } else {
            cleaned.length
        }

        val prefix = if (start > 0) "…" else ""
        val suffix = if (end < cleaned.length) "…" else ""
        return "$prefix${cleaned.substring(start, end).trim()}$suffix"
    }

    /**
     * Calcule les plages d'indices (dans le texte original) correspondant aux mots de [query].
     * La correspondance est insensible à la casse et aux accents français (ex. « derivee » → « Dérivée »).
     */
    fun findHighlightRanges(text: String, query: String): List<IntRange> {
        val tokens = tokenizeQuery(query)
        if (text.isEmpty() || tokens.isEmpty()) return emptyList()

        val normalizedChars = StringBuilder(text.length)
        val originalIndices = ArrayList<Int>(text.length)

        text.forEachIndexed { originalIdx, ch ->
            val decomposed = stripDiacritics(ch.toString()).lowercase(Locale.ROOT)
            for (normChar in decomposed) {
                normalizedChars.append(normChar)
                originalIndices.add(originalIdx)
            }
        }

        val normalizedText = normalizedChars.toString()
        val rawRanges = mutableListOf<IntRange>()

        for (token in tokens) {
            if (token.isEmpty()) continue
            var searchFrom = 0
            while (searchFrom <= normalizedText.length - token.length) {
                val foundIdx = normalizedText.indexOf(token, startIndex = searchFrom)
                if (foundIdx < 0) break
                val origStart = originalIndices[foundIdx]
                val origEnd = originalIndices[foundIdx + token.length - 1]
                rawRanges.add(origStart..origEnd)
                searchFrom = foundIdx + token.length
            }
        }

        if (rawRanges.isEmpty()) return emptyList()

        // Fusion des plages qui se chevauchent ou se touchent pour un rendu propre
        val sorted = rawRanges.sortedBy { it.first }
        val merged = mutableListOf(sorted.first())
        for (i in 1 until sorted.size) {
            val current = sorted[i]
            val last = merged.last()
            if (current.first <= last.last + 1) {
                merged[merged.lastIndex] = last.first..maxOf(last.last, current.last)
            } else {
                merged.add(current)
            }
        }
        return merged
    }

    /** Découpe la requête en mots normalisés non vides. */
    fun tokenizeQuery(query: String): List<String> =
        normalizeForSearch(query)
            .split(Regex("\\s+"))
            .filter { it.isNotBlank() }

    /** Normalise une chaîne pour la recherche (minuscules + suppression des accents latins). */
    fun normalizeForSearch(input: String): String =
        stripDiacritics(input).lowercase(Locale.ROOT).trim()

    private fun stripDiacritics(input: String): String {
        val decomposed = Normalizer.normalize(input, Normalizer.Form.NFD)
        return buildString(decomposed.length) {
            for (ch in decomposed) {
                if (Character.getType(ch) != Character.NON_SPACING_MARK.toInt()) {
                    append(ch)
                }
            }
        }
    }
}
