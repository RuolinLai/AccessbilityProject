package com.emma_zara.accessbilityproject.color

import kotlin.math.abs
import kotlin.math.cbrt
import kotlin.math.pow
import kotlin.math.roundToInt
import kotlin.math.sqrt

/**
 * A color stored as Red, Green, Blue values from 0 to 255.
 * This file is plain Kotlin (no Android code) so it is easy to unit test.
 */
data class RgbColor(val r: Int, val g: Int, val b: Int) {
    init {
        require(r in 0..255 && g in 0..255 && b in 0..255) { "RGB values must be 0..255" }
    }

    /** Hex code like "#1E3A8A". */
    val hex: String get() = "#%02X%02X%02X".format(r, g, b)

    /** Packs the color into one Int (ARGB), the format Android uses. */
    fun toArgb(): Int = (0xFF shl 24) or (r shl 16) or (g shl 8) or b

    companion object {
        fun fromArgb(argb: Int) = RgbColor((argb shr 16) and 0xFF, (argb shr 8) and 0xFF, argb and 0xFF)

        /**
         * Converts HSV to RGB.
         * @param h hue in degrees (0..360), @param s saturation (0..1), @param v value/brightness (0..1)
         */
        fun fromHsv(h: Float, s: Float, v: Float): RgbColor {
            val hue = ((h % 360f) + 360f) % 360f
            val c = v * s                                   // chroma: how much "color" there is
            val x = c * (1 - abs((hue / 60f) % 2f - 1))
            val m = v - c
            val (r1, g1, b1) = when ((hue / 60f).toInt()) {
                0 -> Triple(c, x, 0f)
                1 -> Triple(x, c, 0f)
                2 -> Triple(0f, c, x)
                3 -> Triple(0f, x, c)
                4 -> Triple(x, 0f, c)
                else -> Triple(c, 0f, x)
            }
            fun to255(f: Float) = ((f + m) * 255f).roundToInt().coerceIn(0, 255)
            return RgbColor(to255(r1), to255(g1), to255(b1))
        }
    }
}

/** Hue (0..360 degrees), Saturation (0..1), Value/brightness (0..1). */
data class Hsv(val h: Float, val s: Float, val v: Float)

fun RgbColor.toHsv(): Hsv {
    val rf = r / 255f
    val gf = g / 255f
    val bf = b / 255f
    val max = maxOf(rf, gf, bf)
    val min = minOf(rf, gf, bf)
    val delta = max - min

    val h = when {
        delta == 0f -> 0f
        max == rf -> 60f * (((gf - bf) / delta) % 6f)
        max == gf -> 60f * ((bf - rf) / delta + 2f)
        else -> 60f * ((rf - gf) / delta + 4f)
    }
    val hue = if (h < 0f) h + 360f else h
    val saturation = if (max == 0f) 0f else delta / max
    return Hsv(hue, saturation, max)
}

/** CMYK (printing colors), each 0..1. Part of the "composition" of a color. */
data class Cmyk(val c: Float, val m: Float, val y: Float, val k: Float)

fun RgbColor.toCmyk(): Cmyk {
    val rf = r / 255f
    val gf = g / 255f
    val bf = b / 255f
    val k = 1f - maxOf(rf, gf, bf)
    if (k >= 1f) return Cmyk(0f, 0f, 0f, 1f)
    return Cmyk((1f - rf - k) / (1f - k), (1f - gf - k) / (1f - k), (1f - bf - k) / (1f - k), k)
}

/**
 * CIELAB color: L = lightness (0..100), a = green(-)..red(+), b = blue(-)..yellow(+).
 * Distances in Lab match how different two colors *look* to people much better than RGB distances,
 * so we use it to find the closest color name.
 */
data class Lab(val l: Double, val a: Double, val b: Double)

internal fun channelToLinear(c: Int): Double {
    val v = c / 255.0
    return if (v <= 0.04045) v / 12.92 else ((v + 0.055) / 1.055).pow(2.4)
}

/** How bright the color looks (0 = black, 1 = white). Used for picking readable text colors. */
fun RgbColor.relativeLuminance(): Double =
    0.2126 * channelToLinear(r) + 0.7152 * channelToLinear(g) + 0.0722 * channelToLinear(b)

fun RgbColor.toLab(): Lab {
    val rl = channelToLinear(r)
    val gl = channelToLinear(g)
    val bl = channelToLinear(b)
    // sRGB -> XYZ (D65 white point), then normalize by the white point
    val x = (0.4124 * rl + 0.3576 * gl + 0.1805 * bl) / 0.95047
    val y = (0.2126 * rl + 0.7152 * gl + 0.0722 * bl) / 1.0
    val z = (0.0193 * rl + 0.1192 * gl + 0.9505 * bl) / 1.08883

    fun f(t: Double) = if (t > 216.0 / 24389.0) cbrt(t) else (24389.0 / 27.0 * t + 16.0) / 116.0
    val fx = f(x)
    val fy = f(y)
    val fz = f(z)
    return Lab(116.0 * fy - 16.0, 500.0 * (fx - fy), 200.0 * (fy - fz))
}

/** "Delta E": how different two colors look. Under ~2.3 most people can't tell them apart. */
fun deltaE(c1: Lab, c2: Lab): Double {
    val dl = c1.l - c2.l
    val da = c1.a - c2.a
    val db = c1.b - c2.b
    return sqrt(dl * dl + da * da + db * db)
}
