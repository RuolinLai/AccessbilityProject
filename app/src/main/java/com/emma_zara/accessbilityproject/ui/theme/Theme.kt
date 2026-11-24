package com.emma_zara.accessbilityproject.ui.theme

import android.provider.Settings as SystemSettings
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import com.emma_zara.accessbilityproject.color.ColorVision
import com.emma_zara.accessbilityproject.settings.AppSettings
import com.emma_zara.accessbilityproject.settings.AppTheme
import com.emma_zara.accessbilityproject.settings.TextCasing

private val LightColors = lightColorScheme(
    primary = Indigo40, onPrimary = NeutralLightSurface1,
    primaryContainer = Indigo90, onPrimaryContainer = Indigo30,
    secondary = Indigo40, onSecondary = NeutralLightSurface1,
    secondaryContainer = Indigo90, onSecondaryContainer = Indigo30,
    background = NeutralLightBg, onBackground = NeutralLightText,
    surface = NeutralLightBg, onSurface = NeutralLightText,
    surfaceVariant = NeutralLightSurface2, onSurfaceVariant = NeutralLightTextDim,
    surfaceContainerLowest = NeutralLightSurface1, surfaceContainerLow = NeutralLightSurface1,
    surfaceContainer = NeutralLightSurface1, surfaceContainerHigh = NeutralLightSurface2,
    surfaceContainerHighest = NeutralLightSurface3,
    outline = NeutralLightOutline, outlineVariant = NeutralLightOutlineSoft,
)

private val DarkColors = darkColorScheme(
    primary = Indigo80, onPrimary = Indigo30,
    primaryContainer = Indigo30, onPrimaryContainer = Indigo90,
    secondary = Indigo80, onSecondary = Indigo30,
    secondaryContainer = Indigo30, onSecondaryContainer = Indigo90,
    background = NeutralDarkBg, onBackground = NeutralDarkText,
    surface = NeutralDarkBg, onSurface = NeutralDarkText,
    surfaceVariant = NeutralDarkSurface2, onSurfaceVariant = NeutralDarkTextDim,
    surfaceContainerLowest = NeutralDarkSurface1, surfaceContainerLow = NeutralDarkSurface1,
    surfaceContainer = NeutralDarkSurface1, surfaceContainerHigh = NeutralDarkSurface2,
    surfaceContainerHighest = NeutralDarkSurface3,
    outline = NeutralDarkOutline, outlineVariant = NeutralDarkOutlineSoft,
)

/** Yellow on black everywhere, with yellow borders, for low-vision users. */
private val HighContrastColors = darkColorScheme(
    primary = HcYellow, onPrimary = HcBlack,
    primaryContainer = HcYellow, onPrimaryContainer = HcBlack,
    secondary = HcYellow, onSecondary = HcBlack,
    secondaryContainer = HcDarkGray, onSecondaryContainer = HcYellow,
    tertiary = HcYellow, onTertiary = HcBlack,
    background = HcBlack, onBackground = HcYellow,
    surface = HcBlack, onSurface = HcYellow,
    surfaceVariant = HcBlack, onSurfaceVariant = HcYellow,
    surfaceContainerLowest = HcBlack, surfaceContainerLow = HcBlack,
    surfaceContainer = HcBlack, surfaceContainerHigh = HcBlack, surfaceContainerHighest = HcBlack,
    outline = HcYellow, outlineVariant = HcYellow,
)

/** A deep blue card with white text and light blue buttons (used on the light theme). */
private val NavyCardColors = DarkColors.copy(
    primary = Indigo80, onPrimary = Indigo30,
    surface = Navy, onSurface = Color.White,
    surfaceContainer = Navy, surfaceContainerLow = Navy, surfaceContainerLowest = Navy,
    surfaceContainerHigh = NavyHigh, surfaceContainerHighest = NavyHighest,
    surfaceVariant = NavyHigh, onSurfaceVariant = NavyTextDim,
    outline = NavyOutline, outlineVariant = NavyHighest,
    error = Color(0xFFFFB4AB),
)

/** A light blue card with dark blue text and deep blue buttons (used on the dark theme). */
private val SkyCardColors = LightColors.copy(
    primary = Indigo40, onPrimary = Color.White,
    surface = SkyBlue, onSurface = SkyText,
    surfaceContainer = SkyBlue, surfaceContainerLow = SkyBlue, surfaceContainerLowest = SkyBlue,
    surfaceContainerHigh = SkyBlueHigh, surfaceContainerHighest = SkyBlueHighest,
    surfaceVariant = SkyBlueHigh, onSurfaceVariant = SkyTextDim,
    outline = SkyOutline, outlineVariant = SkyBlueHighest,
)

/**
 * Frosted glass: see-through white panels over a soft colorful background (drawn by the app behind
 * everything). Text is dark slate; filled buttons are dark glass with white text so they stay easy to read.
 */
private val GlassColors = lightColorScheme(
    primary = GlassSlate, onPrimary = Color.White,
    primaryContainer = GlassPanelHigh, onPrimaryContainer = GlassText,
    secondary = GlassSlate, onSecondary = Color.White,
    secondaryContainer = GlassPanelHigh, onSecondaryContainer = GlassText,
    tertiary = GlassSlate, onTertiary = Color.White,
    background = Color.Transparent, onBackground = GlassText,
    surface = GlassPanel, onSurface = GlassText,
    surfaceVariant = GlassPanel, onSurfaceVariant = GlassTextDim,
    surfaceContainerLowest = GlassPanel, surfaceContainerLow = GlassPanel,
    surfaceContainer = GlassPanel, surfaceContainerHigh = GlassPanelHigh,
    surfaceContainerHighest = GlassPanelHighest,
    outline = GlassOutline, outlineVariant = GlassEdge,
    inverseSurface = GlassText, inverseOnSurface = Color.White,
)

/** True when the frosted glass theme is on: glass panels get a bright edge, the tab bar floats. */
val LocalGlass = staticCompositionLocalOf { false }

/** True on the dark theme (and high contrast), for colors that need a dark version. */
val LocalDarkTheme = staticCompositionLocalOf { false }

/** Lets any screen know the chosen letter casing (Compose has no built-in "text-transform"). */
val LocalTextCasing = staticCompositionLocalOf { TextCasing.NORMAL }

/** Lets screens add extra borders when high contrast is on. */
val LocalHighContrast = staticCompositionLocalOf { false }

/** False when animations are turned off, in the app or with Android's "Remove animations" setting. */
val LocalMotion = staticCompositionLocalOf { true }

/** How different a new color must be to play the paint splash (from Settings). */
val LocalSplashDifference = staticCompositionLocalOf { 30 }

/** The user's kind of color vision (from Settings). */
val LocalColorVision = staticCompositionLocalOf { ColorVision.TYPICAL }

/** The opposite theme's colors (dark if the app is light, light if it's dark), for cards that stand out. */
val LocalInvertedColors = staticCompositionLocalOf { DarkColors }

/** True if the phone should buzz when the color changes (from Settings). */
val LocalVibrate = staticCompositionLocalOf { true }

/** True unless the phone's accessibility setting "Remove animations" is on. */
@Composable
fun systemAnimationsOn(): Boolean {
    val resolver = LocalContext.current.contentResolver
    return SystemSettings.Global.getFloat(resolver, SystemSettings.Global.ANIMATOR_DURATION_SCALE, 1f) != 0f
}

@Composable
fun AccessbilityProjectTheme(
    settings: AppSettings,
    content: @Composable () -> Unit,
) {
    val colors = when (settings.theme) {
        AppTheme.LIGHT -> LightColors
        AppTheme.DARK -> DarkColors
        AppTheme.HIGH_CONTRAST -> HighContrastColors
        AppTheme.GLASS -> GlassColors
    }
    // Colors for a card that should stand out: deep blue on the light theme, light blue on the dark theme.
    // High contrast stays yellow on black; glass stays glass.
    val inverted = when (settings.theme) {
        AppTheme.LIGHT -> NavyCardColors
        AppTheme.DARK -> SkyCardColors
        AppTheme.HIGH_CONTRAST -> HighContrastColors
        AppTheme.GLASS -> GlassColors
    }

    CompositionLocalProvider(
        LocalInvertedColors provides inverted,
        LocalGlass provides (settings.theme == AppTheme.GLASS),
        LocalDarkTheme provides (settings.theme == AppTheme.DARK || settings.theme == AppTheme.HIGH_CONTRAST),
        LocalTextCasing provides settings.casing,
        LocalHighContrast provides (settings.theme == AppTheme.HIGH_CONTRAST),
        LocalMotion provides (settings.animations && systemAnimationsOn()),
        LocalSplashDifference provides settings.splashDifference,
        LocalColorVision provides settings.colorVision,
        LocalVibrate provides settings.vibrate,
    ) {
        MaterialTheme(
            colorScheme = colors,
            typography = appTypography(settings.fontScale, settings.bold),
            content = content,
        )
    }
}
