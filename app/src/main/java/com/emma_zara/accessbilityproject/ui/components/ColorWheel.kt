package com.emma_zara.accessbilityproject.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import kotlin.math.PI
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.min
import kotlin.math.sin

/**
 * A round color wheel. Angle around the circle = hue, distance from the center = saturation.
 * [value] (brightness) darkens the whole wheel. Tap or drag to pick a color.
 */
@Composable
fun ColorWheel(
    hue: Float,
    saturation: Float,
    value: Float,
    onChange: (hue: Float, saturation: Float) -> Unit,
    modifier: Modifier = Modifier,
) {
    val currentOnChange by rememberUpdatedState(onChange)
    // Red -> yellow -> green -> cyan -> blue -> magenta -> red
    val hueColors = remember { (0..6).map { Color.hsv((it * 60f) % 360f, 1f, 1f) } }

    Canvas(
        modifier
            .aspectRatio(1f)
            .semantics { contentDescription = "Color wheel. Use the sliders below to change the color precisely." }
            .pointerInput(Unit) {
                detectTapGestures { pos -> handleTouch(pos, size.width, size.height, currentOnChange) }
            }
            .pointerInput(Unit) {
                detectDragGestures(
                    onDragStart = { pos -> handleTouch(pos, size.width, size.height, currentOnChange) },
                ) { change, _ -> handleTouch(change.position, size.width, size.height, currentOnChange) }
            },
    ) {
        val radius = size.minDimension / 2f
        drawCircle(Brush.sweepGradient(hueColors, center), radius)
        drawCircle(Brush.radialGradient(listOf(Color.White, Color.White.copy(alpha = 0f)), center, radius), radius)
        drawCircle(Color.Black.copy(alpha = 1f - value), radius)

        // The selector ring, black + white so it is visible on any color
        val angle = hue / 180f * PI.toFloat()
        val pos = center + Offset(cos(angle), sin(angle)) * (saturation * radius)
        drawCircle(Color.Black, 16.dp.toPx(), pos, style = Stroke(3.dp.toPx()))
        drawCircle(Color.White, 12.dp.toPx(), pos, style = Stroke(3.dp.toPx()))
    }
}

private fun handleTouch(pos: Offset, width: Int, height: Int, onChange: (Float, Float) -> Unit) {
    val center = Offset(width / 2f, height / 2f)
    val d = pos - center
    val radius = min(width, height) / 2f
    val hue = ((atan2(d.y, d.x) * 180f / PI.toFloat()) + 360f) % 360f
    val saturation = (d.getDistance() / radius).coerceIn(0f, 1f)
    onChange(hue, saturation)
}
