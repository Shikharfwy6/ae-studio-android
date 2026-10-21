package com.app.aestudio.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val AeDarkScheme = darkColorScheme(
    primary = AeColors.Accent,
    onPrimary = Color.White,
    background = AeColors.Window,
    onBackground = AeColors.TextPrimary,
    surface = AeColors.Panel,
    onSurface = AeColors.TextPrimary,
    surfaceVariant = AeColors.PanelRaised,
    onSurfaceVariant = AeColors.TextSecondary,
    outline = AeColors.Hairline,
)

@Composable
fun AeStudioTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = AeDarkScheme,
        typography = AeTypography,
        content = content
    )
}
