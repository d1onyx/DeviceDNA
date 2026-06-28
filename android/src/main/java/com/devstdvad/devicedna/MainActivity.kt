package com.devstdvad.devicedna

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import com.devstdvad.devicedna.core.notification.WidgetPromoNotifier
import kotlinx.coroutines.flow.first
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
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
    private val notificationPermissionLauncher =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { showWidgetPromoOnce() }

    // Deep-link route requested by a home-screen widget tap.
    private var pendingRoute by mutableStateOf<String?>(null)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        pendingRoute = intent?.getStringExtra(EXTRA_ROUTE)
        maybeAnnounceWidgets()
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
                    deepLinkRoute = pendingRoute,
                    onDeepLinkHandled = { pendingRoute = null },
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

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        intent.getStringExtra(EXTRA_ROUTE)?.let { pendingRoute = it }
    }

    /** Shows the "widgets are available" notification once, requesting permission if needed. */
    private fun maybeAnnounceWidgets() {
        lifecycleScope.launch {
            if (settingsStore.settings.first().widgetsPromoShown) return@launch
            val needsPermission = Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
                ContextCompat.checkSelfPermission(this@MainActivity, Manifest.permission.POST_NOTIFICATIONS) !=
                PackageManager.PERMISSION_GRANTED
            if (needsPermission) {
                notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
            } else {
                showWidgetPromoOnce()
            }
        }
    }

    private fun showWidgetPromoOnce() {
        WidgetPromoNotifier.show(this)
        lifecycleScope.launch { settingsStore.setWidgetsPromoShown(true) }
    }

    companion object {
        const val EXTRA_ROUTE = "devicedna.extra.ROUTE"
    }
}
