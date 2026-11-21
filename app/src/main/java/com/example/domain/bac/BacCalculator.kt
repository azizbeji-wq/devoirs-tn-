package com.example.domain.bac

/**
 * Pure calculation engine for the Tunisian Baccalaureate (zero Android dependencies).
 *
 * Rules supported:
 * 1. Trimester subject average: (dcWeight * DC + dsWeight * DS) / (dcWeight + dsWeight)
 *    with partial-grade fallback if only DC or only DS is filled.
 * 2. Annual average: weighted average of available trimesters (default T1=1, T2=2, T3=2).
 * 3. Exam average: weighted average of exam grades with section exam coefficients.
 * 4. General average (MG): annualAverage * (annualPercent / 100) + examAverage * (examPercent / 100).
 * 5. Formule Globale (FG): fgGeneralMultiplier * MG + sum(fgCoeff_i * examGrade_i).
 * 6. Reverse simulator: required exam average X to reach target MG given annual average and weights.
 * 7. Mention & Session classification.
 */
object BacCalculator {

    const val MIN_GRADE = 0.0
    const val MAX_GRADE = 20.0
    private const val EPSILON = 1e-9

    /**
     * Validates a raw text grade (accepts '.' or ',' decimal separator, range [0, 20]).
     */
    fun validateGrade(raw: String): GradeValidation {
        val trimmed = raw.trim()
        if (trimmed.isEmpty()) return GradeValidation.Empty

        val normalized = trimmed.replace(',', '.')
        val parsed = normalized.toDoubleOrNull()
            ?: return GradeValidation.Invalid("Nombre invalide")

        if (parsed.isNaN() || parsed.isInfinite()) {
            return GradeValidation.Invalid("Nombre invalide")
        }
        if (parsed < MIN_GRADE || parsed > MAX_GRADE) {
            return GradeValidation.Invalid("Doit être entre 0 et 20")
        }
        return GradeValidation.Valid(parsed)
    }

    /**
     * Parses a raw grade string into a Double in [0.0, 20.0], or null if blank or invalid.
     */
    fun parseGrade(raw: String): Double? = when (val result = validateGrade(raw)) {
        is GradeValidation.Valid -> result.value
        else -> null
    }

    /**
     * Parses a non-negative coefficient (e.g., for editable FG coefficients).
     */
    fun parseCoefficient(raw: String): Double? {
        val trimmed = raw.trim()
        if (trimmed.isEmpty()) return null
        val parsed = trimmed.replace(',', '.').toDoubleOrNull() ?: return null
        if (parsed.isNaN() || parsed.isInfinite() || parsed < 0.0) return null
        return parsed
    }

    /**
     * Computes a subject's average for a single trimester using DC and DS.
     * Standard Tunisian formula: (DC + 2 * DS) / 3.
     */
    fun computeSubjectTrimesterAverage(
        dc: Double?,
        ds: Double?,
        dcWeight: Double = 1.0,
        dsWeight: Double = 2.0
    ): Double? {
        val validDc = dc?.takeIf { it in MIN_GRADE..MAX_GRADE }
        val validDs = ds?.takeIf { it in MIN_GRADE..MAX_GRADE }
        return when {
            validDc != null && validDs != null -> {
                val totalW = dcWeight + dsWeight
                if (totalW <= 0.0) null else (validDc * dcWeight + validDs * dsWeight) / totalW
            }
            validDs != null -> validDs
            validDc != null -> validDc
            else -> null
        }
    }

    /**
     * Computes each trimester's weighted average and the overall annual average.
     */
    fun computeAnnualAverage(
        section: BacSectionConfig,
        annualInputs: Map<String, SubjectAnnualInputs>,
        weights: BacWeights
    ): AnnualCalculationResult {
        val trimesterAverages = mutableMapOf<TrimesterKey, Double?>()
        val subjectTrimesterAverages = mutableMapOf<TrimesterKey, Map<String, Double?>>()

        for (trimester in TrimesterKey.entries) {
            var weightedSum = 0.0
            var coeffSum = 0.0
            val perSubject = mutableMapOf<String, Double?>()

            for (subject in section.subjects) {
                val input = annualInputs[subject.id]?.forTrimester(trimester)
                val dc = input?.let { parseGrade(it.dcText) }
                val ds = input?.let { parseGrade(it.dsText) }
                val subjAvg = computeSubjectTrimesterAverage(dc, ds, weights.dcWeight, weights.dsWeight)
                perSubject[subject.id] = subjAvg

                if (subjAvg != null && subject.annualCoeff > 0.0) {
                    weightedSum += subjAvg * subject.annualCoeff
                    coeffSum += subject.annualCoeff
                }
            }

            subjectTrimesterAverages[trimester] = perSubject
            trimesterAverages[trimester] = if (coeffSum > 0.0) weightedSum / coeffSum else null
        }

        var annualWeightedSum = 0.0
        var annualWeightTotal = 0.0
        for (trimester in TrimesterKey.entries) {
            val avg = trimesterAverages[trimester]
            val tWeight = weights.weightFor(trimester)
            if (avg != null && tWeight > 0.0) {
                annualWeightedSum += avg * tWeight
                annualWeightTotal += tWeight
            }
        }

        val annualAverage = if (annualWeightTotal > 0.0) {
            annualWeightedSum / annualWeightTotal
        } else {
            null
        }

        return AnnualCalculationResult(
            trimesterAverages = trimesterAverages,
            subjectTrimesterAverages = subjectTrimesterAverages,
            annualAverage = annualAverage
        )
    }

    /**
     * Computes the Baccalaureate exam weighted average from exam grades.
     */
    fun computeExamAverage(
        section: BacSectionConfig,
        examGradeTexts: Map<String, String>
    ): ExamCalculationResult {
        var weightedSum = 0.0
        var coeffSum = 0.0

        for (subject in section.subjects) {
            val grade = examGradeTexts[subject.id]?.let { parseGrade(it) }
            if (grade != null && subject.examCoeff > 0.0) {
                weightedSum += grade * subject.examCoeff
                coeffSum += subject.examCoeff
            }
        }

        return ExamCalculationResult(
            examAverage = if (coeffSum > 0.0) weightedSum / coeffSum else null,
            weightedPoints = weightedSum,
            activeCoeffSum = coeffSum
        )
    }

    /**
     * Computes General Average (MG) = annualAverage * (annualPercent / 100) + examAverage * (examPercent / 100).
     * Percentages are normalized if they do not sum to 100, and null is returned if either component is missing.
     */
    fun computeGeneralAverage(
        annualAverage: Double?,
        examAverage: Double?,
        annualPercent: Double = 25.0,
        examPercent: Double = 75.0
    ): Double? {
        if (annualAverage == null || examAverage == null) return null
        if (annualPercent < 0.0 || examPercent < 0.0) return null
        val sum = annualPercent + examPercent
        if (sum <= 0.0) return null

        val normalizedAnnual = annualPercent / sum
        val normalizedExam = examPercent / sum
        return (annualAverage * normalizedAnnual + examAverage * normalizedExam)
            .coerceIn(MIN_GRADE, MAX_GRADE)
    }

    /**
     * Computes Formule Globale (FG) for university orientation:
     * FG = fgGeneralMultiplier * MG + sum(fgCoeff_i * examGrade_i).
     */
    fun computeFormuleGlobale(
        generalAverage: Double?,
        section: BacSectionConfig,
        examGradeTexts: Map<String, String>,
        customFgCoeffs: Map<String, Double> = emptyMap(),
        fgGeneralMultiplier: Double = 4.0
    ): FormuleGlobaleResult {
        val breakdown = mutableListOf<FgTermBreakdown>()
        val safeMultiplier = fgGeneralMultiplier.coerceAtLeast(0.0)
        val generalPart = if (generalAverage != null) generalAverage * safeMultiplier else 0.0

        breakdown.add(
            FgTermBreakdown(
                label = "Moyenne Générale (MG)",
                coeff = safeMultiplier,
                grade = generalAverage,
                contribution = generalPart
            )
        )

        var subjectsPart = 0.0
        for (subject in section.subjects) {
            val coeff = (customFgCoeffs[subject.id] ?: subject.fgCoeff).coerceAtLeast(0.0)
            if (coeff > 0.0) {
                val grade = examGradeTexts[subject.id]?.let { parseGrade(it) }
                val contribution = (grade ?: 0.0) * coeff
                subjectsPart += contribution
                breakdown.add(
                    FgTermBreakdown(
                        label = subject.name,
                        coeff = coeff,
                        grade = grade,
                        contribution = contribution
                    )
                )
            }
        }

        val totalScore = if (generalAverage != null) generalPart + subjectsPart else null

        return FormuleGlobaleResult(
            score = totalScore,
            generalPart = generalPart,
            subjectsPart = subjectsPart,
            breakdown = breakdown
        )
    }

    /**
     * Reverse simulator:
     * "Quelle moyenne me faut-il à l'examen pour obtenir targetGeneralAverage ?"
     */
    fun simulateRequiredExamAverage(
        targetGeneralAverage: Double?,
        annualAverage: Double?,
        annualPercent: Double = 25.0,
        examPercent: Double = 75.0
    ): ReverseSimulationResult {
        if (targetGeneralAverage == null ||
            targetGeneralAverage.isNaN() ||
            targetGeneralAverage < MIN_GRADE ||
            targetGeneralAverage > MAX_GRADE
        ) {
            return ReverseSimulationResult.InvalidTarget(
                "Saisis un objectif valide compris entre 0 et 20."
            )
        }

        if (annualAverage == null ||
            annualAverage.isNaN() ||
            annualAverage < MIN_GRADE ||
            annualAverage > MAX_GRADE
        ) {
            return ReverseSimulationResult.MissingAnnualAverage
        }

        val totalPercent = annualPercent + examPercent
        if (totalPercent <= 0.0 || examPercent <= 0.0) {
            return ReverseSimulationResult.Impossible(
                requiredExamAverage = Double.POSITIVE_INFINITY,
                maxReachableGeneral = annualAverage,
                message = "La part examen doit être supérieure à 0 % pour simuler une note d'examen."
            )
        }

        val pAnnual = annualPercent / totalPercent
        val pExam = examPercent / totalPercent
        val maxReachable = annualAverage * pAnnual + MAX_GRADE * pExam
        val required = (targetGeneralAverage - annualAverage * pAnnual) / pExam

        return when {
            required > MAX_GRADE + EPSILON -> ReverseSimulationResult.Impossible(
                requiredExamAverage = required,
                maxReachableGeneral = maxReachable,
                message = "Objectif impossible : il faudrait %.2f/20 à l'examen (> 20). Moyenne générale maximale atteignable : %.2f/20."
                    .format(required, maxReachable)
            )
            required <= MIN_GRADE + EPSILON -> ReverseSimulationResult.AlreadyAchieved(
                maxReachableGeneral = maxReachable
            )
            else -> ReverseSimulationResult.Possible(
                requiredExamAverage = required.coerceIn(MIN_GRADE, MAX_GRADE),
                maxReachableGeneral = maxReachable
            )
        }
    }

    /**
     * Determines the Tunisian Baccalaureate mention from the General Average.
     */
    fun determineMention(
        generalAverage: Double?,
        thresholds: MentionThresholds = MentionThresholds()
    ): BacMention {
        if (generalAverage == null) return BacMention.INCONNU
        return when {
            generalAverage + EPSILON >= thresholds.tresBien -> BacMention.TRES_BIEN
            generalAverage + EPSILON >= thresholds.bien -> BacMention.BIEN
            generalAverage + EPSILON >= thresholds.assezBien -> BacMention.ASSEZ_BIEN
            generalAverage + EPSILON >= thresholds.passable -> BacMention.PASSABLE
            else -> BacMention.AUCUNE
        }
    }

    /**
     * Determines the session status (Principale, Contrôle, or Ajourné).
     */
    fun determineSession(
        generalAverage: Double?,
        thresholds: SessionThresholds = SessionThresholds()
    ): BacSession {
        if (generalAverage == null) return BacSession.INCONNU
        return when {
            generalAverage + EPSILON >= thresholds.principaleMin -> BacSession.PRINCIPALE
            generalAverage + EPSILON >= thresholds.controleMin -> BacSession.CONTROLE
            else -> BacSession.REFUSE
        }
    }
}
