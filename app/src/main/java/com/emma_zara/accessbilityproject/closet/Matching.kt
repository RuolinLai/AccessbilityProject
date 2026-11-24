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

/**
 * True if two colors are about equally light or dark, so next to each other they can blur together
 * (hard to see where one piece ends and the other begins, especially with low vision).
 */
fun lowContrast(a: RgbColor, b: RgbColor): Boolean = abs(a.toLab().l - b.toLab().l) < LOW_CONTRAST_LIGHTNESS

/** Lightness difference (Lab L, 0..100) below which two colors count as low contrast. */
const val LOW_CONTRAST_LIGHTNESS = 12.0

/**
 * How two items match by color, or null if they don't.
 * The main colors must follow a color rule, and the other colors on each item (like the white in
 * navy-and-white stripes) must also go with the other item's main color.
 */
fun matchItems(a: ClothingItem, b: ClothingItem): MatchType? {
    val main = matchType(a.color, b.color) ?: return null
    if (a.extraColors.any { matchType(it, b.color) == null }) return null
    if (b.extraColors.any { matchType(it, a.color) == null }) return null
    return main
}

/** An item from the closet that matches another one, and why. [lowContrast]: about as light/dark as the other. */
data class Match(val item: ClothingItem, val type: MatchType, val lowContrast: Boolean = false)

/**
 * Two patterned pieces (like stripes with a print) can clash, so wear one pattern at a time.
 * Jewellery is small, so it doesn't count.
 */
fun patternsClash(a: ClothingItem, b: ClothingItem): Boolean =
    a.pattern.busy && b.pattern.busy && a.category != Category.JEWELLERY && b.category != Category.JEWELLERY

/**
 * True if [a] and [b] can be worn together: they go on different parts of the body, suit the same
 * occasions, their colors follow a rule and their patterns don't clash.
 */
fun goTogether(a: ClothingItem, b: ClothingItem): Boolean =
    canWearTogether(a.category, b.category) && sameOccasion(a, b) && matchItems(a, b) != null && !patternsClash(a, b)

/**
 * Finds items that can be worn with [item] (a dress isn't worn with a shirt, pajamas aren't worn out),
 * best matches first. Only items in the [include] categories are suggested.
 */
fun findMatches(
    item: ClothingItem,
    closet: List<ClothingItem>,
    include: Set<Category> = Category.entries.toSet(),
): List<Match> =
    closet
        .filter {
            it.id != item.id && it.category in include && canWearTogether(item.category, it.category) &&
                sameOccasion(item, it) && !patternsClash(item, it)
        }
        .mapNotNull { other -> matchItems(item, other)?.let { Match(other, it, lowContrast(item.color, other.color)) } }
        .sortedWith(compareBy({ it.type.ordinal }, { it.item.category.ordinal }))
