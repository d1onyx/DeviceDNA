package com.devstdvad.devicedna.data.auth

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume

/**
 * iOS [AuthGateway] bridged to the Swift Firebase/GoogleSignIn layer via injected closures
 * (Swift cannot implement a Kotlin `Flow` property directly, so the Swift side *pushes*
 * auth-state changes into [updateUser] from its FirebaseAuth state listener instead).
 *
 * Swift wiring (see ios/DeviceDNAApp/AuthBridge.swift):
 *   let gateway = IosAuthGateway(
 *       configured: FirebaseApp.app() != nil,
 *       uidProvider: { Auth.auth().currentUser?.uid },
 *       tokenFetcher: { done in Auth.auth().currentUser?.getIDToken { t, _ in done(t) } ?? done(nil) },
 *       signOutAction: { done in try? Auth.auth().signOut(); GIDSignIn.sharedInstance.signOut(); done() },
 *       clearSessionAction: { remove, done in ... }
 *   )
 */
class IosAuthGateway(
    private val configured: Boolean,
    private val uidProvider: () -> String?,
    private val tokenFetcher: (onResult: (String?) -> Unit) -> Unit,
    private val signOutAction: (onDone: () -> Unit) -> Unit,
    private val clearSessionAction: (removeGoogleAccount: Boolean, onDone: () -> Unit) -> Unit,
    private val prepareDeletionAction: (onResult: (String) -> Unit) -> Unit,
    private val deleteAccountAction: (onResult: (String) -> Unit) -> Unit,
) : AuthGateway {

    private val userFlow = MutableStateFlow<AuthUser?>(null)

    /** True until Swift pushes the first auth state (session restore in progress). */
    private val initializingFlow = MutableStateFlow(true)

    private val signingInFlow = MutableStateFlow(false)
    private val errorFlow = MutableStateFlow<String?>(null)

    override val currentUser: Flow<AuthUser?> = userFlow

    /** Extra state for the host UI gate (mirrors AuthUiState.isInitializing). */
    val isInitializing: StateFlow<Boolean> = initializingFlow
    val user: StateFlow<AuthUser?> = userFlow

    /** A provider sheet is open or its credential is being exchanged (AuthUiState.isLoading). */
    val isSigningIn: StateFlow<Boolean> = signingInFlow

    /** Last sign-in failure, surfaced on the auth screen (AuthUiState.errorMessage). */
    val errorMessage: StateFlow<String?> = errorFlow

    /** Called from the Swift FirebaseAuth state listener on every auth change. */
    fun updateUser(uid: String?, displayName: String?, email: String?, photoUrl: String?) {
        userFlow.value = uid?.let {
            AuthUser(
                uid = it,
                displayName = displayName.orEmpty().ifBlank { "DeviceDNA user" },
                email = email.orEmpty(),
                photoUrl = photoUrl,
            )
        }
        initializingFlow.value = false
        if (uid != null) {
            signingInFlow.value = false
            errorFlow.value = null
        }
    }

    /** Swift: a provider flow was launched. */
    fun signInStarted() {
        errorFlow.value = null
        signingInFlow.value = true
    }

    /** Swift: the user dismissed the provider sheet — not an error, just release the spinner. */
    fun signInCancelled() {
        signingInFlow.value = false
    }

    /**
     * Swift: the flow failed. Without this the iOS auth screen had no way to render a failure,
     * so every misconfiguration looked like a dead button.
     */
    fun signInFailed(message: String) {
        signingInFlow.value = false
        errorFlow.value = message
    }

    override val isConfigured: Boolean get() = configured

    override val uid: String? get() = uidProvider()

    override suspend fun getIdToken(): String? = suspendCancellableCoroutine { cont ->
        tokenFetcher { token -> if (cont.isActive) cont.resume(token) }
    }

    override suspend fun signOut(): Unit = suspendCancellableCoroutine { cont ->
        signOutAction { if (cont.isActive) cont.resume(Unit) }
    }

    override suspend fun clearLocalSession(removeGoogleAccount: Boolean): Unit =
        suspendCancellableCoroutine { cont ->
            clearSessionAction(removeGoogleAccount) { if (cont.isActive) cont.resume(Unit) }
        }

    override suspend fun prepareAccountDeletion(): AccountDeletionReadiness =
        suspendCancellableCoroutine { cont ->
            prepareDeletionAction { result ->
                if (cont.isActive) {
                    cont.resume(
                        when (result) {
                            "ready" -> AccountDeletionReadiness.Ready
                            "reauth" -> AccountDeletionReadiness.ReauthRequired
                            else -> AccountDeletionReadiness.Failed
                        },
                    )
                }
            }
        }

    override suspend fun deleteAccount(): AccountDeletionResult = suspendCancellableCoroutine { cont ->
        deleteAccountAction { result ->
            if (cont.isActive) {
                cont.resume(
                    when (result) {
                        "deleted" -> AccountDeletionResult.Deleted
                        "reauth" -> AccountDeletionResult.ReauthRequired
                        else -> AccountDeletionResult.Failed
                    },
                )
            }
        }
    }
}
