package com.devstdvad.devicedna.presentation.widgets

import android.appwidget.AppWidgetManager
import android.content.ComponentName
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.material.icons.automirrored.outlined.AddToHomeScreen
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.outlined.BatteryStd
import androidx.compose.material.icons.outlined.HealthAndSafety
import androidx.compose.material.icons.outlined.Lock
import androidx.compose.material.icons.outlined.Memory
import androidx.compose.material.icons.outlined.MonitorHeart
import androidx.compose.material.icons.outlined.Shield
import androidx.compose.material.icons.outlined.Speed
import androidx.compose.material.icons.outlined.Thermostat
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.devstdvad.devicedna.R
import com.devstdvad.devicedna.core.design.AppTheme
import com.devstdvad.devicedna.core.design.component.SectionCard
import com.devstdvad.devicedna.core.feedback.LocalAppFeedback
import com.devstdvad.devicedna.data.subscription.PremiumEntitlements
import com.devstdvad.devicedna.data.subscription.PremiumFeature
import com.devstdvad.devicedna.data.subscription.SubscriptionRepository
import com.devstdvad.devicedna.widget.WidgetRefreshScheduler
import com.devstdvad.devicedna.widget.glance.BatteryDoctorWidgetReceiver
import com.devstdvad.devicedna.widget.glance.BatteryWidgetReceiver
import com.devstdvad.devicedna.widget.glance.CpuThermalWidgetReceiver
import com.devstdvad.devicedna.widget.glance.DeviceHealthWidgetReceiver
import com.devstdvad.devicedna.widget.glance.GuardianWidgetReceiver
import com.devstdvad.devicedna.widget.glance.MemoryWidgetReceiver
import com.devstdvad.devicedna.widget.glance.ThermalGuardWidgetReceiver
import org.koin.compose.koinInject

private data class WidgetEntry(
    val titleRes: Int,
    val detailRes: Int,
    val icon: ImageVector,
    val accent: @Composable () -> androidx.compose.ui.graphics.Color,
    val receiver: Class<*>,
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WidgetsScreen(
    onBackClick: () -> Unit = {},
    onSubscribeClick: () -> Unit = {},
    subscriptionRepository: SubscriptionRepository = koinInject(),
    contentPadding: PaddingValues = PaddingValues(),
) {
    val colors = AppTheme.colors
    val context = LocalContext.current
    val feedback = LocalAppFeedback.current
    val entitlements by subscriptionRepository.entitlements.collectAsState(initial = PremiumEntitlements.Empty)
    val premium = entitlements.hasFeature(PremiumFeature.Widgets)

    // Refresh the home-screen widget cache when widget access changes so existing widgets
    // unlock or lock without waiting for the periodic worker.
    LaunchedEffect(premium) {
        WidgetRefreshScheduler.refreshNow(context)
    }

    val widgets = listOf(
        WidgetEntry(R.string.widget_device_health_title, R.string.widget_device_health_desc, Icons.Outlined.MonitorHeart, { colors.accent }, DeviceHealthWidgetReceiver::class.java),
        WidgetEntry(R.string.widget_battery_doctor_title, R.string.widget_battery_doctor_desc, Icons.Outlined.HealthAndSafety, { colors.batteryColor }, BatteryDoctorWidgetReceiver::class.java),
        WidgetEntry(R.string.widget_thermal_guard_title, R.string.widget_thermal_guard_desc, Icons.Outlined.Thermostat, { colors.thermalColor }, ThermalGuardWidgetReceiver::class.java),
        WidgetEntry(R.string.widget_guardian_title, R.string.widget_guardian_desc, Icons.Outlined.Shield, { colors.accent }, GuardianWidgetReceiver::class.java),
        WidgetEntry(R.string.widget_battery_title, R.string.widget_battery_desc, Icons.Outlined.BatteryStd, { colors.batteryColor }, BatteryWidgetReceiver::class.java),
        WidgetEntry(R.string.widget_memory_title, R.string.widget_memory_desc, Icons.Outlined.Memory, { colors.ramColor }, MemoryWidgetReceiver::class.java),
        WidgetEntry(R.string.widget_cpu_thermal_title, R.string.widget_cpu_thermal_desc, Icons.Outlined.Speed, { colors.cpuColor }, CpuThermalWidgetReceiver::class.java),
    )

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(colors.background)
            .padding(top = contentPadding.calculateTopPadding()),
    ) {
        TopAppBar(
            title = { Text(stringResource(R.string.widgets_title), color = colors.textPrimary) },
            navigationIcon = {
                IconButton(onClick = onBackClick) {
                    Icon(Icons.AutoMirrored.Outlined.ArrowBack, contentDescription = stringResource(R.string.common_back), tint = colors.textSecondary)
                }
            },
            colors = TopAppBarDefaults.topAppBarColors(containerColor = colors.background),
        )

        LazyColumn(
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            if (!premium) {
                item {
                    SectionCard {
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                            Icon(Icons.Outlined.Lock, contentDescription = null, tint = colors.accent)
                            Column(modifier = Modifier.weight(1f)) {
                                Text(stringResource(R.string.widgets_locked_title), style = MaterialTheme.typography.titleMedium, color = colors.textPrimary)
                                Text(stringResource(R.string.widgets_locked_body), style = MaterialTheme.typography.bodySmall, color = colors.textSecondary)
                            }
                        }
                        Spacer(Modifier.height(12.dp))
                        Button(
                            onClick = { feedback?.confirm(); onSubscribeClick() },
                            modifier = Modifier.fillMaxWidth(),
                            colors = ButtonDefaults.buttonColors(containerColor = colors.accent, contentColor = colors.background),
                            shape = RoundedCornerShape(14.dp),
                        ) { Text(stringResource(R.string.widgets_unlock_button)) }
                    }
                }
            }

            items(widgets.size) { index ->
                val entry = widgets[index]
                val accent = entry.accent()
                SectionCard {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        Box(
                            modifier = Modifier.size(40.dp).background(accent.copy(alpha = 0.14f), RoundedCornerShape(10.dp)),
                            contentAlignment = Alignment.Center,
                        ) { Icon(entry.icon, contentDescription = null, tint = accent, modifier = Modifier.size(22.dp)) }
                        Column(modifier = Modifier.weight(1f)) {
                            Text(stringResource(entry.titleRes), style = MaterialTheme.typography.titleMedium, color = colors.textPrimary)
                            Text(stringResource(entry.detailRes), style = MaterialTheme.typography.bodySmall, color = colors.textSecondary)
                        }
                    }
                    Spacer(Modifier.height(10.dp))
                    Button(
                        onClick = {
                            feedback?.light()
                            requestPin(context, entry.receiver)
                        },
                        enabled = premium,
                        modifier = Modifier.fillMaxWidth(),
                        colors = ButtonDefaults.buttonColors(containerColor = accent, contentColor = colors.background),
                        shape = RoundedCornerShape(14.dp),
                    ) {
                        Icon(Icons.AutoMirrored.Outlined.AddToHomeScreen, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.size(8.dp))
                        Text(stringResource(R.string.widgets_add_button))
                    }
                }
            }
        }
    }
}

private fun requestPin(context: android.content.Context, receiver: Class<*>) {
    val manager = AppWidgetManager.getInstance(context)
    if (manager.isRequestPinAppWidgetSupported) {
        manager.requestPinAppWidget(ComponentName(context, receiver), null, null)
    }
}
