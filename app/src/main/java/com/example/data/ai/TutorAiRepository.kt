package com.example.data.ai

import android.content.Context
import android.graphics.Bitmap
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.util.Log
import com.example.data.local.TutorMessageDao
import com.example.data.local.TutorMessageEntity
import com.example.data.model.DevoirExercise
import com.example.data.model.DevoirItem
import com.google.firebase.Firebase
import com.google.firebase.ai.ai
import com.google.firebase.ai.type.content
import java.io.IOException
import java.net.UnknownHostException
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeout

private const val TAG = "TutorAiRepository"
private const val MODEL_NAME = "gemini-3.8-flash"
private const val INFERENCE_TIMEOUT_MS = 60_000L

enum class TutorLanguage(val code: String, val label: String, val isRtl: Boolean) {
    FRENCH("FR", "Français", false),
    ARABIC("AR", "العربية", true);

    companion object {
        fun fromCode(code: String): TutorLanguage = entries.find { it.code == code } ?: FRENCH
    }
}

enum class TutorHelpMode(val id: String, val labelFr: String, val labelAr: String) {
    EXPLAIN("EXPLAIN", "Explication", "شرح الصفحة"),
    HINT_1("HINT_1", "Indice 1", "تلميح 1"),
    HINT_2("HINT_2", "Indice 2", "تلميح 2"),
    SOLUTION("SOLUTION", "Solution complète", "الحل الكامل"),
    CHAT("CHAT", "Question", "سؤال");

    fun label(language: TutorLanguage): String =
        if (language == TutorLanguage.ARABIC) labelAr else labelFr

    companion object {
        fun fromId(id: String): TutorHelpMode = entries.find { it.id == id } ?: CHAT
    }
}

sealed class TutorErrorException(message: String, cause: Throwable? = null) : Exception(message, cause) {
    class NoNetwork(message: String) : TutorErrorException(message)
    class QuotaExceeded(message: String, cause: Throwable? = null) : TutorErrorException(message, cause)
    class EmptyResponse(message: String) : TutorErrorException(message)
    class Timeout(message: String, cause: Throwable? = null) : TutorErrorException(message, cause)
    class ServiceError(message: String, cause: Throwable? = null) : TutorErrorException(message, cause)
}

/**
 * Dépôt du tuteur IA Gemini (via Firebase AI Logic & App Check, sans clé API client).
 *
 * Prend en charge :
 * - l'analyse multimodale d'une page PDF redimensionnée (« Explique cette page »),
 * - le mode d'aide progressif (Indice 1, Indice 2, Solution complète),
 * - les réponses en français ou en arabe (RTL),
 * - la gestion du réseau, du quota (HTTP 429 / RESOURCE_EXHAUSTED) et des timeouts,
 * - la persistance de l'historique des explications par devoir et par page dans Room.
 */
class TutorAiRepository(
    private val appContext: Context,
    private val tutorMessageDao: TutorMessageDao
) {
    companion object {
        /** Prompt système pédagogique conforme au cahier des charges tunisien. */
        const val SYSTEM_PROMPT =
            "Tu es un tuteur bienveillant pour élèves tunisiens, tu suis les programmes officiels, " +
                "tu expliques étape par étape, tu proposes d'abord des indices avant la solution.\n" +
                "Tu utilises les notations scolaires tunisiennes, tu ne donnes jamais de réponse sans justification, " +
                "et si un passage de l'image est flou ou incertain, tu le signales honnêtement.\n" +
                "Règle d'affichage mathématique obligatoire : écris toutes les formules en texte clair et lisible " +
                "avec des symboles Unicode standards (√, ², ³, ×, ÷, ≤, ≥, ≠, ⇒, ⇔, →, ∈, ℝ, ℂ, π, Δ, τ, ∫). " +
                "N'utilise JAMAIS de balises LaTeX brutes comme \\frac, \\sqrt, \\times, $, $$ ou \\[ \\]."
    }

    // Initialisé paresseusement après configureAppCheck() dans MainActivity.onCreate()
    private val generativeModel by lazy {
        Firebase.ai.generativeModel(
            modelName = MODEL_NAME,
            systemInstruction = content {
                text(SYSTEM_PROMPT)
            }
        )
    }

    fun observeConversation(devoirId: String, exerciseNumber: Int): Flow<List<TutorMessageEntity>> =
        tutorMessageDao.observeConversation(devoirId, exerciseNumber)

    fun observeDevoirHistory(devoirId: String): Flow<List<TutorMessageEntity>> =
        tutorMessageDao.observeDevoirHistory(devoirId)

    fun observeDevoirExplanationCount(devoirId: String): Flow<Int> =
        tutorMessageDao.observeDevoirExplanationCount(devoirId)

    suspend fun clearConversation(devoirId: String, exerciseNumber: Int) {
        tutorMessageDao.clearConversation(devoirId, exerciseNumber)
    }

    suspend fun clearDevoirHistory(devoirId: String) {
        tutorMessageDao.clearDevoirHistory(devoirId)
    }

    suspend fun saveMessage(
        devoirId: String,
        exerciseNumber: Int,
        role: String,
        mode: TutorHelpMode,
        language: TutorLanguage,
        contentText: String
    ) {
        tutorMessageDao.insertMessage(
            TutorMessageEntity(
                devoirId = devoirId,
                exerciseNumber = exerciseNumber,
                role = role,
                mode = mode.id,
                language = language.code,
                content = contentText
            )
        )
    }

    fun buildUserDisplayLabel(
        mode: TutorHelpMode,
        language: TutorLanguage,
        question: String? = null,
        pageNumber: Int? = null
    ): String {
        if (!question.isNullOrBlank()) return question.trim()
        val pageSuffixFr = pageNumber?.let { " (Page $it)" }.orEmpty()
        val pageSuffixAr = pageNumber?.let { " (الصفحة $it)" }.orEmpty()
        return when (mode) {
            TutorHelpMode.EXPLAIN -> if (language == TutorLanguage.ARABIC) {
                "اشرح هذه الصفحة خطوة بخطوة$pageSuffixAr"
            } else {
                "Explique cette page étape par étape$pageSuffixFr"
            }
            TutorHelpMode.HINT_1 -> if (language == TutorLanguage.ARABIC) {
                "أعطني التلميح الأول (بدون الحل)$pageSuffixAr"
            } else {
                "Donne-moi l'Indice 1 (sans la solution)$pageSuffixFr"
            }
            TutorHelpMode.HINT_2 -> if (language == TutorLanguage.ARABIC) {
                "أعطني التلميح الثاني (بدون الإجابة النهائية)$pageSuffixAr"
            } else {
                "Donne-moi l'Indice 2 (sans la réponse finale)$pageSuffixFr"
            }
            TutorHelpMode.SOLUTION -> if (language == TutorLanguage.ARABIC) {
                "أريد الحل الكامل والمفصّل الآن$pageSuffixAr"
            } else {
                "Je demande maintenant la solution complète et justifiée$pageSuffixFr"
            }
            TutorHelpMode.CHAT -> question.orEmpty()
        }
    }

    /**
     * Envoie l'image redimensionnée de la page PDF courante à Gemini et streame l'explication
     * en temps réel (mode progressif, FR ou AR).
     */
    fun streamPageTutorResponse(
        devoir: DevoirItem,
        pageNumber: Int,
        documentKindLabel: String,
        pageBitmap: Bitmap,
        mode: TutorHelpMode,
        language: TutorLanguage,
        userQuestion: String? = null
    ): Flow<String> = flow {
        if (!isNetworkAvailable()) {
            throw TutorErrorException.NoNetwork(noNetworkMessage(language))
        }

        val recentHistory = tutorMessageDao
            .getConversationSnapshot(devoir.id, pageNumber)
            .takeLast(8)

        val prompt = buildPdfPagePrompt(
            devoir = devoir,
            pageNumber = pageNumber,
            documentKindLabel = documentKindLabel,
            mode = mode,
            language = language,
            userQuestion = userQuestion,
            history = recentHistory
        )

        // Réduction et compression JPEG de l'image avant envoi (ne recycle pas pageBitmap)
        val preparedBitmap = withContext(Dispatchers.Default) {
            PdfPageImageScaler.prepareBitmapForGemini(pageBitmap)
        }

        val accumulated = StringBuilder()
        try {
            val multimodalInput = content {
                image(preparedBitmap)
                text(prompt)
            }
            withTimeout(INFERENCE_TIMEOUT_MS) {
                generativeModel.generateContentStream(multimodalInput).collect { chunk ->
                    val piece = chunk.text.orEmpty()
                    if (piece.isNotEmpty()) {
                        accumulated.append(piece)
                        emit(MathTextFormatter.formatReadableMath(accumulated.toString()))
                    }
                }
            }
        } catch (e: TimeoutCancellationException) {
            Log.e(TAG, "Timeout Gemini multimodal (page $pageNumber)", e)
            throw TutorErrorException.Timeout(timeoutMessage(language), e)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Log.e(TAG, "Erreur Gemini multimodal (page $pageNumber)", e)
            throw mapException(e, language)
        } finally {
            if (preparedBitmap !== pageBitmap && !preparedBitmap.isRecycled) {
                preparedBitmap.recycle()
            }
        }

        val finalFormatted = MathTextFormatter.formatReadableMath(accumulated.toString())
        if (finalFormatted.isBlank()) {
            throw TutorErrorException.EmptyResponse(emptyResponseMessage(language))
        }
    }

    /**
     * Streame la réponse Gemini sur le contexte textuel d'un devoir/exercice.
     */
    fun streamTutorResponse(
        devoir: DevoirItem,
        exercise: DevoirExercise,
        mode: TutorHelpMode,
        language: TutorLanguage,
        userQuestion: String? = null
    ): Flow<String> = flow {
        if (!isNetworkAvailable()) {
            throw TutorErrorException.NoNetwork(noNetworkMessage(language))
        }

        val recentHistory = tutorMessageDao
            .getConversationSnapshot(devoir.id, exercise.number)
            .takeLast(8)

        val prompt = buildContextualPrompt(
            devoir = devoir,
            exercise = exercise,
            mode = mode,
            language = language,
            userQuestion = userQuestion,
            history = recentHistory
        )

        val accumulated = StringBuilder()
        try {
            withTimeout(INFERENCE_TIMEOUT_MS) {
                generativeModel.generateContentStream(prompt).collect { chunk ->
                    val piece = chunk.text.orEmpty()
                    if (piece.isNotEmpty()) {
                        accumulated.append(piece)
                        emit(MathTextFormatter.formatReadableMath(accumulated.toString()))
                    }
                }
            }
        } catch (e: TimeoutCancellationException) {
            Log.e(TAG, "Gemini streaming timed out", e)
            throw TutorErrorException.Timeout(timeoutMessage(language), e)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Log.e(TAG, "Gemini streaming failed", e)
            throw mapException(e, language)
        }

        val finalFormatted = MathTextFormatter.formatReadableMath(accumulated.toString())
        if (finalFormatted.isBlank()) {
            throw TutorErrorException.EmptyResponse(emptyResponseMessage(language))
        }
    }

    private fun buildPdfPagePrompt(
        devoir: DevoirItem,
        pageNumber: Int,
        documentKindLabel: String,
        mode: TutorHelpMode,
        language: TutorLanguage,
        userQuestion: String?,
        history: List<TutorMessageEntity>
    ): String {
        val languageDirective = languageDirective(language)
        val modeDirective = when (mode) {
            TutorHelpMode.EXPLAIN ->
                "Tâche : Analyse l'image de cette page $pageNumber ($documentKindLabel) du devoir tunisien.\n" +
                    "1. Résume brièvement les notions du programme officiel tunisien mobilisées sur cette page.\n" +
                    "2. Propose d'abord des INDICES progressifs (sans donner immédiatement le résultat final) pour guider l'élève étape par étape.\n" +
                    "3. Indique à l'élève qu'il peut cliquer sur « Indice 2 » ou « Solution complète » s'il souhaite aller plus loin."
            TutorHelpMode.HINT_1 ->
                "Tâche : Mode d'aide progressif — INDICE 1 uniquement à partir de cette page PDF.\n" +
                    "INTERDICTION ABSOLUE de donner la solution complète, les calculs finaux ou le résultat final.\n" +
                    "Donne uniquement une première piste douce : quelle notion du cours mobiliser et par quelle première étape démarrer."
            TutorHelpMode.HINT_2 ->
                "Tâche : Mode d'aide progressif — INDICE 2 (plus précis que l'Indice 1) à partir de cette page PDF.\n" +
                    "INTERDICTION ABSOLUE de donner la réponse numérique ou finale complète.\n" +
                    "Donne la formule clé ou la mise en équation intermédiaire qui débloque l'élève et laisse-lui le dernier pas."
            TutorHelpMode.SOLUTION ->
                "Tâche : L'élève demande maintenant explicitement la SOLUTION COMPLÈTE de cette page PDF.\n" +
                    "Rédige la solution complète, structurée, justifiée étape par étape selon les exigences du barème tunisien."
            TutorHelpMode.CHAT ->
                "Tâche : Réponds précisément à la question de l'élève ci-dessous en t'appuyant sur l'image de cette page PDF. " +
                    "Justifie chaque étape clairement."
        }

        val historyBlock = formatHistoryBlock(history)
        val questionBlock = if (!userQuestion.isNullOrBlank()) {
            "\nQuestion de l'élève : ${userQuestion.trim()}\n"
        } else {
            ""
        }

        return buildString {
            appendLine("=== Contexte du Devoir PDF (Programme Officiel Tunisien) ===")
            appendLine("Titre : ${devoir.title}")
            appendLine("Niveau : ${devoir.level.label}")
            appendLine("Section : ${devoir.section.label}")
            appendLine("Matière : ${devoir.subject}")
            appendLine("Type : ${devoir.type.label} (${devoir.trimester.label})")
            appendLine("Document affiché : $documentKindLabel — Page $pageNumber")
            if (devoir.chapterTags.isNotEmpty()) {
                appendLine("Chapitres : ${devoir.chapterTags.joinToString(", ")}")
            }
            append(historyBlock)
            appendLine()
            appendLine("=== Consignes de réponse ===")
            appendLine(languageDirective)
            appendLine(modeDirective)
            append(questionBlock)
            appendLine("Rappel : pas de LaTeX brut (utilise √, ², ×, ≤, ≥, ⇒, etc.), reste clair, bienveillant et structuré.")
        }
    }

    private fun buildContextualPrompt(
        devoir: DevoirItem,
        exercise: DevoirExercise,
        mode: TutorHelpMode,
        language: TutorLanguage,
        userQuestion: String?,
        history: List<TutorMessageEntity>
    ): String {
        val languageDirective = languageDirective(language)
        val modeDirective = when (mode) {
            TutorHelpMode.EXPLAIN ->
                "Tâche : Explique ce devoir étape par étape pour le niveau « ${devoir.level.label} — ${devoir.section.label} ». " +
                    "Propose d'abord des indices méthodologiques avant d'aller vers la solution, et signale les pièges classiques."
            TutorHelpMode.HINT_1 ->
                "Tâche : Mode aide progressif — INDICE 1 uniquement.\n" +
                    "INTERDICTION ABSOLUE de donner la solution complète, les calculs finaux ou le résultat final.\n" +
                    "Donne uniquement une première piste douce : quelle notion du cours tunisien mobiliser et par quelle première idée commencer."
            TutorHelpMode.HINT_2 ->
                "Tâche : Mode aide progressif — INDICE 2 (plus précis que l'Indice 1).\n" +
                    "INTERDICTION ABSOLUE de donner la réponse numérique/finale complète.\n" +
                    "Donne la formule clé ou la mise en équation intermédiaire qui débloque l'élève et laisse-lui le dernier pas de calcul."
            TutorHelpMode.SOLUTION ->
                "Tâche : L'élève demande maintenant explicitement la SOLUTION COMPLÈTE.\n" +
                    "Rédige la solution complète, structurée, justifiée étape par étape selon les exigences du barème tunisien."
            TutorHelpMode.CHAT ->
                "Tâche : Réponds précisément à la question de l'élève ci-dessous en te basant sur le devoir. " +
                    "Justifie chaque étape clairement."
        }

        val historyBlock = formatHistoryBlock(history)
        val questionBlock = if (!userQuestion.isNullOrBlank()) {
            "\nQuestion de l'élève : ${userQuestion.trim()}\n"
        } else {
            ""
        }

        return buildString {
            appendLine("=== Contexte du Devoir (Régime Tunisien) ===")
            appendLine("Niveau : ${devoir.level.label}")
            appendLine("Section : ${devoir.section.label}")
            appendLine("Matière : ${devoir.subject}")
            appendLine("Type : ${devoir.type.label} (${devoir.trimester.label})")
            appendLine("Chapitres : ${devoir.chapterTags.joinToString(", ")}")
            appendLine()
            appendLine("=== Exercice ${exercise.number} : ${exercise.title} (${exercise.points}) ===")
            appendLine("Énoncé :")
            appendLine(exercise.statement)
            appendLine()
            appendLine("Information corrigé :")
            appendLine(exercise.solution)
            append(historyBlock)
            appendLine()
            appendLine("=== Consignes de réponse ===")
            appendLine(languageDirective)
            appendLine(modeDirective)
            append(questionBlock)
            appendLine("Rappel : pas de LaTeX brut (utilise √, ², ×, ≤, ≥, ⇒, etc.), reste concis et structuré.")
        }
    }

    private fun languageDirective(language: TutorLanguage): String = if (language == TutorLanguage.ARABIC) {
        "Langue de réponse obligatoire : ARABE clair et pédagogique (العربية الفصحى المبسّطة المستعملة في البرامج التونسية). " +
            "Tu peux garder les symboles mathématiques standards (x, f(x), u_n, τ, R, C)."
    } else {
        "Langue de réponse obligatoire : FRANÇAIS clair et pédagogique conforme aux programmes tunisiens."
    }

    private fun formatHistoryBlock(history: List<TutorMessageEntity>): String = if (history.isNotEmpty()) {
        val formattedHistory = history.joinToString("\n") { msg ->
            val speaker = if (msg.role == "user") "Élève" else "Tuteur"
            "$speaker [${msg.mode}]: ${msg.content.take(450)}"
        }
        "\n--- Historique récent ---\n$formattedHistory\n"
    } else {
        ""
    }

    private fun noNetworkMessage(language: TutorLanguage): String = if (language == TutorLanguage.ARABIC) {
        "لا يوجد اتصال بالإنترنت حاليا. الفروض المحمّلة تبقى متاحة للمراجعة دون اتصال."
    } else {
        "Aucune connexion Internet détectée. Tes devoirs PDF téléchargés restent accessibles hors ligne."
    }

    private fun timeoutMessage(language: TutorLanguage): String = if (language == TutorLanguage.ARABIC) {
        "استغرق الرد وقتا طويلا (أكثر من 60 ثانية). اضغط على إعادة المحاولة."
    } else {
        "La réponse a pris trop de temps (60 s). Appuie sur Réessayer."
    }

    private fun emptyResponseMessage(language: TutorLanguage): String = if (language == TutorLanguage.ARABIC) {
        "أرجع المساعد إجابة فارغة. يرجى إعادة المحاولة."
    } else {
        "Le tuteur a renvoyé une réponse vide. Appuie sur Réessayer."
    }

    private fun isNetworkAvailable(): Boolean {
        return try {
            val cm = appContext.getSystemService(Context.CONNECTIVITY_SERVICE) as? ConnectivityManager
                ?: return true
            val network = cm.activeNetwork ?: return false
            val caps = cm.getNetworkCapabilities(network) ?: return false
            caps.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
        } catch (_: Exception) {
            true
        }
    }

    private fun mapException(e: Exception, language: TutorLanguage): TutorErrorException {
        val rawMsg = (e.message ?: e.cause?.message ?: "").lowercase()
        val isArabic = language == TutorLanguage.ARABIC

        return when {
            e is UnknownHostException || e is IOException ||
                rawMsg.contains("unable to resolve host") ||
                rawMsg.contains("network") ||
                rawMsg.contains("offline") ||
                rawMsg.contains("connect") -> {
                TutorErrorException.NoNetwork(
                    if (isArabic) {
                        "تعذّر الاتصال بالخادم. تحقق من اتصالك بالإنترنت ثم أعد المحاولة."
                    } else {
                        "Impossible de joindre le service IA. Vérifie ta connexion Internet puis réessaie."
                    }
                )
            }

            rawMsg.contains("quota") || rawMsg.contains("429") ||
                rawMsg.contains("resource_exhausted") || rawMsg.contains("rate limit") -> {
                TutorErrorException.QuotaExceeded(
                    if (isArabic) {
                        "تم بلوغ الحد الأقصى لطلبات الذكاء الاصطناعي حاليا. انتظر قليلا ثم أعد المحاولة."
                    } else {
                        "Quota de requêtes IA temporairement atteint. Patiente quelques instants puis réessaie."
                    },
                    e
                )
            }

            else -> {
                val detail = e.localizedMessage?.take(140)
                TutorErrorException.ServiceError(
                    if (isArabic) {
                        "حدث خطأ أثناء الاتصال بالمساعد الذكي${detail?.let { " : $it" } ?: ""}. أعد المحاولة."
                    } else {
                        "Erreur du service IA${detail?.let { " : $it" } ?: ""}. Appuie sur Réessayer."
                    },
                    e
                )
            }
        }
    }
}
