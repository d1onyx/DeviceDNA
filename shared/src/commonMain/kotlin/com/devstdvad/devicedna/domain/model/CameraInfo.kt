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
    // Richer capabilities (defaults keep older serialized payloads / other call sites valid).
    // Highest-resolution video format and the fps it supports (e.g. 3840×2160 @ 60).
    val maxVideoWidth: Int = 0,
    val maxVideoHeight: Int = 0,
    val maxVideoFps: Int = 0,
    // Fastest (slow-motion) format: highest fps and the resolution captured at that speed.
    val maxSlowMoFps: Int = 0,
    val slowMoWidth: Int = 0,
    val slowMoHeight: Int = 0,
    // Largest still-photo dimensions.
    val maxPhotoWidth: Int = 0,
    val maxPhotoHeight: Int = 0,
    // Exposure range in nanoseconds (0 = not reported).
    val minExposureNanos: Long = 0L,
    val maxExposureNanos: Long = 0L,
)

@Serializable
enum class CameraFacing { Back, Front, External, Unknown }
