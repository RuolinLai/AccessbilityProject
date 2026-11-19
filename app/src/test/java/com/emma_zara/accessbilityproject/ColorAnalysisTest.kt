package com.emma_zara.accessbilityproject

import com.emma_zara.accessbilityproject.color.ColorNames
import com.emma_zara.accessbilityproject.color.Lightness
import com.emma_zara.accessbilityproject.color.RgbColor
import com.emma_zara.accessbilityproject.color.Temperature
import com.emma_zara.accessbilityproject.color.analyzeColor
import com.emma_zara.accessbilityproject.color.toHsv
import org.junit.Assert.assertEquals
import org.junit.Test

class ColorAnalysisTest {
    @Test
    fun hexCode() {
        assertEquals("#1E3A8A", RgbColor(0x1E, 0x3A, 0x8A).hex)
    }

    @Test
    fun hsvRoundTrip() {
        val original = RgbColor(200, 100, 50)
        val hsv = original.toHsv()
        assertEquals(original, RgbColor.fromHsv(hsv.h, hsv.s, hsv.v))
    }

    @Test
    fun namesAreCorrect() {
        assertEquals("Red", ColorNames.nearest(RgbColor(250, 5, 5)).name)
        assertEquals("Navy", ColorNames.nearest(RgbColor(5, 5, 120)).name)
        assertEquals("White", ColorNames.nearest(RgbColor(254, 254, 254)).name)
        assertEquals("Taupe", ColorNames.nearest(RgbColor(0x6B, 0x5B, 0x4C)).name)
        // Muted colors should get a real color name, not "gray"
        assertEquals("Moss", ColorNames.nearest(RgbColor(0x6B, 0x6B, 0x4C)).name)
        assertEquals("Hunter Green", ColorNames.nearest(RgbColor(0x4C, 0x6B, 0x5B)).name)
        assertEquals("Eggplant", ColorNames.nearest(RgbColor(0x5B, 0x4C, 0x6B)).name)
        assertEquals("Navy", ColorNames.nearest(RgbColor(0x19, 0x23, 0x55)).name)
        assertEquals("Rosewood", ColorNames.nearest(RgbColor(0x6B, 0x4C, 0x4D)).name)
    }

    @Test
    fun temperatureAndLightness() {
        assertEquals(Temperature.WARM, analyzeColor(RgbColor(255, 0, 0)).temperature)
        assertEquals(Temperature.COOL, analyzeColor(RgbColor(0, 0, 255)).temperature)
        assertEquals(Temperature.NEUTRAL, analyzeColor(RgbColor(128, 128, 128)).temperature)
        assertEquals(Lightness.DARK, analyzeColor(RgbColor(0, 0, 128)).lightness)
        assertEquals(Lightness.LIGHT, analyzeColor(RgbColor(255, 255, 200)).lightness)
    }

    @Test
    fun complementaryOfRedIsCyan() {
        assertEquals(RgbColor(0, 255, 255), analyzeColor(RgbColor(255, 0, 0)).complementary)
    }
}
