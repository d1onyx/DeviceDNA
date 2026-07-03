package com.devstdvad.devicedna.presentation.apps

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
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Apps
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import com.devstdvad.devicedna.core.common.PrivacyMask
import com.devstdvad.devicedna.core.design.AppTheme
import com.devstdvad.devicedna.data.settings.UserSettings
import com.devstdvad.devicedna.domain.model.AppDetails
import com.devstdvad.devicedna.presentation.common.LoadingScreen
import com.devstdvad.devicedna.di.resolveViewModel
import com.devstdvad.devicedna.platform.PlatformInfo
import com.devstdvad.devicedna.resources.stringRes

@Composable
fun AppsScreen(
    viewModel: AppsViewModel = resolveViewModel(AppsViewModel::class),
    settings: UserSettings = UserSettings(),
    contentPadding: PaddingValues = PaddingValues(),
) {
    val state by viewModel.state.collectAsState()
    val colors = AppTheme.colors

    if (state.isLoading) { LoadingScreen(); return }

    // No app inventory (iOS sandboxing forbids listing installed apps, or the query
    // failed). Render an honest explanation instead of a blank screen.
    if (state.info == null) {
        AppsUnavailable(contentPadding = contentPadding)
        return
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(
            start = 16.dp,
            end = 16.dp,
            top = 12.dp + contentPadding.calculateTopPadding(),
            bottom = 12.dp + contentPadding.calculateBottomPadding(),
        ),
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        item {
            state.info?.let {
                Text(stringRes("apps_count_summary", it.userCount, it.systemCount), style = MaterialTheme.typography.displaySmall, color = colors.textPrimary)
            }
        }
        item {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(
                    value = state.query,
                    onValueChange = viewModel::onQuery,
                    placeholder = { Text(stringRes("apps_search_placeholder"), color = colors.textMuted) },
                    leadingIcon = { Icon(Icons.Outlined.Search, null, tint = colors.textMuted) },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(10.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = colors.accent,
                        unfocusedBorderColor = colors.border,
                        focusedTextColor = colors.textPrimary,
                        unfocusedTextColor = colors.textPrimary,
                        focusedContainerColor = colors.surfaceElevated,
                        unfocusedContainerColor = colors.surfaceElevated,
                    ),
                )
                FilterChip(
                    selected = state.showSystem,
                    onClick = viewModel::toggleSystem,
                    label = { Text(stringRes("apps_show_system_toggle")) },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = colors.accent.copy(alpha = 0.15f),
                        selectedLabelColor = colors.accent,
                        containerColor = colors.surfaceElevated,
                        labelColor = colors.textSecondary,
                    ),
                )
            }
        }
        items(state.filtered, key = { it.packageName }) { app ->
            AppItem(app = app, maskSensitive = settings.maskSensitive)
        }
    }
}

@Composable
private fun AppsUnavailable(contentPadding: PaddingValues) {
    val colors = AppTheme.colors
    val isIos = PlatformInfo.isIos
    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(
                top = contentPadding.calculateTopPadding(),
                bottom = contentPadding.calculateBottomPadding(),
            )
            .padding(horizontal = 32.dp),
        contentAlignment = Alignment.Center,
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Icon(
                Icons.Outlined.Apps,
                contentDescription = null,
                tint = colors.textMuted,
                modifier = Modifier.size(48.dp),
            )
            Text(
                text = stringRes("apps_unavailable_title"),
                style = MaterialTheme.typography.titleLarge,
                color = colors.textPrimary,
            )
            Text(
                text = if (isIos) {
                    stringRes("apps_unavailable_ios_body")
                } else {
                    stringRes("apps_unavailable_generic_body")
                },
                style = MaterialTheme.typography.bodyMedium,
                color = colors.textMuted,
                modifier = Modifier.fillMaxWidth(),
                textAlign = androidx.compose.ui.text.style.TextAlign.Center,
            )
        }
    }
}

@Composable
private fun AppItem(
    app: AppDetails,
    maskSensitive: Boolean,
) {
    val colors = AppTheme.colors
    Surface(
        modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(10.dp)).border(1.dp, colors.border, RoundedCornerShape(10.dp)),
        color = colors.surfaceElevated,
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Text(app.name, style = MaterialTheme.typography.titleMedium, color = colors.textPrimary, modifier = Modifier.weight(1f))
                Text(app.versionName, style = MaterialTheme.typography.labelSmall, color = colors.textMuted)
            }
            Spacer(Modifier.height(2.dp))
            Text(
                text = if (maskSensitive) PrivacyMask.maskPackage(app.packageName) else app.packageName,
                style = MaterialTheme.typography.bodySmall,
                color = colors.textMuted,
            )
        }
    }
}
