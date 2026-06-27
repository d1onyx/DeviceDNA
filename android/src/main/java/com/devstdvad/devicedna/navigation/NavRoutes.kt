package com.devstdvad.devicedna.navigation

import androidx.compose.material.icons.automirrored.outlined.FactCheck
import androidx.annotation.StringRes
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Apps
import androidx.compose.material.icons.outlined.Memory
import androidx.compose.material.icons.outlined.PhoneAndroid
import androidx.compose.material.icons.outlined.Speed
import androidx.compose.ui.graphics.vector.ImageVector
import com.devstdvad.devicedna.R

object NavRoutes {
    // Bottom nav root destinations
    const val DASHBOARD = "dashboard"
    const val HARDWARE = "hardware"
    const val SYSTEM = "system"
    const val APPS = "apps"
    const val TESTS = "tests"

    // Supplementary screens
    const val SETTINGS = "settings"
    const val SUBSCRIPTION = "subscription"

    // Hardware sub-sections (used as tab IDs within HardwareScreen)
    const val DEVICE = "device"
    const val CPU = "cpu"
    const val BATTERY = "battery"
    const val DISPLAY = "display"
    const val CAMERA = "camera"
    const val THERMAL = "thermal"

    // System sub-sections (used as tab IDs within SystemScreen)
    const val OS = "os"
    const val NETWORK = "network"
    const val CONNECTIVITY = "connectivity"
    const val SENSORS = "sensors"
}

data class BottomNavItem(
    val route: String,
    @param:StringRes val labelRes: Int,
    val icon: ImageVector,
)

val bottomNavItems = listOf(
    BottomNavItem(NavRoutes.DASHBOARD, R.string.nav_dashboard, Icons.Outlined.Speed),
    BottomNavItem(NavRoutes.HARDWARE, R.string.nav_hardware, Icons.Outlined.Memory),
    BottomNavItem(NavRoutes.SYSTEM, R.string.nav_system_hub, Icons.Outlined.PhoneAndroid),
    BottomNavItem(NavRoutes.APPS, R.string.nav_apps, Icons.Outlined.Apps),
    BottomNavItem(NavRoutes.TESTS, R.string.nav_tests, Icons.AutoMirrored.Outlined.FactCheck),
)
