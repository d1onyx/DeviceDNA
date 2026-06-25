package com.devstdvad.devicedna.data.source

import android.content.Context
import android.hardware.camera2.CameraCharacteristics
import android.hardware.camera2.CameraManager
import com.devstdvad.devicedna.core.common.AppError
import com.devstdvad.devicedna.core.common.AppResult
import com.devstdvad.devicedna.domain.model.CameraDetails
import com.devstdvad.devicedna.domain.model.CameraFacing
import com.devstdvad.devicedna.domain.model.CameraInfo
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlin.math.sqrt

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
        )
    }
}
