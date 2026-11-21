package com.emma_zara.accessbilityproject.closet

import com.emma_zara.accessbilityproject.color.RgbColor
import com.emma_zara.accessbilityproject.color.toHsv
import com.emma_zara.accessbilityproject.color.toLab
import kotlin.math.abs
import kotlin.math.min

/** Why two colors look good together. Listed from "safest" to "boldest". */
enum class MatchType(val label: String, val reason: String) {
    NEUTRAL("Neutral", "Neutral colors go with almost everything."),
    SAME_COLOR("Same color family", "Different shades of one color look calm and put together."),
    ANALOGOUS("Neighbors", "Colors next to each other on the wheel blend nicely."),
    COMPLEMENTARY("Opposites", "Opposite colors make each other stand out."),
    TRIADIC("Triangle", "Colors evenly spaced on the wheel look playful and balanced."),
}

/**
 * True for colors that work like neutrals in fashion:
 * grays, black, white, beige/tan/camel/khaki, and navy.
 */
fun isFashionNeutral(color: RgbColor): Boolean {
    val hsv = color.toHsv()
    if (hsv.s < 0.15f || hsv.v < 0.15f) return true                  // gray, black, white
    if (hsv.h in 20f..50f && hsv.s < 0.45f) return true               // beige, tan, camel, khaki, taupe
    if (hsv.h in 200f..250f && hsv.v < 0.45f) return true             // navy, dark denim
    return false
}

/** Shortest distance between two hues around the color wheel (0..180 degrees). */
fun hueDistance(a: Float, b: Float): Float {
    val d = abs(a - b) % 360f
    return min(d, 360f - d)
}

/** Returns how two colors match, or null if they don't follow a color rule. */
fun matchType(a: RgbColor, b: RgbColor): MatchType? {
    if (isFashionNeutral(a) || isFashionNeutral(b)) return MatchType.NEUTRAL
    val d = hueDistance(a.toHsv().h, b.toHsv().h)
    return when {
        d <= 15f -> MatchType.SAME_COLOR
        d <= 45f -> MatchType.ANALOGOUS
        d >= 150f -> MatchType.COMPLEMENTARY
        d in 105f..135f -> MatchType.TRIADIC
        else -> null
    }
}

/** An item from the closet that matches another one, and why. */
data class Match(val item: ClothingItem, val type: MatchType)

/**
 * Two patterned pieces (like stripes with a print) can clash, so wear one pattern at a time.
 * Jewellery is small, so it doesn't count.
 */
fun patternsClash(a: ClothingItem, b: ClothingItem): Boolean =
    a.pattern.busy && b.pattern.busy && a.category != Category.JEWELLERY && b.category != Category.JEWELLERY

/** True if [a] and [b] can be worn together: their colors follow a rule and their patterns don't clash. */
fun goTogether(a: ClothingItem, b: ClothingItem): Boolean =
    matchType(a.color, b.color) != null && !patternsClash(a, b)

/**
 * Finds items in OTHER categories that go with [item], best matches first.
 * Only items in the [include] categories are suggested.
 */
fun findMatches(
    item: ClothingItem,
    closet: List<ClothingItem>,
    include: Set<Category> = Category.entries.toSet(),
): List<Match> =
    closet
        .filter { it.id != item.id && it.category != item.category && it.category in include && !patternsClash(item, it) }
        .mapNotNull { other -> matchType(item.color, other.color)?.let { Match(other, it) } }
        .sortedWith(compareBy({ it.type.ordinal }, { it.item.category.ordinal }))

enum class SortOrder(val label: String) { NEWEST("Newest"), COLOR("Color"), CATEGORY("Category") }

fun sortItems(items: List<ClothingItem>, order: SortOrder): List<ClothingItem> = when (order) {
    SortOrder.NEWEST -> items.sortedByDescending { it.addedAt }
    SortOrder.CATEGORY -> items.sortedWith(compareBy({ it.category.ordinal }, { it.name }))
    // Rainbow order (by hue), with grays/black/white at the end from light to dark
    SortOrder.COLOR -> items.sortedWith(
        compareBy(
            { it.color.toHsv().s < 0.15f || it.color.toHsv().v < 0.15f },
            // +15 so reds just below 360 degrees (like 356) sort together with reds at 0
            { (((it.color.toHsv().h + 15f) % 360f) / 15f).toInt() },
            { -it.color.toLab().l },
        ),
    )
}
