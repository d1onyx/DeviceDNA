package com.devstdvad.devicedna.ui

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.runtime.Composable
import com.devstdvad.devicedna.ads.InterstitialAds
import com.devstdvad.devicedna.ads.NoOpInterstitialAds
import com.devstdvad.devicedna.core.design.AppTheme
import com.devstdvad.devicedna.data.settings.UserSettings
import com.devstdvad.devicedna.navigation.AppNavigation
import com.devstdvad.devicedna.presentation.auth.AuthUiState
import com.devstdvad.devicedna.resources.AppLanguage

/**
 * Shared Compose Multiplatform entry point, wrapping [AppTheme] around the [AppNavigation] shell.
 * Rendered by both platforms:
 *   • Android: setContent { App(...) } (or AppNavigation directly, as MainActivity does today)
 *   • iOS:     ComposeUIViewController { App(...) } from the SwiftUI/UIKit host
 *
 * State (settings, auth) and platform chrome (ads, widgets) are injected so the host owns the
 * platform edges while the whole screen graph stays shared.
 */
@Composable
fun App(
    settings: UserSettings,
    authState: AuthUiState,
    onGoogleSignIn: (forceAccountPicker: Boolean) -> Unit,
    onOnboardingComplete: () -> Unit,
    darkTheme: Boolean,
    language: AppLanguage = AppLanguage.En,
    deepLinkRoute: String? = null,
    onDeepLinkHandled: () -> Unit = {},
    interstitial: InterstitialAds = NoOpInterstitialAds,
    topBanner: @Composable (enabled: Boolean) -> Unit = {},
    widgetsContent: @Composable (onBack: () -> Unit, onSubscribe: () -> Unit, padding: PaddingValues) -> Unit = { _, _, _ -> },
    onAppleSignIn: () -> Unit = {},
    showAppleSignIn: Boolean = false,
) {
    AppTheme(darkTheme = darkTheme, language = language) {
        AppNavigation(
            settings = settings,
            authState = authState,
            onGoogleSignIn = onGoogleSignIn,
            onOnboardingComplete = onOnboardingComplete,
            deepLinkRoute = deepLinkRoute,
            onDeepLinkHandled = onDeepLinkHandled,
            interstitial = interstitial,
            topBanner = topBanner,
            widgetsContent = widgetsContent,
            onAppleSignIn = onAppleSignIn,
            showAppleSignIn = showAppleSignIn,
        )
    }
}
