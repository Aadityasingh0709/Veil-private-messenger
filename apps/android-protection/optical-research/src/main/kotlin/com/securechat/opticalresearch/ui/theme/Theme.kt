package com.securechat.opticalresearch.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable

private val LabColorScheme = darkColorScheme(
    primary = LabPrimary,
    secondary = LabSecondary,
    background = LabDarkBg,
    surface = LabDarkSurface,
    surfaceVariant = LabDarkCard,
    onPrimary = LabDarkBg,
    onSecondary = LabTextPrimary,
    onBackground = LabTextPrimary,
    onSurface = LabTextPrimary,
    outline = LabBorder
)

@Composable
fun VeilOpticalResearchTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = LabColorScheme,
        typography = Typography,
        content = content
    )
}
