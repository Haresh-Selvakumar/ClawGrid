package com.example.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable

private val ClawGridDarkColorScheme = darkColorScheme(
    primary = ClawBlue,
    onPrimary = ClawTextMain,
    primaryContainer = ClawBgCardElevated,
    onPrimaryContainer = ClawBlueLight,
    secondary = ClawCyan,
    onSecondary = ClawBgDark,
    secondaryContainer = ClawEmeraldBg,
    onSecondaryContainer = ClawEmeraldLight,
    tertiary = ClawAmber,
    onTertiary = ClawBgDark,
    background = ClawBgDark,
    onBackground = ClawTextMain,
    surface = ClawBgSidebar,
    onSurface = ClawTextMain,
    surfaceVariant = ClawBgCard,
    onSurfaceVariant = ClawTextMuted,
    outline = ClawBorder,
    outlineVariant = ClawBorderHighlight,
    error = ClawRed,
    onError = ClawTextMain
)

@Composable
fun MyApplicationTheme(
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colorScheme = ClawGridDarkColorScheme,
        typography = Typography,
        content = content
    )
}
