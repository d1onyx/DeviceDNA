package com.devstdvad.devicedna.data.auth

import kotlinx.coroutines.flow.Flow

/** Authenticated user, platform-agnostic. */
data class AuthUser(
    val uid: String,
    val displayName: String,
    val email: String,
    val photoUrl: String?,
)

/**
 * Platform-agnostic authentication surface used by shared code (settings, sync, subscription
 * verification). The actual sign-in *UI* (Google Sign-In Intent on Android, GIDSignIn on iOS)
 * stays platform-specific and is NOT part of this interface — only session state and tokens are
 * shared. Implemented by AndroidAuthGateway (Firebase) and, later, an iOS Firebase gateway.
 */
interface AuthGateway {
    /** Emits the current user (or null when signed out); backed by the auth state listener. */
    val currentUser: Flow<AuthUser?>

    /** Whether the auth backend is configured (Firebase project present). */
    val isConfigured: Boolean

    /** Current user's UID, or null if signed out. */
    val uid: String?

    /** Fresh ID token for authorizing backend requests, or null if signed out. */
    suspend fun getIdToken(): String?

    /** Sign out of the auth backend. */
    suspend fun signOut()

    /** Clear the local session, optionally revoking the linked provider account. */
    suspend fun clearLocalSession(removeGoogleAccount: Boolean = false)

    /** Refreshes recent-login state before any irreversible backend deletion starts. */
    suspend fun prepareAccountDeletion(): AccountDeletionReadiness

    /**
     * Permanently delete the signed-in account (App Store 5.1.1(v) / Google Play requirement).
     * Deleting requires a recent login; when the cached credential is too old the caller must sign
     * in again and retry ([AccountDeletionResult.ReauthRequired]).
     */
    suspend fun deleteAccount(): AccountDeletionResult
}

enum class AccountDeletionReadiness {
    Ready,
    ReauthRequired,
    Failed,
}

/** Outcome of [AuthGateway.deleteAccount]. */
enum class AccountDeletionResult {
    /** Account (and auth session) removed; the user flow returns to sign-in automatically. */
    Deleted,

    /** Credential too old — the user must sign in again, then retry. */
    ReauthRequired,

    /** Deletion failed for another reason (network, backend). */
    Failed,
}
