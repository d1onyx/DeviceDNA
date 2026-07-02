package com.devstdvad.devicedna.core.design

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.graphics.Color
import com.devstdvad.devicedna.resources.AppLanguage
import com.devstdvad.devicedna.resources.LocalStrings
import com.devstdvad.devicedna.resources.stringsFor

private val M3Dark = darkColorScheme(
    background = Color(0xFF09090B),
    surface = Color(0xFF111113),
    surfaceVariant = Color(0xFF18181B),
    outline = Color(0xFF27272A),
    primary = Color(0xFFA7C7FF),
    onPrimary = Color(0xFF09090B),
    onBackground = Color(0xFFFAFAFA),
    onSurface = Color(0xFFFAFAFA),
    onSurfaceVariant = Color(0xFFA1A1AA),
    error = Color(0xFFEF4444),
)

private val M3Light = lightColorScheme(
    background = Color(0xFFFFFFFF),
    surface = Color(0xFFF8FAFC),
    surfaceVariant = Color(0xFFFFFFFF),
    outline = Color(0xFFE4E4E7),
    primary = Color(0xFF2563EB),
    onPrimary = Color(0xFFFFFFFF),
    onBackground = Color(0xFF09090B),
    onSurface = Color(0xFF09090B),
    onSurfaceVariant = Color(0xFF52525B),
    error = Color(0xFFDC2626),
)

@Composable
fun AppTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    language: AppLanguage = AppLanguage.En,
    content: @Composable () -> Unit,
) {
    val appColors = if (darkTheme) DarkColorScheme else LightColorScheme
    val m3Colors = if (darkTheme) M3Dark else M3Light

    CompositionLocalProvider(
        LocalAppColors provides appColors,
        LocalAppSpacing provides AppSpacing(),
        LocalStrings provides stringsFor(language),
    ) {
        MaterialTheme(
            colorScheme = m3Colors,
            typography = AppTypography,
            content = content,
        )
    }
}

object AppTheme {
    val colors: AppColorScheme @Composable get() = LocalAppColors.current
    val spacing: AppSpacing @Composable get() = LocalAppSpacing.current
}
