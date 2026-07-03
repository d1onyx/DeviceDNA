package com.devstdvad.devicedna.presentation.widgets

import androidx.compose.foundation.background
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
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.outlined.BatteryStd
import androidx.compose.material.icons.outlined.HealthAndSafety
import androidx.compose.material.icons.outlined.Lock
import androidx.compose.material.icons.outlined.Memory
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
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import com.devstdvad.devicedna.core.design.AppTheme
import com.devstdvad.devicedna.core.design.component.SectionCard
import com.devstdvad.devicedna.data.subscription.PremiumEntitlements
import com.devstdvad.devicedna.data.subscription.PremiumFeature
import com.devstdvad.devicedna.data.subscription.SubscriptionRepository
import org.koin.compose.koinInject

private data class IosWidgetEntry(
    val title: String,
    val detail: String,
    val icon: ImageVector,
    val accent: @Composable () -> androidx.compose.ui.graphics.Color,
)

/**
 * In-app widgets screen for iOS. iOS cannot pin widgets programmatically (there is no
 * WidgetKit analogue of Android's requestPinAppWidget), so this screen is informational:
 * it lists the widgets DeviceDNA ships and explains how to add them from the Home Screen.
 * Placement is gated behind Premium — without it, a lock card routes the user to the
 * subscription screen. Wired into [MainViewController]'s widgetsContent slot.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun IosWidgetsScreen(
    onBackClick: () -> Unit,
    onSubscribeClick: () -> Unit,
    contentPadding: PaddingValues,
    subscriptionRepository: SubscriptionRepository = koinInject(),
) {
    val colors = AppTheme.colors
    val entitlements by subscriptionRepository.entitlements.collectAsState(initial = PremiumEntitlements.Empty)
    val premium = entitlements.hasFeature(PremiumFeature.Widgets)

    val widgets = listOf(
        IosWidgetEntry("Battery", "Battery level and charging status at a glance.", Icons.Outlined.BatteryStd) { colors.batteryColor },
        IosWidgetEntry("Device Health", "Overall device health score.", Icons.Outlined.HealthAndSafety) { colors.accent },
        IosWidgetEntry("Memory", "RAM usage and available memory.", Icons.Outlined.Memory) { colors.ramColor },
    )

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(colors.background)
            .padding(top = contentPadding.calculateTopPadding()),
    ) {
        TopAppBar(
            title = { Text("Widgets", color = colors.textPrimary) },
            navigationIcon = {
                IconButton(onClick = onBackClick) {
                    Icon(Icons.AutoMirrored.Outlined.ArrowBack, contentDescription = "Back", tint = colors.textSecondary)
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
                                Text("Widgets are a Premium feature", style = MaterialTheme.typography.titleMedium, color = colors.textPrimary)
                                Text("Unlock Premium to add DeviceDNA widgets to your Home Screen.", style = MaterialTheme.typography.bodySmall, color = colors.textSecondary)
                            }
                        }
                        Spacer(Modifier.height(12.dp))
                        Button(
                            onClick = onSubscribeClick,
                            modifier = Modifier.fillMaxWidth(),
                            colors = ButtonDefaults.buttonColors(containerColor = colors.accent, contentColor = colors.background),
                            shape = RoundedCornerShape(14.dp),
                        ) { Text("Unlock Premium") }
                    }
                }
            } else {
                item {
                    SectionCard {
                        Text("How to add a widget", style = MaterialTheme.typography.titleMedium, color = colors.textPrimary)
                        Spacer(Modifier.height(6.dp))
                        Text(
                            "Touch and hold the Home Screen, tap the + button in the top corner, search for \"DeviceDNA\", then choose a widget below.",
                            style = MaterialTheme.typography.bodySmall,
                            color = colors.textSecondary,
                        )
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
                            Text(entry.title, style = MaterialTheme.typography.titleMedium, color = colors.textPrimary)
                            Text(entry.detail, style = MaterialTheme.typography.bodySmall, color = colors.textSecondary)
                        }
                    }
                }
            }
        }
    }
}
