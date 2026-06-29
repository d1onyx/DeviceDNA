package com.devstdvad.devicedna.data.source

import android.content.Context
import android.os.Build
import android.provider.Settings
import android.util.DisplayMetrics
import android.view.Display
import android.view.WindowManager
import com.devstdvad.devicedna.core.common.AppError
import com.devstdvad.devicedna.core.common.AppResult
import com.devstdvad.devicedna.domain.model.DisplayInfo
import kotlin.math.sqrt

class AndroidDisplayDataSource(private val context: Context) {

    fun getDisplayInfo(): AppResult<DisplayInfo> = runCatching {
        val wm = context.getSystemService(Context.WINDOW_SERVICE) as WindowManager

        @Suppress("DEPRECATION")
        val display = wm.defaultDisplay

        val metrics = DisplayMetrics()
        @Suppress("DEPRECATION")
        display.getRealMetrics(metrics)

        val widthPx = metrics.widthPixels
        val heightPx = metrics.heightPixels
        val dpi = metrics.densityDpi
        val xdpi = metrics.xdpi.takeIf { it > 0 } ?: dpi.toFloat()
        val ydpi = metrics.ydpi.takeIf { it > 0 } ?: dpi.toFloat()
        val widthInches = widthPx / xdpi
        val heightInches = heightPx / ydpi
        val diagInches = sqrt(
            widthInches.toDouble().let { it * it } + heightInches.toDouble().let { it * it },
        ).toFloat()

        val fontScale = context.resources.configuration.fontScale

        val supportedRefreshRates = display.supportedModes
            .map { it.refreshRate }
            .distinct()
            .sorted()
            .ifEmpty { listOf(display.refreshRate) }
        val refreshRate = supportedRefreshRates.maxOrNull() ?: display.refreshRate

        val brightnessRaw = runCatching {
            Settings.System.getInt(context.contentResolver, Settings.System.SCREEN_BRIGHTNESS, 128)
        }.getOrDefault(128)

        val isAdaptive = runCatching {
            Settings.System.getInt(context.contentResolver, Settings.System.SCREEN_BRIGHTNESS_MODE) ==
                Settings.System.SCREEN_BRIGHTNESS_MODE_AUTOMATIC
        }.getOrDefault(false)

        val densityBucket = when {
            dpi <= 120 -> "ldpi"
            dpi <= 160 -> "mdpi"
            dpi <= 240 -> "hdpi"
            dpi <= 320 -> "xhdpi"
            dpi <= 480 -> "xxhdpi"
            else -> "xxxhdpi"
        }

        // Detect actual HDR support. API 34 moved this from HdrCapabilities to Display.Mode.
        val hdrTypes = readSupportedHdrTypes(display)
        val hdrNames = hdrTypes.toList().mapNotNull { type ->
            when (type) {
                android.view.Display.HdrCapabilities.HDR_TYPE_DOLBY_VISION -> "Dolby Vision"
                android.view.Display.HdrCapabilities.HDR_TYPE_HDR10 -> "HDR10"
                android.view.Display.HdrCapabilities.HDR_TYPE_HLG -> "HLG"
                android.view.Display.HdrCapabilities.HDR_TYPE_HDR10_PLUS -> "HDR10+"
                else -> null
            }
        }

        DisplayInfo(
            widthPx = widthPx,
            heightPx = heightPx,
            densityDpi = dpi,
            densityBucket = densityBucket,
            fontScale = fontScale,
            physicalSizeInches = diagInches,
            refreshRateHz = refreshRate,
            supportedRefreshRates = supportedRefreshRates,
            hdrCapabilities = hdrNames,
            isHdr = hdrNames.isNotEmpty(),
            isWideColorGamut = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                context.resources.configuration.isScreenWideColorGamut
            } else false,
            brightnessLevel = brightnessRaw / 255f,
            isAdaptiveBrightness = isAdaptive,
            orientation = if (widthPx > heightPx) "Landscape" else "Portrait",
            displayType = if (hdrNames.isNotEmpty()) "OLED/AMOLED" else "LCD",
        )
    }.fold(
        onSuccess = { AppResult.Success(it) },
        onFailure = { AppResult.Error(AppError.Unknown(it.message ?: "Display read failed")) },
    )

    private fun readSupportedHdrTypes(display: Display): IntArray {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            return display.mode.supportedHdrTypes ?: IntArray(0)
        }

        @Suppress("DEPRECATION")
        return display.hdrCapabilities?.supportedHdrTypes ?: IntArray(0)
    }
}
