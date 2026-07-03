package com.devstdvad.devicedna.core.design.component

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.ContentCopy
import androidx.compose.material.icons.outlined.Visibility
import androidx.compose.material.icons.outlined.VisibilityOff
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.unit.dp
import com.devstdvad.devicedna.core.design.AppTheme
import com.devstdvad.devicedna.resources.stringRes

@Suppress("DEPRECATION")
@Composable
fun InfoRow(
    label: String,
    value: String,
    modifier: Modifier = Modifier,
    maskedValue: String? = null,
    copyable: Boolean = true,
    showDivider: Boolean = true,
    trailing: (@Composable () -> Unit)? = null,
) {
    val colors = AppTheme.colors
    val clipboard = LocalClipboardManager.current
    var revealed by remember { mutableStateOf(maskedValue == null) }
    val displayValue = if (revealed) value else (maskedValue ?: value)

    Column(modifier = modifier) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 10.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = label,
                style = MaterialTheme.typography.bodyMedium,
                color = colors.textSecondary,
                modifier = Modifier.weight(0.45f),
            )
            Row(
                modifier = Modifier.weight(0.55f),
                horizontalArrangement = Arrangement.End,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = displayValue,
                    style = MaterialTheme.typography.bodyMedium,
                    color = colors.textPrimary,
                    modifier = Modifier.weight(1f),
                )
                if (trailing != null) {
                    trailing()
                }
                if (maskedValue != null) {
                    IconButton(
                        onClick = { revealed = !revealed },
                        modifier = Modifier.size(32.dp),
                    ) {
                        Icon(
                            imageVector = if (revealed) Icons.Outlined.VisibilityOff else Icons.Outlined.Visibility,
                            contentDescription = if (revealed) stringRes("component_hide") else stringRes("component_reveal"),
                            tint = colors.textMuted,
                            modifier = Modifier.size(16.dp),
                        )
                    }
                }
                if (copyable) {
                    IconButton(
                        onClick = { clipboard.setText(AnnotatedString(if (revealed) value else displayValue)) },
                        modifier = Modifier.size(32.dp),
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.ContentCopy,
                            contentDescription = stringRes("component_copy"),
                            tint = colors.textMuted,
                            modifier = Modifier.size(14.dp),
                        )
                    }
                }
            }
        }
        if (showDivider) {
            HorizontalDivider(color = colors.border, thickness = 0.5.dp)
        }
    }
}
