package com.emma_zara.accessbilityproject

import com.emma_zara.accessbilityproject.closet.Category
import com.emma_zara.accessbilityproject.closet.ClothingItem
import com.emma_zara.accessbilityproject.closet.MatchType
import com.emma_zara.accessbilityproject.closet.SortOrder
import com.emma_zara.accessbilityproject.closet.findMatches
import com.emma_zara.accessbilityproject.closet.isFashionNeutral
import com.emma_zara.accessbilityproject.closet.matchType
import com.emma_zara.accessbilityproject.closet.sortItems
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
    fun sortByColorPutsGraysLast() {
        val gray = item("gray", Category.SHIRTS, RgbColor(128, 128, 128))
        val blue = item("blue", Category.SHIRTS, RgbColor(0, 0, 255))
        val red = item("red", Category.SHIRTS, RgbColor(255, 0, 0))
        val crimson = item("crimson", Category.SHIRTS, RgbColor(200, 30, 40)) // hue 356, just below 360
        // Both reds come first (crimson's hue 356 wraps around to the start), then blue, grays last.
        assertEquals(
            listOf("crimson", "red", "blue", "gray"),
            sortItems(listOf(gray, blue, crimson, red), SortOrder.COLOR).map { it.id },
        )
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
}
