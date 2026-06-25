package com.devstdvad.devicedna.core.design

/**
 * Single source of truth for design constants shared across Android and iOS.
 * Colors are stored as ARGB Long values (0xAARRGGBB).
 * Android: Color(token) — iOS: use rgbFromToken() helper or read components.
 */
object DesignTokens {

    // ── Section accent colors (dark theme) ─────────────────────────────────
    const val colorCpuDark: Long        = 0xFF60A5FA
    const val colorBatteryDark: Long    = 0xFF22C55E
    const val colorThermalDark: Long    = 0xFFF97316
    const val colorRamDark: Long        = 0xFFA78BFA
    const val colorStorageDark: Long    = 0xFFF59E0B
    const val colorDisplayDark: Long    = 0xFF38BDF8
    const val colorCameraDark: Long     = 0xFFEC4899
    const val colorNetworkDark: Long    = 0xFF06B6D4
    const val colorSensorsDark: Long    = 0xFF10B981
    const val colorDeviceDark: Long     = 0xFFA1A1AA
    const val colorAccentDark: Long     = 0xFFA7C7FF
    const val colorSuccessDark: Long    = 0xFF22C55E
    const val colorWarningDark: Long    = 0xFFF59E0B
    const val colorCriticalDark: Long   = 0xFFEF4444
    const val colorInfoDark: Long       = 0xFF38BDF8

    // ── Section accent colors (light theme) ────────────────────────────────
    const val colorCpuLight: Long       = 0xFF2563EB
    const val colorBatteryLight: Long   = 0xFF16A34A
    const val colorThermalLight: Long   = 0xFFEA580C
    const val colorRamLight: Long       = 0xFF7C3AED
    const val colorStorageLight: Long   = 0xFFD97706
    const val colorDisplayLight: Long   = 0xFF0284C7
    const val colorCameraLight: Long    = 0xFFDB2777
    const val colorNetworkLight: Long   = 0xFF0891B2
    const val colorSensorsLight: Long   = 0xFF059669
    const val colorDeviceLight: Long    = 0xFF71717A
    const val colorAccentLight: Long    = 0xFF2563EB
    const val colorSuccessLight: Long   = 0xFF16A34A
    const val colorWarningLight: Long   = 0xFFD97706
    const val colorCriticalLight: Long  = 0xFFDC2626
    const val colorInfoLight: Long      = 0xFF0284C7

    // ── Spacing (dp / pt) ──────────────────────────────────────────────────
    const val spacingXs = 4f
    const val spacingS  = 8f
    const val spacingM  = 16f
    const val spacingL  = 24f
    const val spacingXl = 32f

    // ── Corner radii ───────────────────────────────────────────────────────
    const val radiusS    = 8f
    const val radiusM    = 12f
    const val radiusL    = 16f
    const val radiusXl   = 24f
    const val radiusPill = 99f

    // ── Helpers ────────────────────────────────────────────────────────────

    /** Extract red channel 0–255 from a packed ARGB Long. */
    fun red(argb: Long): Int   = ((argb shr 16) and 0xFF).toInt()

    /** Extract green channel 0–255 from a packed ARGB Long. */
    fun green(argb: Long): Int = ((argb shr 8) and 0xFF).toInt()

    /** Extract blue channel 0–255 from a packed ARGB Long. */
    fun blue(argb: Long): Int  = (argb and 0xFF).toInt()

    /** Extract alpha channel 0–255 from a packed ARGB Long (usually 255). */
    fun alpha(argb: Long): Int = ((argb shr 24) and 0xFF).toInt()
}
