package com.emma_zara.accessbilityproject.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.snap
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.emma_zara.accessbilityproject.color.ColorAnalysis
import com.emma_zara.accessbilityproject.color.ColorNames
import com.emma_zara.accessbilityproject.color.RgbColor
import com.emma_zara.accessbilityproject.color.relativeLuminance
import com.emma_zara.accessbilityproject.ui.theme.LocalHighContrast
import com.emma_zara.accessbilityproject.ui.theme.LocalMotion
import kotlin.math.roundToInt

fun RgbColor.toComposeColor() = Color(toArgb())

/** Black or white, whichever is easier to read on top of this color. */
fun RgbColor.readableTextColor() = if (relativeLuminance() > 0.35) Color.Black else Color.White

/** A rounded square filled with a color, with a border so white/black colors are still visible. */
@Composable
fun Swatch(color: RgbColor, modifier: Modifier = Modifier, corner: Int = 16) {
    // Blend smoothly to a new color instead of jumping
    val shown by animateColorAsState(
        color.toComposeColor(),
        animationSpec = if (LocalMotion.current) tween(350) else snap(),
        label = "swatch",
    )
    Box(
        modifier
            .clip(RoundedCornerShape(corner.dp))
            .background(shown)
            .border(1.5.dp, MaterialTheme.colorScheme.outline, RoundedCornerShape(corner.dp)),
    )
}

/**
 * The main result, styled like a paint sample card: a block of the color with paint drips,
 * then the color name, its hex code, and the three key facts as pills.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun ColorHeroCard(analysis: ColorAnalysis, modifier: Modifier = Modifier, showDescription: Boolean = true) {
    val c = analysis.color
    val highContrast = LocalHighContrast.current
    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(24.dp),
        color = MaterialTheme.colorScheme.surfaceContainer,
        border = if (highContrast) BorderStroke(2.dp, MaterialTheme.colorScheme.outline) else null,
    ) {
        Column {
            PaintSplash(c, Modifier.fillMaxWidth())
            Column(
                Modifier.padding(start = 20.dp, end = 20.dp, bottom = 20.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                Column(
                    Modifier.semantics(mergeDescendants = true) {
                        contentDescription = "${analysis.name}, ${analysis.temperature.label}, " +
                            "${analysis.lightness.label}, ${analysis.saturation.label}. Hex ${c.hex}"
                    },
                ) {
                    AText(analysis.name, style = MaterialTheme.typography.headlineSmall)
                    AText(
                        c.hex,
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    Pill(analysis.temperature.label)
                    Pill(analysis.lightness.label)
                    Pill(analysis.saturation.label)
                }
                if (showDescription) {
                    AText(analysis.description, style = MaterialTheme.typography.bodyLarge)
                }
            }
        }
    }
}

/** Complementary, analogous and triadic colors as tappable tiles. */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun MatchingColorsCard(
    analysis: ColorAnalysis,
    modifier: Modifier = Modifier,
    title: String = "Goes well with",
    onColorClick: ((RgbColor) -> Unit)? = null,
) {
    SectionCard(modifier, title = title) {
        HarmonyGroup("Opposite (complementary)", "Bold contrast", listOf(analysis.complementary), onColorClick)
        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
        HarmonyGroup("Neighbors (analogous)", "Calm and matching", analysis.analogous, onColorClick)
        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
        HarmonyGroup("Triangle (triadic)", "Playful and balanced", analysis.triadic, onColorClick)
        if (onColorClick != null) {
            AText(
                "Tap a color to look at it.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun HarmonyGroup(
    title: String,
    hint: String,
    colors: List<RgbColor>,
    onClick: ((RgbColor) -> Unit)?,
) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Column {
            AText(title, style = MaterialTheme.typography.titleMedium)
            AText(hint, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        FlowRow(
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            colors.forEachIndexed { i, color -> ColorTile(color, onClick, Modifier.popIn(i)) }
        }
    }
}

@Composable
private fun ColorTile(color: RgbColor, onClick: ((RgbColor) -> Unit)?, modifier: Modifier = Modifier) {
    val name = ColorNames.nearest(color).name
    Column(
        modifier
            .widthIn(min = 96.dp, max = 140.dp)
            .clip(RoundedCornerShape(16.dp))
            .then(
                if (onClick != null) {
                    Modifier.clickable(role = Role.Button, onClickLabel = "Look at $name") { onClick(color) }
                } else {
                    Modifier
                },
            )
            .padding(4.dp)
            .semantics(mergeDescendants = true) {},
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Swatch(color, Modifier.fillMaxWidth().height(64.dp))
        Spacer(Modifier.height(6.dp))
        AText(name, style = MaterialTheme.typography.labelLarge, textAlign = TextAlign.Center)
        AText(
            color.hex,
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

/** How the color is made: amounts of red, green, blue, and the codes in other color systems. */
@Composable
fun CompositionCard(analysis: ColorAnalysis, modifier: Modifier = Modifier) {
    val c = analysis.color
    SectionCard(modifier, title = "What it's made of") {
        ChannelBar("Red", c.r, Color(0xFFE53935))
        ChannelBar("Green", c.g, Color(0xFF43A047))
        ChannelBar("Blue", c.b, Color(0xFF1E88E5))
        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
        val h = analysis.hsv
        val k = analysis.cmyk
        CodeRow("HEX", c.hex)
        CodeRow("RGB", "${c.r}, ${c.g}, ${c.b}")
        CodeRow("HSV", "${h.h.roundToInt()}°, ${(h.s * 100).roundToInt()}%, ${(h.v * 100).roundToInt()}%")
        CodeRow("CMYK", listOf(k.c, k.m, k.y, k.k).joinToString(", ") { "${(it * 100).roundToInt()}%" })
    }
}

@Composable
private fun ChannelBar(label: String, value: Int, barColor: Color) {
    val fraction = value / 255f
    Column(
        Modifier.semantics(mergeDescendants = true) { contentDescription = "$label ${(fraction * 100).roundToInt()} percent" },
        verticalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            AText(label, style = MaterialTheme.typography.bodyLarge)
            AText("${(fraction * 100).roundToInt()}%", style = MaterialTheme.typography.titleMedium)
        }
        // Simple bar: gray track with a colored fill
        Box(
            Modifier
                .fillMaxWidth()
                .height(12.dp)
                .clip(RoundedCornerShape(6.dp))
                .background(MaterialTheme.colorScheme.surfaceContainerHighest)
                .border(1.dp, MaterialTheme.colorScheme.outlineVariant, RoundedCornerShape(6.dp)),
        ) {
            Box(
                Modifier
                    .fillMaxWidth(fraction)
                    .height(12.dp)
                    .clip(RoundedCornerShape(6.dp))
                    .background(barColor),
            )
        }
    }
}

@Composable
private fun CodeRow(label: String, value: String) {
    Row(
        Modifier
            .fillMaxWidth()
            .padding(vertical = 2.dp)
            .semantics(mergeDescendants = true) {},
        verticalAlignment = Alignment.CenterVertically,
    ) {
        AText(
            label,
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.widthIn(min = 72.dp).padding(end = 12.dp),
        )
        AText(value, style = MaterialTheme.typography.titleMedium, textAlign = TextAlign.End, modifier = Modifier.weight(1f))
    }
}
