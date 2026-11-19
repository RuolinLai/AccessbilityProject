package com.emma_zara.accessbilityproject.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.staticCompositionLocalOf
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

/** Lets any screen know the chosen letter casing (Compose has no built-in "text-transform"). */
val LocalTextCasing = staticCompositionLocalOf { TextCasing.NORMAL }

/** Lets screens add extra borders when high contrast is on. */
val LocalHighContrast = staticCompositionLocalOf { false }

@Composable
fun AccessbilityProjectTheme(
    settings: AppSettings,
    content: @Composable () -> Unit,
) {
    val colors = when (settings.theme) {
        AppTheme.LIGHT -> LightColors
        AppTheme.DARK -> DarkColors
        AppTheme.HIGH_CONTRAST -> HighContrastColors
    }

    CompositionLocalProvider(
        LocalTextCasing provides settings.casing,
        LocalHighContrast provides (settings.theme == AppTheme.HIGH_CONTRAST),
    ) {
        MaterialTheme(
            colorScheme = colors,
            typography = appTypography(settings.fontScale, settings.bold),
            content = content,
        )
    }
}
