package com.creator.tiktokstudio.ui.theme

import android.app.Activity
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

private val DarkColorScheme = darkColorScheme(
    primary = NeonCyan,
    onPrimary = OnAccentDark,
    primaryContainer = RawDarkSurfaceElevated,
    onPrimaryContainer = NeonCyan,
    secondary = NeonPink,
    onSecondary = RawDarkTextPrimary,
    secondaryContainer = RawDarkSurfaceCard,
    onSecondaryContainer = NeonPink,
    tertiary = GoldVip,
    onTertiary = OnAccentDark,
    background = RawDarkBackground,
    onBackground = RawDarkTextPrimary,
    surface = RawDarkSurfaceCard,
    onSurface = RawDarkTextPrimary,
    surfaceVariant = RawDarkSurfaceElevated,
    onSurfaceVariant = RawDarkTextSecondary,
    outline = RawDarkGlassBorder
)

private val LightColorScheme = lightColorScheme(
    primary = NeonCyan,
    onPrimary = OnAccentDark,
    primaryContainer = RawLightSurfaceElevated,
    onPrimaryContainer = NeonCyan,
    secondary = NeonPink,
    onSecondary = RawLightTextPrimary,
    secondaryContainer = RawLightSurfaceCard,
    onSecondaryContainer = NeonPink,
    tertiary = GoldVipDark,
    onTertiary = RawLightBackground,
    background = RawLightBackground,
    onBackground = RawLightTextPrimary,
    surface = RawLightSurfaceCard,
    onSurface = RawLightTextPrimary,
    surfaceVariant = RawLightSurfaceElevated,
    onSurfaceVariant = RawLightTextSecondary,
    outline = RawLightGlassBorder
)

@Composable
fun TikTokStudioTheme(
    darkTheme: Boolean = true,
    content: @Composable () -> Unit
) {
    val palette = if (darkTheme) DarkPalette else LightPalette
    val colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme
    val view = LocalView.current

    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as? Activity)?.window
            if (window != null) {
                window.statusBarColor = palette.background.toArgb()
                window.navigationBarColor = palette.background.toArgb()
                val insetsController = WindowCompat.getInsetsController(window, view)
                insetsController.isAppearanceLightStatusBars = !darkTheme
                insetsController.isAppearanceLightNavigationBars = !darkTheme
            }
        }
    }

    CompositionLocalProvider(LocalAppPalette provides palette) {
        MaterialTheme(
            colorScheme = colorScheme,
            typography = Typography,
            content = content
        )
    }
}
