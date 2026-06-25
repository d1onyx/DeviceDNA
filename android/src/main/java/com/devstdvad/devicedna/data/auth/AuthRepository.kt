@file:Suppress("DEPRECATION")

package com.devstdvad.devicedna.data.auth

import android.content.Context
import android.content.Intent
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.common.api.ApiException
import com.google.android.gms.tasks.Task
import com.google.firebase.FirebaseApp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.GoogleAuthProvider
import com.google.firebase.auth.FirebaseUser
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

data class AuthUser(
    val displayName: String,
    val email: String,
    val photoUrl: String?,
)

class AuthRepository(private val context: Context) {

    private val webClientId: String
        get() = context.resources.getIdentifier("default_web_client_id", "string", context.packageName)
            .takeIf { it != 0 }
            ?.let(context::getString)
            .orEmpty()

    val isConfigured: Boolean
        get() = FirebaseApp.getApps(context).isNotEmpty() && webClientId.isNotBlank()

    private val firebaseAuth: FirebaseAuth?
        get() = if (isConfigured) FirebaseAuth.getInstance() else null

    val currentUser: Flow<AuthUser?> = callbackFlow {
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

    fun createGoogleSignInIntent(): Intent? {
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
            throw IllegalStateException("Google sign-in was cancelled or rejected.", exception)
        }
        val token = account.idToken ?: error("Google did not return an ID token.")
        auth.signInWithCredential(GoogleAuthProvider.getCredential(token, null)).await()
    }

    suspend fun signOut() {
        firebaseAuth?.signOut()
        GoogleSignIn.getClient(
            context,
            GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN).build(),
        ).signOut().await()
    }

    private fun FirebaseUser.toAuthUser(): AuthUser = AuthUser(
        displayName = displayName.orEmpty().ifBlank { "DeviceDNA user" },
        email = email.orEmpty(),
        photoUrl = photoUrl?.toString(),
    )
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
