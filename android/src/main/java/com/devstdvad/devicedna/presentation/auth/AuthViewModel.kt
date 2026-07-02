package com.devstdvad.devicedna.presentation.auth

import android.content.Intent
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.devstdvad.devicedna.data.auth.AuthRepository
import com.devstdvad.devicedna.data.auth.AuthUser
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class AuthViewModel(private val authRepository: AuthRepository) : ViewModel() {

    private val isLoading = MutableStateFlow(false)
    private val errorMessage = MutableStateFlow<String?>(null)

    val uiState: StateFlow<AuthUiState> = combine(
        authRepository.currentUser,
        isLoading,
        errorMessage,
    ) { user, loading, error ->
        AuthUiState(
            user = user,
            isConfigured = authRepository.isConfigured,
            isLoading = loading,
            errorMessage = error,
            isInitializing = false,
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = AuthUiState(isConfigured = authRepository.isConfigured, isInitializing = true),
    )

    fun launchGoogleSignIn(forceAccountPicker: Boolean = false, launch: (Intent) -> Unit) {
        viewModelScope.launch {
            isLoading.value = true
            errorMessage.value = null
            val intent = runCatching {
                authRepository.createGoogleSignInIntent(forceAccountPicker)
            }.getOrElse {
                errorMessage.value = it.message ?: "Google sign-in failed."
                null
            }
            isLoading.value = false

            if (intent == null) {
                if (errorMessage.value == null) showConfigurationError()
            } else {
                launch(intent)
            }
        }
    }

    fun handleGoogleSignInResult(data: Intent?) {
        viewModelScope.launch {
            isLoading.value = true
            errorMessage.value = null
            runCatching { authRepository.signInWithGoogleResult(data) }
                .onFailure { errorMessage.value = it.message ?: "Google sign-in failed." }
            isLoading.value = false
        }
    }

    fun signOut() {
        viewModelScope.launch {
            isLoading.value = true
            errorMessage.value = null
            runCatching { authRepository.signOut() }
                .onFailure { errorMessage.value = it.message ?: "Sign out failed." }
            isLoading.value = false
        }
    }

    fun showConfigurationError() {
        errorMessage.value = "Add android/google-services.json from Firebase Console, enable Google provider, then rebuild."
    }
}
