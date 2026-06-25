package com.devstdvad.devicedna.core.design

import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import com.devstdvad.devicedna.core.design.DesignTokens

@Immutable
data class AppColorScheme(
    val background: Color,
    val surface: Color,
    val surfaceElevated: Color,
    val surfaceHover: Color,
    val border: Color,
    val textPrimary: Color,
    val textSecondary: Color,
    val textMuted: Color,
    val accent: Color,
    val accentStrong: Color,
    val success: Color,
    val warning: Color,
    val critical: Color,
    val info: Color,
    // Section-specific colors sourced from DesignTokens
    val cpuColor: Color,
    val batteryColor: Color,
    val thermalColor: Color,
    val ramColor: Color,
    val storageColor: Color,
    val displayColor: Color,
    val cameraColor: Color,
    val networkColor: Color,
    val sensorsColor: Color,
    val deviceColor: Color,
)

val DarkColorScheme = AppColorScheme(
    background    = Color(0xFF09090B),
    surface       = Color(0xFF111113),
    surfaceElevated = Color(0xFF18181B),
    surfaceHover  = Color(0xFF1F1F23),
    border        = Color(0xFF27272A),
    textPrimary   = Color(0xFFFAFAFA),
    textSecondary = Color(0xFFA1A1AA),
    textMuted     = Color(0xFF71717A),
    accent        = Color(DesignTokens.colorAccentDark),
    accentStrong  = Color(0xFF60A5FA),
    success       = Color(DesignTokens.colorSuccessDark),
    warning       = Color(DesignTokens.colorWarningDark),
    critical      = Color(DesignTokens.colorCriticalDark),
    info          = Color(DesignTokens.colorInfoDark),
    cpuColor      = Color(DesignTokens.colorCpuDark),
    batteryColor  = Color(DesignTokens.colorBatteryDark),
    thermalColor  = Color(DesignTokens.colorThermalDark),
    ramColor      = Color(DesignTokens.colorRamDark),
    storageColor  = Color(DesignTokens.colorStorageDark),
    displayColor  = Color(DesignTokens.colorDisplayDark),
    cameraColor   = Color(DesignTokens.colorCameraDark),
    networkColor  = Color(DesignTokens.colorNetworkDark),
    sensorsColor  = Color(DesignTokens.colorSensorsDark),
    deviceColor   = Color(DesignTokens.colorDeviceDark),
)

val LightColorScheme = AppColorScheme(
    background    = Color(0xFFFFFFFF),
    surface       = Color(0xFFF8FAFC),
    surfaceElevated = Color(0xFFFFFFFF),
    surfaceHover  = Color(0xFFF1F5F9),
    border        = Color(0xFFE4E4E7),
    textPrimary   = Color(0xFF09090B),
    textSecondary = Color(0xFF52525B),
    textMuted     = Color(0xFF71717A),
    accent        = Color(DesignTokens.colorAccentLight),
    accentStrong  = Color(0xFF1D4ED8),
    success       = Color(DesignTokens.colorSuccessLight),
    warning       = Color(DesignTokens.colorWarningLight),
    critical      = Color(DesignTokens.colorCriticalLight),
    info          = Color(DesignTokens.colorInfoLight),
    cpuColor      = Color(DesignTokens.colorCpuLight),
    batteryColor  = Color(DesignTokens.colorBatteryLight),
    thermalColor  = Color(DesignTokens.colorThermalLight),
    ramColor      = Color(DesignTokens.colorRamLight),
    storageColor  = Color(DesignTokens.colorStorageLight),
    displayColor  = Color(DesignTokens.colorDisplayLight),
    cameraColor   = Color(DesignTokens.colorCameraLight),
    networkColor  = Color(DesignTokens.colorNetworkLight),
    sensorsColor  = Color(DesignTokens.colorSensorsLight),
    deviceColor   = Color(DesignTokens.colorDeviceLight),
)

val LocalAppColors = staticCompositionLocalOf { DarkColorScheme }
