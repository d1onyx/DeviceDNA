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

@Composable
fun CameraScreen(
    viewModel: CameraViewModel = resolveViewModel(CameraViewModel::class),
    contentPadding: PaddingValues = PaddingValues(),
) {
    val state by viewModel.state.collectAsState()
    val colors = AppTheme.colors

    if (state.isLoading) { LoadingScreen(); return }
    val info = state.info ?: run { LoadingScreen(message = state.error ?: stringRes("camera_load_error")); return }

    val backCameras = info.cameras.filter { it.facing == CameraFacing.Back }

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
                text = stringRes("camera_count", info.cameras.size),
                style = MaterialTheme.typography.displaySmall,
                color = colors.textPrimary,
            )
        }

        info.cameras.forEach { cam ->
            item {
                val label = when (cam.facing) {
                    CameraFacing.Back -> if (backCameras.size > 1) stringRes("camera_label_rear_indexed", backCameras.indexOf(cam) + 1) else stringRes("camera_label_rear")
                    CameraFacing.Front -> stringRes("camera_label_front")
                    CameraFacing.External -> stringRes("camera_label_external")
                    CameraFacing.Unknown -> stringRes("camera_label_unknown", cam.id)
                }
                CameraCard(cam, label)
            }
        }
    }
}

@Composable
private fun CameraCard(cam: CameraDetails, label: String) {
    val colors = AppTheme.colors
    val isFront = cam.facing == CameraFacing.Front

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
                    text = if (cam.megapixels > 0) "${Formatters.oneDecimal(cam.megapixels)} MP" else stringRes("camera_value_unknown_mp"),
                    style = MaterialTheme.typography.headlineSmall,
                    color = colors.cameraColor,
                )
                Spacer(Modifier.height(6.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    if (cam.megapixels > 0) {
                        SpecBadge("${cam.resolutionWidth}×${cam.resolutionHeight}")
                    }
                    if (cam.apertures.isNotEmpty()) {
                        SpecBadge("f/${Formatters.oneDecimal(cam.apertures.first())}")
                    }
                    if (cam.focalLengths.isNotEmpty()) {
                        SpecBadge("${Formatters.noDecimals(cam.focalLengths.first())}mm")
                    }
                }
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
        InfoRow(stringRes("camera_field_megapixels"), "${Formatters.oneDecimal(cam.megapixels)} MP", copyable = false)
        InfoRow(stringRes("display_field_resolution"), "${cam.resolutionWidth} × ${cam.resolutionHeight}", copyable = false)
        if (cam.focalLengths.isNotEmpty()) InfoRow(stringRes("camera_field_focal_length"), cam.focalLengths.joinToString(", ") { "${Formatters.oneDecimal(it)} mm" }, copyable = false)
        if (cam.apertures.isNotEmpty()) InfoRow(stringRes("camera_field_aperture"), cam.apertures.joinToString(", ") { "f/${Formatters.oneDecimal(it)}" }, copyable = false)
        InfoRow(stringRes("camera_field_flash"), if (cam.hasFlash) stringRes("common_yes") else stringRes("common_no"), copyable = false)
        InfoRow(stringRes("camera_field_ois"), if (cam.hasOis) stringRes("common_yes") else stringRes("common_no"), copyable = false, showDivider = false)
    }
}

@Composable
private fun SpecBadge(text: String) {
    val colors = AppTheme.colors
    Text(
        text = text,
        style = MaterialTheme.typography.labelMedium,
        color = colors.cameraColor,
        modifier = Modifier
            .clip(RoundedCornerShape(4.dp))
            .background(colors.cameraColor.copy(alpha = 0.1f))
            .padding(horizontal = 6.dp, vertical = 2.dp),
    )
}
