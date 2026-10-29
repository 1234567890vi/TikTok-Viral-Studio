package com.creator.tiktokstudio.ui.theme

import androidx.compose.runtime.Composable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color

// Neon Accents & Signature Brand Colors
val NeonCyan = Color(0xFF00F2FE)
val NeonPink = Color(0xFFFE0979)
val NeonPurple = Color(0xFF9D00FF)
val GoldVip = Color(0xFFFFD700)
val GoldVipDark = Color(0xFFB8860B)

// Always-readable dark content on bright neon buttons/badges
val OnAccentDark = Color(0xFF0F0F13)

// Functional Status Colors
val SuccessGreen = Color(0xFF00E676)
val WarningOrange = Color(0xFFFF9100)
val ErrorRed = Color(0xFFFF3D00)

// Raw Dark Theme Colors
val RawDarkBackground = Color(0xFF0F0F13)
val RawDarkSurfaceCard = Color(0xFF16161F)
val RawDarkSurfaceElevated = Color(0xFF1E1E2A)
val RawDarkGlassBorder = Color(0x3300F2FE)
val RawDarkGlassCardBackground = Color(0xCC161622)
val RawDarkTextPrimary = Color(0xFFFFFFFF)
val RawDarkTextSecondary = Color(0xFFA0A5B5)
val RawDarkTextMuted = Color(0xFF6B7280)

// Raw Light Theme Colors (Soft Luxury Light with Slate & Cyan Accents)
val RawLightBackground = Color(0xFFF5F7FA)
val RawLightSurfaceCard = Color(0xFFFFFFFF)
val RawLightSurfaceElevated = Color(0xFFE9EDF5)
val RawLightGlassBorder = Color(0x3300F2FE)
val RawLightGlassCardBackground = Color(0xF5FFFFFF)
val RawLightTextPrimary = Color(0xFF111827)
val RawLightTextSecondary = Color(0xFF4B5563)
val RawLightTextMuted = Color(0xFF9CA3AF)

data class AppColorPalette(
    val background: Color,
    val surfaceCard: Color,
    val surfaceElevated: Color,
    val glassBorder: Color,
    val glassCardBackground: Color,
    val textPrimary: Color,
    val textSecondary: Color,
    val textMuted: Color,
    val isDark: Boolean
)

val DarkPalette = AppColorPalette(
    background = RawDarkBackground,
    surfaceCard = RawDarkSurfaceCard,
    surfaceElevated = RawDarkSurfaceElevated,
    glassBorder = RawDarkGlassBorder,
    glassCardBackground = RawDarkGlassCardBackground,
    textPrimary = RawDarkTextPrimary,
    textSecondary = RawDarkTextSecondary,
    textMuted = RawDarkTextMuted,
    isDark = true
)

val LightPalette = AppColorPalette(
    background = RawLightBackground,
    surfaceCard = RawLightSurfaceCard,
    surfaceElevated = RawLightSurfaceElevated,
    glassBorder = RawLightGlassBorder,
    glassCardBackground = RawLightGlassCardBackground,
    textPrimary = RawLightTextPrimary,
    textSecondary = RawLightTextSecondary,
    textMuted = RawLightTextMuted,
    isDark = false
)

val LocalAppPalette = staticCompositionLocalOf { DarkPalette }

object AppTheme {
    val colors: AppColorPalette
        @Composable
        get() = LocalAppPalette.current
}

// Dynamic properties enabling instantaneous light/dark adaptation across all screens
val DeepDarkBackground: Color
    @Composable
    get() = AppTheme.colors.background

val DarkSurfaceCard: Color
    @Composable
    get() = AppTheme.colors.surfaceCard

val DarkSurfaceElevated: Color
    @Composable
    get() = AppTheme.colors.surfaceElevated

val GlassBorder: Color
    @Composable
    get() = AppTheme.colors.glassBorder

val GlassCardBackground: Color
    @Composable
    get() = AppTheme.colors.glassCardBackground

val TextPrimary: Color
    @Composable
    get() = AppTheme.colors.textPrimary

val TextSecondary: Color
    @Composable
    get() = AppTheme.colors.textSecondary

val TextMuted: Color
    @Composable
    get() = AppTheme.colors.textMuted
