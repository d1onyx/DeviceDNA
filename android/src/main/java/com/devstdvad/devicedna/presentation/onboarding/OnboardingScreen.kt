package com.devstdvad.devicedna.presentation.onboarding

import androidx.annotation.StringRes
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.BatteryChargingFull
import androidx.compose.material.icons.outlined.Biotech
import androidx.compose.material.icons.outlined.HealthAndSafety
import androidx.compose.material.icons.outlined.Memory
import androidx.compose.material.icons.outlined.NetworkCheck
import androidx.compose.material.icons.outlined.PhoneAndroid
import androidx.compose.material.icons.outlined.PrivacyTip
import androidx.compose.material.icons.outlined.Sensors
import androidx.compose.material.icons.outlined.Shield
import androidx.compose.material.icons.outlined.Speed
import androidx.compose.material.icons.outlined.Storage
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.devstdvad.devicedna.R
import com.devstdvad.devicedna.core.design.AppTheme

@Composable
fun OnboardingScreen(
    onFinished: () -> Unit,
    reducedMotion: Boolean,
    modifier: Modifier = Modifier,
) {
    val colors = AppTheme.colors
    var pageIndex by remember { mutableIntStateOf(0) }
    val pages = remember {
        listOf(
            OnboardingPage(R.string.onboarding_s1_title, R.string.onboarding_s1_body, Icons.Outlined.PhoneAndroid),
            OnboardingPage(R.string.onboarding_s2_title, R.string.onboarding_s2_body, Icons.Outlined.Biotech),
            OnboardingPage(R.string.onboarding_s3_title, R.string.onboarding_s3_body, Icons.Outlined.PrivacyTip),
            OnboardingPage(R.string.onboarding_s4_title, R.string.onboarding_s4_body, Icons.Outlined.HealthAndSafety),
        )
    }
    val progress by animateFloatAsState(
        targetValue = (pageIndex + 1) / pages.size.toFloat(),
        animationSpec = if (reducedMotion) tween(0) else tween(500),
        label = "onboarding_progress",
    )

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(colors.background)
            .padding(horizontal = 24.dp, vertical = 20.dp),
        verticalArrangement = Arrangement.SpaceBetween,
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text("DeviceDNA", style = MaterialTheme.typography.titleLarge, color = colors.textPrimary)
            AnimatedVisibility(visible = pageIndex < pages.lastIndex) {
                TextButton(onClick = onFinished) {
                    Text(stringResource(R.string.onboarding_skip), color = colors.textSecondary)
                }
            }
        }

        AnimatedContent(
            targetState = pages[pageIndex],
            transitionSpec = {
                if (reducedMotion) {
                    fadeIn(tween(0)) togetherWith fadeOut(tween(0))
                } else {
                    (slideInHorizontally { it / 2 } + fadeIn(tween(350))) togetherWith
                        (slideOutHorizontally { -it / 2 } + fadeOut(tween(250)))
                }
            },
            label = "onboarding_page",
        ) { page ->
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                DeviceGlyph(page.icon, pageIndex)
                Spacer(Modifier.height(34.dp))
                Text(
                    text = stringResource(page.titleRes),
                    style = MaterialTheme.typography.displaySmall,
                    color = colors.textPrimary,
                    textAlign = TextAlign.Center,
                )
                Spacer(Modifier.height(12.dp))
                Text(
                    text = stringResource(page.bodyRes),
                    style = MaterialTheme.typography.bodyLarge,
                    color = colors.textSecondary,
                    textAlign = TextAlign.Center,
                )
            }
        }

        Column {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                pages.indices.forEach { index ->
                    val selected = index == pageIndex
                    Box(
                        modifier = Modifier
                            .padding(horizontal = 4.dp)
                            .height(8.dp)
                            .width(if (selected) 24.dp else 8.dp)
                            .clip(RoundedCornerShape(99.dp))
                            .background(if (selected) colors.accent else colors.border),
                    )
                }
            }
            Spacer(Modifier.height(18.dp))
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(3.dp)
                    .clip(RoundedCornerShape(99.dp))
                    .background(colors.border),
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth(progress)
                        .height(3.dp)
                        .background(colors.accent),
                )
            }
            Spacer(Modifier.height(18.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp), modifier = Modifier.fillMaxWidth()) {
                if (pageIndex > 0) {
                    OutlinedButton(
                        onClick = { pageIndex -= 1 },
                        modifier = Modifier.weight(1f),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = colors.textPrimary),
                    ) {
                        Text(stringResource(R.string.onboarding_back))
                    }
                }
                Button(
                    onClick = {
                        if (pageIndex == pages.lastIndex) onFinished() else pageIndex += 1
                    },
                    modifier = Modifier.weight(1f),
                    colors = ButtonDefaults.buttonColors(containerColor = colors.accent, contentColor = colors.background),
                ) {
                    Text(stringResource(if (pageIndex == pages.lastIndex) R.string.onboarding_finish else R.string.onboarding_next))
                }
            }
        }
    }
}

@Composable
private fun DeviceGlyph(icon: ImageVector, pageIndex: Int) {
    val colors = AppTheme.colors
    Box(
        modifier = Modifier
            .size(232.dp)
            .clip(RoundedCornerShape(28.dp))
            .border(1.dp, colors.border, RoundedCornerShape(28.dp))
            .background(colors.surfaceElevated),
        contentAlignment = Alignment.Center,
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                MiniMetric(Icons.Outlined.Memory, if (pageIndex == 1) "Load" else "49%", pageIndex == 1)
                MiniMetric(Icons.Outlined.BatteryChargingFull, if (pageIndex == 3) "Safe" else "86%", pageIndex == 0 || pageIndex == 3)
            }
            Box(
                modifier = Modifier
                    .size(76.dp)
                    .clip(CircleShape)
                    .background(colors.accent),
                contentAlignment = Alignment.Center,
            ) {
                Icon(icon, contentDescription = null, tint = colors.background, modifier = Modifier.size(34.dp))
            }
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                MiniMetric(Icons.Outlined.Storage, if (pageIndex == 1) "I/O" else "174G", pageIndex == 1)
                MiniMetric(Icons.Outlined.Sensors, if (pageIndex == 2) "Mask" else "55", pageIndex == 2)
                MiniMetric(Icons.Outlined.NetworkCheck, if (pageIndex == 2) "Opt-in" else "WiFi", pageIndex == 2)
            }
        }
    }
}

@Composable
private fun MiniMetric(icon: ImageVector, value: String, active: Boolean) {
    val colors = AppTheme.colors
    val fill = if (active) colors.surfaceHover else colors.surface
    Row(
        modifier = Modifier
            .clip(RoundedCornerShape(12.dp))
            .background(fill)
            .border(1.dp, colors.border, RoundedCornerShape(12.dp))
            .padding(horizontal = 10.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        Icon(icon, contentDescription = null, tint = if (active) colors.accent else colors.textMuted, modifier = Modifier.size(16.dp))
        Text(value, style = MaterialTheme.typography.labelMedium, color = colors.textPrimary)
    }
}

private data class OnboardingPage(
    @param:StringRes val titleRes: Int,
    @param:StringRes val bodyRes: Int,
    val icon: ImageVector,
)
