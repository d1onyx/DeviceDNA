package com.devstdvad.devicedna.data.source

import android.content.Context
import android.hardware.camera2.CameraCharacteristics
import android.hardware.camera2.CameraManager
import android.media.CamcorderProfile
import android.util.Range
import com.devstdvad.devicedna.core.common.AppError
import com.devstdvad.devicedna.core.common.AppResult
import com.devstdvad.devicedna.domain.model.CameraDetails
import com.devstdvad.devicedna.domain.model.CameraFacing
import com.devstdvad.devicedna.domain.model.CameraInfo
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class AndroidCameraDataSource(private val context: Context) {

    suspend fun getCameraInfo(): AppResult<CameraInfo> = withContext(Dispatchers.IO) {
        runCatching {
            val manager = context.getSystemService(Context.CAMERA_SERVICE) as CameraManager
            val cameras = manager.cameraIdList.mapNotNull { id ->
                runCatching { buildCameraDetails(manager, id) }.getOrNull()
            }
            CameraInfo(cameras = cameras)
        }.fold(
            onSuccess = { AppResult.Success(it) },
            onFailure = { AppResult.Error(AppError.Unknown(it.message ?: "Camera read failed")) },
        )
    }

    private fun buildCameraDetails(manager: CameraManager, id: String): CameraDetails {
        val chars = manager.getCameraCharacteristics(id)
        val facingInt = chars.get(CameraCharacteristics.LENS_FACING) ?: CameraCharacteristics.LENS_FACING_BACK
        val facing = when (facingInt) {
            CameraCharacteristics.LENS_FACING_BACK -> CameraFacing.Back
            CameraCharacteristics.LENS_FACING_FRONT -> CameraFacing.Front
            CameraCharacteristics.LENS_FACING_EXTERNAL -> CameraFacing.External
            else -> CameraFacing.Unknown
        }
        val streamMap = chars.get(CameraCharacteristics.SCALER_STREAM_CONFIGURATION_MAP)
        val maxSize = streamMap?.getOutputSizes(android.graphics.ImageFormat.JPEG)
            ?.maxByOrNull { it.width.toLong() * it.height }
        val mp = if (maxSize != null) maxSize.width.toLong() * maxSize.height / 1_000_000f else 0f
        val focalLengths = chars.get(CameraCharacteristics.LENS_INFO_AVAILABLE_FOCAL_LENGTHS)?.toList() ?: emptyList()
        val apertures = chars.get(CameraCharacteristics.LENS_INFO_AVAILABLE_APERTURES)?.toList() ?: emptyList()
        val hasFlash = chars.get(CameraCharacteristics.FLASH_INFO_AVAILABLE) ?: false
        val hasOis = chars.get(CameraCharacteristics.LENS_INFO_AVAILABLE_OPTICAL_STABILIZATION)
            ?.contains(CameraCharacteristics.LENS_OPTICAL_STABILIZATION_MODE_ON) ?: false

        // Best recordable video profile (dimensions + frame rate) via CamcorderProfile.
        var maxVideoWidth = 0
        var maxVideoHeight = 0
        var maxVideoFps = 0
        id.toIntOrNull()?.let { camId ->
            val qualities = intArrayOf(
                CamcorderProfile.QUALITY_2160P,
                CamcorderProfile.QUALITY_1080P,
                CamcorderProfile.QUALITY_720P,
                CamcorderProfile.QUALITY_HIGH,
            )
            for (q in qualities) {
                if (CamcorderProfile.hasProfile(camId, q)) {
                    val p = CamcorderProfile.get(camId, q)
                    maxVideoWidth = p.videoFrameWidth
                    maxVideoHeight = p.videoFrameHeight
                    maxVideoFps = p.videoFrameRate
                    break
                }
            }
        }

        // Fastest slow-motion format: highest high-speed fps and the size captured at it.
        var maxSlowMoFps = 0
        var slowMoWidth = 0
        var slowMoHeight = 0
        streamMap?.highSpeedVideoSizes?.forEach { size ->
            val fps = streamMap.getHighSpeedVideoFpsRangesFor(size).maxOfOrNull { it.upper } ?: 0
            if (fps > maxSlowMoFps) {
                maxSlowMoFps = fps
                slowMoWidth = size.width
                slowMoHeight = size.height
            }
        }

        val exposureRange: Range<Long>? = chars.get(CameraCharacteristics.SENSOR_INFO_EXPOSURE_TIME_RANGE)

        return CameraDetails(
            id = id,
            facing = facing,
            megapixels = mp,
            resolutionWidth = maxSize?.width ?: 0,
            resolutionHeight = maxSize?.height ?: 0,
            focalLengths = focalLengths,
            hasFlash = hasFlash,
            hasOis = hasOis,
            apertures = apertures,
            supportedModes = emptyList(),
            maxVideoWidth = maxVideoWidth,
            maxVideoHeight = maxVideoHeight,
            maxVideoFps = maxVideoFps,
            maxSlowMoFps = maxSlowMoFps,
            slowMoWidth = slowMoWidth,
            slowMoHeight = slowMoHeight,
            maxPhotoWidth = maxSize?.width ?: 0,
            maxPhotoHeight = maxSize?.height ?: 0,
            minExposureNanos = exposureRange?.lower ?: 0L,
            maxExposureNanos = exposureRange?.upper ?: 0L,
        )
    }
}
