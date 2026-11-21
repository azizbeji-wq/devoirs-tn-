package com.example.data.local

import android.content.Context
import com.example.domain.bac.BacConfig
import com.example.domain.bac.BacSectionConfig
import com.example.domain.bac.BacSubjectConfig
import com.example.domain.bac.BacWeights
import com.example.domain.bac.MentionThresholds
import com.example.domain.bac.SessionThresholds
import org.json.JSONObject

/**
 * Loads assets/bac_config.json into pure Kotlin [BacConfig] models.
 */
object BacConfigLoader {

    private const val ASSET_FILE_NAME = "bac_config.json"

    fun load(context: Context): BacConfig {
        return try {
            val rawJson = context.assets.open(ASSET_FILE_NAME).bufferedReader().use { it.readText() }
            parseJson(rawJson)
        } catch (_: Exception) {
            fallbackConfig()
        }
    }

    internal fun parseJson(rawJson: String): BacConfig {
        val root = JSONObject(rawJson)
        val comment = root.optString(
            "_comment",
            "à valider avec les textes officiels du ministère"
        )
        val source = root.optString(
            "source",
            "Ministère de l'Éducation (Tunisie) & Guide de l'Orientation Universitaire"
        )
        val updatedAt = root.optString("dateMiseAJour", "").ifBlank {
            root.optString("updatedAt", "2025-09-01")
        }

        val weightsObj = root.optJSONObject("weights")
        val weights = BacWeights(
            annualPercent = weightsObj?.optDouble("annualPercent", 25.0) ?: 25.0,
            examPercent = weightsObj?.optDouble("examPercent", 75.0) ?: 75.0,
            dcWeight = weightsObj?.optDouble("dcWeight", 1.0) ?: 1.0,
            dsWeight = weightsObj?.optDouble("dsWeight", 2.0) ?: 2.0,
            trimester1Weight = weightsObj?.optDouble("trimester1Weight", 1.0) ?: 1.0,
            trimester2Weight = weightsObj?.optDouble("trimester2Weight", 2.0) ?: 2.0,
            trimester3Weight = weightsObj?.optDouble("trimester3Weight", 2.0) ?: 2.0
        )

        val fgMultiplier = root.optDouble("fgGeneralMultiplier", 4.0)

        val mentionsObj = root.optJSONObject("mentions")
        val mentions = MentionThresholds(
            passable = mentionsObj?.optDouble("passable", 10.0) ?: 10.0,
            assezBien = mentionsObj?.optDouble("assezBien", 12.0) ?: 12.0,
            bien = mentionsObj?.optDouble("bien", 14.0) ?: 14.0,
            tresBien = mentionsObj?.optDouble("tresBien", 16.0) ?: 16.0
        )

        val sessionsObj = root.optJSONObject("sessions")
        val sessions = SessionThresholds(
            principaleMin = sessionsObj?.optDouble("principaleMin", 10.0) ?: 10.0,
            controleMin = sessionsObj?.optDouble("controleMin", 7.0) ?: 7.0
        )

        val sectionsArray = root.optJSONArray("sections")
        val sections = mutableListOf<BacSectionConfig>()
        if (sectionsArray != null) {
            for (i in 0 until sectionsArray.length()) {
                val secObj = sectionsArray.getJSONObject(i)
                val subjectsArray = secObj.optJSONArray("subjects")
                val subjects = mutableListOf<BacSubjectConfig>()
                if (subjectsArray != null) {
                    for (j in 0 until subjectsArray.length()) {
                        val subObj = subjectsArray.getJSONObject(j)
                        subjects.add(
                            BacSubjectConfig(
                                id = subObj.getString("id"),
                                name = subObj.getString("name"),
                                annualCoeff = subObj.optDouble("annualCoeff", 1.0),
                                examCoeff = subObj.optDouble("examCoeff", 1.0),
                                fgCoeff = subObj.optDouble("fgCoeff", 0.0)
                            )
                        )
                    }
                }
                sections.add(
                    BacSectionConfig(
                        id = secObj.getString("id"),
                        title = secObj.getString("title"),
                        shortTitle = secObj.optString("shortTitle", secObj.getString("title")),
                        description = secObj.optString("description", ""),
                        subjects = subjects
                    )
                )
            }
        }

        return BacConfig(
            comment = comment,
            source = source,
            updatedAt = updatedAt,
            weights = weights,
            fgGeneralMultiplier = fgMultiplier,
            mentions = mentions,
            sessions = sessions,
            sections = sections.ifEmpty { fallbackConfig().sections }
        )
    }

    fun fallbackConfig(): BacConfig = BacConfig(
        comment = "Configuration de secours — à valider avec les textes officiels du ministère",
        sections = listOf(
            BacSectionConfig(
                id = "bac_maths",
                title = "Bac Mathématiques",
                shortTitle = "Maths",
                description = "Maths & Physique coeff 4",
                subjects = listOf(
                    BacSubjectConfig("maths", "Mathématiques", 4.0, 4.0, 2.0),
                    BacSubjectConfig("physique", "Physique-Chimie", 4.0, 4.0, 1.5),
                    BacSubjectConfig("info", "Informatique", 1.5, 1.5, 0.5),
                    BacSubjectConfig("francais", "Français", 1.0, 1.0, 1.0),
                    BacSubjectConfig("anglais", "Anglais", 1.0, 1.0, 1.0)
                )
            )
        )
    )
}
