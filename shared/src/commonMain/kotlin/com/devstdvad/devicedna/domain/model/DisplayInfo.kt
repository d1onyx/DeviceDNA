package com.devstdvad.devicedna.domain.model

import kotlinx.serialization.Serializable

@Serializable
data class DisplayInfo(
    val widthPx: Int,
    val heightPx: Int,
    val densityDpi: Int,
    val densityBucket: String,
    val fontScale: Float,
    val physicalSizeInches: Float,
    val refreshRateHz: Float,
    val supportedRefreshRates: List<Float>,
    val hdrCapabilities: List<String>,
    val isHdr: Boolean,
    val isWideColorGamut: Boolean,
    val brightnessLevel: Float,
    val isAdaptiveBrightness: Boolean,
    val orientation: String,
    val displayType: String,
)
