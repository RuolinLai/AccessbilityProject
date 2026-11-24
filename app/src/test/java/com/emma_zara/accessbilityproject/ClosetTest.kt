package com.emma_zara.accessbilityproject

import com.emma_zara.accessbilityproject.closet.Category
import com.emma_zara.accessbilityproject.closet.ClothingItem
import com.emma_zara.accessbilityproject.closet.MatchType
import com.emma_zara.accessbilityproject.closet.categoryForLabel
import com.emma_zara.accessbilityproject.closet.findMatches
import com.emma_zara.accessbilityproject.closet.isFashionNeutral
import com.emma_zara.accessbilityproject.closet.matchType
import com.emma_zara.accessbilityproject.color.RgbColor
import com.emma_zara.accessbilityproject.color.dominantColors
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class ClosetTest {
    private fun item(id: String, category: Category, color: RgbColor, addedAt: Long = 0) =
        ClothingItem(id, id, category, color, "$id.jpg", addedAt)

    @Test
    fun neutrals() {
        assertTrue(isFashionNeutral(RgbColor(0, 0, 0)))        // black
        assertTrue(isFashionNeutral(RgbColor(222, 204, 170)))  // beige
        assertTrue(isFashionNeutral(RgbColor(20, 30, 90)))     // navy
        assertFalse(isFashionNeutral(RgbColor(255, 0, 0)))     // red
    }

    @Test
    fun colorRules() {
        val red = RgbColor(220, 20, 20)
        assertEquals(MatchType.NEUTRAL, matchType(red, RgbColor(255, 255, 255)))
        assertEquals(MatchType.SAME_COLOR, matchType(red, RgbColor(150, 10, 10)))
        assertEquals(MatchType.ANALOGOUS, matchType(red, RgbColor(230, 120, 20)))   // orange
        assertEquals(MatchType.COMPLEMENTARY, matchType(red, RgbColor(20, 200, 200))) // cyan
        assertNull(matchType(red, RgbColor(120, 220, 20)))                          // lime: ~90Â° away
    }

    @Test
    fun matchesSkipSameCategory() {
        val shirt = item("shirt", Category.SHIRTS, RgbColor(220, 20, 20))
        val otherShirt = item("shirt2", Category.SHIRTS, RgbColor(255, 255, 255))
        val pants = item("pants", Category.PANTS, RgbColor(255, 255, 255))
        val matches = findMatches(shirt, listOf(shirt, otherShirt, pants))
        assertEquals(listOf("pants"), matches.map { it.item.id })
    }

    @Test
    fun findsMainColorInMiddleOfPhoto() {
        // 20x20 white "background" with a red 12x12 square in the middle
        val w = 20
        val pixels = IntArray(w * w) { i ->
            val x = i % w
            val y = i / w
            if (x in 4..15 && y in 4..15) 0xFFDC1414.toInt() else 0xFFFFFFFF.toInt()
        }
        assertEquals(RgbColor(220, 20, 20), dominantColors(pixels, w, w).first())
    }

    @Test
    fun maskOnlyCountsTheItem() {
        // Mostly white background, but the mask says only the small blue patch is the item
        val w = 20
        val pixels = IntArray(w * w) { i -> if (i % w < 5) 0xFF1E3CB4.toInt() else 0xFFFFFFFF.toInt() }
        val mask = BooleanArray(w * w) { i -> i % w < 5 }
        assertEquals(listOf(RgbColor(30, 60, 180)), dominantColors(pixels, w, w, mask = mask))
    }

    @Test
    fun turnsLabelsIntoCategories() {
        assertEquals(Category.SHIRTS, categoryForLabel("T-shirt"))
        assertEquals(Category.PANTS, categoryForLabel("Jeans"))
        assertEquals(Category.SHOES, categoryForLabel("Sneakers"))
        assertEquals(Category.BAGS, categoryForLabel("Handbag"))
        assertEquals(Category.JEWELLERY, categoryForLabel("Earring"))
        assertEquals(Category.DRESSES, categoryForLabel("Dress"))
        assertNull(categoryForLabel("Dog"))
        assertNull(categoryForLabel("Toppings")) // whole words only, not "top"
    }
}
