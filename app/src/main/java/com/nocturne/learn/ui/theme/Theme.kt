package com.nocturne.learn.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val NocturneColorScheme = darkColorScheme(
    primary = NocturneColors.Champagne,
    onPrimary = NocturneColors.Ink,
    primaryContainer = NocturneColors.Champagne.copy(alpha = 0.15f),
    onPrimaryContainer = NocturneColors.ChampagneLight,
    secondary = NocturneColors.Moon,
    onSecondary = NocturneColors.Night950,
    background = NocturneColors.Night950,
    onBackground = Color.White,
    surface = NocturneColors.Night900,
    onSurface = Color.White,
    surfaceVariant = NocturneColors.Steel800,
    onSurfaceVariant = NocturneColors.TextSecondary,
    outline = NocturneColors.Hairline,
    error = Color(0xFFE08A8A),
)

@Composable
fun NocturneTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = NocturneColorScheme,
        typography = NocturneTypography,
        content = content,
    )
}
