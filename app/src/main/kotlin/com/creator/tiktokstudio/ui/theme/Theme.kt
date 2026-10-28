package com.creator.tiktokstudio.ui.theme

import android.app.Activity
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

private val DarkColorScheme = darkColorScheme(
    primary = NeonCyan,
    onPrimary = DeepDarkBackground,
    primaryContainer = DarkSurfaceElevated,
    onPrimaryContainer = NeonCyan,
    secondary = NeonPink,
    onSecondary = TextPrimary,
    secondaryContainer = DarkSurfaceCard,
    onSecondaryContainer = NeonPink,
    tertiary = GoldVip,
    onTertiary = DeepDarkBackground,
    background = DeepDarkBackground,
    onBackground = TextPrimary,
    surface = DarkSurfaceCard,
    onSurface = TextPrimary,
    surfaceVariant = DarkSurfaceElevated,
    onSurfaceVariant = TextSecondary,
    outline = GlassBorder
)

@Composable
fun TikTokStudioTheme(
    darkTheme: Boolean = true, // Always enforce deep dark mode for luxury aesthetic
    content: @Composable () -> Unit
) {
    val colorScheme = DarkColorScheme
    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as? Activity)?.window
            if (window != null) {
                window.statusBarColor = DeepDarkBackground.toArgb()
                window.navigationBarColor = DeepDarkBackground.toArgb()
                WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars = false
                WindowCompat.getInsetsController(window, view).isAppearanceLightNavigationBars = false
            }
        }
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}
