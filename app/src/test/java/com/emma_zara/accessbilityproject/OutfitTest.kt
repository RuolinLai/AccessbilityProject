package com.emma_zara.accessbilityproject

import com.emma_zara.accessbilityproject.closet.Category
import com.emma_zara.accessbilityproject.closet.ClothingItem
import com.emma_zara.accessbilityproject.closet.CustomPattern
import com.emma_zara.accessbilityproject.closet.summary
import com.emma_zara.accessbilityproject.closet.Pattern
import com.emma_zara.accessbilityproject.closet.detectPattern
import com.emma_zara.accessbilityproject.closet.findMatches
import com.emma_zara.accessbilityproject.closet.goTogether
import com.emma_zara.accessbilityproject.closet.reasons
import com.emma_zara.accessbilityproject.closet.suggestOutfits
import com.emma_zara.accessbilityproject.color.RgbColor
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.math.hypot
import kotlin.random.Random

class OutfitTest {
    private fun item(id: String, category: Category, color: RgbColor, pattern: Pattern = Pattern.SOLID) =
        ClothingItem(id, id, category, color, "$id.jpg", 0, pattern)

    private val white = RgbColor(245, 245, 245)
    private val navy = RgbColor(20, 30, 90)
    private val red = RgbColor(220, 20, 20)
    private val green = RgbColor(30, 200, 40)

    // ---- Patterns ----

    private val size = 96
    private fun image(light: (Int, Int) -> Boolean): IntArray = IntArray(size * size) { i ->
        if (light(i % size, i / size)) 0xFFE6E6E6.toInt() else 0xFF282828.toInt()
    }

    @Test
    fun plainFabricWithNoiseIsSolid() {
        val random = Random(1)
        val pixels = IntArray(size * size) {
            val v = 120 + random.nextInt(-4, 5)
            (0xFF shl 24) or (v shl 16) or (v shl 8) or v
        }
        assertEquals(Pattern.SOLID, detectPattern(pixels, size, size))
    }

    @Test
    fun findsStripesInAnyDirection() {
        assertEquals(Pattern.STRIPES, detectPattern(image { x, _ -> x / 6 % 2 == 0 }, size, size))       // vertical
        assertEquals(Pattern.STRIPES, detectPattern(image { _, y -> y / 6 % 2 == 0 }, size, size))       // horizontal
        assertEquals(Pattern.STRIPES, detectPattern(image { x, y -> (x + y) / 8 % 2 == 0 }, size, size)) // diagonal
    }

    @Test
    fun findsChecks() {
        assertEquals(Pattern.CHECKS, detectPattern(image { x, y -> (x / 8 + y / 8) % 2 == 0 }, size, size))
        // Gingham-style: thin lines crossing
        assertEquals(Pattern.CHECKS, detectPattern(image { x, y -> x % 12 < 3 || y % 12 < 3 }, size, size))
    }

    @Test
    fun findsDotsAsPrint() {
        val pixels = image { x, y -> hypot((x % 16 - 8).toDouble(), (y % 16 - 8).toDouble()) < 5 }
        assertEquals(Pattern.PRINT, detectPattern(pixels, size, size))
    }

    @Test
    fun ignoresTheItemOutline() {
        // A plain gray square on white: the only edge is the outline, which the mask removes
        val pixels = IntArray(size * size) { i ->
            if (i % size in 20..75 && i / size in 20..75) 0xFF787878.toInt() else 0xFFFFFFFF.toInt()
        }
        val mask = BooleanArray(size * size) { i -> i % size in 20..75 && i / size in 20..75 }
        assertEquals(Pattern.SOLID, detectPattern(pixels, size, size, mask))
    }

    // ---- Matching with patterns and chosen categories ----

    @Test
    fun twoPatternsDontGoTogether() {
        val stripedShirt = item("shirt", Category.SHIRTS, white, Pattern.STRIPES)
        val plaidSkirt = item("skirt", Category.SKIRTS, navy, Pattern.CHECKS)
        val plainPants = item("pants", Category.PANTS, navy)
        assertFalse(goTogether(stripedShirt, plaidSkirt))
        assertTrue(goTogether(stripedShirt, plainPants))
        // Jewellery doesn't count as a pattern clash
        assertTrue(goTogether(stripedShirt, item("ring", Category.JEWELLERY, white, Pattern.PRINT)))
    }

    @Test
    fun matchesOnlyFromChosenCategories() {
        val shirt = item("shirt", Category.SHIRTS, red)
        val closet = listOf(shirt, item("pants", Category.PANTS, navy), item("shoes", Category.SHOES, white))
        assertEquals(listOf("shoes"), findMatches(shirt, closet, setOf(Category.SHOES)).map { it.item.id })
    }

    // ---- Outfits ----

    private val closet = listOf(
        item("red shirt", Category.SHIRTS, red),
        item("green shirt", Category.SHIRTS, green),
        item("navy pants", Category.PANTS, navy),
        item("white shoes", Category.SHOES, white),
        item("red bag", Category.BAGS, red),
        item("green bag", Category.BAGS, green),
    )
    private val all = Category.entries.toSet()

    @Test
    fun outfitsHaveATopAndBottomAndEverythingMatches() {
        val outfits = suggestOutfits(closet, all)
        assertTrue(outfits.isNotEmpty())
        for (outfit in outfits) {
            val categories = outfit.items.map { it.category }
            assertTrue(Category.SHIRTS in categories && Category.PANTS in categories)
            for (a in outfit.items) for (b in outfit.items) if (a != b) assertTrue(goTogether(a, b))
            assertTrue(outfit.reasons().isNotEmpty())
        }
        // Red shirt + green bag are opposites, red shirt + red bag the same color: both fine.
        // But the best red-shirt outfit includes shoes and a bag too.
        val redOutfit = outfits.first { it.items.any { it.id == "red shirt" } }
        assertEquals(4, redOutfit.items.size)
    }

    @Test
    fun outfitsLeaveOutCategoriesNotChosen() {
        val outfits = suggestOutfits(closet, all - Category.BAGS)
        assertTrue(outfits.isNotEmpty())
        assertTrue(outfits.none { o -> o.items.any { it.category == Category.BAGS } })
    }

    @Test
    fun outfitsAroundANewItem() {
        // Shopping: a striped white skirt that is not in the closet yet
        val newSkirt = item("new", Category.SKIRTS, white, Pattern.STRIPES)
        val outfits = suggestOutfits(closet, all, anchor = newSkirt)
        assertTrue(outfits.isNotEmpty())
        assertTrue(outfits.all { o -> o.items.any { it.id == "new" } })
        assertTrue(outfits.none { o -> o.items.any { it.category == Category.PANTS } })
    }

    @Test
    fun noOutfitWithoutABottom() {
        val tops = listOf(item("a", Category.SHIRTS, red), item("b", Category.SHOES, white))
        assertTrue(suggestOutfits(tops, all).isEmpty())
    }

    // ---- The user's own types and patterns ----

    @Test
    fun ownTypeWorksLikeItsCategoryInOutfits() {
        // "Shorts" that work like pants can be the bottom of an outfit
        val shorts = item("shorts", Category.PANTS, navy).copy(typeName = "Shorts")
        val outfits = suggestOutfits(listOf(item("top", Category.SHIRTS, red), shorts), all)
        assertEquals(listOf("top", "shorts"), outfits.single().items.map { it.id })
        assertEquals("Navy · Shorts", shorts.summary())
    }

    @Test
    fun ownBusyPatternClashesButCalmOneDoesnt() {
        val floral = CustomPattern("Floral", busy = true)
        val ribbed = CustomPattern("Ribbed", busy = false)
        val floralSkirt = item("skirt", Category.SKIRTS, navy, floral.asPattern).copy(patternName = floral.name)
        val ribbedTop = item("top", Category.SHIRTS, white, ribbed.asPattern).copy(patternName = ribbed.name)
        val stripedTop = item("top2", Category.SHIRTS, white, Pattern.STRIPES)
        assertTrue(goTogether(floralSkirt, ribbedTop))
        assertFalse(goTogether(floralSkirt, stripedTop))
        assertEquals("Navy, floral · Skirt", floralSkirt.summary())
        assertEquals("White, ribbed · Shirt", ribbedTop.summary())
    }
}
