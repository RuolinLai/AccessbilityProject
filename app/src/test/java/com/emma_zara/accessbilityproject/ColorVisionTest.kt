package com.emma_zara.accessbilityproject

import com.emma_zara.accessbilityproject.closet.Category
import com.emma_zara.accessbilityproject.closet.ClothingItem
import com.emma_zara.accessbilityproject.closet.MatchType
import com.emma_zara.accessbilityproject.closet.Outfit
import com.emma_zara.accessbilityproject.closet.colorNames
import com.emma_zara.accessbilityproject.closet.findMatches
import com.emma_zara.accessbilityproject.closet.goTogether
import com.emma_zara.accessbilityproject.closet.lowContrast
import com.emma_zara.accessbilityproject.closet.matchItems
import com.emma_zara.accessbilityproject.closet.reasons
import com.emma_zara.accessbilityproject.color.ColorVision
import com.emma_zara.accessbilityproject.color.RgbColor
import com.emma_zara.accessbilityproject.color.looksAlike
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/** Tests for extra colors, low contrast and color-vision warnings. */
class ColorVisionTest {
    private fun item(id: String, category: Category, color: RgbColor, extra: List<RgbColor> = emptyList()) =
        ClothingItem(id, id, category, color, "$id.jpg", 0, extraColors = extra)

    private val white = RgbColor(245, 245, 245)
    private val navy = RgbColor(30, 40, 80)
    private val red = RgbColor(180, 40, 30)
    private val olive = RgbColor(110, 110, 20)
    private val green = RgbColor(30, 200, 40)
    private val orange = RgbColor(255, 140, 0)

    // ---- Color vision ----

    @Test
    fun typicalVisionNeverWarns() {
        assertFalse(looksAlike(red, olive, ColorVision.TYPICAL))
    }

    @Test
    fun redAndOliveLookAlikeWhenGreenWeak() {
        assertTrue(looksAlike(red, olive, ColorVision.DEUTAN))
    }

    @Test
    fun navyAndWhiteNeverLookAlike() {
        ColorVision.entries.forEach { assertFalse(it.name, looksAlike(navy, white, it)) }
    }

    @Test
    fun nearlySameColorsAreNotWarnedAbout() {
        // They really are almost the same, so there's nothing to warn about
        assertFalse(looksAlike(red, RgbColor(185, 45, 35), ColorVision.DEUTAN))
    }

    @Test
    fun monochromacyKeepsOnlyLightness() {
        val gray = ColorVision.MONOCHROMACY.simulate(green)
        assertEquals(gray.r, gray.g)
        assertEquals(gray.g, gray.b)
    }

    @Test
    fun simulationKeepsBlackAndWhite() {
        ColorVision.entries.forEach {
            assertEquals(RgbColor(0, 0, 0), it.simulate(RgbColor(0, 0, 0)))
            val w = it.simulate(RgbColor(255, 255, 255))
            assertTrue("${it.name}: $w", w.r >= 250 && w.g >= 250 && w.b >= 250)
        }
    }

    @Test
    fun outfitWarnsAboutLookAlikePieces() {
        val outfit = Outfit(listOf(item("Red top", Category.SHIRTS, red), item("Olive pants", Category.PANTS, olive)), 1.0)
        assertTrue(outfit.reasons(ColorVision.DEUTAN).any { "look alike" in it })
        assertFalse(outfit.reasons(ColorVision.TYPICAL).any { "look alike" in it })
    }

    // ---- Extra colors ----

    @Test
    fun extraColorThatClashesBreaksTheMatch() {
        val stripedTop = item("top", Category.SHIRTS, navy, extra = listOf(green))
        val orangeSkirt = item("skirt", Category.SKIRTS, orange)
        // Navy alone goes with anything, but the green stripes don't go with orange
        assertEquals(MatchType.NEUTRAL, matchItems(stripedTop.copy(extraColors = emptyList()), orangeSkirt))
        assertNull(matchItems(stripedTop, orangeSkirt))
        assertFalse(goTogether(stripedTop, orangeSkirt))
    }

    @Test
    fun neutralExtraColorKeepsTheMatch() {
        val stripedTop = item("top", Category.SHIRTS, navy, extra = listOf(white))
        assertTrue(goTogether(stripedTop, item("skirt", Category.SKIRTS, orange)))
    }

    @Test
    fun colorNamesListsEveryColorOnce() {
        val top = item("top", Category.SHIRTS, navy, extra = listOf(white, RgbColor(250, 250, 250)))
        assertEquals("Navy & White", top.colorNames())
    }

    // ---- Low contrast ----

    @Test
    fun similarLightnessIsLowContrast() {
        assertTrue(lowContrast(navy, RgbColor(20, 20, 20)))
        assertFalse(lowContrast(navy, white))
    }

    @Test
    fun matchesAreMarkedLowContrast() {
        val top = item("top", Category.SHIRTS, navy)
        val black = item("black", Category.PANTS, RgbColor(20, 20, 20))
        val whitePants = item("white", Category.PANTS, white)
        val matches = findMatches(top, listOf(top, black, whitePants)).associateBy { it.item.id }
        assertTrue(matches.getValue("black").lowContrast)
        assertFalse(matches.getValue("white").lowContrast)
    }

    @Test
    fun outfitGivesLowContrastTip() {
        val outfit = Outfit(listOf(item("Navy top", Category.SHIRTS, navy), item("Black pants", Category.PANTS, RgbColor(20, 20, 20))), 1.0)
        assertTrue(outfit.reasons().any { "blur together" in it })
    }
}
