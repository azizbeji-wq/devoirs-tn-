package com.example

import com.example.domain.bac.BacCalculator
import com.example.domain.bac.BacMention
import com.example.domain.bac.BacSectionConfig
import com.example.domain.bac.BacSession
import com.example.domain.bac.BacSubjectConfig
import com.example.domain.bac.BacWeights
import com.example.domain.bac.GradeValidation
import com.example.domain.bac.ReverseSimulationResult
import com.example.domain.bac.SubjectAnnualInputs
import com.example.domain.bac.TrimesterGradeInput
import com.example.domain.bac.TrimesterKey
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class BacCalculatorTest {

    private val sampleSection = BacSectionConfig(
        id = "bac_maths",
        title = "Bac Mathématiques",
        shortTitle = "Maths",
        description = "Test section",
        subjects = listOf(
            BacSubjectConfig("maths", "Mathématiques", annualCoeff = 4.0, examCoeff = 4.0, fgCoeff = 2.0),
            BacSubjectConfig("physique", "Physique-Chimie", annualCoeff = 4.0, examCoeff = 4.0, fgCoeff = 1.5),
            BacSubjectConfig("francais", "Français", annualCoeff = 1.0, examCoeff = 1.0, fgCoeff = 1.0),
            BacSubjectConfig("anglais", "Anglais", annualCoeff = 1.0, examCoeff = 1.0, fgCoeff = 1.0)
        )
    )

    // --- Validation & Parsing ---

    @Test
    fun validateGrade_acceptsDotCommaAndBoundaries() {
        assertEquals(GradeValidation.Valid(0.0), BacCalculator.validateGrade("0"))
        assertEquals(GradeValidation.Valid(20.0), BacCalculator.validateGrade("20"))
        assertEquals(GradeValidation.Valid(14.75), BacCalculator.validateGrade("14.75"))
        assertEquals(GradeValidation.Valid(14.75), BacCalculator.validateGrade(" 14,75 "))
        assertEquals(GradeValidation.Empty, BacCalculator.validateGrade("   "))
    }

    @Test
    fun validateGrade_rejectsOutOfRangeAndInvalidText() {
        assertTrue(BacCalculator.validateGrade("-0.5") is GradeValidation.Invalid)
        assertTrue(BacCalculator.validateGrade("20.01") is GradeValidation.Invalid)
        assertTrue(BacCalculator.validateGrade("25") is GradeValidation.Invalid)
        assertTrue(BacCalculator.validateGrade("abc") is GradeValidation.Invalid)
        assertNull(BacCalculator.parseGrade("21"))
        assertNull(BacCalculator.parseGrade("-1"))
    }

    // --- Subject Trimester & Annual Average ---

    @Test
    fun computeSubjectTrimesterAverage_normalAndPartialCases() {
        // (12 + 2*15) / 3 = 14.0
        assertEquals(14.0, BacCalculator.computeSubjectTrimesterAverage(12.0, 15.0)!!, 0.0001)
        // Missing DC -> uses DS
        assertEquals(16.0, BacCalculator.computeSubjectTrimesterAverage(null, 16.0)!!, 0.0001)
        // Missing DS -> uses DC
        assertEquals(11.5, BacCalculator.computeSubjectTrimesterAverage(11.5, null)!!, 0.0001)
        // Both missing -> null
        assertNull(BacCalculator.computeSubjectTrimesterAverage(null, null))
    }

    @Test
    fun computeAnnualAverage_withThreeTrimestersAndMissingSubjects() {
        val inputs = mapOf(
            "maths" to SubjectAnnualInputs(
                t1 = TrimesterGradeInput(dcText = "12", dsText = "15"), // 14.0
                t2 = TrimesterGradeInput(dcText = "16", dsText = "16"), // 16.0
                t3 = TrimesterGradeInput(dcText = "15", dsText = "18")  // 17.0
            ),
            "francais" to SubjectAnnualInputs(
                t1 = TrimesterGradeInput(dcText = "10", dsText = "10"), // 10.0
                t2 = TrimesterGradeInput(dcText = "12", dsText = "12"), // 12.0
                t3 = TrimesterGradeInput(dcText = "", dsText = "")      // missing in T3
            )
        )
        // T1: (14*4 + 10*1)/5 = 66/5 = 13.2
        // T2: (16*4 + 12*1)/5 = 76/5 = 15.2
        // T3: only maths present -> 17.0
        // Annual (weights 1, 2, 2): (13.2*1 + 15.2*2 + 17.0*2) / 5 = (13.2 + 30.4 + 34.0) / 5 = 77.6 / 5 = 15.52
        val result = BacCalculator.computeAnnualAverage(sampleSection, inputs, BacWeights())
        assertEquals(13.2, result.trimesterAverages[TrimesterKey.T1]!!, 0.001)
        assertEquals(15.2, result.trimesterAverages[TrimesterKey.T2]!!, 0.001)
        assertEquals(17.0, result.trimesterAverages[TrimesterKey.T3]!!, 0.001)
        assertEquals(15.52, result.annualAverage!!, 0.001)
    }

    @Test
    fun computeAnnualAverage_returnsNullWhenAllGradesMissing() {
        val result = BacCalculator.computeAnnualAverage(sampleSection, emptyMap(), BacWeights())
        assertNull(result.annualAverage)
        assertNull(result.trimesterAverages[TrimesterKey.T1])
    }

    // --- Exam Average & General Average ---

    @Test
    fun computeExamAverage_normalPartialAndBoundaryZeroTwenty() {
        val normal = BacCalculator.computeExamAverage(
            sampleSection,
            mapOf("maths" to "16,5", "physique" to "14.5", "francais" to "12")
        )
        // (16.5*4 + 14.5*4 + 12*1) / 9 = (66 + 58 + 12) / 9 = 136 / 9 = 15.1111...
        assertEquals(136.0 / 9.0, normal.examAverage!!, 0.0001)
        assertEquals(9.0, normal.activeCoeffSum, 0.0001)

        val allZero = BacCalculator.computeExamAverage(
            sampleSection,
            mapOf("maths" to "0", "physique" to "0")
        )
        assertEquals(0.0, allZero.examAverage!!, 0.0001)

        val allTwenty = BacCalculator.computeExamAverage(
            sampleSection,
            mapOf("maths" to "20", "physique" to "20", "francais" to "20", "anglais" to "20")
        )
        assertEquals(20.0, allTwenty.examAverage!!, 0.0001)

        val missing = BacCalculator.computeExamAverage(sampleSection, emptyMap())
        assertNull(missing.examAverage)
    }

    @Test
    fun computeGeneralAverage_default25_75AndCustomWeightsAndBoundaries() {
        // Default 25% annual + 75% exam: 14*0.25 + 16*0.75 = 3.5 + 12.0 = 15.5
        assertEquals(15.5, BacCalculator.computeGeneralAverage(14.0, 16.0, 25.0, 75.0)!!, 0.0001)

        // Custom 30% annual + 70% exam: 10*0.3 + 15*0.7 = 3 + 10.5 = 13.5
        assertEquals(13.5, BacCalculator.computeGeneralAverage(10.0, 15.0, 30.0, 70.0)!!, 0.0001)

        // Boundaries 0 and 20
        assertEquals(0.0, BacCalculator.computeGeneralAverage(0.0, 0.0, 25.0, 75.0)!!, 0.0001)
        assertEquals(20.0, BacCalculator.computeGeneralAverage(20.0, 20.0, 25.0, 75.0)!!, 0.0001)

        // Missing component returns null
        assertNull(BacCalculator.computeGeneralAverage(null, 15.0))
        assertNull(BacCalculator.computeGeneralAverage(15.0, null))
    }

    // --- Formule Globale (FG) ---

    @Test
    fun computeFormuleGlobale_usesDefaultAndCustomCoefficients() {
        val exams = mapOf(
            "maths" to "17",
            "physique" to "15",
            "francais" to "13",
            "anglais" to "14"
        )
        // MG = 15.0 -> 4*15 = 60
        // Subjects default: 2*17 (34) + 1.5*15 (22.5) + 1*13 (13) + 1*14 (14) = 83.5 -> FG = 143.5
        val defaultFg = BacCalculator.computeFormuleGlobale(
            generalAverage = 15.0,
            section = sampleSection,
            examGradeTexts = exams
        )
        assertEquals(143.5, defaultFg.score!!, 0.0001)

        // Custom override: maths fgCoeff = 2.5, fgGeneralMultiplier = 4.0 -> +8.5 = 152.0
        val customFg = BacCalculator.computeFormuleGlobale(
            generalAverage = 15.0,
            section = sampleSection,
            examGradeTexts = exams,
            customFgCoeffs = mapOf("maths" to 2.5),
            fgGeneralMultiplier = 4.0
        )
        assertEquals(152.0, customFg.score!!, 0.0001)
    }

    // --- Reverse Simulator ---

    @Test
    fun simulateRequiredExamAverage_normalPossibleCase() {
        // Annual = 12.0 (25% -> 3.0). Target MG = 15.0 -> need 12.0 from 75% -> 12.0 / 0.75 = 16.0
        val result = BacCalculator.simulateRequiredExamAverage(
            targetGeneralAverage = 15.0,
            annualAverage = 12.0,
            annualPercent = 25.0,
            examPercent = 75.0
        )
        assertTrue(result is ReverseSimulationResult.Possible)
        assertEquals(16.0, (result as ReverseSimulationResult.Possible).requiredExamAverage, 0.0001)
    }

    @Test
    fun simulateRequiredExamAverage_impossibleWhenExceeds20() {
        // Annual = 8.0 (25% -> 2.0). Max reachable = 2.0 + 15.0 = 17.0. Target = 18.0 -> required = 21.333...
        val result = BacCalculator.simulateRequiredExamAverage(
            targetGeneralAverage = 18.0,
            annualAverage = 8.0,
            annualPercent = 25.0,
            examPercent = 75.0
        )
        assertTrue(result is ReverseSimulationResult.Impossible)
        val impossible = result as ReverseSimulationResult.Impossible
        assertEquals(16.0 / 0.75, impossible.requiredExamAverage, 0.001)
        assertEquals(17.0, impossible.maxReachableGeneral, 0.001)
        assertTrue(impossible.message.contains("impossible", ignoreCase = true))
    }

    @Test
    fun simulateRequiredExamAverage_boundaryExact20AndAlreadyAchievedAndInvalid() {
        // Annual = 20, Target = 20 -> required = 20.0 (Possible)
        val exactTwenty = BacCalculator.simulateRequiredExamAverage(20.0, 20.0, 25.0, 75.0)
        assertTrue(exactTwenty is ReverseSimulationResult.Possible)
        assertEquals(20.0, (exactTwenty as ReverseSimulationResult.Possible).requiredExamAverage, 0.0001)

        // Annual = 16, Target = 3 -> annual contribution alone is 4.0 >= 3.0 -> AlreadyAchieved
        val already = BacCalculator.simulateRequiredExamAverage(3.0, 16.0, 25.0, 75.0)
        assertTrue(already is ReverseSimulationResult.AlreadyAchieved)

        // Missing annual average
        assertEquals(
            ReverseSimulationResult.MissingAnnualAverage,
            BacCalculator.simulateRequiredExamAverage(14.0, null, 25.0, 75.0)
        )

        // Invalid target > 20 or < 0
        assertTrue(BacCalculator.simulateRequiredExamAverage(21.0, 12.0) is ReverseSimulationResult.InvalidTarget)
        assertTrue(BacCalculator.simulateRequiredExamAverage(-1.0, 12.0) is ReverseSimulationResult.InvalidTarget)
    }

    // --- Mention & Session ---

    @Test
    fun determineMentionAndSession_allThresholds() {
        assertEquals(BacMention.INCONNU, BacCalculator.determineMention(null))
        assertEquals(BacSession.INCONNU, BacCalculator.determineSession(null))

        assertEquals(BacMention.AUCUNE, BacCalculator.determineMention(6.99))
        assertEquals(BacSession.REFUSE, BacCalculator.determineSession(6.99))

        assertEquals(BacMention.AUCUNE, BacCalculator.determineMention(7.0))
        assertEquals(BacSession.CONTROLE, BacCalculator.determineSession(7.0))
        assertEquals(BacSession.CONTROLE, BacCalculator.determineSession(9.99))

        assertEquals(BacMention.PASSABLE, BacCalculator.determineMention(10.0))
        assertEquals(BacSession.PRINCIPALE, BacCalculator.determineSession(10.0))

        assertEquals(BacMention.ASSEZ_BIEN, BacCalculator.determineMention(12.0))
        assertEquals(BacMention.BIEN, BacCalculator.determineMention(14.0))
        assertEquals(BacMention.TRES_BIEN, BacCalculator.determineMention(16.0))
        assertEquals(BacMention.TRES_BIEN, BacCalculator.determineMention(20.0))
    }
}
