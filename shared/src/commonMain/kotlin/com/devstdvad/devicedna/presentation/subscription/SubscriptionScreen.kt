package com.devstdvad.devicedna.presentation.subscription

import com.devstdvad.devicedna.resources.stringRes
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.outlined.BatteryChargingFull
import androidx.compose.material.icons.outlined.Block
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.MonitorHeart
import androidx.compose.material.icons.outlined.RestartAlt
import androidx.compose.material.icons.outlined.Widgets
import androidx.compose.material.icons.outlined.WorkspacePremium
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.devstdvad.devicedna.core.common.MetricStatus
import com.devstdvad.devicedna.core.design.AppTheme
import com.devstdvad.devicedna.core.design.component.ErrorBanner
import com.devstdvad.devicedna.core.design.component.SectionCard
import com.devstdvad.devicedna.core.design.component.StatusPill
import com.devstdvad.devicedna.core.feedback.LocalAppFeedback
import com.devstdvad.devicedna.di.resolveViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SubscriptionScreen(
    viewModel: SubscriptionViewModel = resolveViewModel(SubscriptionViewModel::class),
    onBackClick: () -> Unit = {},
    contentPadding: PaddingValues = PaddingValues(),
) {
    val state by viewModel.state.collectAsState()
    val colors = AppTheme.colors
    val feedback = LocalAppFeedback.current

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(colors.background)
            .padding(top = contentPadding.calculateTopPadding()),
    ) {
        TopAppBar(
            title = {
                Column {
                    Text(
                        text = stringRes("subscription_title"),
                        style = MaterialTheme.typography.titleLarge,
                        color = colors.textPrimary,
                    )
                    Text(
                        text = stringRes("subscription_subtitle"),
                        style = MaterialTheme.typography.labelMedium,
                        color = colors.textMuted,
                    )
                }
            },
            navigationIcon = {
                IconButton(onClick = onBackClick) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Outlined.ArrowBack,
                        contentDescription = stringRes("common_back"),
                        tint = colors.textSecondary,
                    )
                }
            },
            colors = TopAppBarDefaults.topAppBarColors(containerColor = colors.background),
        )

        LazyColumn(
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            item {
                SectionCard {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            StatusPill(
                                status = if (state.isPremiumActive) MetricStatus.Normal else MetricStatus.Unavailable,
                                label = if (state.isPremiumActive) {
                                    stringRes("subscription_status_premium")
                                } else {
                                    stringRes("subscription_status_free")
                                },
                            )
                            Spacer(Modifier.height(12.dp))
                            Text(
                                text = if (state.isPremiumActive) {
                                    stringRes("subscription_hero_active")
                                } else {
                                    stringRes("subscription_hero_free")
                                },
                                style = MaterialTheme.typography.displaySmall,
                                color = colors.textPrimary,
                            )
                            Spacer(Modifier.height(8.dp))
                            Text(
                                text = stringRes("subscription_hero_body"),
                                style = MaterialTheme.typography.bodyMedium,
                                color = colors.textSecondary,
                            )
                        }
                        Box(
                            modifier = Modifier
                                .size(70.dp)
                                .clip(CircleShape)
                                .background(colors.accent.copy(alpha = 0.12f))
                                .border(1.dp, colors.accent.copy(alpha = 0.25f), CircleShape),
                            contentAlignment = Alignment.Center,
                        ) {
                            Icon(
                                imageVector = Icons.Outlined.WorkspacePremium,
                                contentDescription = null,
                                tint = colors.accent,
                                modifier = Modifier.size(34.dp),
                            )
                        }
                    }
                }
            }

            item {
                SectionCard {
                    Text(
                        text = stringRes("subscription_included_title"),
                        style = MaterialTheme.typography.titleLarge,
                        color = colors.textPrimary,
                    )
                    Spacer(Modifier.height(12.dp))
                    PremiumFeatureRow(
                        icon = Icons.Outlined.Block,
                        title = stringRes("subscription_feature_no_ads_title"),
                        detail = stringRes("subscription_feature_no_ads_detail"),
                        active = state.removesAds,
                    )
                    Spacer(Modifier.height(8.dp))
                    PremiumFeatureRow(
                        icon = Icons.Outlined.Widgets,
                        title = stringRes("subscription_feature_widgets_title"),
                        detail = stringRes("subscription_feature_widgets_detail"),
                        active = state.widgets,
                    )
                    Spacer(Modifier.height(8.dp))
                    PremiumFeatureRow(
                        icon = Icons.Outlined.BatteryChargingFull,
                        title = stringRes("subscription_feature_battery_title"),
                        detail = stringRes("subscription_feature_battery_detail"),
                        active = state.batteryIntelligence,
                    )
                    Spacer(Modifier.height(8.dp))
                    PremiumFeatureRow(
                        icon = Icons.Outlined.MonitorHeart,
                        title = stringRes("subscription_feature_smart_alerts_title"),
                        detail = stringRes("subscription_feature_smart_alerts_detail"),
                        active = state.smartAlerts,
                    )
                }
            }

            item {
                SectionCard {
                    Text(
                        text = stringRes("subscription_dev_title"),
                        style = MaterialTheme.typography.titleLarge,
                        color = colors.textPrimary,
                    )
                    Spacer(Modifier.height(6.dp))
                    Text(
                        text = stringRes("subscription_dev_body"),
                        style = MaterialTheme.typography.bodyMedium,
                        color = colors.textSecondary,
                    )
                    Spacer(Modifier.height(14.dp))

                    Button(
                        onClick = {
                            viewModel.activateDevPremium()
                            feedback?.confirm()
                        },
                        enabled = !state.isLoading && !state.isPremiumActive,
                        modifier = Modifier.fillMaxWidth(),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = colors.accent,
                            contentColor = colors.background,
                        ),
                        shape = RoundedCornerShape(14.dp),
                    ) {
                        AnimatedContent(
                            targetState = state.isLoading,
                            transitionSpec = { fadeIn() togetherWith fadeOut() },
                            label = "subscription_primary_action",
                        ) { loading ->
                            if (loading) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                                ) {
                                    CircularProgressIndicator(
                                        modifier = Modifier.size(16.dp),
                                        color = colors.background,
                                        strokeWidth = 2.dp,
                                    )
                                    Text(stringRes("subscription_processing"))
                                }
                            } else {
                                Text(
                                    text = if (state.isPremiumActive) {
                                        stringRes("subscription_active_button")
                                    } else {
                                        stringRes("subscription_activate_dev")
                                    },
                                )
                            }
                        }
                    }

                    // Purchase errors are shown right under the activate button, where the user
                    // is looking after tapping it (collapses to nothing when there is no error).
                    if (state.errorMessage != null) {
                        Spacer(Modifier.height(10.dp))
                    }
                    ErrorBanner(
                        message = state.errorMessage,
                        onDismiss = viewModel::dismissError,
                    )

                    Spacer(Modifier.height(10.dp))
                    OutlinedButton(
                        onClick = {
                            viewModel.restorePurchases()
                            feedback?.light()
                        },
                        enabled = !state.isLoading,
                        modifier = Modifier.fillMaxWidth(),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = colors.accent),
                        shape = RoundedCornerShape(14.dp),
                    ) {
                        Icon(Icons.Outlined.RestartAlt, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.size(8.dp))
                        Text(stringRes("subscription_restore"))
                    }

                    // Dev builds only (no real Play billing): allow clearing the local premium
                    // regardless of source (Dev locally, or Backend after dev-via-backend/refresh).
                    if (state.showDevControls) {
                        Spacer(Modifier.height(4.dp))
                        TextButton(
                            onClick = {
                                viewModel.cancelDevPremium()
                                feedback?.light()
                            },
                            enabled = !state.isLoading,
                            modifier = Modifier.align(Alignment.CenterHorizontally),
                        ) {
                            Text(
                                text = stringRes("subscription_cancel_dev"),
                                color = colors.critical,
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun PremiumFeatureRow(
    icon: ImageVector,
    title: String,
    detail: String,
    active: Boolean,
) {
    val colors = AppTheme.colors
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(colors.surface)
            .border(1.dp, colors.border, RoundedCornerShape(12.dp))
            .padding(12.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier = Modifier
                .size(38.dp)
                .clip(RoundedCornerShape(10.dp))
                .background(if (active) colors.success.copy(alpha = 0.14f) else colors.surfaceHover),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                imageVector = if (active) Icons.Outlined.CheckCircle else icon,
                contentDescription = null,
                tint = if (active) colors.success else colors.textMuted,
                modifier = Modifier.size(21.dp),
            )
        }
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold),
                color = colors.textPrimary,
            )
            Spacer(Modifier.height(3.dp))
            Text(
                text = detail,
                style = MaterialTheme.typography.bodySmall,
                color = colors.textSecondary,
            )
        }
        StatusPill(
            status = if (active) MetricStatus.Normal else MetricStatus.Unavailable,
            label = if (active) stringRes("common_on") else stringRes("common_off"),
        )
    }
}
