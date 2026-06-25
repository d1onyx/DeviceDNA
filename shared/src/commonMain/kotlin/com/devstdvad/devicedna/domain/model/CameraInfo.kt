package com.devstdvad.devicedna.domain.model

data class CameraInfo(
    val cameras: List<CameraDetails>,
)

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

enum class CameraFacing { Back, Front, External, Unknown }
