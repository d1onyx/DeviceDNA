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
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.devstdvad.devicedna.core.design.AppTheme
import com.devstdvad.devicedna.core.feedback.AppFeedback
import com.devstdvad.devicedna.core.feedback.HapticManager
import com.devstdvad.devicedna.core.feedback.LocalAppFeedback
import com.devstdvad.devicedna.core.feedback.SoundManager
import com.devstdvad.devicedna.data.settings.UserSettings
import com.devstdvad.devicedna.presentation.apps.AppsScreen
import com.devstdvad.devicedna.presentation.auth.AuthScreen
import com.devstdvad.devicedna.presentation.auth.AuthUiState
import com.devstdvad.devicedna.presentation.hardware.HardwareScreen
import com.devstdvad.devicedna.presentation.common.LoadingScreen
import com.devstdvad.devicedna.presentation.onboarding.OnboardingScreen
import com.devstdvad.devicedna.presentation.overview.OverviewScreen
import com.devstdvad.devicedna.presentation.settings.SettingsScreen
import com.devstdvad.devicedna.presentation.system.SystemHubScreen
import com.devstdvad.devicedna.presentation.sync.SyncViewModel
import com.devstdvad.devicedna.presentation.tests.TestsScreen
import org.koin.androidx.compose.koinViewModel
import org.koin.compose.koinInject

@Composable
fun AppNavigation(
    settings: UserSettings,
    authState: AuthUiState,
    onGoogleSignIn: () -> Unit,
    onOnboardingComplete: () -> Unit,
) {
    // Wait for Firebase to restore the session before deciding what to show,
    // otherwise the sign-in screen flashes for already-signed-in users.
    if (authState.isInitializing) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(AppTheme.colors.background),
        ) {
            LoadingScreen()
        }
        return
    }

    if (!authState.isSignedIn) {
        AuthScreen(state = authState, onGoogleSignIn = onGoogleSignIn)
        return
    }

    if (!settings.onboardingComplete) {
        OnboardingScreen(onFinished = onOnboardingComplete, reducedMotion = settings.reducedMotion)
        return
    }

    // Check/sync the device on startup (runs in background, does not block UI).
    val syncViewModel = koinViewModel<SyncViewModel>()
    LaunchedEffect(authState.user?.email) { syncViewModel.triggerOnce() }

    val hapticManager = koinInject<HapticManager>()
    val soundManager = koinInject<SoundManager>()
    val feedback = remember(settings.hapticFeedback, settings.soundEffects) {
        AppFeedback(hapticManager, soundManager, settings.hapticFeedback, settings.soundEffects)
    }

    DisposableEffect(Unit) {
        onDispose { soundManager.release() }
    }

    CompositionLocalProvider(LocalAppFeedback provides feedback) {
        val navController = rememberNavController()
        val backStack by navController.currentBackStackEntryAsState()
        val currentRoute = backStack?.destination?.route ?: NavRoutes.DASHBOARD
        val colors = AppTheme.colors

        val rootRoutes = bottomNavItems.map { it.route }.toSet()
        val showBottomBar = currentRoute in rootRoutes

        Scaffold(
            bottomBar = {
                AnimatedContent(
                    targetState = showBottomBar,
                    transitionSpec = {
                        (slideInVertically { it } + fadeIn(tween(220))) togetherWith
                            (slideOutVertically { it } + fadeOut(tween(180)))
                    },
                    label = "bottom_bar_visibility",
                ) { visible ->
                    if (visible) {
                        FloatingPillNavBar(
                            items = bottomNavItems,
                            currentRoute = currentRoute,
                            onItemClick = { item ->
                                if (item.route != currentRoute) {
                                    feedback.navTap()
                                    navController.navigate(item.route) {
                                        popUpTo(navController.graph.findStartDestination().id) {
                                            saveState = true
                                        }
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
                    slideInVertically(
                        initialOffsetY = { (it * 0.04f).toInt() },
                        animationSpec = spring(stiffness = Spring.StiffnessMediumLow),
                    ) + fadeIn(tween(220))
                },
                exitTransition = { fadeOut(tween(160)) },
                popEnterTransition = { fadeIn(tween(220)) },
                popExitTransition = {
                    slideOutVertically(
                        targetOffsetY = { (it * 0.04f).toInt() },
                        animationSpec = spring(stiffness = Spring.StiffnessMediumLow),
                    ) + fadeOut(tween(160))
                },
            ) {
                composable(NavRoutes.DASHBOARD) {
                    OverviewScreen(
                        onSettingsClick = { navController.navigate(NavRoutes.SETTINGS) },
                        contentPadding = padding,
                    )
                }
                composable(NavRoutes.HARDWARE) {
                    HardwareScreen(contentPadding = padding)
                }
                composable(NavRoutes.SYSTEM) {
                    SystemHubScreen(contentPadding = padding)
                }
                composable(NavRoutes.APPS) {
                    AppsScreen(contentPadding = padding)
                }
                composable(NavRoutes.TESTS) {
                    TestsScreen(contentPadding = padding)
                }
                composable(NavRoutes.SETTINGS) {
                    SettingsScreen()
                }
            }
        }
    }
}

@Composable
private fun FloatingPillNavBar(
    items: List<BottomNavItem>,
    currentRoute: String,
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
                val selected = item.route == currentRoute
                NavPillItem(
                    item = item,
                    selected = selected,
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
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = AppTheme.colors
    val scale by animateFloatAsState(
        targetValue = if (selected) 1f else 0.92f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessMedium,
        ),
        label = "nav_item_scale",
    )
    val labelAlpha by animateFloatAsState(
        targetValue = if (selected) 1f else 0.55f,
        animationSpec = tween(180),
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
                .background(
                    if (selected) colors.accent.copy(alpha = 0.15f) else colors.surface,
                ),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                imageVector = item.icon,
                contentDescription = stringResource(item.labelRes),
                tint = if (selected) colors.accent else colors.textMuted,
                modifier = Modifier.size(20.dp),
            )
        }
        Text(
            text = stringResource(item.labelRes),
            style = MaterialTheme.typography.labelSmall,
            color = if (selected) colors.accent else colors.textMuted,
            modifier = Modifier.graphicsLayer { alpha = labelAlpha },
        )
    }
}
