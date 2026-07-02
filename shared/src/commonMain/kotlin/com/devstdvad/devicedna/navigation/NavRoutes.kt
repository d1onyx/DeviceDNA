package com.devstdvad.devicedna.navigation

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.FactCheck
import androidx.compose.material.icons.outlined.Apps
import androidx.compose.material.icons.outlined.BatteryChargingFull
import androidx.compose.material.icons.outlined.Memory
import androidx.compose.material.icons.outlined.PhoneAndroid
import androidx.compose.material.icons.outlined.Speed
import androidx.compose.ui.graphics.vector.ImageVector

object NavRoutes {
    // Bottom nav root destinations
    const val DASHBOARD = "dashboard"
    const val HARDWARE = "hardware"
    const val SYSTEM = "system"
    const val APPS = "apps"
    const val TESTS = "tests"
    const val BATTERY_INTELLIGENCE = "battery_intelligence"

    // Supplementary screens
    const val SETTINGS = "settings"
    const val SUBSCRIPTION = "subscription"
    const val WIDGETS = "widgets"
    const val BATTERY_CHARGING_SESSION = "battery_charging_session"
    const val BATTERY_CHARGING_PERIODS = "battery_charging_periods"
    const val SESSION_START_ARG = "sessionStart"
    const val SESSION_END_ARG = "sessionEnd"
    const val DAY_START_ARG = "dayStart"

    fun batteryChargingSession(startMillis: Long, endMillis: Long?): String =
        "$BATTERY_CHARGING_SESSION/$startMillis/${endMillis ?: -1L}"

    fun batteryChargingPeriods(dayStartMillis: Long): String =
        "$BATTERY_CHARGING_PERIODS/$dayStartMillis"

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
    val labelKey: String,
    val icon: ImageVector,
)

val bottomNavItems = listOf(
    BottomNavItem(NavRoutes.DASHBOARD, "nav_dashboard", Icons.Outlined.Speed),
    BottomNavItem(NavRoutes.HARDWARE, "nav_hardware", Icons.Outlined.Memory),
    BottomNavItem(NavRoutes.SYSTEM, "nav_system_hub", Icons.Outlined.PhoneAndroid),
    BottomNavItem(NavRoutes.APPS, "nav_apps", Icons.Outlined.Apps),
    BottomNavItem(NavRoutes.TESTS, "nav_tests", Icons.AutoMirrored.Outlined.FactCheck),
    BottomNavItem(NavRoutes.BATTERY_INTELLIGENCE, "nav_battery_intelligence", Icons.Outlined.BatteryChargingFull),
)
