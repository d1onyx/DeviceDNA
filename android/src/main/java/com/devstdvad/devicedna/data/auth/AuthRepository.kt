@file:Suppress("DEPRECATION")

package com.devstdvad.devicedna.data.auth

import android.content.Context
import android.content.Intent
import android.util.Log
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.auth.api.signin.GoogleSignInStatusCodes
import com.google.android.gms.common.api.ApiException
import com.google.android.gms.common.api.CommonStatusCodes
import com.google.android.gms.tasks.Task
import com.google.firebase.FirebaseApp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseAuthRecentLoginRequiredException
import com.google.firebase.auth.GoogleAuthProvider
import com.google.firebase.auth.FirebaseUser
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withTimeoutOrNull
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

class AuthRepository(private val context: Context) : AuthGateway {

    private val webClientId: String
        get() = context.resources.getIdentifier("default_web_client_id", "string", context.packageName)
            .takeIf { it != 0 }
            ?.let(context::getString)
            .orEmpty()

    override val isConfigured: Boolean
        get() = FirebaseApp.getApps(context).isNotEmpty() && webClientId.isNotBlank()

    private val firebaseAuth: FirebaseAuth?
        get() = if (isConfigured) FirebaseAuth.getInstance() else null

    override val currentUser: Flow<AuthUser?> = callbackFlow {
        val auth = firebaseAuth
        if (auth == null) {
            trySend(null)
            close()
            return@callbackFlow
        }

        val listener = FirebaseAuth.AuthStateListener { source ->
            trySend(source.currentUser?.toAuthUser())
        }
        auth.addAuthStateListener(listener)
        awaitClose { auth.removeAuthStateListener(listener) }
    }

    /** Current user's Firebase UID, or null if not signed in. */
    override val uid: String?
        get() = firebaseAuth?.currentUser?.uid

    /** Fresh Firebase ID token for authorizing backend requests. */
    override suspend fun getIdToken(): String? {
        val user = firebaseAuth?.currentUser ?: return null
        return user.getIdToken(false).await().token
    }

    suspend fun createGoogleSignInIntent(forceAccountPicker: Boolean = false): Intent? {
        if (forceAccountPicker) {
            clearLocalSession(removeGoogleAccount = true)
        }

        val clientId = webClientId.takeIf { it.isNotBlank() } ?: return null
        val options = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
            .requestIdToken(clientId)
            .requestEmail()
            .build()
        return GoogleSignIn.getClient(context, options).signInIntent
    }

    suspend fun signInWithGoogleResult(data: Intent?) {
        val auth = firebaseAuth ?: error("Firebase is not configured yet.")
        val account = try {
            GoogleSignIn.getSignedInAccountFromIntent(data).await()
        } catch (exception: ApiException) {
            Log.w(AUTH_LOG_TAG, "Google Sign-In failed with status ${exception.statusCode}.", exception)
            if (exception.statusCode == GoogleSignInStatusCodes.SIGN_IN_CANCELLED) {
                throw GoogleSignInCancelledException(exception)
            }
            throw IllegalStateException(
                exception.googleSignInMessage(),
                exception,
            )
        }
        val token = account.idToken ?: error("Google did not return an ID token.")
        try {
            auth.signInWithCredential(GoogleAuthProvider.getCredential(token, null)).await()
        } catch (exception: Exception) {
            Log.w(AUTH_LOG_TAG, "Firebase rejected the Google credential.", exception)
            throw IllegalStateException(
                "Firebase sign-in failed. Check that the Google provider is enabled in Firebase.",
                exception,
            )
        }
    }

    override suspend fun signOut() {
        firebaseAuth?.signOut()
        GoogleSignIn.getClient(
            context,
            GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN).build(),
        ).signOut().await()
    }

    override suspend fun clearLocalSession(removeGoogleAccount: Boolean) {
        firebaseAuth?.signOut()
        if (removeGoogleAccount) {
            withTimeoutOrNull(3_000L) {
                GoogleSignIn.getClient(
                    context,
                    GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN).build(),
                ).revokeAccess().await()
            }
        }
    }

    override suspend fun prepareAccountDeletion(): AccountDeletionReadiness {
        val user = firebaseAuth?.currentUser ?: return AccountDeletionReadiness.Ready
        val lastSignIn = user.metadata?.lastSignInTimestamp ?: 0L
        val loginAge = System.currentTimeMillis() - lastSignIn
        if (loginAge in 0..RECENT_LOGIN_WINDOW_MS) {
            return AccountDeletionReadiness.Ready
        }

        val clientId = webClientId.takeIf { it.isNotBlank() }
            ?: return AccountDeletionReadiness.ReauthRequired
        val options = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
            .requestIdToken(clientId)
            .requestEmail()
            .build()
        val account = runCatching {
            GoogleSignIn.getClient(context, options).silentSignIn().await()
        }.getOrNull() ?: return AccountDeletionReadiness.ReauthRequired
        val token = account.idToken ?: return AccountDeletionReadiness.ReauthRequired

        return runCatching {
            user.reauthenticate(GoogleAuthProvider.getCredential(token, null)).await()
        }.fold(
            onSuccess = { AccountDeletionReadiness.Ready },
            onFailure = {
                if (it is FirebaseAuthRecentLoginRequiredException) {
                    AccountDeletionReadiness.ReauthRequired
                } else {
                    AccountDeletionReadiness.Failed
                }
            },
        )
    }

    override suspend fun deleteAccount(): AccountDeletionResult {
        val auth = firebaseAuth ?: return AccountDeletionResult.Failed
        val user = auth.currentUser ?: return AccountDeletionResult.Deleted
        return try {
            // Delete needs a recent login; refresh the credential silently first.
            reauthenticateSilently(user)
            user.delete().await()
            // Revoke Google access so a later sign-in shows the account picker.
            clearLocalSession(removeGoogleAccount = true)
            AccountDeletionResult.Deleted
        } catch (_: FirebaseAuthRecentLoginRequiredException) {
            AccountDeletionResult.ReauthRequired
        } catch (_: Exception) {
            AccountDeletionResult.Failed
        }
    }

    private suspend fun reauthenticateSilently(user: FirebaseUser) {
        val clientId = webClientId.takeIf { it.isNotBlank() } ?: return
        val options = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
            .requestIdToken(clientId)
            .requestEmail()
            .build()
        val account = runCatching {
            GoogleSignIn.getClient(context, options).silentSignIn().await()
        }.getOrNull() ?: return
        val token = account.idToken ?: return
        runCatching { user.reauthenticate(GoogleAuthProvider.getCredential(token, null)).await() }
    }

    private fun FirebaseUser.toAuthUser(): AuthUser = AuthUser(
        uid = uid,
        displayName = displayName.orEmpty().ifBlank { "DeviceDNA user" },
        email = email.orEmpty(),
        photoUrl = photoUrl?.toString(),
    )
}

class GoogleSignInCancelledException(cause: Throwable) :
    Exception("Google sign-in was canceled before Google returned an account.", cause)

private fun ApiException.googleSignInMessage(): String = when (statusCode) {
    CommonStatusCodes.DEVELOPER_ERROR ->
        "Google sign-in is not configured for this app. " +
            "Refresh android/google-services.json after adding the SHA-1 in Firebase, then rebuild."
    GoogleSignInStatusCodes.SIGN_IN_FAILED ->
        "Google sign-in failed. Check that the Google provider is enabled in Firebase."
    GoogleSignInStatusCodes.SIGN_IN_CURRENTLY_IN_PROGRESS ->
        "Google sign-in is already in progress."
    else -> "Google sign-in failed with status $statusCode."
}

private suspend fun <T> Task<T>.await(): T = suspendCancellableCoroutine { continuation ->
    addOnCompleteListener { task ->
        if (task.isSuccessful) {
            continuation.resume(task.result)
        } else {
            continuation.resumeWithException(task.exception ?: IllegalStateException("Task failed."))
        }
    }
}

private const val AUTH_LOG_TAG = "DeviceDNA/Auth"
private const val RECENT_LOGIN_WINDOW_MS = 4L * 60L * 1000L
