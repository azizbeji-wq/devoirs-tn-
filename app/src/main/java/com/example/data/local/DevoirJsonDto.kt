package com.example.data.local

import com.example.data.model.CycleLevel
import com.example.data.model.DevoirItem
import com.example.data.model.DevoirType
import com.example.data.model.Trimester
import com.example.data.model.TunisianSection
import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass
import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory

/**
 * Représentation Moshi du fichier distant ou mis en cache `catalogue.json`.
 */
@JsonClass(generateAdapter = true)
data class CatalogueDto(
    @Json(name = "schemaVersion") val schemaVersion: Int = 3,
    @Json(name = "catalogueVersion") val catalogueVersion: Int = 1,
    @Json(name = "devoirs") val devoirs: List<CatalogueDevoirDto>? = null
)

/**
 * Métadonnées d'un devoir PDF hébergé en ligne telles que définies dans `catalogue.json`.
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

data class DevoirParseReport(
    val catalogueVersion: Int,
    val devoirs: List<DevoirItem>,
    val skippedCount: Int,
    val errors: List<String>
)

/**
 * Parseur pur Kotlin (testable sur JVM sans Android) utilisant Moshi
 * pour décoder et valider `catalogue.json` (URL sujetUrl / corrigeUrl + intégrité).
 * Tout fichier ou devoir invalide est ignoré et journalisé sans lever d'exception.
 */
class DevoirJsonParser(
    private val onError: (message: String, throwable: Throwable?) -> Unit = { _, _ -> }
) {
    private val moshi: Moshi = Moshi.Builder()
        .addLast(KotlinJsonAdapterFactory())
        .build()

    private val catalogueAdapter = moshi.adapter(CatalogueDto::class.java)

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
            val result = validateAndMap(
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

    private fun validateAndMap(
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
                sujetSha256 = dto.sujetSha256?.trim()?.lowercase()?.takeIf { it.isNotEmpty() },
                corrigeSha256 = dto.corrigeSha256?.trim()?.lowercase()?.takeIf { it.isNotEmpty() }
            )
        )
    }

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
