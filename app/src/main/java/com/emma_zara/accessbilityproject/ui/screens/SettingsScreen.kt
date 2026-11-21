package com.emma_zara.accessbilityproject.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Slider
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.emma_zara.accessbilityproject.R
import com.emma_zara.accessbilityproject.settings.AppSettings
import com.emma_zara.accessbilityproject.settings.AppTheme
import com.emma_zara.accessbilityproject.settings.DEFAULT_SPLASH_DIFFERENCE
import com.emma_zara.accessbilityproject.settings.MAX_SPLASH_DIFFERENCE
import com.emma_zara.accessbilityproject.settings.MIN_SPLASH_DIFFERENCE
import com.emma_zara.accessbilityproject.settings.TextCasing
import com.emma_zara.accessbilityproject.ui.components.AText
import com.emma_zara.accessbilityproject.ui.components.ScreenTitle
import com.emma_zara.accessbilityproject.ui.components.SectionCard
import com.emma_zara.accessbilityproject.ui.theme.systemAnimationsOn
import kotlin.math.roundToInt

private const val MIN_SCALE = 0.8f
private const val MAX_SCALE = 2f

@Composable
fun SettingsScreen(settings: AppSettings, onChange: (AppSettings) -> Unit) {
    fun setScale(s: Float) {
        // Round to steps of 10% so the number is easy to read
        val rounded = ((s * 10).roundToInt() / 10f).coerceIn(MIN_SCALE, MAX_SCALE)
        if (rounded != settings.fontScale) onChange(settings.copy(fontScale = rounded))
    }

    Column(
        Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        ScreenTitle("Settings", "Make the app easier to see and read.")

        // Live preview: changes as soon as a setting is changed
        SectionCard(title = "Preview") {
            AText(
                "Navy blue goes well with white and camel.",
                style = MaterialTheme.typography.bodyLarge,
            )
            AText(
                "This is how text will look everywhere in the app.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }

        SectionCard(title = "Theme") {
            Column(Modifier.selectableGroup(), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                AppTheme.entries.forEach { theme ->
                    ThemeOption(theme, selected = settings.theme == theme) { onChange(settings.copy(theme = theme)) }
                }
            }
        }

        SectionCard(title = "Text") {
            // Text size: A- / A+ buttons for easy tapping, and a slider for quick changes
            AText("Size", style = MaterialTheme.typography.titleMedium)
            Row(
                Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                OutlinedButton(
                    onClick = { setScale(settings.fontScale - 0.1f) },
                    enabled = settings.fontScale > MIN_SCALE,
                    modifier = Modifier.semantics { contentDescription = "Smaller text" },
                ) { Text("A−", fontSize = 16.sp, fontWeight = FontWeight.Bold) }
                AText(
                    "${(settings.fontScale * 100).roundToInt()}%",
                    style = MaterialTheme.typography.titleLarge,
                    modifier = Modifier.weight(1f),
                    textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                )
                OutlinedButton(
                    onClick = { setScale(settings.fontScale + 0.1f) },
                    enabled = settings.fontScale < MAX_SCALE,
                    modifier = Modifier.semantics { contentDescription = "Bigger text" },
                ) { Text("A+", fontSize = 22.sp, fontWeight = FontWeight.Bold) }
            }
            Slider(
                value = settings.fontScale,
                onValueChange = { setScale(it) },
                valueRange = MIN_SCALE..MAX_SCALE,
                modifier = Modifier.semantics {
                    contentDescription = "Text size"
                    stateDescription = "${(settings.fontScale * 100).roundToInt()} percent"
                },
            )

            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)

            Row(
                Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .toggleable(
                        value = settings.bold,
                        role = Role.Switch,
                        onValueChange = { onChange(settings.copy(bold = it)) },
                    )
                    .padding(vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column(Modifier.weight(1f)) {
                    AText("Bold text", style = MaterialTheme.typography.titleMedium)
                    AText(
                        "Thicker letters are easier to read",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                Spacer(Modifier.width(12.dp))
                Switch(checked = settings.bold, onCheckedChange = null)
            }

            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)

            AText("Letter casing", style = MaterialTheme.typography.titleMedium)
            Column(Modifier.selectableGroup()) {
                TextCasing.entries.forEach { casing ->
                    Row(
                        Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .selectable(
                                selected = settings.casing == casing,
                                role = Role.RadioButton,
                                onClick = { onChange(settings.copy(casing = casing)) },
                            )
                            .padding(vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        RadioButton(selected = settings.casing == casing, onClick = null)
                        Spacer(Modifier.width(12.dp))
                        // Each option is shown in its own casing so you can see what it does
                        Text(casing.label, style = MaterialTheme.typography.bodyLarge)
                    }
                }
            }
        }

        SectionCard(title = "Motion") {
            val systemOn = systemAnimationsOn()
            Row(
                Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .toggleable(
                        value = settings.animations,
                        role = Role.Switch,
                        onValueChange = { onChange(settings.copy(animations = it)) },
                    )
                    .padding(vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column(Modifier.weight(1f)) {
                    AText("Animations", style = MaterialTheme.typography.titleMedium)
                    AText(
                        "Paint splashes, the spinning camera ring and sliding pages. " +
                            "Turn off if moving things bother you.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                Spacer(Modifier.width(12.dp))
                Switch(checked = settings.animations, onCheckedChange = null)
            }
            if (!systemOn) {
                AText(
                    "Animations are off because \"Remove animations\" is on in your phone's settings.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            if (settings.animations && systemOn) {
                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                val value = settings.splashDifference
                AText("When to play the paint splash", style = MaterialTheme.typography.titleMedium)
                AText(
                    "How different a new color must be before the paint splash plays. " +
                        "Lower numbers play it more often.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                val meaning = when {
                    value <= 15 -> "even small changes, like red to dark red"
                    value <= 40 -> "clear changes, like red to orange"
                    else -> "only big changes, like red to blue"
                }
                AText("$value: plays for $meaning", style = MaterialTheme.typography.bodyLarge)
                Slider(
                    value = value.toFloat(),
                    onValueChange = { onChange(settings.copy(splashDifference = (it / 5f).roundToInt() * 5)) },
                    valueRange = MIN_SPLASH_DIFFERENCE.toFloat()..MAX_SPLASH_DIFFERENCE.toFloat(),
                    steps = (MAX_SPLASH_DIFFERENCE - MIN_SPLASH_DIFFERENCE) / 5 - 1,
                    modifier = Modifier.semantics {
                        contentDescription = "When to play the paint splash"
                        stateDescription = "$value, plays for $meaning"
                    },
                )
                if (value != DEFAULT_SPLASH_DIFFERENCE) {
                    OutlinedButton(onClick = { onChange(settings.copy(splashDifference = DEFAULT_SPLASH_DIFFERENCE)) }) {
                        AText("Reset to $DEFAULT_SPLASH_DIFFERENCE")
                    }
                }
            }
        }
    }
}

/** One theme choice, with a small sample showing that theme's colors. */
@Composable
private fun ThemeOption(theme: AppTheme, selected: Boolean, onClick: () -> Unit) {
    val (bg, fg) = when (theme) {
        AppTheme.LIGHT -> Color(0xFFFFFFFF) to Color(0xFF1A1A1A)
        AppTheme.DARK -> Color(0xFF1C1C1C) to Color(0xFFECECEC)
        AppTheme.HIGH_CONTRAST -> Color.Black to Color(0xFFFFFF00)
    }
    val borderColor = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant

    Surface(
        shape = RoundedCornerShape(16.dp),
        border = BorderStroke(if (selected) 3.dp else 1.dp, borderColor),
        color = MaterialTheme.colorScheme.surfaceContainer,
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .selectable(selected = selected, role = Role.RadioButton, onClick = onClick),
    ) {
        Row(Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
            Box(
                Modifier
                    .size(56.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(bg)
                    .border(1.dp, MaterialTheme.colorScheme.outline, RoundedCornerShape(12.dp))
                    .clearAndSetSemantics {},
                contentAlignment = Alignment.Center,
            ) {
                Text("Aa", color = fg, fontSize = 22.sp, fontWeight = FontWeight.Bold)
            }
            Spacer(Modifier.width(16.dp))
            AText(theme.label, style = MaterialTheme.typography.titleMedium, modifier = Modifier.weight(1f))
            if (selected) {
                Icon(
                    painterResource(R.drawable.ic_check),
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(28.dp),
                )
            }
        }
    }
}
