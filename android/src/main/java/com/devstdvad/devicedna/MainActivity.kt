package com.devstdvad.devicedna

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import com.devstdvad.devicedna.core.design.AppTheme
import com.devstdvad.devicedna.data.settings.AppThemeMode
import com.devstdvad.devicedna.data.settings.SettingsStore
import com.devstdvad.devicedna.data.settings.UserSettings
import com.devstdvad.devicedna.navigation.AppNavigation
import com.devstdvad.devicedna.presentation.auth.AuthViewModel
import kotlinx.coroutines.launch
import org.koin.android.ext.android.inject
import org.koin.androidx.viewmodel.ext.android.viewModel

class MainActivity : ComponentActivity() {
    private val settingsStore: SettingsStore by inject()
    private val authViewModel: AuthViewModel by viewModel()
    private val googleSignInLauncher = registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        authViewModel.handleGoogleSignInResult(result.data)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            val settings by settingsStore.settings.collectAsState(initial = UserSettings())
            val authState by authViewModel.uiState.collectAsState()
            val scope = rememberCoroutineScope()
            val systemDark = isSystemInDarkTheme()
            val darkTheme = when (settings.theme) {
                AppThemeMode.Light -> false
                AppThemeMode.Dark -> true
                AppThemeMode.System -> systemDark
            }
            AppTheme(darkTheme = darkTheme) {
                AppNavigation(
                    settings = settings,
                    authState = authState,
                    onGoogleSignIn = {
                        authViewModel.createGoogleSignInIntent()?.let(googleSignInLauncher::launch)
                    },
                    onOnboardingComplete = {
                        scope.launch { settingsStore.setOnboardingComplete(true) }
                    },
                )
            }
        }
    }
}
