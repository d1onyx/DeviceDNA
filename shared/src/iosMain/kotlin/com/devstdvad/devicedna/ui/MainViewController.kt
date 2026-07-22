@file:OptIn(kotlinx.cinterop.ExperimentalForeignApi::class)

package com.devstdvad.devicedna.ui

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.material3.Text
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.UIKitView
import androidx.compose.ui.window.ComposeUIViewController
import com.devstdvad.devicedna.ads.InterstitialAds
import com.devstdvad.devicedna.ads.NoOpInterstitialAds
import com.devstdvad.devicedna.ads.IosAdsState
import com.devstdvad.devicedna.data.settings.AppThemeMode
import com.devstdvad.devicedna.data.settings.UserSettings
import com.devstdvad.devicedna.di.KoinBridge
import com.devstdvad.devicedna.navigation.DeepLinkHolder
import com.devstdvad.devicedna.presentation.auth.AuthUiState
import com.devstdvad.devicedna.presentation.widgets.IosWidgetsScreen
import com.devstdvad.devicedna.resources.AppLanguage
import kotlinx.coroutines.launch
import platform.UIKit.UIView
import platform.UIKit.UIViewController

/**
 * iOS entry point for the shared Compose Multiplatform UI. The Swift host calls this after
 * starting Koin (KoinBridge.start) and passes its platform edges:
 *   • [onGoogleSignIn]     — launches the GIDSignIn flow (Swift)
 *   • [interstitial]       — AdMob interstitial bridge (Swift implements the Kotlin protocol)
 *   • [bannerViewFactory]  — creates the AdMob banner UIView; null disables the banner slot
 *
 * Settings, auth state and deep links are collected here so the whole App() shell reacts to
 * them exactly like Android's MainActivity does.
 *
 * COMPILES ONLY ON macOS (Kotlin/Native iOS target).
 */
fun MainViewController(
    onGoogleSignIn: (forceAccountPicker: Boolean) -> Unit,
    onAppleSignIn: () -> Unit = {},
    interstitial: InterstitialAds = NoOpInterstitialAds,
    bannerViewFactory: (() -> UIView)? = null,
    onAdPrivacyOptions: () -> Unit = {},
    showAdDiagnostics: Boolean = false,
): UIViewController = ComposeUIViewController {
    val settingsStore = KoinBridge.settingsStore()
    val authGateway = KoinBridge.authGateway()

    val scope = rememberCoroutineScope()
    val settings by settingsStore.settings.collectAsState(initial = UserSettings())
    val user by authGateway.user.collectAsState()
    val isInitializing by authGateway.isInitializing.collectAsState()
    val isSigningIn by authGateway.isSigningIn.collectAsState()
    val authError by authGateway.errorMessage.collectAsState()
    val deepLink by DeepLinkHolder.route.collectAsState()
    val canShowAds by IosAdsState.canShowAds.collectAsState()
    val showAdPrivacyOptions by IosAdsState.privacyOptionsRequired.collectAsState()

    val systemDark = isSystemInDarkTheme()
    val darkTheme = when (settings.theme) {
        AppThemeMode.Light -> false
        AppThemeMode.Dark -> true
        AppThemeMode.System -> systemDark
    }

    val authState = AuthUiState(
        user = user,
        isConfigured = authGateway.isConfigured,
        isLoading = isSigningIn,
        errorMessage = authError,
        isInitializing = isInitializing,
    )

    App(
        settings = settings,
        authState = authState,
        onGoogleSignIn = onGoogleSignIn,
        onAppleSignIn = onAppleSignIn,
        showAppleSignIn = true,
        onAdPrivacyOptions = onAdPrivacyOptions,
        showAdPrivacyOptions = showAdPrivacyOptions,
        onContinueWithoutAccount = {
            scope.launch { settingsStore.setGuestMode(true) }
        },
        onExitGuestMode = {
            scope.launch { settingsStore.setGuestMode(false) }
        },
        onOnboardingComplete = {
            scope.launch { settingsStore.setOnboardingComplete(true) }
        },
        darkTheme = darkTheme,
        language = AppLanguage.fromTag(settings.appLanguage),
        deepLinkRoute = deepLink,
        onDeepLinkHandled = { DeepLinkHolder.consume() },
        interstitial = interstitial,
        topBanner = { enabled ->
            val showBanner = enabled && canShowAds && bannerViewFactory != null
            if (showBanner || showAdDiagnostics) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .windowInsetsPadding(WindowInsets.statusBars),
                ) {
                    if (showBanner) {
                        UIKitView(
                            factory = bannerViewFactory!!,
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(60.dp),
                        )
                    }
                    if (showAdDiagnostics) {
                        val bannerStatus by IosAdsState.bannerStatus.collectAsState()
                        val interstitialStatus by IosAdsState.interstitialStatus.collectAsState()
                        Text(
                            text = "ads debug — enabled=$enabled canShow=$canShowAds " +
                                "banner=${bannerStatus ?: "—"} interstitial=${interstitialStatus ?: "—"}",
                            color = Color.Red,
                            fontSize = 10.sp,
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(4.dp),
                        )
                    }
                }
            }
        },
        // In-app widgets screen: lists the available WidgetKit widgets with add
        // instructions and a Premium gate (iOS cannot pin widgets programmatically).
        widgetsContent = { onBack, onSubscribe, padding ->
            IosWidgetsScreen(onBackClick = onBack, onSubscribeClick = onSubscribe, contentPadding = padding)
        },
    )
}
