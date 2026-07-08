package com.devstdvad.devicedna.presentation.camera

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.CameraAlt
import androidx.compose.material.icons.outlined.CameraFront
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import com.devstdvad.devicedna.core.common.Formatters
import com.devstdvad.devicedna.core.design.AppTheme
import com.devstdvad.devicedna.core.design.component.AccentCard
import com.devstdvad.devicedna.core.design.component.InfoRow
import com.devstdvad.devicedna.domain.model.CameraDetails
import com.devstdvad.devicedna.domain.model.CameraFacing
import com.devstdvad.devicedna.presentation.common.LoadingScreen
import com.devstdvad.devicedna.di.resolveViewModel
import com.devstdvad.devicedna.resources.stringRes
import kotlin.math.roundToInt

@Composable
fun CameraScreen(
    viewModel: CameraViewModel = resolveViewModel(CameraViewModel::class),
    contentPadding: PaddingValues = PaddingValues(),
) {
    val state by viewModel.state.collectAsState()
    val colors = AppTheme.colors

    if (state.isLoading) { LoadingScreen(); return }
    val info = state.info ?: run { LoadingScreen(message = state.error ?: stringRes("camera_load_error")); return }

    // Group physical lenses by facing so each side renders as ONE section (like iOS Settings):
    // this also collapses the front camera that iOS enumerates twice (wide-angle + TrueDepth).
    val facingOrder = listOf(CameraFacing.Back, CameraFacing.Front, CameraFacing.External, CameraFacing.Unknown)
    val groups = facingOrder.mapNotNull { facing ->
        info.cameras.filter { it.facing == facing }.takeIf { it.isNotEmpty() }?.let { facing to it }
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(
            start = 16.dp,
            end = 16.dp,
            top = 12.dp,
            bottom = 12.dp + contentPadding.calculateBottomPadding(),
        ),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        item {
            Text(
                text = stringRes("camera_count", groups.sumOf { it.second.size }),
                style = MaterialTheme.typography.displaySmall,
                color = colors.textPrimary,
            )
        }

        groups.forEach { (facing, lenses) ->
            item {
                val label = when (facing) {
                    CameraFacing.Back -> stringRes("camera_label_back")
                    CameraFacing.Front -> stringRes("camera_label_front")
                    CameraFacing.External -> stringRes("camera_label_external")
                    CameraFacing.Unknown -> stringRes("camera_label_unknown", lenses.first().id)
                }
                CameraCard(facing, lenses, label)
            }
        }
    }
}

@Composable
private fun CameraCard(facing: CameraFacing, lenses: List<CameraDetails>, label: String) {
    val colors = AppTheme.colors
    val isFront = facing == CameraFacing.Front

    // Aggregate the group's lenses into one representative spec set.
    val megapixels = lenses.maxOf { it.megapixels }
    val lensPrefix = when (lenses.size) {
        2 -> stringRes("camera_lens_prefix_dual")
        3 -> stringRes("camera_lens_prefix_triple")
        in 4..Int.MAX_VALUE -> stringRes("camera_lens_prefix_quad")
        else -> ""
    }
    val minAperture = lenses.flatMap { it.apertures }.minOrNull()
    val bestVideo = lenses.filter { it.maxVideoWidth > 0 }.maxByOrNull { it.maxVideoWidth.toLong() * it.maxVideoHeight }
    val bestSlowMo = lenses.filter { it.maxSlowMoFps > 0 }.maxByOrNull { it.maxSlowMoFps }
    val hasOis = lenses.any { it.hasOis }
    val bestPhoto = lenses.filter { it.maxPhotoWidth > 0 }.maxByOrNull { it.maxPhotoWidth.toLong() * it.maxPhotoHeight }
    val shortestExposure = lenses.mapNotNull { it.minExposureNanos.takeIf { n -> n > 0L } }.minOrNull()
    val longestExposure = lenses.mapNotNull { it.maxExposureNanos.takeIf { n -> n > 0L } }.maxOrNull()

    AccentCard(accentColor = colors.cameraColor) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.Top,
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(label, style = MaterialTheme.typography.titleLarge, color = colors.textPrimary)
                Spacer(Modifier.height(2.dp))
                Text(
                    text = if (megapixels > 0) {
                        stringRes("camera_value_megapixels", lensPrefix, megapixels.roundToInt())
                    } else {
                        stringRes("camera_value_unknown_mp")
                    },
                    style = MaterialTheme.typography.headlineSmall,
                    color = colors.cameraColor,
                )
            }
            Box(
                modifier = Modifier.size(44.dp).clip(RoundedCornerShape(12.dp))
                    .background(colors.cameraColor.copy(alpha = 0.12f))
                    .border(1.dp, colors.cameraColor.copy(alpha = 0.25f), RoundedCornerShape(12.dp)),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    if (isFront) Icons.Outlined.CameraFront else Icons.Outlined.CameraAlt,
                    null,
                    tint = colors.cameraColor,
                    modifier = Modifier.size(22.dp),
                )
            }
        }

        Spacer(Modifier.height(12.dp))

        if (megapixels > 0) {
            InfoRow(
                stringRes("camera_field_resolution"),
                stringRes("camera_value_megapixels", lensPrefix, megapixels.roundToInt()),
                copyable = false,
            )
        }
        if (minAperture != null) {
            InfoRow(stringRes("camera_field_aperture"), Formatters.oneDecimal(minAperture), copyable = false)
        }
        if (bestVideo != null) {
            InfoRow(
                stringRes("camera_field_max_video_resolution"),
                stringRes(
                    "camera_value_video_res_fps",
                    resolutionLabel(bestVideo.maxVideoWidth, bestVideo.maxVideoHeight),
                    bestVideo.maxVideoFps,
                ),
                copyable = false,
            )
        }
        if (bestSlowMo != null) {
            InfoRow(
                stringRes("camera_field_max_video_speed"),
                stringRes(
                    "camera_value_video_speed",
                    bestSlowMo.maxSlowMoFps,
                    resolutionLabel(bestSlowMo.slowMoWidth, bestSlowMo.slowMoHeight),
                ),
                copyable = false,
            )
        }
        InfoRow(
            stringRes("camera_field_optical_stabilization"),
            if (hasOis) "1" else "0",
            copyable = false,
        )
        if (bestPhoto != null) {
            InfoRow(
                stringRes("camera_field_max_photo_resolution"),
                "${bestPhoto.maxPhotoWidth} × ${bestPhoto.maxPhotoHeight}",
                copyable = false,
            )
        }
        if (shortestExposure != null) {
            InfoRow(
                stringRes("camera_field_shortest_exposure"),
                formatExposure(shortestExposure),
                copyable = false,
            )
        }
        InfoRow(stringRes("camera_field_flash"), if (lenses.any { it.hasFlash }) stringRes("common_yes") else stringRes("common_no"), copyable = false, showDivider = longestExposure != null)
        if (longestExposure != null) {
            InfoRow(
                stringRes("camera_field_longest_exposure"),
                formatExposure(longestExposure),
                copyable = false,
                showDivider = false,
            )
        }
    }
}

/** Maps a pixel size to a familiar video label (based on the shorter side): 4k, 1080p, 720p, … */
private fun resolutionLabel(width: Int, height: Int): String {
    val v = minOf(width, height)
    return when {
        v >= 4320 -> "8k"
        v >= 2160 -> "4k"
        v >= 1440 -> "1440p"
        v >= 1080 -> "1080p"
        v >= 720 -> "720p"
        v > 0 -> "${v}p"
        else -> "—"
    }
}

/** Exposure as a shutter fraction, e.g. 21µs → "1/47619 sec", 1s → "1/1 sec". */
@Composable
private fun formatExposure(nanos: Long): String {
    val seconds = nanos / 1_000_000_000.0
    val denom = if (seconds > 0.0) (1.0 / seconds).roundToInt().coerceAtLeast(1) else 1
    return stringRes("camera_value_exposure", denom)
}
