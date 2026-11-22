package com.example.data.firebase

import android.content.Context
import com.example.R
import com.google.firebase.Firebase
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.auth
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.FirebaseFirestoreException
import com.google.firebase.firestore.Query
import com.google.firebase.firestore.snapshots
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.tasks.await

/**
 * Repository Cloud Firestore synchronisant le profil d'étude, les favoris, devoirs terminés,
 * tentatives d'examens, rappels et matières personnalisées sous `/users/{userId}`.
 *
 * Utilise obligatoirement la base nommée définie dans `R.string.firestore_database_id`.
 */
class CloudSyncRepository(
    private val db: FirebaseFirestore,
    private val auth: FirebaseAuth = Firebase.auth
) {
    constructor(context: Context) : this(
        db = FirebaseFirestore.getInstance(
            context.applicationContext.getString(R.string.firestore_database_id)
        ),
        auth = Firebase.auth
    )

    private fun requireUserId(): String =
        auth.currentUser?.uid
            ?: throw IllegalStateException("L'utilisateur doit être connecté avant d'accéder à Firestore.")

    private fun sanitizeDocumentId(rawId: String, fallbackPrefix: String): String {
        val cleaned = rawId.replace(Regex("[^a-zA-Z0-9_-]"), "_").take(120)
        return if (cleaned.isNotEmpty()) cleaned else "${fallbackPrefix}_${System.currentTimeMillis()}"
    }

    fun observeUserProfile(): Flow<UserProfile?> {
        val uid = requireUserId()
        val path = "users/$uid"
        return db.collection("users")
            .document(uid)
            .snapshots()
            .map { snapshot ->
                if (!snapshot.exists()) {
                    null
                } else {
                    snapshot.toObject(
                        UserProfile::class.java,
                        DocumentSnapshot.ServerTimestampBehavior.ESTIMATE
                    )
                }
            }
            .catch { e ->
                if (e is CancellationException) throw e
                handleFirestoreError(e, OperationType.GET, path)
            }
    }

    fun observeExamAttempts(): Flow<List<CloudExamAttempt>> {
        val uid = requireUserId()
        val path = "users/$uid/examAttempts"
        return db.collection("users")
            .document(uid)
            .collection("examAttempts")
            .orderBy("createdAt", Query.Direction.DESCENDING)
            .snapshots()
            .map { snapshot ->
                snapshot.documents.mapNotNull { doc ->
                    doc.toObject(
                        CloudExamAttempt::class.java,
                        DocumentSnapshot.ServerTimestampBehavior.ESTIMATE
                    )
                }
            }
            .catch { e ->
                if (e is CancellationException) throw e
                handleFirestoreError(e, OperationType.LIST, path)
            }
    }

    fun observeReminders(): Flow<List<CloudExamReminder>> {
        val uid = requireUserId()
        val path = "users/$uid/reminders"
        return db.collection("users")
            .document(uid)
            .collection("reminders")
            .orderBy("examDateMillis", Query.Direction.ASCENDING)
            .snapshots()
            .map { snapshot ->
                snapshot.documents.mapNotNull { doc ->
                    doc.toObject(
                        CloudExamReminder::class.java,
                        DocumentSnapshot.ServerTimestampBehavior.ESTIMATE
                    )
                }
            }
            .catch { e ->
                if (e is CancellationException) throw e
                handleFirestoreError(e, OperationType.LIST, path)
            }
    }

    fun observeCustomSubjects(): Flow<List<CloudSubject>> {
        val uid = requireUserId()
        val path = "users/$uid/subjects"
        return db.collection("users")
            .document(uid)
            .collection("subjects")
            .orderBy("createdAt", Query.Direction.ASCENDING)
            .snapshots()
            .map { snapshot ->
                snapshot.documents.mapNotNull { doc ->
                    doc.toObject(
                        CloudSubject::class.java,
                        DocumentSnapshot.ServerTimestampBehavior.ESTIMATE
                    )
                }
            }
            .catch { e ->
                if (e is CancellationException) throw e
                handleFirestoreError(e, OperationType.LIST, path)
            }
    }

    /**
     * Crée le document `/users/{uid}` s'il n'existe pas encore dans Firestore.
     * Préserve le document existant pour éviter d'écraser une sauvegarde cloud existante.
     */
    suspend fun ensureUserProfile(
        displayName: String,
        weeklyGoalTarget: Int,
        favoriteDevoirIds: List<String>,
        completedDevoirIds: List<String>,
        starredMethodIds: List<String>
    ): Result<Unit> {
        val uid = runCatching { requireUserId() }.getOrElse { return Result.failure(it) }
        val path = "users/$uid"
        return try {
            val docRef = db.collection("users").document(uid)
            val snapshot = docRef.get().await()
            if (!snapshot.exists()) {
                val payload = mapOf(
                    "userId" to uid,
                    "displayName" to displayName.trim().take(120),
                    "weeklyGoalTarget" to weeklyGoalTarget.coerceIn(1, 20),
                    "favoriteDevoirIds" to favoriteDevoirIds.distinct().take(200),
                    "completedDevoirIds" to completedDevoirIds.distinct().take(200),
                    "starredMethodIds" to starredMethodIds.distinct().take(100),
                    "createdAt" to FieldValue.serverTimestamp(),
                    "updatedAt" to FieldValue.serverTimestamp()
                )
                docRef.set(payload).await()
            }
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            recordAndWrapError(e, OperationType.CREATE, path)
        }
    }

    /**
     * Met à jour les champs mutables du profil d'étude sans toucher `userId` ni `createdAt`.
     */
    suspend fun updateUserStudyProfile(
        displayName: String,
        weeklyGoalTarget: Int,
        favoriteDevoirIds: List<String>,
        completedDevoirIds: List<String>,
        starredMethodIds: List<String>
    ): Result<Unit> {
        val uid = runCatching { requireUserId() }.getOrElse { return Result.failure(it) }
        val path = "users/$uid"
        val safeName = displayName.trim().take(120)
        val safeGoal = weeklyGoalTarget.coerceIn(1, 20)
        val safeFavs = favoriteDevoirIds.distinct().take(200)
        val safeCompleted = completedDevoirIds.distinct().take(200)
        val safeStarred = starredMethodIds.distinct().take(100)

        return try {
            val docRef = db.collection("users").document(uid)
            val partialUpdate = mapOf<String, Any?>(
                "displayName" to safeName,
                "weeklyGoalTarget" to safeGoal,
                "favoriteDevoirIds" to safeFavs,
                "completedDevoirIds" to safeCompleted,
                "starredMethodIds" to safeStarred,
                "updatedAt" to FieldValue.serverTimestamp()
            ).filterValues { it != null }

            try {
                docRef.update(partialUpdate).await()
            } catch (notFound: FirebaseFirestoreException) {
                if (notFound.code == FirebaseFirestoreException.Code.NOT_FOUND) {
                    val createPayload = mapOf(
                        "userId" to uid,
                        "displayName" to safeName,
                        "weeklyGoalTarget" to safeGoal,
                        "favoriteDevoirIds" to safeFavs,
                        "completedDevoirIds" to safeCompleted,
                        "starredMethodIds" to safeStarred,
                        "createdAt" to FieldValue.serverTimestamp(),
                        "updatedAt" to FieldValue.serverTimestamp()
                    )
                    docRef.set(createPayload).await()
                } else {
                    throw notFound
                }
            }
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            recordAndWrapError(e, OperationType.UPDATE, path)
        }
    }

    suspend fun saveExamAttempt(
        attemptId: String,
        devoirId: String,
        devoirTitle: String,
        subject: String,
        totalScore: Double,
        maxScore: Double,
        actualDurationSeconds: Int,
        attemptedAtMillis: Long
    ): Result<Unit> {
        val uid = runCatching { requireUserId() }.getOrElse { return Result.failure(it) }
        val safeId = sanitizeDocumentId(attemptId, "attempt")
        val path = "users/$uid/examAttempts/$safeId"
        return try {
            val payload = mapOf(
                "userId" to uid,
                "devoirId" to devoirId.trim().take(128).ifEmpty { "devoir" },
                "devoirTitle" to devoirTitle.trim().take(200).ifEmpty { "Devoir" },
                "subject" to subject.trim().take(80).ifEmpty { "Matière" },
                "totalScore" to totalScore.coerceIn(0.0, 100.0),
                "maxScore" to maxScore.coerceIn(1.0, 100.0),
                "actualDurationSeconds" to actualDurationSeconds.coerceIn(0, 86400),
                "attemptedAtMillis" to attemptedAtMillis.coerceAtLeast(1L),
                "createdAt" to FieldValue.serverTimestamp(),
                "updatedAt" to FieldValue.serverTimestamp()
            )
            db.collection("users")
                .document(uid)
                .collection("examAttempts")
                .document(safeId)
                .set(payload)
                .await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            recordAndWrapError(e, OperationType.CREATE, path)
        }
    }

    suspend fun saveReminder(
        reminderId: String,
        title: String,
        subject: String,
        examType: String,
        examDateMillis: Long
    ): Result<Unit> {
        val uid = runCatching { requireUserId() }.getOrElse { return Result.failure(it) }
        val safeId = sanitizeDocumentId(reminderId, "reminder")
        val path = "users/$uid/reminders/$safeId"
        return try {
            val payload = mapOf(
                "userId" to uid,
                "title" to title.trim().take(160).ifEmpty { "Devoir" },
                "subject" to subject.trim().take(80).ifEmpty { "Matière" },
                "examType" to examType.trim().take(40).ifEmpty { "DC" },
                "examDateMillis" to examDateMillis.coerceAtLeast(1L),
                "createdAt" to FieldValue.serverTimestamp(),
                "updatedAt" to FieldValue.serverTimestamp()
            )
            db.collection("users")
                .document(uid)
                .collection("reminders")
                .document(safeId)
                .set(payload)
                .await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            recordAndWrapError(e, OperationType.CREATE, path)
        }
    }

    suspend fun deleteReminder(reminderId: String): Result<Unit> {
        val uid = runCatching { requireUserId() }.getOrElse { return Result.failure(it) }
        val safeId = sanitizeDocumentId(reminderId, "reminder")
        val path = "users/$uid/reminders/$safeId"
        return try {
            db.collection("users")
                .document(uid)
                .collection("reminders")
                .document(safeId)
                .delete()
                .await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            recordAndWrapError(e, OperationType.DELETE, path)
        }
    }

    suspend fun saveSubject(
        subjectId: String,
        name: String,
        subtitle: String
    ): Result<Unit> {
        val uid = runCatching { requireUserId() }.getOrElse { return Result.failure(it) }
        val safeId = sanitizeDocumentId(subjectId, "subject")
        val path = "users/$uid/subjects/$safeId"
        return try {
            val payload = mapOf(
                "userId" to uid,
                "name" to name.trim().take(80).ifEmpty { "Matière" },
                "subtitle" to subtitle.trim().take(160),
                "createdAt" to FieldValue.serverTimestamp(),
                "updatedAt" to FieldValue.serverTimestamp()
            )
            db.collection("users")
                .document(uid)
                .collection("subjects")
                .document(safeId)
                .set(payload)
                .await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            recordAndWrapError(e, OperationType.CREATE, path)
        }
    }

    suspend fun deleteSubject(subjectId: String): Result<Unit> {
        val uid = runCatching { requireUserId() }.getOrElse { return Result.failure(it) }
        val safeId = sanitizeDocumentId(subjectId, "subject")
        val path = "users/$uid/subjects/$safeId"
        return try {
            db.collection("users")
                .document(uid)
                .collection("subjects")
                .document(safeId)
                .delete()
                .await()
            Result.success(Unit)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            recordAndWrapError(e, OperationType.DELETE, path)
        }
    }

    private fun recordAndWrapError(
        exception: Exception,
        operationType: OperationType,
        path: String
    ): Result<Unit> {
        return try {
            handleFirestoreError(exception, operationType, path)
        } catch (wrapped: IllegalStateException) {
            Result.failure(wrapped)
        }
    }
}
