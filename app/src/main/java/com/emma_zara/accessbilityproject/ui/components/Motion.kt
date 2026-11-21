package com.emma_zara.accessbilityproject.ui.components

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.TileMode
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.clipPath
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.emma_zara.accessbilityproject.R
import com.emma_zara.accessbilityproject.color.RgbColor
import com.emma_zara.accessbilityproject.color.deltaE
import com.emma_zara.accessbilityproject.color.toLab
import com.emma_zara.accessbilityproject.ui.theme.LocalHighContrast
import com.emma_zara.accessbilityproject.ui.theme.LocalMotion
import com.emma_zara.accessbilityproject.ui.theme.LocalSplashDifference
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.exp
import kotlin.math.max
import kotlin.math.min
import kotlin.math.sin

/** Red, orange, yellow, green, blue, purple and back to red, so a gradient of it loops smoothly. */
val Spectrum = listOf(0f, 30f, 60f, 120f, 200f, 260f, 300f, 360f).map { Color.hsv(it % 360f, 0.75f, 0.95f) }

/**
 * A thin rainbow bar that slowly flows sideways, shown under each screen title.
 * Just decoration, so screen readers skip it. Solid yellow in high contrast mode.
 */
@Composable
fun SpectrumRibbon(modifier: Modifier = Modifier, width: Dp = 88.dp) {
    val motion = LocalMotion.current
    val highContrast = LocalHighContrast.current
    val shift = if (motion && !highContrast) {
        rememberInfiniteTransition(label = "ribbon").animateFloat(
            initialValue = 0f,
            targetValue = 1f,
            animationSpec = infiniteRepeatable(tween(6000, easing = LinearEasing)),
            label = "shift",
        ).value
    } else {
        0f
    }
    val solid = MaterialTheme.colorScheme.primary
    Canvas(
        modifier
            .width(width)
            .height(6.dp)
            .clip(RoundedCornerShape(50))
            .clearAndSetSemantics {},
    ) {
        if (highContrast) {
            drawRect(solid)
        } else {
            val w = size.width
            drawRect(
                Brush.linearGradient(
                    Spectrum,
                    start = Offset(-shift * w, 0f),
                    end = Offset(w - shift * w, 0f),
                    tileMode = TileMode.Repeated,
                ),
            )
        }
    }
}

/** One paint drip: [x] across the block (0..1), size of its round end, and how far it hangs (0..1 of the drip space). */
private class Drip(val x: Float, val radius: Dp, val length: Float)

private val Drips = listOf(
    Drip(0.13f, 6.dp, 0.52f),
    Drip(0.37f, 8.dp, 0.72f),
    Drip(0.61f, 6.5.dp, 0.40f),   // this one lets go of a falling drop
    Drip(0.85f, 7.dp, 0.62f),
)
private const val FALLING_DRIP = 2
private const val RELEASE = 0.62f   // when (0..1 of the animation) the drop lets go

/**
 * A big block of color like a paint sample, with paint drips along the bottom.
 * When the color changes a lot:
 * 1. the new paint pours down from the top with a wavy edge,
 * 2. the drips get pushed longer by the new paint and settle back,
 * 3. one drip swells, lets go of a falling drop, and springs back up.
 */
@Composable
fun PaintSplash(color: RgbColor, modifier: Modifier = Modifier, blockHeight: Dp = 116.dp, dripSpace: Dp = 44.dp) {
    val motion = LocalMotion.current
    val highContrast = LocalHighContrast.current
    var under by remember { mutableStateOf(color) }   // the old paint
    var over by remember { mutableStateOf(color) }    // the new paint pouring in
    val splashDifference = LocalSplashDifference.current   // chosen in Settings
    var splashFrom by remember { mutableStateOf(color) }   // the color when the last splash started
    val progress = remember { Animatable(1f) }
    val scope = rememberCoroutineScope()
    val edge = if (highContrast) MaterialTheme.colorScheme.outline else MaterialTheme.colorScheme.outlineVariant

    LaunchedEffect(color) {
        if (color == over) return@LaunchedEffect
        // Only a really different color gets the splash. We compare with the color of the LAST splash,
        // so slowly moving the camera from red to blue still splashes once the color has changed enough.
        val big = deltaE(color.toLab(), splashFrom.toLab()) > splashDifference
        if (motion && big && !progress.isRunning) {
            under = over
            over = color
            splashFrom = color
            // The camera sends a new color many times a second, and each one restarts this effect.
            // So the animation runs in its own coroutine, where the next color can't cancel it.
            scope.launch {
                progress.snapTo(0f)
                progress.animateTo(1f, tween(1500, easing = LinearEasing))
            }
        } else {
            // Small change (camera noise, dragging a slider), or a splash is already playing:
            // the new paint just takes the new shade
            over = color
            if (!progress.isRunning) {
                under = color
                if (!motion) splashFrom = color
            }
        }
    }

    Canvas(modifier.height(blockHeight + dripSpace).clearAndSetSemantics {}) {
        val w = size.width
        val h = blockHeight.toPx()
        val space = dripSpace.toPx()
        val t = progress.value
        val animating = t < 1f

        // Where each drip is right now: (center x, radius, bottom y)
        val drips = Drips.mapIndexed { i, d ->
            val r = d.radius.toPx()
            var length = d.length
            if (animating) {
                // The new paint pushes each drip a little longer, one after another, then it settles back
                val k = ((t - 0.25f - i * 0.05f) / 0.6f).coerceIn(0f, 1f)
                length += 0.22f * sin(PI.toFloat() * k) * exp(-1.5f * k)
                if (i == FALLING_DRIP) length += dropStretch(t)
            }
            Triple(d.x * w, r, max(h + r * 2.6f, h + space * length))
        }
        val shape = paintShape(w, h, drips)

        clipPath(shape) {
            drawRect(under.toComposeColor())
            if (animating) {
                drawPath(pourPath(w, h + space, t, 5.dp.toPx()), over.toComposeColor())
            } else {
                drawRect(over.toComposeColor())
            }
            // A small shine on each drip makes it look wet
            if (!highContrast) {
                drips.forEach { (cx, r, bottom) ->
                    drawCircle(Color.White.copy(alpha = 0.32f), r * 0.3f, Offset(cx - r * 0.38f, bottom - r * 1.35f))
                }
            }
        }
        // Thin edge so white paint still shows on a white card
        drawPath(shape, edge, style = Stroke(1.5.dp.toPx()))

        // The drop that falls off: speeds up like real falling, stretches a little, and fades away
        if (animating && t >= RELEASE) {
            val u = (t - RELEASE) / (1f - RELEASE)
            val d = Drips[FALLING_DRIP]
            val cx = d.x * w
            val r0 = d.radius.toPx()
            val startY = max(h + r0 * 2.6f, h + space * (d.length + dropStretch(RELEASE))) - r0
            val y = startY + (size.height - startY) * u * u
            val r = r0 * (0.85f - 0.25f * u)
            val alpha = 1f - u * u * u
            val drop = teardrop(cx, y, r, tip = r * (2.1f + 0.7f * u))
            drawPath(drop, over.toComposeColor(), alpha = alpha)
            if (!highContrast) {
                drawCircle(Color.White.copy(alpha = 0.32f * alpha), r * 0.3f, Offset(cx - r * 0.38f, y - r * 0.35f))
            }
            drawPath(drop, edge, alpha = alpha, style = Stroke(1.5.dp.toPx()))
        }
    }
}

/** Extra length of the falling drip: it slowly swells, lets go at [RELEASE], then springs back up with a wobble. */
private fun dropStretch(t: Float): Float = when {
    t < 0.4f -> 0f
    t < RELEASE -> {
        val k = (t - 0.4f) / (RELEASE - 0.4f)
        0.3f * k * k
    }
    else -> {
        val u = (t - RELEASE) / (1f - RELEASE)
        0.3f * exp(-4f * u) * cos(9f * u)
    }
}

/**
 * The paint block and its drips as ONE outline. Each drip narrows to a thin neck under the
 * block and ends in a round drop, like paint that is about to fall.
 */
private fun paintShape(w: Float, h: Float, drips: List<Triple<Float, Float, Float>>): Path = Path().apply {
    moveTo(0f, 0f)
    lineTo(w, 0f)
    lineTo(w, h)
    // Walk along the bottom edge from right to left, going down and around each drip
    for ((cx, r, bottom) in drips.sortedByDescending { it.first }) {
        val flare = r * 1.9f          // how wide the drip is where it joins the block
        val neck = r * 0.4f           // half-width of the thin part
        val cy = bottom - r           // center of the round end
        val neckY = h + (cy - h) * 0.45f
        lineTo(cx + flare, h)
        // Right side: curve in to the thin neck, then out to the round end
        cubicTo(cx + flare * 0.45f, h, cx + neck, neckY - (neckY - h) * 0.5f, cx + neck, neckY)
        cubicTo(cx + neck, neckY + (cy - neckY) * 0.4f, cx + r, cy - (cy - neckY) * 0.6f, cx + r, cy)
        // Round end
        arcTo(Rect(cx - r, cy - r, cx + r, cy + r), 0f, 180f, false)
        // Left side: the same curves, mirrored, going back up
        cubicTo(cx - r, cy - (cy - neckY) * 0.6f, cx - neck, neckY + (cy - neckY) * 0.4f, cx - neck, neckY)
        cubicTo(cx - neck, neckY - (neckY - h) * 0.5f, cx - flare * 0.45f, h, cx - flare, h)
    }
    lineTo(0f, h)
    close()
}

/** A water drop: pointed on top, round at the bottom. [tip] is how far the point is above the center. */
private fun teardrop(cx: Float, cy: Float, r: Float, tip: Float): Path = Path().apply {
    moveTo(cx, cy - tip)
    cubicTo(cx + r * 0.2f, cy - tip * 0.6f, cx + r, cy - r * 0.8f, cx + r, cy)
    arcTo(Rect(cx - r, cy - r, cx + r, cy + r), 0f, 180f, false)
    cubicTo(cx - r, cy - r * 0.8f, cx - r * 0.2f, cy - tip * 0.6f, cx, cy - tip)
    close()
}

/** The new paint pouring down from the top, with a wavy front edge that calms down as it finishes. */
private fun pourPath(w: Float, bottom: Float, t: Float, maxWave: Float): Path {
    val fill = FastOutSlowInEasing.transform((t / 0.55f).coerceIn(0f, 1f))
    val wave = maxWave * sin(PI.toFloat() * fill)
    val level = -wave + fill * (bottom + 2 * wave)
    return Path().apply {
        moveTo(0f, -1f)
        lineTo(w, -1f)
        val steps = 32
        for (i in steps downTo 0) {
            val x = w * i / steps
            lineTo(x, level + wave * sin(x / w * 3f * PI.toFloat() + t * 12f))
        }
        close()
    }
}

/** A clothes hanger that gently swings, for the empty closet. */
@Composable
fun SwingingHanger(modifier: Modifier = Modifier) {
    val motion = LocalMotion.current
    val angle = if (motion) {
        rememberInfiniteTransition(label = "hanger").animateFloat(
            initialValue = -10f,
            targetValue = 10f,
            animationSpec = infiniteRepeatable(tween(1400, easing = FastOutSlowInEasing), RepeatMode.Reverse),
            label = "swing",
        ).value
    } else {
        0f
    }
    Box(
        modifier
            .size(112.dp)
            .clip(CircleShape)
            .background(MaterialTheme.colorScheme.primaryContainer)
            .clearAndSetSemantics {},
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            painterResource(R.drawable.ic_closet),
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onPrimaryContainer,
            modifier = Modifier
                .size(64.dp)
                // Swing from the hook at the top, like a real hanger
                .graphicsLayer {
                    rotationZ = angle
                    transformOrigin = TransformOrigin(0.5f, 0.1f)
                },
        )
    }
}

/**
 * Makes something fade, grow and float up into place when it first appears.
 * [index] staggers a list so items arrive one after another.
 */
@Composable
fun Modifier.popIn(index: Int = 0): Modifier {
    if (!LocalMotion.current) return this
    val p = remember { Animatable(0f) }
    LaunchedEffect(Unit) {
        delay(min(index, 6) * 50L)
        p.animateTo(1f, spring(dampingRatio = 0.7f, stiffness = 300f))
    }
    return graphicsLayer {
        val v = p.value
        alpha = v.coerceIn(0f, 1f)
        val s = 0.88f + 0.12f * v
        scaleX = s
        scaleY = s
        translationY = (1f - v) * 24.dp.toPx()
    }
}
