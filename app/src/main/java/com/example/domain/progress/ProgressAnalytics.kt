package com.example.domain.progress

import com.example.data.local.ExamAttemptEntity
import com.example.data.local.ExerciseCodec
import com.example.data.local.QcmResultEntity
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import java.util.TimeZone
import kotlin.math.roundToInt

data class GradePoint(
    val attemptId: Long,
    val subject: String,
    val devoirTitle: String,
    val scoreOn20: Double,
    val timestampMillis: Long,
    val shortDateLabel: String
)

data class WeakChapterInsight(
    val subject: String,
    val chapter: String,
    val averageOn20: Double,
    val evaluationsCount: Int,
    val severityLabel: String
)

data class StudyStreakSummary(
    val currentStreakDays: Int,
    val longestStreakDays: Int,
    val activeDaysCount: Int
)

/**
 * Moteur analytique pur Kotlin (sans dépendance Android) pour :
 * - l'évolution chronologique des notes ramenées sur 20 par matière,
 * - la détection des chapitres faibles (notes < seuil),
 * - le calcul de la série de jours d'étude (streak) et du temps cumulé.
 */
object ProgressAnalytics {

    const val WEAK_CHAPTER_THRESHOLD_ON_20 = 12.0
    private const val CRITICAL_THRESHOLD_ON_20 = 9.0

    fun normalizeTo20(score: Double, maxScore: Double): Double {
        if (maxScore <= 0.0) return 0.0
        return ((score / maxScore) * 20.0).coerceIn(0.0, 20.0)
    }

    fun buildGradeEvolutionBySubject(
        attempts: List<ExamAttemptEntity>,
        locale: Locale = Locale.FRENCH,
        timeZone: TimeZone = TimeZone.getDefault()
    ): Map<String, List<GradePoint>> {
        val formatter = SimpleDateFormat("dd/MM", locale).apply {
            this.timeZone = timeZone
        }
        return attempts
            .sortedBy { it.attemptedAtMillis }
            .groupBy { it.subject }
            .mapValues { (_, list) ->
                list.map { attempt ->
                    GradePoint(
                        attemptId = attempt.id,
                        subject = attempt.subject,
                        devoirTitle = attempt.devoirTitle,
                        scoreOn20 = roundToOneDecimal(normalizeTo20(attempt.totalScore, attempt.maxScore)),
                        timestampMillis = attempt.attemptedAtMillis,
                        shortDateLabel = formatter.format(Date(attempt.attemptedAtMillis))
                    )
                }
            }
    }

    fun detectWeakChapters(
        attempts: List<ExamAttemptEntity>,
        qcmResults: List<QcmResultEntity>,
        thresholdOn20: Double = WEAK_CHAPTER_THRESHOLD_ON_20
    ): List<WeakChapterInsight> {
        val bucket = linkedMapOf<Pair<String, String>, MutableList<Double>>()

        for (attempt in attempts) {
            val grade20 = normalizeTo20(attempt.totalScore, attempt.maxScore)
            val tags = ExerciseCodec.decodeTags(attempt.chapterTagsSerialized)
                .map { it.trim() }
                .filter { it.isNotEmpty() }
            for (tag in tags) {
                bucket.getOrPut(attempt.subject to tag) { mutableListOf() }.add(grade20)
            }
        }

        for (qcm in qcmResults) {
            if (qcm.totalQuestions <= 0) continue
            val grade20 = normalizeTo20(qcm.score.toDouble(), qcm.totalQuestions.toDouble())
            bucket.getOrPut(qcm.subject to qcm.chapterName) { mutableListOf() }.add(grade20)
        }

        return bucket.entries
            .map { (key, scores) ->
                val avg = scores.average()
                WeakChapterInsight(
                    subject = key.first,
                    chapter = key.second,
                    averageOn20 = roundToOneDecimal(avg),
                    evaluationsCount = scores.size,
                    severityLabel = if (avg < CRITICAL_THRESHOLD_ON_20) "Prioritaire" else "À renforcer"
                )
            }
            .filter { it.averageOn20 < thresholdOn20 }
            .sortedWith(compareBy<WeakChapterInsight> { it.averageOn20 }.thenByDescending { it.evaluationsCount })
    }

    fun computeStudyStreak(
        activityTimestampsMillis: List<Long>,
        nowMillis: Long = System.currentTimeMillis(),
        timeZone: TimeZone = TimeZone.getDefault()
    ): StudyStreakSummary {
        if (activityTimestampsMillis.isEmpty()) {
            return StudyStreakSummary(0, 0, 0)
        }

        val uniqueDayKeys = activityTimestampsMillis
            .map { toEpochDay(it, timeZone) }
            .distinct()
            .sorted()

        var longest = 1
        var running = 1
        for (i in 1 until uniqueDayKeys.size) {
            if (uniqueDayKeys[i] == uniqueDayKeys[i - 1] + 1L) {
                running++
                if (running > longest) longest = running
            } else {
                running = 1
            }
        }

        val todayKey = toEpochDay(nowMillis, timeZone)
        val daySet = uniqueDayKeys.toHashSet()
        val anchorDay = when {
            todayKey in daySet -> todayKey
            (todayKey - 1L) in daySet -> todayKey - 1L
            else -> null
        }

        var current = 0
        if (anchorDay != null) {
            var cursor = anchorDay
            while (cursor in daySet) {
                current++
                cursor--
            }
        }

        return StudyStreakSummary(
            currentStreakDays = current,
            longestStreakDays = longest,
            activeDaysCount = uniqueDayKeys.size
        )
    }

    fun computeTotalStudySeconds(
        attempts: List<ExamAttemptEntity>,
        qcmResults: List<QcmResultEntity>
    ): Long {
        val examSeconds = attempts.sumOf { it.actualDurationSeconds.coerceAtLeast(0).toLong() }
        val qcmSeconds = qcmResults.sumOf { it.durationSeconds.coerceAtLeast(0).toLong() }
        return examSeconds + qcmSeconds
    }

    fun computeAverageGradeOn20(attempts: List<ExamAttemptEntity>): Double? {
        if (attempts.isEmpty()) return null
        val avg = attempts.map { normalizeTo20(it.totalScore, it.maxScore) }.average()
        return roundToOneDecimal(avg)
    }

    fun formatStudyDuration(totalSeconds: Long): String {
        val safe = totalSeconds.coerceAtLeast(0L)
        val hours = safe / 3600
        val minutes = (safe % 3600) / 60
        return when {
            hours > 0 -> "${hours}h ${minutes}m"
            minutes > 0 -> "${minutes} min"
            safe > 0 -> "< 1 min"
            else -> "0 min"
        }
    }

    private fun toEpochDay(millis: Long, timeZone: TimeZone): Long {
        val cal = Calendar.getInstance(timeZone).apply {
            timeInMillis = millis
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }
        // Utilisation d'un calendrier UTC sur la date locale (année, mois, jour) pour éviter les décalages DST
        val utc = Calendar.getInstance(TimeZone.getTimeZone("UTC")).apply {
            clear()
            set(
                cal.get(Calendar.YEAR),
                cal.get(Calendar.MONTH),
                cal.get(Calendar.DAY_OF_MONTH)
            )
        }
        return utc.timeInMillis / 86_400_000L
    }

    private fun roundToOneDecimal(value: Double): Double =
        (value * 10.0).roundToInt() / 10.0
}
