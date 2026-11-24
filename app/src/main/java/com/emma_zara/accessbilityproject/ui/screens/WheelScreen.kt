package com.emma_zara.accessbilityproject.ui.screens

import com.emma_zara.accessbilityproject.ui.components.LocalBottomBarSpace
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.unit.dp
import com.emma_zara.accessbilityproject.color.Hsv
import com.emma_zara.accessbilityproject.color.RgbColor
import com.emma_zara.accessbilityproject.color.analyzeColor
import com.emma_zara.accessbilityproject.color.toHsv
import com.emma_zara.accessbilityproject.ui.components.AText
import com.emma_zara.accessbilityproject.ui.components.VibrateOnColorChange
import com.emma_zara.accessbilityproject.ui.components.ColorHeroCard
import com.emma_zara.accessbilityproject.ui.components.ColorWheel
import com.emma_zara.accessbilityproject.ui.components.CompositionCard
import com.emma_zara.accessbilityproject.ui.components.MatchingColorsCard
import com.emma_zara.accessbilityproject.ui.components.ScreenTitle
import com.emma_zara.accessbilityproject.ui.components.SectionCard
import kotlin.math.roundToInt

/**
 * Color wheel + sliders for the three parts of a color: hue, saturation and brightness (value).
 */
@Composable
fun WheelScreen(color: RgbColor, onColorChange: (RgbColor) -> Unit) {
    // We keep HSV here (not just RGB) so the hue isn't lost when saturation goes to 0.
    var hsv by remember { mutableStateOf(color.toHsv()) }

    fun update(newHsv: Hsv) {
        hsv = newHsv
        onColorChange(RgbColor.fromHsv(newHsv.h, newHsv.s, newHsv.v))
    }

    val analysis = analyzeColor(color)
    VibrateOnColorChange(analysis.name)

    Column(
        Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(bottom = LocalBottomBarSpace.current)
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        ScreenTitle("Color wheel", "Tap the wheel or move the sliders.")

        ColorHeroCard(analysis, showDescription = false)

        SectionCard {
            ColorWheel(
                hue = hsv.h,
                saturation = hsv.s,
                value = hsv.v,
                onChange = { h, s -> update(hsv.copy(h = h, s = s)) },
                modifier = Modifier
                    .fillMaxWidth()
                    .widthIn(max = 320.dp)
                    .padding(8.dp)
                    .align(Alignment.CenterHorizontally),
            )
        }

        SectionCard(title = "The 3 parts of a color") {
            GradientSlider(
                label = "Hue",
                hint = "Which color",
                valueText = "${hsv.h.roundToInt()}°",
                value = hsv.h,
                range = 0f..360f,
                gradient = (0..6).map { Color.hsv((it * 60f) % 360f, 1f, 1f) },
                onChange = { update(hsv.copy(h = it)) },
            )
            GradientSlider(
                label = "Saturation",
                hint = "How colorful",
                valueText = "${(hsv.s * 100).roundToInt()}%",
                value = hsv.s,
                range = 0f..1f,
                gradient = listOf(Color.hsv(hsv.h, 0f, hsv.v), Color.hsv(hsv.h, 1f, hsv.v)),
                onChange = { update(hsv.copy(s = it)) },
            )
            GradientSlider(
                label = "Brightness",
                hint = "How light",
                valueText = "${(hsv.v * 100).roundToInt()}%",
                value = hsv.v,
                range = 0f..1f,
                gradient = listOf(Color.Black, Color.hsv(hsv.h, hsv.s, 1f)),
                onChange = { update(hsv.copy(v = it)) },
            )
        }

        MatchingColorsCard(analysis, onColorClick = { update(it.toHsv()) })
        CompositionCard(analysis)
    }
}

/** A slider with a color bar above it showing what moving the slider will do. */
@Composable
private fun GradientSlider(
    label: String,
    hint: String,
    valueText: String,
    value: Float,
    range: ClosedFloatingPointRange<Float>,
    gradient: List<Color>,
    onChange: (Float) -> Unit,
) {
    Column {
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.Bottom) {
            Column(Modifier.weight(1f)) {
                AText(label, style = MaterialTheme.typography.titleMedium)
                AText(hint, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            AText(valueText, style = MaterialTheme.typography.titleLarge)
        }
        Box(
            Modifier
                .padding(top = 8.dp)
                .padding(horizontal = 10.dp)
                .fillMaxWidth()
                .height(14.dp)
                .clip(RoundedCornerShape(7.dp))
                .background(Brush.horizontalGradient(gradient))
                .border(1.dp, MaterialTheme.colorScheme.outlineVariant, RoundedCornerShape(7.dp))
                .clearAndSetSemantics {},
        )
        Slider(
            value = value,
            onValueChange = onChange,
            valueRange = range,
            modifier = Modifier.semantics {
                contentDescription = "$label, $hint"
                stateDescription = valueText
            },
        )
    }
}
