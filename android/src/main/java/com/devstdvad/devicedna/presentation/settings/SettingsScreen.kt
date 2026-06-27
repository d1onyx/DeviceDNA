package com.devstdvad.devicedna.presentation.settings

import androidx.compose.material.icons.automirrored.outlined.Logout
import androidx.compose.material.icons.automirrored.outlined.KeyboardArrowRight
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.devstdvad.devicedna.R
import com.devstdvad.devicedna.core.common.MetricStatus
import com.devstdvad.devicedna.core.design.AppTheme
import com.devstdvad.devicedna.core.design.component.InfoRow
import com.devstdvad.devicedna.core.design.component.SectionCard
import com.devstdvad.devicedna.core.design.component.StatusPill
import com.devstdvad.devicedna.core.feedback.LocalAppFeedback
import com.devstdvad.devicedna.data.settings.AppThemeMode
import com.devstdvad.devicedna.data.settings.DataUnit
import com.devstdvad.devicedna.data.settings.ExportFormat
import com.devstdvad.devicedna.data.settings.TemperatureUnit
import com.devstdvad.devicedna.data.subscription.PremiumEntitlements
import com.devstdvad.devicedna.data.subscription.PremiumFeature
import com.devstdvad.devicedna.data.subscription.SubscriptionRepository
import org.koin.androidx.compose.koinViewModel
import org.koin.compose.koinInject

@Composable
fun SettingsScreen(
    viewModel: SettingsViewModel = koinViewModel(),
    exportViewModel: ExportViewModel = koinViewModel(),
    subscriptionRepository: SubscriptionRepository = koinInject(),
    onSubscriptionClick: () -> Unit = {},
) {
    val colors = AppTheme.colors
    val settings by viewModel.settings.collectAsState()
    val user by viewModel.user.collectAsState()
    val exportState by exportViewModel.state.collectAsState()
    val entitlements by subscriptionRepository.entitlements.collectAsState(initial = PremiumEntitlements.Empty)
    val premiumActive = entitlements.hasFeature(PremiumFeature.RemoveAds)
    val feedback = LocalAppFeedback.current
    var privacyExpanded by remember { mutableStateOf(false) }
    val privacyScore by animateFloatAsState(
        targetValue = listOf(settings.maskSensitive, !settings.publicIpEnabled, !settings.showImei).count { it } / 3f,
        animationSpec = if (settings.reducedMotion) tween(0) else tween(500),
        label = "privacy_score",
    )

    val shareLauncher = rememberLauncherForActivityResult(ActivityResultContracts.StartActivityForResult()) {}

    LaunchedEffect(exportState.shareIntent) {
        exportState.shareIntent?.let { intent ->
            shareLauncher.launch(intent)
            exportViewModel.clearShareIntent()
        }
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp),
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
                        Text(stringResource(R.string.settings_title), style = MaterialTheme.typography.displaySmall, color = colors.textPrimary)
                        Spacer(Modifier.height(6.dp))
                        Text(stringResource(R.string.settings_subtitle), style = MaterialTheme.typography.bodyMedium, color = colors.textSecondary)
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
            SettingsPanel(title = stringResource(R.string.settings_account), icon = Icons.Outlined.AccountCircle) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(user?.displayName ?: stringResource(R.string.settings_account_signed_in), style = MaterialTheme.typography.titleMedium, color = colors.textPrimary)
                        Spacer(Modifier.height(4.dp))
                        Text(user?.email.orEmpty(), style = MaterialTheme.typography.bodyMedium, color = colors.textSecondary)
                    }
                    TextButton(onClick = viewModel::signOut) {
                        Icon(Icons.AutoMirrored.Outlined.Logout, contentDescription = null, tint = colors.accent, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.size(6.dp))
                        Text(stringResource(R.string.settings_sign_out), color = colors.accent)
                    }
                }
            }
        }

        item {
            SettingsPanel(title = stringResource(R.string.settings_premium), icon = Icons.Outlined.WorkspacePremium) {
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
                            text = stringResource(R.string.settings_premium_manage),
                            style = MaterialTheme.typography.titleMedium,
                            color = colors.textPrimary,
                        )
                        Spacer(Modifier.height(4.dp))
                        Text(
                            text = stringResource(R.string.settings_premium_summary),
                            style = MaterialTheme.typography.bodySmall,
                            color = colors.textSecondary,
                        )
                    }
                    StatusPill(
                        status = if (premiumActive) MetricStatus.Normal else MetricStatus.Unavailable,
                        label = if (premiumActive) {
                            stringResource(R.string.subscription_status_premium)
                        } else {
                            stringResource(R.string.subscription_status_free)
                        },
                    )
                    Icon(Icons.AutoMirrored.Outlined.KeyboardArrowRight, contentDescription = null, tint = colors.textMuted)
                }
            }
        }

        item {
            SettingsPanel(title = stringResource(R.string.settings_appearance), icon = Icons.Outlined.DarkMode) {
                OptionRow(
                    label = stringResource(R.string.settings_theme),
                    options = listOf(
                        Choice(stringResource(R.string.settings_theme_system), AppThemeMode.System),
                        Choice(stringResource(R.string.settings_theme_dark), AppThemeMode.Dark),
                        Choice(stringResource(R.string.settings_theme_light), AppThemeMode.Light),
                    ),
                    selected = settings.theme,
                    onSelected = { viewModel.setTheme(it); feedback?.light() },
                )
                OptionRow(
                    label = stringResource(R.string.settings_language_picker),
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
            SettingsPanel(title = stringResource(R.string.settings_units), icon = Icons.Outlined.Thermostat) {
                OptionRow(
                    label = stringResource(R.string.settings_temp_unit),
                    options = listOf(
                        Choice("°C", TemperatureUnit.Celsius),
                        Choice("°F", TemperatureUnit.Fahrenheit),
                    ),
                    selected = settings.temperatureUnit,
                    onSelected = { viewModel.setTemperatureUnit(it); feedback?.light() },
                )
                OptionRow(
                    label = stringResource(R.string.settings_data_unit),
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
            SettingsPanel(title = stringResource(R.string.settings_privacy), icon = Icons.Outlined.PrivacyTip) {
                SwitchRow(
                    icon = Icons.Outlined.Security,
                    label = stringResource(R.string.settings_mask_sensitive),
                    value = settings.maskSensitive,
                    onChanged = { viewModel.setMaskSensitive(it); feedback?.toggle(it) },
                )
                SwitchRow(
                    icon = Icons.Outlined.Public,
                    label = stringResource(R.string.settings_public_ip),
                    value = settings.publicIpEnabled,
                    onChanged = { viewModel.setPublicIpEnabled(it); feedback?.toggle(it) },
                )
                SwitchRow(
                    icon = Icons.Outlined.Security,
                    label = stringResource(R.string.settings_show_imei),
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
                            Text(stringResource(R.string.settings_privacy_policy_title), style = MaterialTheme.typography.titleMedium, color = colors.textPrimary)
                            Text(stringResource(R.string.settings_privacy_policy_summary), style = MaterialTheme.typography.bodySmall, color = colors.textSecondary)
                        }
                    }
                    Icon(if (privacyExpanded) Icons.Outlined.ExpandLess else Icons.Outlined.ExpandMore, contentDescription = null, tint = colors.textMuted)
                }
                if (privacyExpanded) {
                    Spacer(Modifier.height(12.dp))
                    Text(
                        text = stringResource(R.string.settings_privacy_policy_body),
                        style = MaterialTheme.typography.bodyMedium,
                        color = colors.textSecondary,
                    )
                }
            }
        }

        item {
            SettingsPanel(title = stringResource(R.string.settings_performance), icon = Icons.Outlined.Speed) {
                SwitchRow(
                    icon = Icons.Outlined.RestartAlt,
                    label = stringResource(R.string.settings_reduced_motion),
                    value = settings.reducedMotion,
                    onChanged = { viewModel.setReducedMotion(it); feedback?.toggle(it) },
                )
                SwitchRow(
                    icon = Icons.Outlined.Speed,
                    label = stringResource(R.string.settings_fast_refresh),
                    value = settings.fastRefresh,
                    onChanged = { viewModel.setFastRefresh(it); feedback?.toggle(it) },
                )
                SwitchRow(
                    icon = Icons.Outlined.MonitorHeart,
                    label = stringResource(R.string.settings_bg_monitoring),
                    value = settings.backgroundMonitoring,
                    onChanged = { viewModel.setBackgroundMonitoring(it); feedback?.toggle(it) },
                )
            }
        }

        item {
            SettingsPanel(title = stringResource(R.string.settings_feedback), icon = Icons.Outlined.Vibration) {
                SwitchRow(
                    icon = Icons.Outlined.Vibration,
                    label = stringResource(R.string.settings_haptic_feedback),
                    value = settings.hapticFeedback,
                    onChanged = { viewModel.setHapticFeedback(it); feedback?.toggle(it) },
                )
                SwitchRow(
                    icon = Icons.Outlined.MusicNote,
                    label = stringResource(R.string.settings_sound_effects),
                    value = settings.soundEffects,
                    onChanged = { viewModel.setSoundEffects(it); feedback?.toggle(it) },
                )
            }
        }

        item {
            SettingsPanel(title = stringResource(R.string.settings_export), icon = Icons.Outlined.FileDownload) {
                OptionRow(
                    label = stringResource(R.string.settings_export_format),
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
                        text = error,
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
                                Text(stringResource(R.string.settings_export_exporting))
                            }
                        } else {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                            ) {
                                Icon(Icons.Outlined.Share, contentDescription = null, modifier = Modifier.size(18.dp))
                                Text(stringResource(R.string.settings_export_button))
                            }
                        }
                    }
                }
            }
        }

        item {
            SettingsPanel(title = stringResource(R.string.settings_about), icon = Icons.Outlined.DataUsage) {
                InfoRow(stringResource(R.string.settings_platform), "Android ${Build.VERSION.RELEASE}", copyable = false)
                InfoRow(stringResource(R.string.settings_version), "1.3", copyable = false)
                InfoRow(stringResource(R.string.settings_licences), "Open source libraries", copyable = false, showDivider = false)
            }
        }
    }
}

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
