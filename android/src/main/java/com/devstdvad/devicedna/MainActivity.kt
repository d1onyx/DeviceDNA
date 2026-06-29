package com.devstdvad.devicedna

import android.Manifest
import android.content.Context
import android.content.ContextWrapper
import android.content.Intent
import android.content.pm.PackageManager
import android.content.res.Configuration
import android.content.res.Resources
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
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
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import com.devstdvad.devicedna.core.design.AppTheme
import com.devstdvad.devicedna.data.settings.AppThemeMode
import com.devstdvad.devicedna.data.settings.SettingsStore
import com.devstdvad.devicedna.data.settings.UserSettings
import com.devstdvad.devicedna.navigation.AppNavigation
import com.devstdvad.devicedna.presentation.auth.AuthViewModel
import com.devstdvad.devicedna.widget.WidgetRefreshScheduler
import kotlinx.coroutines.launch
import org.koin.android.ext.android.inject
import org.koin.androidx.viewmodel.ext.android.viewModel
import java.util.Locale

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
        pendingRoute = extractRoute(intent)
        maybeAnnounceWidgets()
        setContent {
            val settings by settingsStore.settings.collectAsState(initial = UserSettings())
            val authState by authViewModel.uiState.collectAsState()
            val scope = rememberCoroutineScope()
            val systemDark = isSystemInDarkTheme()
            val localizedContext = remember(settings.appLanguage) {
                createLocalizedContext(settings.appLanguage)
            }
            val localizedConfiguration = remember(localizedContext) {
                Configuration(localizedContext.resources.configuration)
            }
            val darkTheme = when (settings.theme) {
                AppThemeMode.Light -> false
                AppThemeMode.Dark -> true
                AppThemeMode.System -> systemDark
            }
            LaunchedEffect(settings.backgroundMonitoring) {
                if (settings.backgroundMonitoring) {
                    WidgetRefreshScheduler.enqueuePeriodic(this@MainActivity)
                } else {
                    WidgetRefreshScheduler.cancelPeriodic(this@MainActivity)
                }
            }
            CompositionLocalProvider(
                LocalContext provides localizedContext,
                LocalConfiguration provides localizedConfiguration,
            ) {
                AppTheme(darkTheme = darkTheme) {
                    AppNavigation(
                        settings = settings,
                        authState = authState,
                        deepLinkRoute = pendingRoute,
                        onDeepLinkHandled = { pendingRoute = null },
                        onGoogleSignIn = { forceAccountPicker ->
                            authViewModel.launchGoogleSignIn(
                                forceAccountPicker = forceAccountPicker,
                                launch = googleSignInLauncher::launch,
                            )
                        },
                        onOnboardingComplete = {
                            scope.launch { settingsStore.setOnboardingComplete(true) }
                        },
                    )
                }
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        extractRoute(intent)?.let { pendingRoute = it }
    }

    /**
     * Resolves the deep-link route from a launch Intent. The route is read from the data Uri
     * first (`devicedna://open/<route>`) because PendingIntent equality ignores extras, so the
     * data Uri is the reliable carrier across widgets; the [EXTRA_ROUTE] extra is a fallback.
     */
    private fun extractRoute(intent: Intent?): String? {
        if (intent == null) return null
        val fromData = intent.data
            ?.takeIf { it.scheme == "devicedna" }
            ?.toString()
            ?.removePrefix("devicedna://open/")
            ?.trim('/')
            ?.takeIf { it.isNotBlank() }
        return fromData ?: intent.getStringExtra(EXTRA_ROUTE)
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

    private fun createLocalizedContext(languageTag: String): Context {
        if (languageTag.isBlank()) return this
        val locale = Locale.forLanguageTag(languageTag)
        val configuration = Configuration(resources.configuration).apply {
            setLocale(locale)
            setLayoutDirection(locale)
        }
        val localizedContext = createConfigurationContext(configuration)
        return object : ContextWrapper(this) {
            override fun getResources(): Resources = localizedContext.resources
            override fun getAssets() = localizedContext.assets
        }
    }

    companion object {
        const val EXTRA_ROUTE = "devicedna.extra.ROUTE"
    }
}
