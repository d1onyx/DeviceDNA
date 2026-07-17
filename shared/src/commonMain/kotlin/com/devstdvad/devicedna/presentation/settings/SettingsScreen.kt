package com.devstdvad.devicedna.presentation.settings

import com.devstdvad.devicedna.data.alerts.settingKey
import com.devstdvad.devicedna.platform.PlatformInfo
import com.devstdvad.devicedna.resources.stringRes
import androidx.compose.material.icons.automirrored.outlined.Logout
import androidx.compose.material.icons.automirrored.outlined.KeyboardArrowRight
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
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
import androidx.compose.material.icons.outlined.AccountCircle
import androidx.compose.material.icons.outlined.DarkMode
import androidx.compose.material.icons.outlined.DataUsage
import androidx.compose.material.icons.outlined.ExpandLess
import androidx.compose.material.icons.outlined.ExpandMore
import androidx.compose.material.icons.outlined.FileDownload
import androidx.compose.material.icons.outlined.Language
import androidx.compose.material.icons.outlined.MonitorHeart
import androidx.compose.material.icons.outlined.MusicNote
import androidx.compose.material.icons.outlined.Policy
import androidx.compose.material.icons.outlined.PrivacyTip
import androidx.compose.material.icons.outlined.Public
import androidx.compose.material.icons.outlined.RestartAlt
import androidx.compose.material.icons.outlined.Security
import androidx.compose.material.icons.outlined.Share
import androidx.compose.material.icons.outlined.Speed
import androidx.compose.material.icons.outlined.Thermostat
import androidx.compose.material.icons.outlined.Vibration
import androidx.compose.material.icons.outlined.WorkspacePremium
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import com.devstdvad.devicedna.core.common.MetricStatus
import com.devstdvad.devicedna.core.design.AppTheme
import com.devstdvad.devicedna.core.design.component.InfoRow
import com.devstdvad.devicedna.core.design.component.SectionCard
import com.devstdvad.devicedna.core.design.component.StatusPill
import com.devstdvad.devicedna.core.feedback.LocalAppFeedback
import com.devstdvad.devicedna.data.alerts.SmartAlertType
import com.devstdvad.devicedna.data.settings.AppThemeMode
import com.devstdvad.devicedna.data.settings.DataUnit
import com.devstdvad.devicedna.data.settings.ExportFormat
import com.devstdvad.devicedna.data.settings.TemperatureUnit
import com.devstdvad.devicedna.data.subscription.PremiumEntitlements
import com.devstdvad.devicedna.data.subscription.PremiumFeature
import com.devstdvad.devicedna.data.subscription.SubscriptionRepository
import com.devstdvad.devicedna.di.resolveViewModel
import org.koin.compose.koinInject

@Composable
fun SettingsScreen(
    viewModel: SettingsViewModel = resolveViewModel(SettingsViewModel::class),
    exportViewModel: ExportViewModel = resolveViewModel(ExportViewModel::class),
    subscriptionRepository: SubscriptionRepository = koinInject(),
    onSubscriptionClick: () -> Unit = {},
    onWidgetsClick: () -> Unit = {},
    onAdPrivacyOptions: () -> Unit = {},
    showAdPrivacyOptions: Boolean = false,
    onSignInClick: () -> Unit = {},
    contentPadding: PaddingValues = PaddingValues(),
) {
    val colors = AppTheme.colors
    val settings by viewModel.settings.collectAsState()
    val user by viewModel.user.collectAsState()
    val exportState by exportViewModel.state.collectAsState()
    val entitlements by subscriptionRepository.entitlements.collectAsState(initial = PremiumEntitlements.Empty)
    val premiumActive = entitlements.hasFeature(PremiumFeature.RemoveAds)
    val smartAlertsUnlocked = entitlements.hasFeature(PremiumFeature.SmartAlerts)
    val feedback = LocalAppFeedback.current
    val uriHandler = LocalUriHandler.current
    var privacyExpanded by remember { mutableStateOf(false) }
    val privacyScore by animateFloatAsState(
        targetValue = listOf(settings.maskSensitive, !settings.publicIpEnabled, !settings.showImei).count { it } / 3f,
        animationSpec = if (settings.reducedMotion) tween(0) else tween(500),
        label = "privacy_score",
    )

    // Sharing is now handled inside DiagnosticsExporter via the platform FileSharer.

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(
            start = 16.dp,
            end = 16.dp,
            top = 12.dp + contentPadding.calculateTopPadding(),
            bottom = 12.dp + contentPadding.calculateBottomPadding(),
        ),
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
                        Text(stringRes("settings_title"), style = MaterialTheme.typography.displaySmall, color = colors.textPrimary)
                        Spacer(Modifier.height(6.dp))
                        Text(stringRes("settings_subtitle"), style = MaterialTheme.typography.bodyMedium, color = colors.textSecondary)
                    }
                    Box(
                        modifier = Modifier
                            .size(70.dp)
                            .clip(CircleShape)
                            .background(colors.surfaceHover)
                            .border(1.dp, colors.border, CircleShape),
                        contentAlignment = Alignment.Center,
                    ) {
                        Text("${(privacyScore * 100).toInt()}", style = MaterialTheme.typography.titleLarge, color = colors.accent)
                    }
                }
            }
        }

        item {
            SettingsPanel(title = stringRes("settings_account"), icon = Icons.Outlined.AccountCircle) {
                if (user == null) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(stringRes("settings_guest_mode"), style = MaterialTheme.typography.titleMedium, color = colors.textPrimary)
                            Spacer(Modifier.height(4.dp))
                            Text(stringRes("settings_guest_mode_detail"), style = MaterialTheme.typography.bodySmall, color = colors.textSecondary)
                        }
                        TextButton(onClick = onSignInClick) {
                            Text(stringRes("settings_sign_in"), color = colors.accent)
                        }
                    }
                    return@SettingsPanel
                }
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(user?.displayName ?: stringRes("settings_account_signed_in"), style = MaterialTheme.typography.titleMedium, color = colors.textPrimary)
                        Spacer(Modifier.height(4.dp))
                        Text(user?.email.orEmpty(), style = MaterialTheme.typography.bodyMedium, color = colors.textSecondary)
                    }
                    TextButton(onClick = viewModel::signOut) {
                        Icon(Icons.AutoMirrored.Outlined.Logout, contentDescription = null, tint = colors.accent, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.size(6.dp))
                        Text(stringRes("settings_sign_out"), color = colors.accent)
                    }
                }

                val deletion by viewModel.accountDeletion.collectAsState()
                var showDeleteDialog by remember { mutableStateOf(false) }

                Spacer(Modifier.height(16.dp))
                Button(
                    onClick = { showDeleteDialog = true },
                    enabled = deletion != AccountDeletionUi.Deleting,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = colors.critical,
                        contentColor = Color.White,
                    ),
                    modifier = Modifier.fillMaxWidth(),
                ) {
                    if (deletion == AccountDeletionUi.Deleting) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(18.dp),
                            color = Color.White,
                            strokeWidth = 2.dp,
                        )
                    } else {
                        Text(stringRes("settings_delete_account"))
                    }
                }

                if (showDeleteDialog) {
                    AlertDialog(
                        onDismissRequest = { showDeleteDialog = false },
                        title = { Text(stringRes("settings_delete_account_title")) },
                        text = {
                            val message = stringRes("settings_delete_account_message") +
                                if (premiumActive) {
                                    "\n\n" + stringRes("settings_delete_account_subscription_note")
                                } else {
                                    ""
                                }
                            Text(message)
                        },
                        confirmButton = {
                            TextButton(onClick = {
                                showDeleteDialog = false
                                viewModel.deleteAccount()
                            }) {
                                Text(stringRes("settings_delete_account_confirm"), color = colors.critical)
                            }
                        },
                        dismissButton = {
                            TextButton(onClick = { showDeleteDialog = false }) {
                                Text(stringRes("settings_delete_account_cancel"))
                            }
                        },
                    )
                }

                val deletionError = when (deletion) {
                    AccountDeletionUi.ReauthRequired -> stringRes("settings_delete_account_reauth")
                    AccountDeletionUi.Failed -> stringRes("settings_delete_account_failed")
                    else -> null
                }
                if (deletionError != null) {
                    AlertDialog(
                        onDismissRequest = viewModel::dismissDeletionError,
                        title = { Text(stringRes("settings_delete_account_title")) },
                        text = { Text(deletionError) },
                        confirmButton = {
                            TextButton(onClick = viewModel::dismissDeletionError) { Text("OK") }
                        },
                    )
                }
            }
        }

        item {
            SettingsPanel(title = stringRes("settings_premium"), icon = Icons.Outlined.WorkspacePremium) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .background(colors.surface)
                        .border(1.dp, colors.border, RoundedCornerShape(12.dp))
                        .clickable {
                            feedback?.light()
                            onSubscriptionClick()
                        }
                        .padding(horizontal = 12.dp, vertical = 12.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = stringRes("settings_premium_manage"),
                            style = MaterialTheme.typography.titleMedium,
                            color = colors.textPrimary,
                        )
                        Spacer(Modifier.height(4.dp))
                        Text(
                            text = stringRes("settings_premium_summary"),
                            style = MaterialTheme.typography.bodySmall,
                            color = colors.textSecondary,
                        )
                    }
                    StatusPill(
                        status = if (premiumActive) MetricStatus.Normal else MetricStatus.Unavailable,
                        label = if (premiumActive) {
                            stringRes("subscription_status_premium")
                        } else {
                            stringRes("subscription_status_free")
                        },
                    )
                    Icon(Icons.AutoMirrored.Outlined.KeyboardArrowRight, contentDescription = null, tint = colors.textMuted)
                }
                Spacer(Modifier.height(8.dp))
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .background(colors.surface)
                        .border(1.dp, colors.border, RoundedCornerShape(12.dp))
                        .clickable {
                            feedback?.light()
                            onWidgetsClick()
                        }
                        .padding(horizontal = 12.dp, vertical = 12.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = stringRes("settings_widgets_title"),
                            style = MaterialTheme.typography.titleMedium,
                            color = colors.textPrimary,
                        )
                        Spacer(Modifier.height(4.dp))
                        Text(
                            text = stringRes(if (PlatformInfo.isIos) "settings_widgets_summary_ios" else "settings_widgets_summary"),
                            style = MaterialTheme.typography.bodySmall,
                            color = colors.textSecondary,
                        )
                    }
                    Icon(Icons.AutoMirrored.Outlined.KeyboardArrowRight, contentDescription = null, tint = colors.textMuted)
                }
            }
        }

        item {
            SettingsPanel(title = stringRes("settings_appearance"), icon = Icons.Outlined.DarkMode) {
                OptionRow(
                    label = stringRes("settings_theme"),
                    options = listOf(
                        Choice(stringRes("settings_theme_system"), AppThemeMode.System),
                        Choice(stringRes("settings_theme_dark"), AppThemeMode.Dark),
                        Choice(stringRes("settings_theme_light"), AppThemeMode.Light),
                    ),
                    selected = settings.theme,
                    onSelected = { viewModel.setTheme(it); feedback?.light() },
                )
                OptionRow(
                    label = stringRes("settings_language_picker"),
                    options = listOf(
                        Choice("System", ""),
                        Choice("English", "en"),
                        Choice("Deutsch", "de"),
                        Choice("Русский", "ru"),
                    ),
                    selected = settings.appLanguage,
                    onSelected = { viewModel.setAppLanguage(it); feedback?.light() },
                )
            }
        }

        item {
            SettingsPanel(title = stringRes("settings_units"), icon = Icons.Outlined.Thermostat) {
                OptionRow(
                    label = stringRes("settings_temp_unit"),
                    options = listOf(
                        Choice("°C", TemperatureUnit.Celsius),
                        Choice("°F", TemperatureUnit.Fahrenheit),
                    ),
                    selected = settings.temperatureUnit,
                    onSelected = { viewModel.setTemperatureUnit(it); feedback?.light() },
                )
                OptionRow(
                    label = stringRes("settings_data_unit"),
                    options = listOf(
                        Choice("GB", DataUnit.GB),
                        Choice("GiB", DataUnit.GiB),
                    ),
                    selected = settings.dataUnit,
                    onSelected = { viewModel.setDataUnit(it); feedback?.light() },
                )
            }
        }

        item {
            SettingsPanel(title = stringRes("settings_privacy"), icon = Icons.Outlined.PrivacyTip) {
                SwitchRow(
                    icon = Icons.Outlined.Security,
                    label = stringRes("settings_mask_sensitive"),
                    value = settings.maskSensitive,
                    onChanged = { viewModel.setMaskSensitive(it); feedback?.toggle(it) },
                )
                SwitchRow(
                    icon = Icons.Outlined.Public,
                    label = stringRes("settings_public_ip"),
                    value = settings.publicIpEnabled,
                    onChanged = { viewModel.setPublicIpEnabled(it); feedback?.toggle(it) },
                )
                SwitchRow(
                    icon = Icons.Outlined.Security,
                    label = stringRes("settings_show_imei"),
                    value = settings.showImei,
                    onChanged = { viewModel.setShowImei(it); feedback?.toggle(it) },
                )
                Spacer(Modifier.height(8.dp))
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .background(colors.surface)
                        .border(1.dp, colors.border, RoundedCornerShape(12.dp))
                        .clickable { privacyExpanded = !privacyExpanded }
                        .padding(horizontal = 12.dp, vertical = 12.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.weight(1f)) {
                        Icon(Icons.Outlined.Policy, contentDescription = null, tint = colors.accent, modifier = Modifier.size(20.dp))
                        Column {
                            Text(stringRes("settings_privacy_policy_title"), style = MaterialTheme.typography.titleMedium, color = colors.textPrimary)
                            Text(stringRes("settings_privacy_policy_summary"), style = MaterialTheme.typography.bodySmall, color = colors.textSecondary)
                        }
                    }
                    Icon(if (privacyExpanded) Icons.Outlined.ExpandLess else Icons.Outlined.ExpandMore, contentDescription = null, tint = colors.textMuted)
                }
                if (privacyExpanded) {
                    Spacer(Modifier.height(12.dp))
                    Text(
                        text = stringRes(if (PlatformInfo.isIos) "settings_privacy_policy_body_ios" else "settings_privacy_policy_body"),
                        style = MaterialTheme.typography.bodyMedium,
                        color = colors.textSecondary,
                    )
                    if (PlatformInfo.isIos && showAdPrivacyOptions) {
                        TextButton(onClick = onAdPrivacyOptions) {
                            Text(stringRes("settings_ad_privacy_options"), color = colors.accent)
                        }
                    }
                    if (PlatformInfo.isIos) {
                        TextButton(onClick = { uriHandler.openUri(IOS_PRIVACY_POLICY_URL) }) {
                            Text(stringRes("subscription_privacy"), color = colors.accent)
                        }
                    }
                }
            }
        }

        item {
            SettingsPanel(title = stringRes("settings_performance"), icon = Icons.Outlined.Speed) {
                SwitchRow(
                    icon = Icons.Outlined.RestartAlt,
                    label = stringRes("settings_reduced_motion"),
                    value = settings.reducedMotion,
                    onChanged = { viewModel.setReducedMotion(it); feedback?.toggle(it) },
                )
                SwitchRow(
                    icon = Icons.Outlined.Speed,
                    label = stringRes("settings_fast_refresh"),
                    value = settings.fastRefresh,
                    onChanged = { viewModel.setFastRefresh(it); feedback?.toggle(it) },
                )
                SwitchRow(
                    icon = Icons.Outlined.MonitorHeart,
                    label = stringRes("settings_bg_monitoring"),
                    value = settings.backgroundMonitoring,
                    onChanged = { viewModel.setBackgroundMonitoring(it); feedback?.toggle(it) },
                )
            }
        }

        item {
            SmartAlertsSettingsPanel(
                settings = settings,
                unlocked = smartAlertsUnlocked,
                onSubscribeClick = {
                    feedback?.light()
                    onSubscriptionClick()
                },
                onMasterChanged = {
                    viewModel.setSmartAlertsEnabled(it)
                    feedback?.toggle(it)
                },
                onTypeChanged = { type, enabled ->
                    viewModel.setSmartAlertTypeEnabled(type.key, enabled)
                    feedback?.toggle(enabled)
                },
            )
        }

        item {
            SettingsPanel(title = stringRes("settings_feedback"), icon = Icons.Outlined.Vibration) {
                SwitchRow(
                    icon = Icons.Outlined.Vibration,
                    label = stringRes("settings_haptic_feedback"),
                    value = settings.hapticFeedback,
                    onChanged = { viewModel.setHapticFeedback(it); feedback?.toggle(it) },
                )
                SwitchRow(
                    icon = Icons.Outlined.MusicNote,
                    label = stringRes("settings_sound_effects"),
                    value = settings.soundEffects,
                    onChanged = { viewModel.setSoundEffects(it); feedback?.toggle(it) },
                )
            }
        }

        item {
            SettingsPanel(title = stringRes("settings_export"), icon = Icons.Outlined.FileDownload) {
                OptionRow(
                    label = stringRes("settings_export_format"),
                    options = listOf(
                        Choice("JSON", ExportFormat.Json),
                        Choice("CSV", ExportFormat.Csv),
                        Choice("TXT", ExportFormat.Txt),
                    ),
                    selected = settings.exportFormat,
                    onSelected = { viewModel.setExportFormat(it); feedback?.light() },
                )
                Spacer(Modifier.height(12.dp))
                exportState.errorMessage?.let { error ->
                    Text(
                        text = error.ifEmpty { stringRes("export_failed_fallback") },
                        style = MaterialTheme.typography.bodySmall,
                        color = colors.critical,
                        modifier = Modifier.padding(bottom = 8.dp),
                    )
                }
                Button(
                    onClick = {
                        exportViewModel.export(settings.exportFormat)
                        feedback?.confirm()
                    },
                    enabled = !exportState.isExporting,
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = colors.accent,
                        contentColor = colors.background,
                    ),
                    shape = RoundedCornerShape(14.dp),
                ) {
                    AnimatedContent(
                        targetState = exportState.isExporting,
                        transitionSpec = { fadeIn(tween(150)) togetherWith fadeOut(tween(100)) },
                        label = "export_button_content",
                    ) { loading ->
                        if (loading) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                            ) {
                                CircularProgressIndicator(modifier = Modifier.size(16.dp), color = colors.background, strokeWidth = 2.dp)
                                Text(stringRes("settings_export_exporting"))
                            }
                        } else {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                            ) {
                                Icon(Icons.Outlined.Share, contentDescription = null, modifier = Modifier.size(18.dp))
                                Text(stringRes("settings_export_button"))
                            }
                        }
                    }
                }
            }
        }

        item {
            SettingsPanel(title = stringRes("settings_about"), icon = Icons.Outlined.DataUsage) {
                InfoRow(stringRes("settings_platform"), "${PlatformInfo.osName} ${PlatformInfo.osVersion}", copyable = false)
                InfoRow(stringRes("settings_version"), PlatformInfo.appVersion, copyable = false)
                InfoRow(stringRes("settings_licences"), "Open source libraries", copyable = false, showDivider = false)
            }
        }
    }
}

private const val IOS_PRIVACY_POLICY_URL = "https://github.com/d1onyx/DeviceDNA/blob/master/PRIVACY_POLICY.md"

@Composable
private fun SettingsPanel(
    title: String,
    icon: ImageVector,
    content: @Composable ColumnScope.() -> Unit,
) {
    val colors = AppTheme.colors
    SectionCard {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .background(colors.surfaceHover),
                contentAlignment = Alignment.Center,
            ) {
                Icon(icon, contentDescription = null, tint = colors.accent, modifier = Modifier.size(20.dp))
            }
            Text(title, style = MaterialTheme.typography.titleLarge, color = colors.textPrimary)
        }
        Spacer(Modifier.height(14.dp))
        content()
    }
}

@Composable
private fun SmartAlertsSettingsPanel(
    settings: com.devstdvad.devicedna.data.settings.UserSettings,
    unlocked: Boolean,
    onSubscribeClick: () -> Unit,
    onMasterChanged: (Boolean) -> Unit,
    onTypeChanged: (SmartAlertType, Boolean) -> Unit,
) {
    val colors = AppTheme.colors
    SettingsPanel(title = stringRes("settings_smart_alerts"), icon = Icons.Outlined.MonitorHeart) {
        Text(
            text = stringRes("settings_smart_alerts_summary"),
            style = MaterialTheme.typography.bodySmall,
            color = colors.textSecondary,
        )
        Spacer(Modifier.height(8.dp))
        if (!unlocked) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .background(colors.surface)
                    .border(1.dp, colors.border, RoundedCornerShape(12.dp))
                    .clickable(onClick = onSubscribeClick)
                    .padding(horizontal = 12.dp, vertical = 12.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = stringRes("settings_smart_alerts_premium_title"),
                        style = MaterialTheme.typography.titleMedium,
                        color = colors.textPrimary,
                    )
                    Spacer(Modifier.height(4.dp))
                    Text(
                        text = stringRes("settings_smart_alerts_premium_body"),
                        style = MaterialTheme.typography.bodySmall,
                        color = colors.textSecondary,
                    )
                }
                StatusPill(
                    status = MetricStatus.Unavailable,
                    label = stringRes("subscription_status_premium"),
                )
                Icon(Icons.AutoMirrored.Outlined.KeyboardArrowRight, contentDescription = null, tint = colors.textMuted)
            }
            return@SettingsPanel
        }

        SwitchRow(
            icon = Icons.Outlined.MonitorHeart,
            label = stringRes("settings_smart_alerts_master"),
            value = settings.smartAlertsEnabled,
            onChanged = onMasterChanged,
        )
        SmartAlertType.entries
            .filterNot { PlatformInfo.isIos && it == SmartAlertType.SlowCharging }
            .forEach { type ->
            SwitchRow(
                icon = type.settingsIcon(),
                label = stringRes(type.settingKey),
                value = type.key in settings.smartAlertTypes,
                onChanged = { enabled -> onTypeChanged(type, enabled) },
            )
        }
    }
}

private fun SmartAlertType.settingsIcon(): ImageVector = when (this) {
    SmartAlertType.CpuOverheating -> Icons.Outlined.Thermostat
    SmartAlertType.LowBattery -> Icons.Outlined.Security
    SmartAlertType.StorageFull -> Icons.Outlined.DataUsage
    SmartAlertType.HighRam -> Icons.Outlined.Speed
    SmartAlertType.SlowCharging -> Icons.Outlined.MonitorHeart
}

@Composable
private fun SwitchRow(
    icon: ImageVector,
    label: String,
    value: Boolean,
    onChanged: (Boolean) -> Unit,
) {
    val colors = AppTheme.colors
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 7.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.weight(1f)) {
            Icon(icon, contentDescription = null, tint = colors.textMuted, modifier = Modifier.size(18.dp))
            Text(label, style = MaterialTheme.typography.bodyLarge, color = colors.textPrimary)
        }
        Switch(checked = value, onCheckedChange = onChanged)
    }
}

@Composable
private fun <T> OptionRow(
    label: String,
    options: List<Choice<T>>,
    selected: T,
    onSelected: (T) -> Unit,
) {
    val colors = AppTheme.colors
    Column(modifier = Modifier.padding(vertical = 7.dp)) {
        Text(label, style = MaterialTheme.typography.bodyMedium, color = colors.textSecondary)
        Spacer(Modifier.height(8.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
            options.forEach { option ->
                val active = option.value == selected
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(12.dp))
                        .background(if (active) colors.accent else colors.surface)
                        .border(1.dp, if (active) colors.accent else colors.border, RoundedCornerShape(12.dp))
                        .clickable { onSelected(option.value) }
                        .padding(horizontal = 10.dp, vertical = 10.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    Text(
                        option.label,
                        style = MaterialTheme.typography.labelLarge,
                        color = if (active) colors.background else colors.textSecondary,
                    )
                }
            }
        }
    }
}

private data class Choice<T>(val label: String, val value: T)
