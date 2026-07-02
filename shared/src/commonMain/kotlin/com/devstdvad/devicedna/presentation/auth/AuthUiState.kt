package com.devstdvad.devicedna.presentation.auth

import com.devstdvad.devicedna.data.auth.AuthUser

data class AuthUiState(
    val user: AuthUser? = null,
    val isConfigured: Boolean = false,
    val isLoading: Boolean = false,
    val errorMessage: String? = null,
    // True until the auth backend reports the first state — avoids flashing the
    // sign-in screen while the session is still being restored.
    val isInitializing: Boolean = true,
) {
    val isSignedIn: Boolean get() = user != null
}
