package com.emma_zara.accessbilityproject.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.isSpecified

/**
 * Builds the app's text styles from the user's settings.
 * Every text style is made bigger by [scale], and made bold if [bold] is on.
 */
fun appTypography(scale: Float, bold: Boolean): Typography {
    val base = Typography()

    fun TextStyle.adjust() = copy(
        fontSize = if (fontSize.isSpecified) fontSize * scale else fontSize,
        lineHeight = if (lineHeight.isSpecified) lineHeight * scale else lineHeight,
        fontWeight = if (bold) FontWeight.Bold else fontWeight,
    )

    return Typography(
        displayLarge = base.displayLarge.adjust(),
        displayMedium = base.displayMedium.adjust(),
        displaySmall = base.displaySmall.adjust(),
        headlineLarge = base.headlineLarge.adjust(),
        headlineMedium = base.headlineMedium.adjust(),
        headlineSmall = base.headlineSmall.adjust(),
        titleLarge = base.titleLarge.adjust(),
        titleMedium = base.titleMedium.adjust(),
        titleSmall = base.titleSmall.adjust(),
        bodyLarge = base.bodyLarge.adjust(),
        bodyMedium = base.bodyMedium.adjust(),
        bodySmall = base.bodySmall.adjust(),
        labelLarge = base.labelLarge.adjust(),
        labelMedium = base.labelMedium.adjust(),
        labelSmall = base.labelSmall.adjust(),
    )
}
