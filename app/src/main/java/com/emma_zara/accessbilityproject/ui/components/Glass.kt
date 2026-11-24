package com.emma_zara.accessbilityproject.ui.components

import androidx.annotation.DrawableRes
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import com.emma_zara.accessbilityproject.ui.theme.GlassBg1
import com.emma_zara.accessbilityproject.ui.theme.GlassBg2
import com.emma_zara.accessbilityproject.ui.theme.GlassBg3
import com.emma_zara.accessbilityproject.ui.theme.GlassBg4
import com.emma_zara.accessbilityproject.ui.theme.GlassEdge
import com.emma_zara.accessbilityproject.ui.theme.LocalGlass
import com.emma_zara.accessbilityproject.ui.theme.LocalHighContrast

/**
 * How much room to leave at the bottom of a page for the floating glass tab bar. In the glass theme
 * pages scroll underneath the bar (you see them through the glass), so they end with this much space
 * to let the last thing scroll up above it. Zero in other themes (the tab bar isn't over the page).
 */
val LocalBottomBarSpace = compositionLocalOf { 0.dp }

/**
 * The edge around a card or panel: a thin bright line for frosted glass (like light catching the
 * edge of glass), a thick line for high contrast, nothing otherwise.
 */
@Composable
fun panelBorder(): BorderStroke? = when {
    LocalHighContrast.current -> BorderStroke(2.dp, MaterialTheme.colorScheme.outline)
    LocalGlass.current -> BorderStroke(1.dp, GlassEdge)
    else -> null
}

/**
 * Behind everything in the frosted glass theme: a soft colorful wash with a few big blurry blobs,
 * so the see-through panels on top look like glass. Other themes just get their plain background.
 */
@Composable
fun AppBackground(content: @Composable BoxScope.() -> Unit) {
    if (!LocalGlass.current) {
        Box(Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background), content = content)
        return
    }
    Box(Modifier.fillMaxSize()) {
        Canvas(Modifier.fillMaxSize()) {
            drawRect(Brush.linearGradient(listOf(GlassBg1, GlassBg2, GlassBg3, GlassBg4), Offset.Zero, Offset(size.width, size.height)))
            fun blob(color: Color, x: Float, y: Float, r: Float) =
                drawCircle(Brush.radialGradient(listOf(color, color.copy(alpha = 0f)), Offset(x, y), r), r, Offset(x, y))
            blob(Color(0x99A9B8FF), size.width * 0.15f, size.height * 0.18f, size.width * 0.7f)
            blob(Color(0x80FFC1D9), size.width * 0.9f, size.height * 0.45f, size.width * 0.65f)
            blob(Color(0x80B3F0DA), size.width * 0.2f, size.height * 0.85f, size.width * 0.7f)
        }
        content()
    }
}

/**
 * A round button with just an icon, like the back and delete buttons at the top of a page.
 * Frosted glass in the glass theme; a soft gray circle otherwise.
 */
@Composable
fun RoundIconButton(
    @DrawableRes icon: Int,
    contentDescription: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    // surfaceContainerHigh is see-through white in the glass theme, soft gray in the others
    Surface(
        onClick = onClick,
        shape = CircleShape,
        color = MaterialTheme.colorScheme.surfaceContainerHigh,
        contentColor = MaterialTheme.colorScheme.onSurface,
        border = panelBorder(),
        modifier = modifier.size(48.dp),
    ) {
        Box(contentAlignment = Alignment.Center) {
            Icon(painterResource(icon), contentDescription = contentDescription, modifier = Modifier.size(24.dp))
        }
    }
}
