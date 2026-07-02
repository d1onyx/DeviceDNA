package com.devstdvad.devicedna.presentation.auth

import com.devstdvad.devicedna.resources.stringRes
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
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.CloudDone
import androidx.compose.material.icons.outlined.Fingerprint
import androidx.compose.material.icons.outlined.Memory
import androidx.compose.material.icons.outlined.Security
import androidx.compose.material.icons.outlined.VerifiedUser
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.LinkAnnotation
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextLinkStyles
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.withLink
import androidx.compose.ui.unit.dp
import com.devstdvad.devicedna.core.design.AppTheme

@Composable
fun AuthScreen(
    state: AuthUiState,
    onGoogleSignIn: () -> Unit,
    modifier: Modifier = Modifier,
    requirePrivacyConsent: Boolean = true,
) {
    val colors = AppTheme.colors
    var privacyAccepted by rememberSaveable(requirePrivacyConsent) { mutableStateOf(!requirePrivacyConsent) }
    var showPolicy by rememberSaveable { mutableStateOf(false) }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(colors.background)
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 22.dp, vertical = 28.dp),
        verticalArrangement = Arrangement.SpaceBetween,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text("DeviceDNA", style = MaterialTheme.typography.titleLarge, color = colors.textPrimary)
                TrustBadge(
                    label = if (state.isConfigured) stringRes("auth_badge_ready") else stringRes("auth_badge_setup"),
                    icon = if (state.isConfigured) Icons.Outlined.VerifiedUser else Icons.Outlined.Security,
                )
            }

            Spacer(Modifier.height(34.dp))
            HeroConsole()
            Spacer(Modifier.height(34.dp))

            Text(
                text = stringRes("auth_title"),
                style = MaterialTheme.typography.displayMedium,
                color = colors.textPrimary,
                textAlign = TextAlign.Center,
            )
            Spacer(Modifier.height(12.dp))
            Text(
                text = stringRes("auth_subtitle"),
                style = MaterialTheme.typography.bodyLarge,
                color = colors.textSecondary,
                textAlign = TextAlign.Center,
            )
            Spacer(Modifier.height(24.dp))

            Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth()) {
//                AuthValue(Icons.Outlined.Fingerprint, stringRes("auth_value_private"), Modifier.weight(1f))
                AuthValue(Icons.Outlined.CloudDone, stringRes("auth_value_sync"), Modifier.weight(1f))
                AuthValue(Icons.Outlined.Memory, stringRes("auth_value_device"), Modifier.weight(1f))
            }
        }

        Column(modifier = Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
            if (requirePrivacyConsent) {
                PrivacyConsentRow(
                    accepted = privacyAccepted,
                    onAcceptedChange = { privacyAccepted = it },
                    onPolicyClick = { showPolicy = true },
                )
            }

            // Reserved slot below the card: keeps the card fixed in place while the
            // error / consent message appears or disappears.
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(min = 44.dp)
                    .padding(vertical = 8.dp),
                contentAlignment = Alignment.Center,
            ) {
                val message = state.errorMessage
                    ?: stringRes("auth_privacy_required")
                        .takeIf { requirePrivacyConsent && !privacyAccepted }
                if (message != null) {
                    Text(
                        text = message,
                        style = MaterialTheme.typography.bodySmall,
                        color = colors.warning,
                        textAlign = TextAlign.Center,
                    )
                }
            }

            Button(
                onClick = onGoogleSignIn,
                enabled = privacyAccepted && !state.isLoading,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(54.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = colors.textPrimary,
                    contentColor = colors.background,
                    disabledContainerColor = colors.surfaceHover,
                    disabledContentColor = colors.textMuted,
                ),
                shape = RoundedCornerShape(14.dp),
            ) {
                if (state.isLoading) {
                    CircularProgressIndicator(modifier = Modifier.size(22.dp), strokeWidth = 2.dp, color = colors.textMuted)
                } else {
                    GoogleMark()
                    Spacer(Modifier.size(10.dp))
                    Text(stringRes("auth_google"), style = MaterialTheme.typography.titleMedium)
                }
            }
            Spacer(Modifier.height(12.dp))
            Text(
                text = stringRes("auth_terms"),
                style = MaterialTheme.typography.bodySmall,
                color = colors.textMuted,
                textAlign = TextAlign.Center,
            )
        }
    }

    if (showPolicy) {
        PrivacyPolicySheet(onDismiss = { showPolicy = false })
    }
}

@Composable
private fun PrivacyConsentRow(
    accepted: Boolean,
    onAcceptedChange: (Boolean) -> Unit,
    onPolicyClick: () -> Unit,
) {
    val colors = AppTheme.colors
    val linkText = stringRes("auth_privacy_link")
    val consentText = buildAnnotatedString {
        append(stringRes("auth_privacy_accept_prefix"))
        append(" ")
        withLink(
            LinkAnnotation.Clickable(
                tag = "privacy_policy",
                styles = TextLinkStyles(
                    style = SpanStyle(
                        color = colors.accent,
                        fontWeight = FontWeight.SemiBold,
                        textDecoration = TextDecoration.Underline,
                    ),
                ),
            ) { onPolicyClick() },
        ) {
            append(linkText)
        }
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(colors.surfaceElevated)
            .border(1.dp, colors.border, RoundedCornerShape(16.dp))
            .padding(horizontal = 14.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Text(
            text = consentText,
            style = MaterialTheme.typography.bodyMedium,
            color = colors.textSecondary,
            modifier = Modifier.weight(1f),
        )
        Switch(
            checked = accepted,
            onCheckedChange = onAcceptedChange,
            colors = SwitchDefaults.colors(
                checkedThumbColor = colors.background,
                checkedTrackColor = colors.accent,
                uncheckedThumbColor = colors.textMuted,
                uncheckedTrackColor = colors.surfaceHover,
                uncheckedBorderColor = colors.border,
            ),
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun PrivacyPolicySheet(onDismiss: () -> Unit) {
    val colors = AppTheme.colors
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = colors.surfaceElevated,
        dragHandle = null,
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 22.dp, vertical = 24.dp),
        ) {
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .clip(RoundedCornerShape(14.dp))
                    .background(colors.accent.copy(alpha = 0.14f)),
                contentAlignment = Alignment.Center,
            ) {
                Icon(Icons.Outlined.Security, contentDescription = null, tint = colors.accent, modifier = Modifier.size(26.dp))
            }
            Spacer(Modifier.height(16.dp))
            Text(
                text = stringRes("auth_privacy_policy_title"),
                style = MaterialTheme.typography.headlineSmall,
                color = colors.textPrimary,
            )
            Spacer(Modifier.height(8.dp))
            Text(
                text = stringRes("auth_privacy_policy_summary"),
                style = MaterialTheme.typography.bodyMedium,
                color = colors.textSecondary,
            )
            Spacer(Modifier.height(20.dp))
            Text(
                text = stringRes("auth_privacy_policy_body"),
                style = MaterialTheme.typography.bodyMedium,
                color = colors.textSecondary,
            )
            Spacer(Modifier.height(24.dp))
            Button(
                onClick = onDismiss,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(50.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = colors.accent,
                    contentColor = colors.background,
                ),
                shape = RoundedCornerShape(14.dp),
            ) {
                Text(stringRes("auth_privacy_policy_close"), style = MaterialTheme.typography.titleMedium)
            }
            Spacer(Modifier.height(8.dp))
        }
    }
}

@Composable
private fun HeroConsole() {
    val colors = AppTheme.colors
    Box(
        modifier = Modifier
            .size(250.dp)
            .clip(RoundedCornerShape(30.dp))
            .background(colors.surfaceElevated)
            .border(1.dp, colors.border, RoundedCornerShape(30.dp))
            .padding(18.dp),
    ) {
        Column(verticalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxSize()) {
            Row(horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
                PulseDot(colors.success)
                PulseDot(colors.info)
                PulseDot(colors.warning)
            }
            Box(
                modifier = Modifier
                    .size(112.dp)
                    .clip(CircleShape)
                    .background(colors.surfaceHover)
                    .border(1.dp, colors.border, CircleShape)
                    .align(Alignment.CenterHorizontally),
                contentAlignment = Alignment.Center,
            ) {
                Box(
                    modifier = Modifier
                        .size(72.dp)
                        .clip(CircleShape)
                        .background(colors.accent),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(Icons.Outlined.Fingerprint, contentDescription = null, tint = colors.background, modifier = Modifier.size(38.dp))
                }
            }
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                SignalBar(0.92f)
                SignalBar(0.64f)
                SignalBar(0.78f)
            }
        }
    }
}

@Composable
private fun PulseDot(color: Color) {
    Box(
        modifier = Modifier
            .size(42.dp)
            .clip(CircleShape)
            .background(color.copy(alpha = 0.14f)),
        contentAlignment = Alignment.Center,
    ) {
        Box(
            modifier = Modifier
                .size(13.dp)
                .clip(CircleShape)
                .background(color),
        )
    }
}

@Composable
private fun SignalBar(widthFraction: Float) {
    val colors = AppTheme.colors
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(9.dp)
            .clip(RoundedCornerShape(99.dp))
            .background(colors.surface),
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth(widthFraction)
                .height(9.dp)
                .clip(RoundedCornerShape(99.dp))
                .background(colors.accentStrong),
        )
    }
}

@Composable
private fun TrustBadge(label: String, icon: ImageVector) {
    val colors = AppTheme.colors
    Row(
        modifier = Modifier
            .clip(RoundedCornerShape(99.dp))
            .background(colors.surfaceElevated)
            .border(1.dp, colors.border, RoundedCornerShape(99.dp))
            .padding(horizontal = 12.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(7.dp),
    ) {
        Icon(icon, contentDescription = null, tint = colors.accent, modifier = Modifier.size(16.dp))
        Text(label, style = MaterialTheme.typography.labelMedium, color = colors.textSecondary)
    }
}

@Composable
private fun AuthValue(icon: ImageVector, label: String, modifier: Modifier = Modifier) {
    val colors = AppTheme.colors
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(16.dp))
            .background(colors.surfaceElevated)
            .border(1.dp, colors.border, RoundedCornerShape(16.dp))
            .padding(12.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Icon(icon, contentDescription = null, tint = colors.accent, modifier = Modifier.size(22.dp))
        Text(label, style = MaterialTheme.typography.labelMedium, color = colors.textSecondary, textAlign = TextAlign.Center, fontWeight = FontWeight.Medium)
    }
}

@Composable
private fun GoogleMark() {
    Box(
        modifier = Modifier
            .size(24.dp)
            .clip(CircleShape)
            .background(Color.White),
        contentAlignment = Alignment.Center,
    ) {
        Text("G", color = Color(0xFF1A73E8), style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
    }
}
