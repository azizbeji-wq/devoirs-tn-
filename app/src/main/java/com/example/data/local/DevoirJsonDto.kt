package com.example.data.local

import com.example.data.model.CycleLevel
import com.example.data.model.DevoirExercise
import com.example.data.model.DevoirItem
import com.example.data.model.DevoirType
import com.example.data.model.Trimester
import com.example.data.model.TunisianSection
import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass
import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory

/**
 * Représentation Moshi du fichier distant ou mis en cache `catalogue.json` (source principale PDF).
 */
@JsonClass(generateAdapter = true)
data class CatalogueDto(
    @Json(name = "schemaVersion") val schemaVersion: Int = 3,
    @Json(name = "catalogueVersion") val catalogueVersion: Int = 1,
    @Json(name = "devoirs") val devoirs: List<CatalogueDevoirDto>? = null
)

/**
 * Métadonnées d'un devoir PDF hébergé en ligne telles que définies dans `catalogue.json`.
 * Les empreintes `sujetSha256` et `corrigeSha256` sont optionnelles et rétrocompatibles.
 */
@JsonClass(generateAdapter = true)
data class CatalogueDevoirDto(
    @Json(name = "id") val id: String? = null,
    @Json(name = "titre") val titre: String? = null,
    @Json(name = "matiere") val matiere: String? = null,
    @Json(name = "niveau") val niveau: String? = null,
    @Json(name = "section") val section: String? = null,
    @Json(name = "type") val type: String? = null,
    @Json(name = "trimestre") val trimestre: String? = null,
    @Json(name = "duree") val duree: Int? = null,
    @Json(name = "difficulte") val difficulte: String? = null,
    @Json(name = "chapitres") val chapitres: List<String>? = null,
    @Json(name = "source") val source: String? = null,
    @Json(name = "version") val version: Int? = null,
    @Json(name = "tailleMo") val tailleMo: Double? = null,
    @Json(name = "sujetUrl") val sujetUrl: String? = null,
    @Json(name = "corrigeUrl") val corrigeUrl: String? = null,
    @Json(name = "sujetSizeBytes") val sujetSizeBytes: Long? = null,
    @Json(name = "corrigeSizeBytes") val corrigeSizeBytes: Long? = null,
    @Json(name = "sujetSha256") val sujetSha256: String? = null,
    @Json(name = "corrigeSha256") val corrigeSha256: String? = null
)

/**
 * Représentation Moshi des anciens fichiers JSON `assets/devoirs/ (JSON)` (sujets consultables en texte).
 */
@JsonClass(generateAdapter = true)
data class LegacyDevoirFileDto(
    @Json(name = "schemaVersion") val schemaVersion: Int = 1,
    @Json(name = "contentVersion") val contentVersion: Int = 1,
    @Json(name = "matiere") val matiere: String? = null,
    @Json(name = "devoirs") val devoirs: List<LegacyDevoirDto>? = null
)

@JsonClass(generateAdapter = true)
data class LegacyDevoirDto(
    @Json(name = "id") val id: String? = null,
    @Json(name = "titre") val titre: String? = null,
    @Json(name = "matiere") val matiere: String? = null,
    @Json(name = "niveau") val niveau: String? = null,
    @Json(name = "section") val section: String? = null,
    @Json(name = "type") val type: String? = null,
    @Json(name = "trimestre") val trimestre: String? = null,
    @Json(name = "duree") val duree: Int? = null,
    @Json(name = "difficulte") val difficulte: String? = null,
    @Json(name = "chapitres") val chapitres: List<String>? = null,
    @Json(name = "version") val version: Int? = null,
    @Json(name = "exercices") val exercices: List<LegacyExerciseDto>? = null
)

@JsonClass(generateAdapter = true)
data class LegacyExerciseDto(
    @Json(name = "numero") val numero: Int? = null,
    @Json(name = "titre") val titre: String? = null,
    @Json(name = "bareme") val bareme: String? = null,
    @Json(name = "enonce") val enonce: String? = null,
    @Json(name = "corrige") val corrige: String? = null
)

data class DevoirParseReport(
    val catalogueVersion: Int,
    val devoirs: List<DevoirItem>,
    val skippedCount: Int,
    val errors: List<String>
)

/**
 * Unificateur pur Kotlin des deux systèmes de contenu :
 * - Le catalogue PDF (`catalogue.json`) est la source principale.
 * - Les anciens sujets JSON (`assets/devoirs/ (JSON)`) sont migrés en devoirs « consultables en texte »
 *   uniquement lorsqu'aucun devoir PDF ne partage le même `id` (garantie zéro doublon).
 */
object DevoirCatalogueUnifier {

    fun unify(
        pdfDevoirs: List<DevoirItem>,
        legacyTextDevoirs: List<DevoirItem>
    ): List<DevoirItem> {
        val unifiedById = LinkedHashMap<String, DevoirItem>()

        // 1. Source principale : devoirs PDF (déduplication par id, version la plus récente conservée)
        for (pdf in pdfDevoirs) {
            val existing = unifiedById[pdf.id]
            if (existing == null || pdf.version >= existing.version) {
                unifiedById[pdf.id] = pdf
            }
        }

        // 2. Source de repli : anciens sujets texte uniquement s'ils n'ont pas de PDF (clé = id)
        for (legacy in legacyTextDevoirs) {
            if (!unifiedById.containsKey(legacy.id)) {
                unifiedById[legacy.id] = legacy
            }
        }

        return unifiedById.values.toList()
    }
}

/**
 * Parseur pur Kotlin (testable sur JVM sans Android) utilisant Moshi pour :
 * 1. décoder et valider `catalogue.json` (URL sujetUrl / corrigeUrl + SHA-256 optionnel),
 * 2. décoder et migrer les anciens sujets texte `assets/devoirs/ (JSON)`.
 * Tout fichier ou devoir invalide est ignoré et journalisé sans lever d'exception.
 */
class DevoirJsonParser(
    private val onError: (message: String, throwable: Throwable?) -> Unit = { _, _ -> }
) {
    private val moshi: Moshi = Moshi.Builder()
        .addLast(KotlinJsonAdapterFactory())
        .build()

    private val catalogueAdapter = moshi.adapter(CatalogueDto::class.java)
    private val legacyFileAdapter = moshi.adapter(LegacyDevoirFileDto::class.java)

    fun parseCatalogueJson(sourceName: String, jsonContent: String): DevoirParseReport {
        val errors = mutableListOf<String>()
        val catalogue = try {
            catalogueAdapter.fromJson(jsonContent)
        } catch (e: Exception) {
            val msg = "Catalogue JSON invalide ignoré ($sourceName) : ${e.message}"
            errors.add(msg)
            onError(msg, e)
            return DevoirParseReport(0, emptyList(), skippedCount = 1, errors = errors)
        }

        if (catalogue == null || catalogue.devoirs == null) {
            val msg = "Structure JSON manquante ('devoirs') dans $sourceName"
            errors.add(msg)
            onError(msg, null)
            return DevoirParseReport(0, emptyList(), skippedCount = 1, errors = errors)
        }

        val resolvedCatalogueVersion = catalogue.catalogueVersion.coerceAtLeast(1)
        val validDevoirs = mutableListOf<DevoirItem>()
        var skipped = 0

        catalogue.devoirs.forEachIndexed { index, dto ->
            val result = validateAndMapPdf(
                dto = dto,
                defaultVersion = resolvedCatalogueVersion
            )
            result.fold(
                onSuccess = { validDevoirs.add(it) },
                onFailure = { err ->
                    skipped++
                    val msg = "Devoir ignoré dans $sourceName [index=$index, id=${dto.id}] : ${err.message}"
                    errors.add(msg)
                    onError(msg, err)
                }
            )
        }

        return DevoirParseReport(
            catalogueVersion = resolvedCatalogueVersion,
            devoirs = validDevoirs,
            skippedCount = skipped,
            errors = errors
        )
    }

    /**
     * Parse un ancien fichier JSON texte (`assets/devoirs/ (JSON)`) vers des [DevoirItem]
     * consultables directement en texte (`sujetUrl = ""`, `exercises` renseignés).
     */
    fun parseLegacyTextDevoirsJson(sourceName: String, jsonContent: String): DevoirParseReport {
        val errors = mutableListOf<String>()
        val fileDto = try {
            legacyFileAdapter.fromJson(jsonContent)
        } catch (e: Exception) {
            val msg = "Fichier texte JSON invalide ignoré ($sourceName) : ${e.message}"
            errors.add(msg)
            onError(msg, e)
            return DevoirParseReport(0, emptyList(), skippedCount = 1, errors = errors)
        }

        if (fileDto == null || fileDto.devoirs == null) {
            val msg = "Liste 'devoirs' absente dans $sourceName"
            errors.add(msg)
            onError(msg, null)
            return DevoirParseReport(0, emptyList(), skippedCount = 1, errors = errors)
        }

        val defaultVersion = fileDto.contentVersion.coerceAtLeast(1)
        val fallbackSubject = fileDto.matiere?.trim().orEmpty()
        val validDevoirs = mutableListOf<DevoirItem>()
        var skipped = 0

        fileDto.devoirs.forEachIndexed { index, dto ->
            val result = validateAndMapLegacyText(
                dto = dto,
                fallbackSubject = fallbackSubject,
                defaultVersion = defaultVersion
            )
            result.fold(
                onSuccess = { validDevoirs.add(it) },
                onFailure = { err ->
                    skipped++
                    val msg = "Sujet texte ignoré dans $sourceName [index=$index, id=${dto.id}] : ${err.message}"
                    errors.add(msg)
                    onError(msg, err)
                }
            )
        }

        return DevoirParseReport(
            catalogueVersion = defaultVersion,
            devoirs = validDevoirs,
            skippedCount = skipped,
            errors = errors
        )
    }

    private fun validateAndMapPdf(
        dto: CatalogueDevoirDto,
        defaultVersion: Int
    ): Result<DevoirItem> {
        val id = dto.id?.trim().orEmpty()
        if (id.isEmpty()) return Result.failure(IllegalArgumentException("Champ 'id' manquant ou vide"))

        val title = dto.titre?.trim().orEmpty()
        if (title.isEmpty()) return Result.failure(IllegalArgumentException("Champ 'titre' manquant"))

        val subject = dto.matiere?.trim().orEmpty()
        if (subject.isEmpty()) return Result.failure(IllegalArgumentException("Champ 'matiere' manquant"))

        val level = parseStrictEnum(dto.niveau, CycleLevel.entries, CycleLevel.ALL) { it.id }
            ?: return Result.failure(IllegalArgumentException("Niveau invalide : '${dto.niveau}'"))

        val section = parseStrictEnum(dto.section, TunisianSection.entries, TunisianSection.ALL) { it.id }
            ?: return Result.failure(IllegalArgumentException("Section invalide : '${dto.section}'"))

        val type = parseStrictEnum(dto.type, DevoirType.entries, DevoirType.ALL) { it.id }
            ?: return Result.failure(IllegalArgumentException("Type d'épreuve invalide : '${dto.type}'"))

        val trimester = parseStrictEnum(dto.trimestre, Trimester.entries, Trimester.ALL) { it.id }
            ?: return Result.failure(IllegalArgumentException("Trimestre invalide : '${dto.trimestre}'"))

        val duration = dto.duree ?: 0
        if (duration <= 0) return Result.failure(IllegalArgumentException("Durée invalide : $duration"))

        val sujetUrl = dto.sujetUrl?.trim().orEmpty()
        if (!isValidHttpUrl(sujetUrl)) {
            return Result.failure(IllegalArgumentException("URL 'sujetUrl' manquante ou invalide : '$sujetUrl'"))
        }

        val rawCorrigeUrl = dto.corrigeUrl?.trim()?.takeIf { it.isNotEmpty() }
        if (rawCorrigeUrl != null && !isValidHttpUrl(rawCorrigeUrl)) {
            return Result.failure(IllegalArgumentException("URL 'corrigeUrl' invalide : '$rawCorrigeUrl'"))
        }

        val difficulty = dto.difficulte?.trim().takeUnless { it.isNullOrEmpty() } ?: "Standard"
        val source = dto.source?.trim().takeUnless { it.isNullOrEmpty() } ?: "Sujet d'entraînement"
        val resolvedVersion = (dto.version ?: defaultVersion).coerceAtLeast(1)
        val sujetBytes = (dto.sujetSizeBytes ?: 0L).coerceAtLeast(0L)
        val corrigeBytes = (dto.corrigeSizeBytes ?: 0L).coerceAtLeast(0L)
        val computedMb = if (sujetBytes + corrigeBytes > 0L) {
            (sujetBytes + corrigeBytes) / (1024.0 * 1024.0)
        } else {
            0.0
        }
        val sizeMb = (dto.tailleMo ?: computedMb).let { if (it.isFinite() && it >= 0.0) it else 0.0 }

        val chapters = dto.chapitres
            ?.map { it.trim() }
            ?.filter { it.isNotEmpty() }
            .orEmpty()

        return Result.success(
            DevoirItem(
                id = id,
                title = title,
                subject = subject,
                level = level,
                section = section,
                type = type,
                trimester = trimester,
                durationMinutes = duration,
                difficulty = difficulty,
                chapterTags = chapters,
                source = source,
                version = resolvedVersion,
                sizeMb = sizeMb,
                sujetUrl = sujetUrl,
                corrigeUrl = rawCorrigeUrl,
                sujetSizeBytes = sujetBytes,
                corrigeSizeBytes = corrigeBytes,
                sujetSha256 = normalizeOptionalSha256(dto.sujetSha256),
                corrigeSha256 = normalizeOptionalSha256(dto.corrigeSha256)
            )
        )
    }

    private fun validateAndMapLegacyText(
        dto: LegacyDevoirDto,
        fallbackSubject: String,
        defaultVersion: Int
    ): Result<DevoirItem> {
        val id = dto.id?.trim().orEmpty()
        if (id.isEmpty()) return Result.failure(IllegalArgumentException("Champ 'id' manquant ou vide"))

        val title = dto.titre?.trim().orEmpty()
        if (title.isEmpty()) return Result.failure(IllegalArgumentException("Champ 'titre' manquant"))

        val subject = dto.matiere?.trim().takeUnless { it.isNullOrEmpty() } ?: fallbackSubject
        if (subject.isEmpty()) return Result.failure(IllegalArgumentException("Champ 'matiere' manquant"))

        val level = parseStrictEnum(dto.niveau, CycleLevel.entries, CycleLevel.ALL) { it.id }
            ?: return Result.failure(IllegalArgumentException("Niveau invalide : '${dto.niveau}'"))

        val section = parseStrictEnum(dto.section, TunisianSection.entries, TunisianSection.ALL) { it.id }
            ?: return Result.failure(IllegalArgumentException("Section invalide : '${dto.section}'"))

        val type = parseStrictEnum(dto.type, DevoirType.entries, DevoirType.ALL) { it.id }
            ?: return Result.failure(IllegalArgumentException("Type d'épreuve invalide : '${dto.type}'"))

        val trimester = parseStrictEnum(dto.trimestre, Trimester.entries, Trimester.ALL) { it.id }
            ?: return Result.failure(IllegalArgumentException("Trimestre invalide : '${dto.trimestre}'"))

        val duration = dto.duree ?: 0
        if (duration <= 0) return Result.failure(IllegalArgumentException("Durée invalide : $duration"))

        val exercises = dto.exercices.orEmpty().mapIndexedNotNull { idx, ex ->
            val statement = ex.enonce?.trim().orEmpty()
            if (statement.isEmpty()) return@mapIndexedNotNull null
            DevoirExercise(
                number = (ex.numero ?: (idx + 1)).coerceAtLeast(1),
                title = ex.titre?.trim().takeUnless { it.isNullOrEmpty() } ?: "Exercice ${idx + 1}",
                points = ex.bareme?.trim().orEmpty(),
                statement = statement,
                solution = ex.corrige?.trim().orEmpty()
            )
        }
        if (exercises.isEmpty()) {
            return Result.failure(IllegalArgumentException("Aucun exercice texte valide"))
        }

        val difficulty = dto.difficulte?.trim().takeUnless { it.isNullOrEmpty() } ?: "Standard"
        val resolvedVersion = (dto.version ?: defaultVersion).coerceAtLeast(1)
        val chapters = dto.chapitres
            ?.map { it.trim() }
            ?.filter { it.isNotEmpty() }
            .orEmpty()

        return Result.success(
            DevoirItem(
                id = id,
                title = title,
                subject = subject,
                level = level,
                section = section,
                type = type,
                trimester = trimester,
                durationMinutes = duration,
                difficulty = difficulty,
                chapterTags = chapters,
                source = "Sujet d'entraînement",
                version = resolvedVersion,
                sizeMb = 0.0,
                sujetUrl = "",
                corrigeUrl = null,
                exercises = exercises
            )
        )
    }

    private fun normalizeOptionalSha256(raw: String?): String? =
        raw?.trim()?.lowercase()?.takeIf { it.isNotEmpty() }

    private fun isValidHttpUrl(url: String): Boolean =
        url.startsWith("https://", ignoreCase = true) || url.startsWith("http://", ignoreCase = true)

    private fun <T> parseStrictEnum(
        raw: String?,
        entries: List<T>,
        excluded: T,
        idSelector: (T) -> String
    ): T? {
        val normalized = raw?.trim()?.uppercase() ?: return null
        val match = entries.find { idSelector(it) == normalized } ?: return null
        return if (match == excluded) null else match
    }
}
