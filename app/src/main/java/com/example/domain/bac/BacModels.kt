package com.example.domain.bac

/**
 * Pure Kotlin models for the Tunisian Baccalaureate calculator (no Android dependencies).
 * Coefficients & weights are loaded from assets/bac_config.json
 * (à valider avec les textes officiels du ministère).
 */

enum class TrimesterKey(val label: String, val shortLabel: String) {
    T1("Trimestre 1", "T1"),
    T2("Trimestre 2", "T2"),
    T3("Trimestre 3", "T3")
}

data class BacWeights(
    val annualPercent: Double = 25.0,
    val examPercent: Double = 75.0,
    val dcWeight: Double = 1.0,
    val dsWeight: Double = 2.0,
    val trimester1Weight: Double = 1.0,
    val trimester2Weight: Double = 2.0,
    val trimester3Weight: Double = 2.0
) {
    fun weightFor(trimester: TrimesterKey): Double = when (trimester) {
        TrimesterKey.T1 -> trimester1Weight
        TrimesterKey.T2 -> trimester2Weight
        TrimesterKey.T3 -> trimester3Weight
    }
}

data class MentionThresholds(
    val passable: Double = 10.0,
    val assezBien: Double = 12.0,
    val bien: Double = 14.0,
    val tresBien: Double = 16.0
)

data class SessionThresholds(
    val principaleMin: Double = 10.0,
    val controleMin: Double = 7.0
)

data class BacSubjectConfig(
    val id: String,
    val name: String,
    val annualCoeff: Double,
    val examCoeff: Double,
    val fgCoeff: Double
)

data class BacSectionConfig(
    val id: String,
    val title: String,
    val shortTitle: String,
    val description: String,
    val subjects: List<BacSubjectConfig>
)

data class BacConfig(
    val comment: String = "à valider avec les textes officiels du ministère",
    val source: String = "Ministère de l'Éducation (Tunisie) & Guide de l'Orientation Universitaire",
    val updatedAt: String = "2025-09-01",
    val weights: BacWeights = BacWeights(),
    val fgGeneralMultiplier: Double = 4.0,
    val mentions: MentionThresholds = MentionThresholds(),
    val sessions: SessionThresholds = SessionThresholds(),
    val sections: List<BacSectionConfig> = emptyList()
)

data class TrimesterGradeInput(
    val dcText: String = "",
    val dsText: String = ""
)

data class SubjectAnnualInputs(
    val t1: TrimesterGradeInput = TrimesterGradeInput(),
    val t2: TrimesterGradeInput = TrimesterGradeInput(),
    val t3: TrimesterGradeInput = TrimesterGradeInput()
) {
    fun forTrimester(trimester: TrimesterKey): TrimesterGradeInput = when (trimester) {
        TrimesterKey.T1 -> t1
        TrimesterKey.T2 -> t2
        TrimesterKey.T3 -> t3
    }

    fun updateTrimester(trimester: TrimesterKey, value: TrimesterGradeInput): SubjectAnnualInputs =
        when (trimester) {
            TrimesterKey.T1 -> copy(t1 = value)
            TrimesterKey.T2 -> copy(t2 = value)
            TrimesterKey.T3 -> copy(t3 = value)
        }
}

sealed class GradeValidation {
    data object Empty : GradeValidation()
    data class Valid(val value: Double) : GradeValidation()
    data class Invalid(val reason: String) : GradeValidation()
}

enum class BacMention(val label: String) {
    TRES_BIEN("Très Bien"),
    BIEN("Bien"),
    ASSEZ_BIEN("Assez Bien"),
    PASSABLE("Passable"),
    AUCUNE("Sans mention"),
    INCONNU("En attente")
}

enum class BacSession(val label: String) {
    PRINCIPALE("Session Principale · Admis"),
    CONTROLE("Session de Contrôle"),
    REFUSE("Ajourné (< 7/20)"),
    INCONNU("Saisie incomplète")
}

data class AnnualCalculationResult(
    val trimesterAverages: Map<TrimesterKey, Double?>,
    val subjectTrimesterAverages: Map<TrimesterKey, Map<String, Double?>>,
    val annualAverage: Double?
)

data class ExamCalculationResult(
    val examAverage: Double?,
    val weightedPoints: Double,
    val activeCoeffSum: Double
)

data class FgTermBreakdown(
    val label: String,
    val coeff: Double,
    val grade: Double?,
    val contribution: Double
)

data class FormuleGlobaleResult(
    val score: Double?,
    val generalPart: Double,
    val subjectsPart: Double,
    val breakdown: List<FgTermBreakdown>
)

sealed class ReverseSimulationResult {
    data class Possible(
        val requiredExamAverage: Double,
        val maxReachableGeneral: Double
    ) : ReverseSimulationResult()

    data class AlreadyAchieved(
        val maxReachableGeneral: Double
    ) : ReverseSimulationResult()

    data class Impossible(
        val requiredExamAverage: Double,
        val maxReachableGeneral: Double,
        val message: String
    ) : ReverseSimulationResult()

    data object MissingAnnualAverage : ReverseSimulationResult()

    data class InvalidTarget(val message: String) : ReverseSimulationResult()
}
