package com.emma_zara.accessbilityproject.color

import kotlin.math.pow
import kotlin.math.roundToInt

/**
 * The kinds of color vision the app can adjust for.
 * [matrix] changes a color (in linear RGB) into how it looks with that kind of color blindness.
 * The numbers are from Machado, Oliveira & Fernandes (2009), "A Physiologically-based Model for
 * Simulation of Color Vision Deficiency", at full strength.
 */
enum class ColorVision(val label: String, val description: String, private val matrix: DoubleArray?) {
    TYPICAL("Typical color vision", "No changes.", null),
    PROTAN(
        "Red-weak (protanopia)",
        "Reds look darker and can be mixed up with greens and browns.",
        doubleArrayOf(
            0.152286, 1.052583, -0.204868,
            0.114503, 0.786281, 0.099216,
            -0.003882, -0.048116, 1.051998,
        ),
    ),
    DEUTAN(
        "Green-weak (deuteranopia)",
        "The most common kind. Reds, greens, browns and oranges can look alike.",
        doubleArrayOf(
            0.367322, 0.860646, -0.227968,
            0.280085, 0.672501, 0.047413,
            -0.011820, 0.042940, 0.968881,
        ),
    ),
    TRITAN(
        "Blue-yellow (tritanopia)",
        "Blues can look like greens, and yellows like pinks or light grays.",
        doubleArrayOf(
            1.255528, -0.076749, -0.178779,
            -0.078411, 0.930809, 0.147602,
            0.004733, 0.691367, 0.303900,
        ),
    ),
    MONOCHROMACY("No color (monochromacy)", "Only light and dark can be seen, no colors.", null);

    /** Roughly how [color] looks with this kind of color vision. */
    fun simulate(color: RgbColor): RgbColor {
        if (this == TYPICAL) return color
        val r = channelToLinear(color.r)
        val g = channelToLinear(color.g)
        val b = channelToLinear(color.b)
        val m = matrix
        if (m == null) {
            // Only brightness is left: a gray with the same luminance
            val y = toChannel(0.2126 * r + 0.7152 * g + 0.0722 * b)
            return RgbColor(y, y, y)
        }
        return RgbColor(
            toChannel(m[0] * r + m[1] * g + m[2] * b),
            toChannel(m[3] * r + m[4] * g + m[5] * b),
            toChannel(m[6] * r + m[7] * g + m[8] * b),
        )
    }
}

/** Linear light (0..1) back to a 0..255 sRGB channel. */
private fun toChannel(linear: Double): Int {
    val v = linear.coerceIn(0.0, 1.0)
    val s = if (v <= 0.0031308) v * 12.92 else 1.055 * v.pow(1 / 2.4) - 0.055
    return (s * 255).roundToInt().coerceIn(0, 255)
}

/**
 * True if [a] and [b] are clearly different colors, but probably look alike with [vision].
 * Used to warn "these two may look the same to you".
 */
fun looksAlike(a: RgbColor, b: RgbColor, vision: ColorVision): Boolean {
    if (vision == ColorVision.TYPICAL) return false
    val really = deltaE(a.toLab(), b.toLab())
    val seen = deltaE(vision.simulate(a).toLab(), vision.simulate(b).toLab())
    return really >= 20 && seen < 15
}
