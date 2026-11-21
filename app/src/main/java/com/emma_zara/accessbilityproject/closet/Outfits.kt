package com.emma_zara.accessbilityproject.closet

import com.emma_zara.accessbilityproject.closet.Category.BAGS
import com.emma_zara.accessbilityproject.closet.Category.DRESSES
import com.emma_zara.accessbilityproject.closet.Category.JEWELLERY
import com.emma_zara.accessbilityproject.closet.Category.PANTS
import com.emma_zara.accessbilityproject.closet.Category.SHIRTS
import com.emma_zara.accessbilityproject.closet.Category.SHOES
import com.emma_zara.accessbilityproject.closet.Category.SKIRTS
import com.emma_zara.accessbilityproject.color.ColorNames
import com.emma_zara.accessbilityproject.color.ColorVision
import com.emma_zara.accessbilityproject.color.looksAlike

/** A whole outfit: a top and a bottom (or a dress), plus shoes, a bag and jewellery when they fit. */
data class Outfit(val items: List<ClothingItem>, val score: Double)

/** The ways to make the main part of an outfit. */
private val Bases = listOf(listOf(SHIRTS, PANTS), listOf(SHIRTS, SKIRTS), listOf(DRESSES))
/** Added on top when something fits. */
private val Extras = listOf(SHOES, BAGS, JEWELLERY)
private val BaseCategories = Bases.flatten().toSet()

/** Most partial outfits kept at each step, so big closets stay fast. */
private const val BEAM = 60

/** How good each kind of color match is. Neutrals are safe but a bit plain. */
private fun pairScore(type: MatchType): Double = when (type) {
    MatchType.NEUTRAL -> 1.0
    MatchType.SAME_COLOR -> 1.5
    MatchType.ANALOGOUS -> 1.6
    MatchType.COMPLEMENTARY -> 1.4
    MatchType.TRIADIC -> 1.1
}

/** Pieces that sit next to each other on the body, where low contrast matters. Jewellery is too small. */
private fun bigPieces(a: ClothingItem, b: ClothingItem) = a.category != JEWELLERY && b.category != JEWELLERY

/**
 * Average of every pair in the outfit, plus a little extra for each piece so fuller outfits win.
 * Pairs that are equally light or dark score a bit lower, since they can blur together.
 */
private fun score(items: List<ClothingItem>): Double {
    if (items.size < 2) return 0.0
    var total = 0.0
    var pairs = 0
    for (i in items.indices) for (j in i + 1 until items.size) {
        val a = items[i]
        val b = items[j]
        val pair = matchItems(a, b)?.let(::pairScore) ?: 0.0
        total += if (bigPieces(a, b) && lowContrast(a.color, b.color)) pair * 0.85 else pair
        pairs++
    }
    return total / pairs + 0.12 * items.size
}

/**
 * Puts together whole outfits from the closet where every piece goes with every other piece.
 *
 * @param include only use these categories (what the user chose to get suggestions for)
 * @param anchor if given, every outfit is built around this item (it can be a new item that isn't in the closet yet)
 */
fun suggestOutfits(
    closet: List<ClothingItem>,
    include: Set<Category>,
    anchor: ClothingItem? = null,
    limit: Int = 8,
): List<Outfit> {
    val allowed = include + listOfNotNull(anchor?.category)
    val pool = closet.filter { it.id != anchor?.id }
    val found = mutableListOf<Outfit>()

    for (base in Bases) {
        if (!base.all { it in allowed }) continue
        // A dress can't go in a shirt + pants outfit, and so on
        if (anchor != null && anchor.category in BaseCategories && anchor.category !in base) continue

        // Fill the outfit one category at a time. Base pieces are required; extras only if one fits.
        var partial = listOf(emptyList<ClothingItem>())
        for (category in base + Extras.filter { it in allowed }) {
            val isAnchorSlot = anchor != null && category == anchor.category
            val required = category in base || isAnchorSlot
            val options = if (anchor != null && isAnchorSlot) listOf(anchor) else pool.filter { it.category == category }
            val next = mutableListOf<List<ClothingItem>>()
            for (outfit in partial) {
                val fits = options.filter { option -> outfit.all { goTogether(option, it) } }
                fits.forEach { next += outfit + it }
                if (fits.isEmpty() && !required) next += outfit
            }
            partial = next.sortedByDescending(::score).take(BEAM)
            if (partial.isEmpty()) break
        }
        partial.filter { it.size >= 2 }.forEach { found += Outfit(it, score(it)) }
    }

    // Best first, but don't show the same top + bottom twice, and don't use one piece in too many outfits
    val chosen = mutableListOf<Outfit>()
    val usedBases = mutableSetOf<Set<String>>()
    val timesUsed = mutableMapOf<String, Int>()
    for (outfit in found.sortedByDescending { it.score }) {
        if (chosen.size >= limit) break
        val baseIds = outfit.items.filter { it.category in BaseCategories }.map { it.id }.toSet()
        if (baseIds in usedBases) continue
        if (outfit.items.any { it.id != anchor?.id && (timesUsed[it.id] ?: 0) >= 3 }) continue
        chosen += outfit
        usedBases += baseIds
        outfit.items.forEach { timesUsed[it.id] = (timesUsed[it.id] ?: 0) + 1 }
    }
    return chosen
}

/**
 * Plain-language reasons why the outfit works, for reading (or listening to) instead of judging colors by eye.
 * Also gives tips: pieces that may blur together, and (with [vision]) pieces that may look alike to you.
 */
fun Outfit.reasons(vision: ColorVision = ColorVision.TYPICAL): List<String> {
    val lines = mutableListOf<String>()
    // The most interesting color pair of each kind (skip neutral pairs if there's anything else)
    val pairs = buildList {
        for (i in items.indices) for (j in i + 1 until items.size) {
            matchItems(items[i], items[j])?.let { add(Triple(it, items[i], items[j])) }
        }
    }
    val colorful = pairs.filter { it.first != MatchType.NEUTRAL }.distinctBy { it.first }.sortedBy { it.first.ordinal }
    if (colorful.isEmpty()) {
        lines += "All neutral colors: calm and easy to wear."
    } else {
        colorful.forEach { (type, a, b) -> lines += "${a.name} and ${b.name}: ${type.reason}" }
        if (pairs.any { it.first == MatchType.NEUTRAL }) lines += "The neutral pieces tie it all together."
    }
    items.firstOrNull { it.pattern.busy && it.category != JEWELLERY }?.let {
        lines += "Only one patterned piece (${it.patternLabel.lowercase()}), so nothing clashes."
    }
    items.firstOrNull { it.extraColors.isNotEmpty() }?.let {
        lines += "The other colors in ${it.name} (${it.extraColors.joinToString { c -> ColorNames.nearest(c).name }}) " +
            "go with the rest too."
    }
    pairs.firstOrNull { (_, a, b) -> bigPieces(a, b) && lowContrast(a.color, b.color) }?.let { (_, a, b) ->
        lines += "Tip: ${a.name} and ${b.name} are about as light or dark as each other, so they may blur together. " +
            "A lighter or darker one would stand out more."
    }
    val allPairs = items.indices.flatMap { i -> (i + 1 until items.size).map { j -> items[i] to items[j] } }
    allPairs.firstOrNull { (a, b) -> looksAlike(a.color, b.color, vision) }?.let { (a, b) ->
        lines += "Heads up: ${a.name} (${ColorNames.nearest(a.color).name}) and ${b.name} " +
            "(${ColorNames.nearest(b.color).name}) may look alike to you, but they are different colors."
    }
    return lines
}
