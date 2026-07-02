package com.devstdvad.devicedna.core.design.component

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.devstdvad.devicedna.core.design.AppTheme

@Composable
fun AccentCard(
    accentColor: Color,
    modifier: Modifier = Modifier,
    content: @Composable ColumnScope.() -> Unit,
) {
    val colors = AppTheme.colors
    Surface(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .border(1.dp, accentColor.copy(alpha = 0.25f), RoundedCornerShape(14.dp)),
        color = colors.surfaceElevated,
        shape = RoundedCornerShape(14.dp),
    ) {
        Column {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(2.dp)
                    .background(
                        Brush.horizontalGradient(
                            colors = listOf(
                                accentColor,
                                accentColor.copy(alpha = 0.5f),
                                Color.Transparent,
                            ),
                        ),
                    ),
            )
            Column(modifier = Modifier.padding(16.dp), content = content)
        }
    }
}
