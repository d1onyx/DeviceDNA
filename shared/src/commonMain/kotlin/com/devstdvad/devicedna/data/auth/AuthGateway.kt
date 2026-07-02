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
}
