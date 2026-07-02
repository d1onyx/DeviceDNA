package com.devstdvad.devicedna.navigation

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.unit.dp
import androidx.navigation.NavBackStackEntry
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.devstdvad.devicedna.ads.InterstitialAds
import com.devstdvad.devicedna.ads.NoOpInterstitialAds
import com.devstdvad.devicedna.core.design.AppTheme
import com.devstdvad.devicedna.core.feedback.AppFeedback
import com.devstdvad.devicedna.core.feedback.HapticManager
import com.devstdvad.devicedna.core.feedback.LocalAppFeedback
import com.devstdvad.devicedna.core.feedback.SoundManager
import com.devstdvad.devicedna.data.settings.UserSettings
import com.devstdvad.devicedna.data.subscription.PremiumEntitlements
import com.devstdvad.devicedna.data.subscription.PremiumFeature
import com.devstdvad.devicedna.data.subscription.SubscriptionRepository
import com.devstdvad.devicedna.data.sync.AccountCheckOutcome
import com.devstdvad.devicedna.presentation.apps.AppsScreen
import com.devstdvad.devicedna.presentation.auth.AuthScreen
import com.devstdvad.devicedna.presentation.auth.AuthUiState
import com.devstdvad.devicedna.presentation.batteryintelligence.BatteryChargingPeriodsScreen
import com.devstdvad.devicedna.presentation.batteryintelligence.BatteryChargingSessionScreen
import com.devstdvad.devicedna.presentation.batteryintelligence.BatteryIntelligenceScreen
import com.devstdvad.devicedna.presentation.common.LoadingScreen
import com.devstdvad.devicedna.presentation.hardware.HardwareScreen
import com.devstdvad.devicedna.presentation.onboarding.OnboardingScreen
import com.devstdvad.devicedna.presentation.overview.OverviewScreen
import com.devstdvad.devicedna.presentation.settings.SettingsScreen
import com.devstdvad.devicedna.presentation.subscription.SubscriptionScreen
import com.devstdvad.devicedna.presentation.system.SystemHubScreen
import com.devstdvad.devicedna.presentation.sync.SyncViewModel
import com.devstdvad.devicedna.presentation.tests.TestsScreen
import com.devstdvad.devicedna.resources.stringRes
import org.koin.compose.koinInject
import org.koin.compose.viewmodel.koinViewModel

/**
 * Shared Compose Multiplatform app shell: auth gate → account check → onboarding → main scaffold
 * with the bottom-nav NavHost. Platform-specific chrome is injected as slots:
 *   • [topBanner]      — banner ad host (Android AdMob; iOS no-op)
 *   • [interstitial]   — interstitial ad controller (Android AdMob; iOS no-op)
 *   • [widgetsContent] — home-screen widgets management (Android Glance; iOS WidgetKit/no-op)
 * The Google Sign-In *action* is delegated via [onGoogleSignIn] (platform launches the flow).
 */
@Composable
fun AppNavigation(
    settings: UserSettings,
    authState: AuthUiState,
    onGoogleSignIn: (forceAccountPicker: Boolean) -> Unit,
    onOnboardingComplete: () -> Unit,
    deepLinkRoute: String? = null,
    onDeepLinkHandled: () -> Unit = {},
    interstitial: InterstitialAds = NoOpInterstitialAds,
    topBanner: @Composable (enabled: Boolean) -> Unit = {},
    widgetsContent: @Composable (onBack: () -> Unit, onSubscribe: () -> Unit, padding: PaddingValues) -> Unit = { _, _, _ -> },
) {
    if (authState.isInitializing) {
        Box(modifier = Modifier.fillMaxSize().background(AppTheme.colors.background)) { LoadingScreen() }
        return
    }

    if (!authState.isSignedIn) {
        AuthScreen(state = authState, onGoogleSignIn = { onGoogleSignIn(false) })
        return
    }

    val syncViewModel = koinViewModel<SyncViewModel>()
    val syncState by syncViewModel.state.collectAsState()

    LaunchedEffect(authState.user?.uid) {
        syncViewModel.verifyAccountOnce(authState.user?.uid)
    }

    val accountCheckPending = syncState.accountCheckKey != authState.user?.uid ||
        syncState.lastAccountCheck == null

    if (accountCheckPending || syncState.isCheckingAccount) {
        Box(modifier = Modifier.fillMaxSize().background(AppTheme.colors.background)) { LoadingScreen() }
        return
    }

    if (syncState.lastAccountCheck != AccountCheckOutcome.Verified) {
        val accountError = when (syncState.lastAccountCheck) {
            AccountCheckOutcome.Removed -> "Account no longer exists. Sign in again."
            AccountCheckOutcome.Disabled -> "Account is disabled."
            AccountCheckOutcome.NotSignedIn -> "Sign in again."
            is AccountCheckOutcome.Failed -> "Could not verify account with the server."
            AccountCheckOutcome.Verified -> null
            else -> null
        }
        AuthScreen(
            state = authState.copy(user = null, isLoading = false, errorMessage = accountError),
            onGoogleSignIn = { onGoogleSignIn(true) },
            requirePrivacyConsent = false,
        )
        return
    }

    if (!settings.onboardingComplete) {
        OnboardingScreen(onFinished = onOnboardingComplete, reducedMotion = settings.reducedMotion)
        return
    }

    LaunchedEffect(authState.user?.uid) { syncViewModel.triggerOnce() }

    val hapticManager = koinInject<HapticManager>()
    val soundManager = koinInject<SoundManager>()
    val subscriptionRepository = koinInject<SubscriptionRepository>()
    val entitlements by subscriptionRepository.entitlements.collectAsState(initial = PremiumEntitlements.Empty)
    val feedback = remember(settings.hapticFeedback, settings.soundEffects) {
        AppFeedback(hapticManager, soundManager, settings.hapticFeedback, settings.soundEffects)
    }

    DisposableEffect(Unit) { onDispose { soundManager.release() } }

    CompositionLocalProvider(LocalAppFeedback provides feedback) {
        val navController = rememberNavController()
        val backStack by navController.currentBackStackEntryAsState()
        val currentRoute = backStack?.destination?.route ?: NavRoutes.DASHBOARD

        PlatformBackHandler(enabled = currentRoute != NavRoutes.DASHBOARD) {
            if (!navController.popBackStack()) {
                navController.navigate(NavRoutes.DASHBOARD) {
                    popUpTo(navController.graph.findStartDestination().id) { inclusive = true }
                    launchSingleTop = true
                }
            }
        }
        val colors = AppTheme.colors

        val rootRoutes = bottomNavItems.map { it.route }.toSet()
        val showBottomBar = currentRoute in rootRoutes

        val showAds = !entitlements.hasFeature(PremiumFeature.RemoveAds)
        var interstitialShowing by remember { mutableStateOf(false) }
        var navCount by remember { mutableIntStateOf(0) }
        var lastRootRoute by remember { mutableStateOf<String?>(null) }

        fun onNavEvent() {
            if (!showAds) return
            navCount++
            if (navCount >= 15) {
                interstitial.showIfReady(
                    onShowing = { interstitialShowing = true },
                    onDismissed = { interstitialShowing = false },
                )
                navCount = 0
            }
        }

        LaunchedEffect(currentRoute) {
            if (currentRoute !in rootRoutes) return@LaunchedEffect
            val prev = lastRootRoute
            if (prev != null && currentRoute != prev) onNavEvent()
            lastRootRoute = currentRoute
        }

        var requestedHardwareTab by remember { mutableStateOf<String?>(null) }
        var requestedSystemTab by remember { mutableStateOf<String?>(null) }

        LaunchedEffect(deepLinkRoute) {
            val route = deepLinkRoute ?: return@LaunchedEffect
            val target = when {
                route.startsWith("hardware/") -> {
                    requestedHardwareTab = route.substringAfter("hardware/")
                    NavRoutes.HARDWARE
                }
                route.startsWith("system/") -> {
                    requestedSystemTab = route.substringAfter("system/")
                    NavRoutes.SYSTEM
                }
                else -> route
            }
            if (target != currentRoute) {
                navController.navigate(target) {
                    popUpTo(navController.graph.findStartDestination().id) { saveState = true }
                    launchSingleTop = true
                    restoreState = true
                }
            }
            onDeepLinkHandled()
        }

        Scaffold(
            topBar = { topBanner(showAds && !interstitialShowing) },
            bottomBar = {
                AnimatedContent(
                    targetState = showBottomBar,
                    transitionSpec = {
                        if (settings.reducedMotion) {
                            fadeIn(tween(0)) togetherWith fadeOut(tween(0))
                        } else {
                            (slideInVertically { it } + fadeIn(tween(220))) togetherWith
                                (slideOutVertically { it } + fadeOut(tween(180)))
                        }
                    },
                    label = "bottom_bar_visibility",
                ) { visible ->
                    if (visible) {
                        FloatingPillNavBar(
                            items = bottomNavItems,
                            currentRoute = currentRoute,
                            reducedMotion = settings.reducedMotion,
                            onItemClick = { item ->
                                if (item.route != currentRoute) {
                                    feedback.navTap()
                                    navController.navigate(item.route) {
                                        popUpTo(navController.graph.findStartDestination().id) { saveState = true }
                                        launchSingleTop = true
                                        restoreState = true
                                    }
                                }
                            },
                        )
                    } else {
                        Spacer(Modifier.height(0.dp))
                    }
                }
            },
            containerColor = colors.background,
        ) { padding ->
            NavHost(
                navController = navController,
                startDestination = NavRoutes.DASHBOARD,
                enterTransition = {
                    if (settings.reducedMotion) {
                        fadeIn(tween(0))
                    } else {
                        slideInVertically(
                            initialOffsetY = { (it * 0.04f).toInt() },
                            animationSpec = spring(stiffness = Spring.StiffnessMediumLow),
                        ) + fadeIn(tween(220))
                    }
                },
                exitTransition = { fadeOut(tween(if (settings.reducedMotion) 0 else 160)) },
                popEnterTransition = { fadeIn(tween(if (settings.reducedMotion) 0 else 220)) },
                popExitTransition = {
                    if (settings.reducedMotion) {
                        fadeOut(tween(0))
                    } else {
                        slideOutVertically(
                            targetOffsetY = { (it * 0.04f).toInt() },
                            animationSpec = spring(stiffness = Spring.StiffnessMediumLow),
                        ) + fadeOut(tween(160))
                    }
                },
            ) {
                composable(NavRoutes.DASHBOARD) {
                    OverviewScreen(
                        settings = settings,
                        onSettingsClick = { navController.navigate(NavRoutes.SETTINGS) },
                        contentPadding = padding,
                    )
                }
                composable(NavRoutes.HARDWARE) {
                    HardwareScreen(
                        settings = settings,
                        contentPadding = padding,
                        initialTab = requestedHardwareTab,
                        onTabConsumed = { requestedHardwareTab = null },
                        onTabSelected = ::onNavEvent,
                    )
                }
                composable(NavRoutes.SYSTEM) {
                    SystemHubScreen(
                        settings = settings,
                        contentPadding = padding,
                        initialTab = requestedSystemTab,
                        onTabConsumed = { requestedSystemTab = null },
                        onTabSelected = ::onNavEvent,
                    )
                }
                composable(NavRoutes.APPS) {
                    AppsScreen(settings = settings, contentPadding = padding)
                }
                composable(NavRoutes.TESTS) {
                    TestsScreen(contentPadding = padding)
                }
                composable(NavRoutes.BATTERY_INTELLIGENCE) {
                    BatteryIntelligenceScreen(
                        settings = settings,
                        onSubscribeClick = { navController.navigate(NavRoutes.SUBSCRIPTION) },
                        onChargingSessionClick = { session ->
                            navController.navigate(
                                NavRoutes.batteryChargingSession(session.startMillis, session.endMillis),
                            )
                        },
                        onShowAllChargingSessionsClick = { dayStartMillis ->
                            navController.navigate(NavRoutes.batteryChargingPeriods(dayStartMillis))
                        },
                        contentPadding = padding,
                    )
                }
                composable(NavRoutes.SETTINGS) {
                    SettingsScreen(
                        onSubscriptionClick = { navController.navigate(NavRoutes.SUBSCRIPTION) },
                        onWidgetsClick = { navController.navigate(NavRoutes.WIDGETS) },
                        contentPadding = padding,
                    )
                }
                composable(NavRoutes.SUBSCRIPTION) {
                    SubscriptionScreen(
                        onBackClick = { navController.popBackStack() },
                        contentPadding = padding,
                    )
                }
                composable(NavRoutes.WIDGETS) {
                    widgetsContent(
                        { navController.popBackStack() },
                        { navController.navigate(NavRoutes.SUBSCRIPTION) },
                        padding,
                    )
                }
                composable(
                    route = "${NavRoutes.BATTERY_CHARGING_SESSION}/{${NavRoutes.SESSION_START_ARG}}/{${NavRoutes.SESSION_END_ARG}}",
                    arguments = listOf(
                        navArgument(NavRoutes.SESSION_START_ARG) { type = NavType.LongType },
                        navArgument(NavRoutes.SESSION_END_ARG) { type = NavType.LongType },
                    ),
                ) { entry ->
                    val startMillis = entry.longArgument(NavRoutes.SESSION_START_ARG)
                    val rawEndMillis = entry.longArgument(NavRoutes.SESSION_END_ARG, defaultValue = -1L)
                    BatteryChargingSessionScreen(
                        sessionStartMillis = startMillis,
                        sessionEndMillis = rawEndMillis.takeIf { it >= 0L },
                        settings = settings,
                        onBackClick = { navController.popBackStack() },
                        contentPadding = padding,
                    )
                }
                composable(
                    route = "${NavRoutes.BATTERY_CHARGING_PERIODS}/{${NavRoutes.DAY_START_ARG}}",
                    arguments = listOf(
                        navArgument(NavRoutes.DAY_START_ARG) { type = NavType.LongType },
                    ),
                ) { entry ->
                    val dayStartMillis = entry.longArgument(NavRoutes.DAY_START_ARG)
                    BatteryChargingPeriodsScreen(
                        dayStartMillis = dayStartMillis,
                        settings = settings,
                        onBackClick = { navController.popBackStack() },
                        onChargingSessionClick = { session ->
                            navController.navigate(
                                NavRoutes.batteryChargingSession(session.startMillis, session.endMillis),
                            )
                        },
                        contentPadding = padding,
                    )
                }
            }
        }
    }
}

private fun NavBackStackEntry.longArgument(key: String, defaultValue: Long = 0L): Long =
    arguments?.let { NavType.LongType.get(it, key) } ?: defaultValue

@Composable
private fun FloatingPillNavBar(
    items: List<BottomNavItem>,
    currentRoute: String,
    reducedMotion: Boolean,
    onItemClick: (BottomNavItem) -> Unit,
) {
    val colors = AppTheme.colors
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .windowInsetsPadding(WindowInsets.navigationBars)
            .padding(horizontal = 16.dp, vertical = 12.dp),
        contentAlignment = Alignment.Center,
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .shadow(
                    elevation = 12.dp,
                    shape = RoundedCornerShape(28.dp),
                    ambientColor = colors.accent.copy(alpha = 0.08f),
                    spotColor = colors.accent.copy(alpha = 0.12f),
                )
                .clip(RoundedCornerShape(28.dp))
                .background(colors.surface)
                .padding(horizontal = 8.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.SpaceEvenly,
        ) {
            items.forEach { item ->
                NavPillItem(
                    item = item,
                    selected = item.route == currentRoute,
                    reducedMotion = reducedMotion,
                    onClick = { onItemClick(item) },
                    modifier = Modifier.weight(1f),
                )
            }
        }
    }
}

@Composable
private fun NavPillItem(
    item: BottomNavItem,
    selected: Boolean,
    reducedMotion: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = AppTheme.colors
    val scale by animateFloatAsState(
        targetValue = if (selected) 1f else 0.92f,
        animationSpec = if (reducedMotion) {
            tween(0)
        } else {
            spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessMedium)
        },
        label = "nav_item_scale",
    )
    val labelAlpha by animateFloatAsState(
        targetValue = if (selected) 1f else 0.55f,
        animationSpec = tween(if (reducedMotion) 0 else 180),
        label = "nav_label_alpha",
    )

    Column(
        modifier = modifier
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = onClick,
            )
            .graphicsLayer { scaleX = scale; scaleY = scale }
            .padding(vertical = 4.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(3.dp),
    ) {
        Box(
            modifier = Modifier
                .size(40.dp)
                .clip(CircleShape)
                .background(if (selected) colors.accent.copy(alpha = 0.15f) else colors.surface),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                imageVector = item.icon,
                contentDescription = stringRes(item.labelKey),
                tint = if (selected) colors.accent else colors.textMuted,
                modifier = Modifier.size(20.dp),
            )
        }
        Text(
            text = stringRes(item.labelKey),
            style = MaterialTheme.typography.labelSmall,
            color = if (selected) colors.accent else colors.textMuted,
            modifier = Modifier.graphicsLayer { alpha = labelAlpha },
        )
    }
}
