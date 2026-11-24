package com.emma_zara.accessbilityproject.closet

import com.emma_zara.accessbilityproject.color.toLab
import com.emma_zara.accessbilityproject.weather.TempFeel
import com.emma_zara.accessbilityproject.weather.Weather

/**
 * Can this piece be worn for [occasion]? Pieces with no occasions chosen work for anything,
 * except home-only pieces (like pajamas), which are only for Home.
 */
fun ClothingItem.okFor(occasion: Occasion?): Boolean = when {
    homeOnly -> occasion == Occasion.HOME
    occasion == null || occasions.isEmpty() -> true
    else -> occasion in occasions
}

/** Can this piece be worn in this weather? Warm pieces are too hot on hot days, light ones too cold on cold days. */
fun ClothingItem.okIn(weather: Weather?): Boolean = when (weather?.feel) {
    null, TempFeel.MILD -> true
    TempFeel.HOT -> warmth != Warmth.WARM
    TempFeel.COOL -> true
    TempFeel.COLD, TempFeel.FREEZING -> warmth != Warmth.LIGHT
}

/** The weather a piece is best for, or null if it fits anything. */
private fun Weather.wants(): Warmth? = when (feel) {
    TempFeel.HOT -> Warmth.LIGHT
    TempFeel.COLD, TempFeel.FREEZING -> Warmth.WARM
    else -> null
}

/**
 * Extra points for how well the outfit fits where you're going and the weather.
 * Pieces chosen for the occasion or weather beat ones that just "work for anything".
 */
private fun fitBonus(outfit: Outfit, occasion: Occasion?, weather: Weather?): Double {
    var bonus = 0.0
    val clothes = outfit.items.filter { it.category != Category.JEWELLERY }
    if (occasion != null) {
        bonus += 0.25 * outfit.items.count { occasion in it.occasions }
        if (occasion == Occasion.FORMAL) {
            // Dressy: plain fabric and calm colors
            bonus += 0.1 * clothes.count { !it.pattern.busy && it.patternName == null }
            bonus += 0.1 * clothes.count { isFashionNeutral(it.color) }
        }
    }
    if (weather != null) {
        val wanted = weather.wants()
        if (wanted != null) bonus += 0.25 * outfit.items.count { it.warmth == wanted }
        // Light colors reflect sunlight (cooler), dark colors soak it up (warmer)
        if (clothes.isNotEmpty()) {
            val avgLightness = clothes.map { it.color.toLab().l }.average() / 100.0
            when (weather.feel) {
                TempFeel.HOT -> bonus += 0.3 * avgLightness
                TempFeel.COLD, TempFeel.FREEZING -> bonus += 0.3 * (1 - avgLightness)
                else -> Unit
            }
        }
    }
    return bonus
}

/**
 * Outfits for where you're going ([occasion], or any if null) and today's [weather] (or any if null).
 * Pieces that don't fit are left out; the rest are ranked by color match plus how well they fit.
 */
fun recommendOutfits(
    closet: List<ClothingItem>,
    include: Set<Category>,
    occasion: Occasion?,
    weather: Weather?,
    limit: Int = 6,
): List<Outfit> {
    val pool = closet.filter { it.okFor(occasion) && it.okIn(weather) }
    // No jewellery for sport (it can get lost or hurt you)
    val categories = if (occasion == Occasion.SPORT) include - Category.JEWELLERY else include
    return suggestOutfits(pool, categories, limit = limit * 3)
        .map { it.copy(score = it.score + fitBonus(it, occasion, weather)) }
        .sortedByDescending { it.score }
        .take(limit)
}

/** Why the outfit fits the occasion and the weather, in plain words (added before the color reasons). */
fun Outfit.fitReasons(occasion: Occasion?, weather: Weather?): List<String> = buildList {
    if (occasion != null) {
        val chosen = items.filter { occasion in it.occasions }
        if (chosen.isNotEmpty()) {
            add("Marked for ${occasion.label.lowercase()}: ${chosen.joinToString { it.name }}.")
        }
    }
    if (weather != null) {
        val wanted = weather.wants()
        val fitting = items.filter { wanted != null && it.warmth == wanted }
        if (fitting.isNotEmpty() && wanted != null) {
            add("Good for ${wanted.label.lowercase()}: ${fitting.joinToString { it.name }}.")
        }
        val clothes = items.filter { it.category != Category.JEWELLERY }
        if (clothes.isNotEmpty()) {
            val avg = clothes.map { it.color.toLab().l }.average()
            if (weather.feel == TempFeel.HOT && avg >= 60) add("Mostly light colors, which stay cooler in the sun.")
            if (weather.feel <= TempFeel.COLD && avg <= 45) add("Mostly dark colors, which soak up the sun's warmth.")
        }
    }
}
