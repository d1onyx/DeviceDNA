package com.devstdvad.devicedna.domain.model

import kotlinx.serialization.Serializable

@Serializable
data class CameraInfo(
    val cameras: List<CameraDetails>,
)

@Serializable
data class CameraDetails(
    val id: String,
    val facing: CameraFacing,
    val megapixels: Float,
    val resolutionWidth: Int,
    val resolutionHeight: Int,
    val focalLengths: List<Float>,
    val hasFlash: Boolean,
    val hasOis: Boolean,
    val apertures: List<Float>,
    val supportedModes: List<String>,
)

@Serializable
enum class CameraFacing { Back, Front, External, Unknown }
