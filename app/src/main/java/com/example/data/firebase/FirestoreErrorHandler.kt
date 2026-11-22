package com.example.data.firebase

import android.util.Log
import com.google.firebase.Firebase
import com.google.firebase.auth.auth
import org.json.JSONArray
import org.json.JSONObject

enum class OperationType(val value: String) {
    CREATE("create"),
    UPDATE("update"),
    DELETE("delete"),
    LIST("list"),
    GET("get"),
    WRITE("write")
}

/**
 * Formats and logs structured Firestore diagnostic metadata before raising an exception.
 */
fun handleFirestoreError(
    exception: Throwable,
    operationType: OperationType,
    path: String?
): Nothing {
    val currentUser = runCatching { Firebase.auth.currentUser }.getOrNull()
    val providers = JSONArray().apply {
        currentUser?.providerData?.forEach { profile ->
            put(
                JSONObject().apply {
                    put("providerId", profile.providerId)
                    put("displayName", profile.displayName)
                    put("email", profile.email)
                    put("photoUrl", profile.photoUrl?.toString())
                }
            )
        }
    }

    val payload = JSONObject().apply {
        put("error", exception.message ?: exception.toString())
        put("operationType", operationType.value)
        put("path", path)
        put(
            "authInfo",
            JSONObject().apply {
                put("userId", currentUser?.uid)
                put("email", currentUser?.email)
                put("emailVerified", currentUser?.isEmailVerified)
                put("isAnonymous", currentUser?.isAnonymous)
                put("providerInfo", providers)
            }
        )
    }.toString()

    Log.e("FirestoreError", payload, exception)
    throw IllegalStateException("FirestoreError: $payload", exception)
}
