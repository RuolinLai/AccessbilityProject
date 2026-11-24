package com.emma_zara.accessbilityproject

import com.emma_zara.accessbilityproject.closet.Category
import com.emma_zara.accessbilityproject.closet.ClothingItem
import com.emma_zara.accessbilityproject.closet.Occasion
import com.emma_zara.accessbilityproject.closet.Warmth
import com.emma_zara.accessbilityproject.closet.canWearTogether
import com.emma_zara.accessbilityproject.closet.findMatches
import com.emma_zara.accessbilityproject.closet.homeOnly
import com.emma_zara.accessbilityproject.closet.fitReasons
import com.emma_zara.accessbilityproject.closet.recommendOutfits
import com.emma_zara.accessbilityproject.color.RgbColor
import com.emma_zara.accessbilityproject.weather.TempFeel
import com.emma_zara.accessbilityproject.weather.Weather
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class RecommendTest {
    private fun item(
        id: String,
        category: Category,
        color: RgbColor,
        occasions: Set<Occasion> = emptySet(),
        warmth: Warmth? = null,
    ) = ClothingItem(id, id, category, color, "$id.jpg", 0, occasions = occasions, warmth = warmth)

    private fun weather(feelsLike: Double, code: Int = 0, wind: Double = 5.0) =
        Weather(feelsLike, feelsLike, code, 0.0, wind, isDay = true)

    private val white = RgbColor(245, 245, 245)
    private val black = RgbColor(15, 15, 15)
    private val navy = RgbColor(20, 30, 90)
    private val all = Category.entries.toSet()

    // ---- Weather ----

    @Test
    fun feelsLikeDecidesHowItFeels() {
        assertEquals(TempFeel.FREEZING, weather(-5.0).feel)
        assertEquals(TempFeel.COLD, weather(5.0).feel)
        assertEquals(TempFeel.COOL, weather(15.0).feel)
        assertEquals(TempFeel.MILD, weather(20.0).feel)
        assertEquals(TempFeel.HOT, weather(30.0).feel)
    }

    @Test
    fun rainAndSnowCodes() {
        assertTrue(weather(10.0, code = 61).rainy)
        assertTrue(weather(-2.0, code = 73).snowy)
        assertFalse(weather(-2.0, code = 73).rainy)
        assertFalse(weather(20.0, code = 2).rainy)
    }

    @Test
    fun tipsMentionUmbrellaInRainAndWindWhenWindy() {
        val tips = weather(12.0, code = 63, wind = 40.0).tips()
        assertTrue(tips.any { "umbrella" in it })
        assertTrue(tips.any { it.startsWith("Windy") })
    }

    @Test
    fun summaryShowsFeelsLikeOnlyWhenDifferent() {
        assertEquals("20°C. Clear sky.", weather(20.0).summary())
        assertEquals("3°C, feels like -1°C. Light rain.", Weather(3.0, -1.0, 61, 0.5, 10.0, true).summary())
    }

    // ---- Recommendations ----

    @Test
    fun formalLeavesOutSportOnlyPieces() {
        val shirt = item("shirt", Category.SHIRTS, white, setOf(Occasion.FORMAL))
        val dressPants = item("dressPants", Category.PANTS, black, setOf(Occasion.FORMAL))
        val joggers = item("joggers", Category.PANTS, navy, setOf(Occasion.SPORT))
        val outfits = recommendOutfits(listOf(shirt, dressPants, joggers), all, Occasion.FORMAL, null)
        assertTrue(outfits.isNotEmpty())
        assertTrue(outfits.none { o -> o.items.any { it.id == "joggers" } })
    }

    @Test
    fun untaggedPiecesWorkForAnyOccasion() {
        val shirt = item("shirt", Category.SHIRTS, white)
        val pants = item("pants", Category.PANTS, black)
        assertEquals(1, recommendOutfits(listOf(shirt, pants), all, Occasion.SCHOOL, null).size)
    }

    @Test
    fun hotDayLeavesOutWarmPieces() {
        val tee = item("tee", Category.SHIRTS, white, warmth = Warmth.LIGHT)
        val sweater = item("sweater", Category.SHIRTS, navy, warmth = Warmth.WARM)
        val pants = item("pants", Category.PANTS, black)
        val outfits = recommendOutfits(listOf(tee, sweater, pants), all, null, weather(30.0))
        assertTrue(outfits.isNotEmpty())
        assertTrue(outfits.none { o -> o.items.any { it.id == "sweater" } })
    }

    @Test
    fun coldDayPrefersWarmPieces() {
        val tee = item("tee", Category.SHIRTS, white)
        val sweater = item("sweater", Category.SHIRTS, navy, warmth = Warmth.WARM)
        val pants = item("pants", Category.PANTS, black)
        val best = recommendOutfits(listOf(tee, sweater, pants), all, null, weather(2.0)).first()
        assertTrue(best.items.any { it.id == "sweater" })
        assertTrue(best.fitReasons(null, weather(2.0)).any { "cold days" in it })
    }

    // ---- What can be worn together ----

    @Test
    fun dressIsNotWornWithTopsOrBottoms() {
        val dress = item("dress", Category.DRESSES, black)
        val shirt = item("shirt", Category.SHIRTS, white)
        val skirt = item("skirt", Category.SKIRTS, white)
        val shoes = item("shoes", Category.SHOES, white)
        val matches = findMatches(dress, listOf(shirt, skirt, shoes)).map { it.item.id }
        assertEquals(listOf("shoes"), matches)
    }

    @Test
    fun pantsAndSkirtAreBothBottoms() {
        assertFalse(canWearTogether(Category.PANTS, Category.SKIRTS))
        assertTrue(canWearTogether(Category.SHIRTS, Category.SKIRTS))
    }

    @Test
    fun pajamasByNameOnlyGoWithHomeClothes() {
        val pajamas = ClothingItem("pj", "Stone pajama", Category.PANTS, black, "pj.jpg", 0, typeName = "pajama")
        val shirt = item("shirt", Category.SHIRTS, white)
        val sleepShirt = item("sleepShirt", Category.SHIRTS, white, setOf(Occasion.HOME))
        assertTrue(pajamas.homeOnly)
        assertEquals(listOf("sleepShirt"), findMatches(pajamas, listOf(shirt, sleepShirt)).map { it.item.id })
        assertTrue(findMatches(shirt, listOf(pajamas)).isEmpty())
        // Not in outfits for going out, only for Home
        assertTrue(recommendOutfits(listOf(pajamas, shirt), all, null, null).isEmpty())
        assertEquals(1, recommendOutfits(listOf(pajamas, sleepShirt), all, Occasion.HOME, null).size)
    }

    @Test
    fun piecesForDifferentOccasionsDontMix() {
        val joggers = item("joggers", Category.PANTS, black, setOf(Occasion.SPORT))
        val blouse = item("blouse", Category.SHIRTS, white, setOf(Occasion.FORMAL))
        assertTrue(findMatches(blouse, listOf(joggers)).isEmpty())
    }

    @Test
    fun noJewelleryForSport() {
        val shirt = item("shirt", Category.SHIRTS, white)
        val pants = item("pants", Category.PANTS, black)
        val necklace = item("necklace", Category.JEWELLERY, black)
        val outfits = recommendOutfits(listOf(shirt, pants, necklace), all, Occasion.SPORT, null)
        assertTrue(outfits.isNotEmpty())
        assertTrue(outfits.none { o -> o.items.any { it.category == Category.JEWELLERY } })
    }
}
