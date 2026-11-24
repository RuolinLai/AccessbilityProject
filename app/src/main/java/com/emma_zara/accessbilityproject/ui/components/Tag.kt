package com.emma_zara.accessbilityproject.ui.components

import androidx.annotation.DrawableRes
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.emma_zara.accessbilityproject.R
import com.emma_zara.accessbilityproject.closet.ClothingItem
import com.emma_zara.accessbilityproject.closet.Occasion
import com.emma_zara.accessbilityproject.closet.Warmth
import com.emma_zara.accessbilityproject.closet.homeGuessed
import com.emma_zara.accessbilityproject.closet.patternLabel
import com.emma_zara.accessbilityproject.closet.typeLabel
import com.emma_zara.accessbilityproject.color.ColorNames
import com.emma_zara.accessbilityproject.color.RgbColor
import com.emma_zara.accessbilityproject.ui.theme.LocalDarkTheme
import com.emma_zara.accessbilityproject.ui.theme.LocalGlass
import com.emma_zara.accessbilityproject.ui.theme.LocalHighContrast

/**
 * What a label is about. Each kind has its own color AND its own little icon, so the kinds can be
 * told apart without seeing colors (in high contrast all labels are yellow on black, and the icon does the job).
 */
enum class TagKind(
    val lightBg: Color, val lightFg: Color,
    val darkBg: Color, val darkFg: Color,
) {
    COLOR(Color(0xFFEDEDED), Color(0xFF262626), Color(0xFF333333), Color(0xFFEDEDED)),
    TYPE(Color(0xFFDCE6FF), Color(0xFF1C3A80), Color(0xFF233559), Color(0xFFCAD8FF)),
    PATTERN(Color(0xFFEDE0FF), Color(0xFF4A2A85), Color(0xFF3A2C5A), Color(0xFFE1D3FF)),
    OCCASION(Color(0xFFD9F2E1), Color(0xFF1E5A32), Color(0xFF1F3D2A), Color(0xFFBDE8CB)),
    WEATHER(Color(0xFFFFEAC2), Color(0xFF6B4300), Color(0xFF4A3510), Color(0xFFFFDDA0)),
    WISH(Color(0xFFFFDDE6), Color(0xFF7A1F3D), Color(0xFF4D1F2D), Color(0xFFFFC2D2)),
}

/**
 * A real label: a rounded colored pill with an icon (or a color dot) and a word.
 * [dot] shows a dot of that color instead of an icon (for color labels).
 */
@Composable
fun Tag(
    text: String,
    kind: TagKind,
    modifier: Modifier = Modifier,
    @DrawableRes icon: Int? = null,
    dot: RgbColor? = null,
    small: Boolean = false,
) {
    val highContrast = LocalHighContrast.current
    val dark = LocalDarkTheme.current
    val glass = LocalGlass.current
    val (bg, fg) = when {
        highContrast -> MaterialTheme.colorScheme.surface to MaterialTheme.colorScheme.onSurface
        dark -> kind.darkBg to kind.darkFg
        // In the glass theme the label is tinted glass: the same color, a little see-through
        glass -> kind.lightBg.copy(alpha = 0.8f) to kind.lightFg
        else -> kind.lightBg to kind.lightFg
    }
    val iconSize: Dp = if (small) 14.dp else 16.dp
    Surface(
        shape = RoundedCornerShape(50),
        color = bg,
        contentColor = fg,
        border = when {
            highContrast -> BorderStroke(1.5.dp, MaterialTheme.colorScheme.outline)
            glass -> BorderStroke(1.dp, Color.White.copy(alpha = 0.7f))
            else -> null
        },
        modifier = modifier,
    ) {
        Row(
            Modifier.padding(start = if (small) 7.dp else 9.dp, end = if (small) 9.dp else 12.dp, top = if (small) 3.dp else 5.dp, bottom = if (small) 3.dp else 5.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            if (dot != null) {
                Box(
                    Modifier
                        .size(iconSize)
                        .clip(CircleShape)
                        .background(dot.toComposeColor())
                        .border(1.dp, fg.copy(alpha = 0.35f), CircleShape),
                )
                Spacer(Modifier.width(5.dp))
            } else if (icon != null) {
                Icon(painterResource(icon), contentDescription = null, tint = fg, modifier = Modifier.size(iconSize))
                Spacer(Modifier.width(4.dp))
            }
            AText(text, style = if (small) MaterialTheme.typography.labelMedium else MaterialTheme.typography.labelLarge, color = fg, maxLines = 1)
        }
    }
}

fun Occasion.iconRes(): Int = when (this) {
    Occasion.SCHOOL -> R.drawable.ic_occasion_school
    Occasion.FORMAL -> R.drawable.ic_occasion_formal
    Occasion.SPORT -> R.drawable.ic_occasion_sport
    Occasion.HOME -> R.drawable.ic_occasion_home
}

fun Warmth.iconRes(): Int = when (this) {
    Warmth.LIGHT -> R.drawable.ic_weather_sun
    Warmth.WARM -> R.drawable.ic_weather_snow
}

/**
 * All the labels for one piece: its colors, type, pattern (if it has one), occasions, weather,
 * and "Wish list". [showPlain] also shows "Plain" for pieces with no pattern.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun ItemTags(item: ClothingItem, modifier: Modifier = Modifier, small: Boolean = false, showPlain: Boolean = false) {
    FlowRow(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(6.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp),
    ) {
        if (item.wished) Tag("Wish list", TagKind.WISH, icon = R.drawable.ic_heart, small = small)
        (listOf(item.color) + item.extraColors).distinctBy { ColorNames.nearest(it).name }.forEach { c ->
            Tag(ColorNames.nearest(c).name, TagKind.COLOR, dot = c, small = small)
        }
        Tag(item.typeLabel, TagKind.TYPE, icon = R.drawable.ic_closet, small = small)
        if (showPlain || item.pattern.busy || item.patternName != null) {
            Tag(item.patternLabel, TagKind.PATTERN, icon = R.drawable.ic_pattern, small = small)
        }
        item.occasions.sortedBy { it.ordinal }.forEach { Tag(it.label, TagKind.OCCASION, icon = it.iconRes(), small = small) }
        if (item.homeGuessed) Tag("Home", TagKind.OCCASION, icon = R.drawable.ic_occasion_home, small = small)
        item.warmth?.let { Tag(it.label, TagKind.WEATHER, icon = it.iconRes(), small = small) }
    }
}
