package com.creator.tiktokstudio.admin.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable

private val DarkColorScheme = darkColorScheme(
    primary = RoyalGold,
    secondary = NeonCyan,
    tertiary = NeonPink,
    background = DeepDarkBackground,
    surface = DarkSurfaceCard,
    onPrimary = DeepDarkBackground,
    onSecondary = DeepDarkBackground,
    onBackground = TextPrimary,
    onSurface = TextPrimary
)

@Composable
fun AdminAppTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = DarkColorScheme,
        content = content
    )
}
