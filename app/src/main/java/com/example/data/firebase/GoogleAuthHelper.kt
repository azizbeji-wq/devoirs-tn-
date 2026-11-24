package com.example.data.firebase

import android.content.Context
import android.util.Log
import androidx.credentials.ClearCredentialStateRequest
import androidx.credentials.CredentialManager
import androidx.credentials.CustomCredential
import androidx.credentials.GetCredentialRequest
import androidx.credentials.exceptions.GetCredentialCancellationException
import androidx.credentials.exceptions.GetCredentialException
import androidx.credentials.exceptions.NoCredentialException
import com.example.R
import com.google.android.libraries.identity.googleid.GetGoogleIdOption
import com.google.android.libraries.identity.googleid.GetSignInWithGoogleOption
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential
import com.google.android.libraries.identity.googleid.GoogleIdTokenParsingException
import com.google.firebase.Firebase
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseAuthRecentLoginRequiredException
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.GoogleAuthProvider
import com.google.firebase.auth.auth
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext

/**
 * Observe les changements d'état de session Firebase Authentication en temps réel.
 */
fun FirebaseAuth.authStateFlow(): Flow<FirebaseUser?> = callbackFlow {
    val listener = FirebaseAuth.AuthStateListener { firebaseAuth ->
        trySend(firebaseAuth.currentUser)
    }
    addAuthStateListener(listener)
    awaitClose { removeAuthStateListener(listener) }
}

/**
 * Tente une reconnexion silencieuse via Jetpack Credential Manager pour les comptes déjà autorisés.
 * Une seule option (GetGoogleIdOption) par GetCredentialRequest pour compatibilité Android API 36.
 */
suspend fun attemptAutoSignIn(
    context: Context,
    onAuthSuccess: (FirebaseUser) -> Unit,
    onNoSavedCredential: () -> Unit
) {
    val webClientId = context.getString(R.string.default_web_client_id)
    val credentialManager = CredentialManager.create(context)

    val googleIdOption = GetGoogleIdOption.Builder()
        .setFilterByAuthorizedAccounts(true)
        .setServerClientId(webClientId)
        .setAutoSelectEnabled(true)
        .build()

    val request = GetCredentialRequest.Builder()
        .addCredentialOption(googleIdOption)
        .build()

    try {
        val result = credentialManager.getCredential(request = request, context = context)
        val credential = result.credential
        if (credential is CustomCredential &&
            credential.type == GoogleIdTokenCredential.TYPE_GOOGLE_ID_TOKEN_CREDENTIAL
        ) {
            val googleIdTokenCredential = GoogleIdTokenCredential.createFrom(credential.data)
            val firebaseCredential = GoogleAuthProvider.getCredential(googleIdTokenCredential.idToken, null)
            val authResult = Firebase.auth.signInWithCredential(firebaseCredential).await()
            val user = authResult.user
            if (user != null) {
                onAuthSuccess(user)
            } else {
                onNoSavedCredential()
            }
        } else {
            onNoSavedCredential()
        }
    } catch (e: CancellationException) {
        throw e
    } catch (e: NoCredentialException) {
        Log.d("Auth", "No saved Google credential found for silent sign-in")
        onNoSavedCredential()
    } catch (e: GetCredentialCancellationException) {
        Log.d("Auth", "Silent Google sign-in cancelled")
        onNoSavedCredential()
    } catch (e: GetCredentialException) {
        Log.d("Auth", "Silent Google sign-in skipped: ${e.message}")
        onNoSavedCredential()
    } catch (e: GoogleIdTokenParsingException) {
        Log.w("Auth", "Failed to parse Google ID token during silent sign-in", e)
        onNoSavedCredential()
    } catch (e: Exception) {
        Log.w("Auth", "Unexpected silent sign-in error: ${e.message}")
        onNoSavedCredential()
    }
}

/**
 * Déclenche le flux interactif officiel « Se connecter avec Google » via Jetpack Credential Manager.
 * Une seule option (GetSignInWithGoogleOption) par GetCredentialRequest.
 */
suspend fun onGoogleSignInClicked(
    context: Context,
    onAuthSuccess: (FirebaseUser) -> Unit,
    onAuthCancelled: () -> Unit,
    onAuthError: (String) -> Unit
) {
    val webClientId = context.getString(R.string.default_web_client_id)
    val credentialManager = CredentialManager.create(context)

    val signInWithGoogleOption = GetSignInWithGoogleOption.Builder(
        serverClientId = webClientId
    ).build()

    val request = GetCredentialRequest.Builder()
        .addCredentialOption(signInWithGoogleOption)
        .build()

    try {
        val result = credentialManager.getCredential(request = request, context = context)
        val credential = result.credential
        if (credential is CustomCredential &&
            credential.type == GoogleIdTokenCredential.TYPE_GOOGLE_ID_TOKEN_CREDENTIAL
        ) {
            val googleIdTokenCredential = GoogleIdTokenCredential.createFrom(credential.data)
            val firebaseCredential = GoogleAuthProvider.getCredential(googleIdTokenCredential.idToken, null)
            val authResult = Firebase.auth.signInWithCredential(firebaseCredential).await()
            val user = authResult.user
            if (user != null) {
                onAuthSuccess(user)
            } else {
                onAuthError(context.getString(R.string.auth_error_generic))
            }
        } else {
            onAuthError(context.getString(R.string.auth_error_generic))
        }
    } catch (e: CancellationException) {
        throw e
    } catch (e: GetCredentialCancellationException) {
        Log.w("Auth", "Google sign-in cancelled by user", e)
        onAuthCancelled()
    } catch (e: GoogleIdTokenParsingException) {
        Log.e("Auth", "Invalid Google ID token received", e)
        onAuthError(context.getString(R.string.auth_error_generic))
    } catch (e: GetCredentialException) {
        val msg = e.message ?: e.errorMessage?.toString() ?: ""
        Log.e("Auth", "Credential Manager sign-in failed: $msg", e)
        if (msg.contains("10:") || msg.contains("DEVELOPER_ERROR")) {
            onAuthError(context.getString(R.string.auth_error_developer))
        } else {
            onAuthError(msg.takeIf { it.isNotBlank() } ?: context.getString(R.string.auth_error_generic))
        }
    } catch (e: Exception) {
        val msg = e.message ?: ""
        Log.e("Auth", "Firebase sign-in with Google failed: $msg", e)
        if (msg.contains("10:") || msg.contains("DEVELOPER_ERROR")) {
            onAuthError(context.getString(R.string.auth_error_developer))
        } else {
            onAuthError(e.localizedMessage ?: context.getString(R.string.auth_error_generic))
        }
    }
}

/**
 * Déconnecte proprement l'utilisateur de Credential Manager et de Firebase Auth
 * dans un contexte NonCancellable afin d'éviter toute annulation prématurée.
 */
suspend fun signOutFromFirebase(context: Context) {
    withContext(NonCancellable) {
        runCatching {
            CredentialManager.create(context).clearCredentialState(ClearCredentialStateRequest())
        }.onFailure { e ->
            Log.w("Auth", "Failed to clear credential state on sign-out: ${e.message}")
        }
        Firebase.auth.signOut()
    }
}

sealed interface AccountDeletionResult {
    data object Success : AccountDeletionResult
    data object RequiresRecentLogin : AccountDeletionResult
    data class Error(val message: String) : AccountDeletionResult
}

/**
 * Supprime définitivement le compte Firebase Authentication de l'utilisateur courant.
 * Si Firebase exige une connexion récente, propose automatiquement une ré-authentification Google
 * via Jetpack Credential Manager (avec un Context d'Activity) puis relance la suppression.
 */
suspend fun deleteFirebaseAccount(activityContext: Context): AccountDeletionResult =
    withContext(NonCancellable) {
        val currentUser = Firebase.auth.currentUser ?: return@withContext AccountDeletionResult.Success
        try {
            currentUser.delete().await()
            runCatching {
                CredentialManager.create(activityContext)
                    .clearCredentialState(ClearCredentialStateRequest())
            }
            Firebase.auth.signOut()
            AccountDeletionResult.Success
        } catch (e: CancellationException) {
            throw e
        } catch (_: FirebaseAuthRecentLoginRequiredException) {
            // Tente une ré-authentification Google interactive puis supprime le compte
            try {
                val webClientId = activityContext.getString(R.string.default_web_client_id)
                val credentialManager = CredentialManager.create(activityContext)
                val signInWithGoogleOption = GetSignInWithGoogleOption.Builder(
                    serverClientId = webClientId
                ).build()
                val request = GetCredentialRequest.Builder()
                    .addCredentialOption(signInWithGoogleOption)
                    .build()
                val result = credentialManager.getCredential(
                    request = request,
                    context = activityContext
                )
                val credential = result.credential
                if (credential is CustomCredential &&
                    credential.type == GoogleIdTokenCredential.TYPE_GOOGLE_ID_TOKEN_CREDENTIAL
                ) {
                    val googleToken = GoogleIdTokenCredential.createFrom(credential.data)
                    val firebaseCred = GoogleAuthProvider.getCredential(googleToken.idToken, null)
                    currentUser.reauthenticate(firebaseCred).await()
                    currentUser.delete().await()
                    runCatching {
                        credentialManager.clearCredentialState(ClearCredentialStateRequest())
                    }
                    Firebase.auth.signOut()
                    AccountDeletionResult.Success
                } else {
                    AccountDeletionResult.RequiresRecentLogin
                }
            } catch (cancel: GetCredentialCancellationException) {
                Log.w("Auth", "Re-authentication cancelled during account deletion", cancel)
                AccountDeletionResult.RequiresRecentLogin
            } catch (reauthError: Exception) {
                Log.e("Auth", "Re-authentication failed before account deletion: ${reauthError.message}", reauthError)
                AccountDeletionResult.RequiresRecentLogin
            }
        } catch (e: Exception) {
            Log.e("Auth", "Failed to delete Firebase Auth account: ${e.message}", e)
            AccountDeletionResult.Error(
                e.localizedMessage ?: activityContext.getString(R.string.delete_account_error_generic)
            )
        }
    }
