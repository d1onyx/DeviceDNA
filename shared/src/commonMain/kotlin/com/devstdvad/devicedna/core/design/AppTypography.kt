package com.devstdvad.devicedna.core.design

import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

val AppTypography = Typography(
    displayLarge = TextStyle(fontSize = 36.sp, fontWeight = FontWeight.Bold, lineHeight = 44.sp, letterSpacing = (-0.5).sp),
    displayMedium = TextStyle(fontSize = 28.sp, fontWeight = FontWeight.Bold, lineHeight = 36.sp, letterSpacing = (-0.3).sp),
    displaySmall = TextStyle(fontSize = 20.sp, fontWeight = FontWeight.SemiBold, lineHeight = 26.sp, letterSpacing = (-0.2).sp),
    headlineLarge = TextStyle(fontSize = 40.sp, fontWeight = FontWeight.Bold, lineHeight = 48.sp, letterSpacing = (-1.0).sp),
    headlineMedium = TextStyle(fontSize = 28.sp, fontWeight = FontWeight.Bold, lineHeight = 36.sp, letterSpacing = (-0.5).sp),
    headlineSmall = TextStyle(fontSize = 22.sp, fontWeight = FontWeight.SemiBold, lineHeight = 30.sp, letterSpacing = (-0.3).sp),
    titleLarge = TextStyle(fontSize = 15.sp, fontWeight = FontWeight.SemiBold, lineHeight = 22.sp, letterSpacing = 0.sp),
    titleMedium = TextStyle(fontSize = 14.sp, fontWeight = FontWeight.Medium, lineHeight = 20.sp, letterSpacing = 0.1.sp),
    titleSmall = TextStyle(fontSize = 13.sp, fontWeight = FontWeight.Medium, lineHeight = 18.sp, letterSpacing = 0.1.sp),
    bodyLarge = TextStyle(fontSize = 14.sp, fontWeight = FontWeight.Normal, lineHeight = 20.sp),
    bodyMedium = TextStyle(fontSize = 13.sp, fontWeight = FontWeight.Normal, lineHeight = 18.sp),
    bodySmall = TextStyle(fontSize = 12.sp, fontWeight = FontWeight.Normal, lineHeight = 16.sp),
    labelLarge = TextStyle(fontSize = 12.sp, fontWeight = FontWeight.Medium, lineHeight = 16.sp, letterSpacing = 0.5.sp),
    labelMedium = TextStyle(fontSize = 11.sp, fontWeight = FontWeight.Medium, lineHeight = 14.sp, letterSpacing = 0.5.sp),
    labelSmall = TextStyle(fontSize = 10.sp, fontWeight = FontWeight.Medium, lineHeight = 14.sp, letterSpacing = 0.8.sp),
)
