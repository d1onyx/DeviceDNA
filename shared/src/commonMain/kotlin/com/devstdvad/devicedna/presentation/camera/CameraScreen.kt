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
import org.koin.compose.viewmodel.koinViewModel

@Composable
fun CameraScreen(
    viewModel: CameraViewModel = koinViewModel(),
    contentPadding: PaddingValues = PaddingValues(),
) {
    val state by viewModel.state.collectAsState()
    val colors = AppTheme.colors

    if (state.isLoading) { LoadingScreen(); return }
    val info = state.info ?: run { LoadingScreen(message = state.error ?: "No camera info available"); return }

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
                text = "${info.cameras.size} Camera${if (info.cameras.size != 1) "s" else ""}",
                style = MaterialTheme.typography.displaySmall,
                color = colors.textPrimary,
            )
        }

        info.cameras.forEach { cam ->
            item {
                val label = when (cam.facing) {
                    CameraFacing.Back -> if (backCameras.size > 1) "Rear ${backCameras.indexOf(cam) + 1}" else "Rear Camera"
                    CameraFacing.Front -> "Front Camera"
                    CameraFacing.External -> "External Camera"
                    CameraFacing.Unknown -> "Camera ${cam.id}"
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
                    text = if (cam.megapixels > 0) "${Formatters.oneDecimal(cam.megapixels)} MP" else "Unknown MP",
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
        InfoRow("Megapixels", "${Formatters.oneDecimal(cam.megapixels)} MP", copyable = false)
        InfoRow("Resolution", "${cam.resolutionWidth} × ${cam.resolutionHeight}", copyable = false)
        if (cam.focalLengths.isNotEmpty()) InfoRow("Focal Length", cam.focalLengths.joinToString(", ") { "${Formatters.oneDecimal(it)} mm" }, copyable = false)
        if (cam.apertures.isNotEmpty()) InfoRow("Aperture", cam.apertures.joinToString(", ") { "f/${Formatters.oneDecimal(it)}" }, copyable = false)
        InfoRow("Flash", if (cam.hasFlash) "Yes" else "No", copyable = false)
        InfoRow("OIS", if (cam.hasOis) "Yes" else "No", copyable = false, showDivider = false)
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
