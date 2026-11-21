package com.emma_zara.accessbilityproject.closet

import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.hypot
import kotlin.math.sin

/**
 * Guesses the pattern on a piece of clothing by looking at its "edges"
 * (places where the brightness changes sharply):
 * - almost no edges           -> plain
 * - edges all facing one way  -> stripes
 * - edges facing two ways at a right angle (like a grid) -> checks / plaid
 * - edges facing every way    -> a print (dots, flowers, logos...)
 *
 * @param pixels ARGB pixels, row by row
 * @param mask optional, true where the pixel is part of the item. Without it the middle of the photo is used.
 */
fun detectPattern(pixels: IntArray, width: Int, height: Int, mask: BooleanArray? = null): Pattern {
    val n = width * height
    val light = FloatArray(n) { i ->
        val p = pixels[i]
        0.299f * ((p shr 16) and 0xFF) + 0.587f * ((p shr 8) and 0xFF) + 0.114f * (p and 0xFF)
    }
    val inside = BooleanArray(n) { i ->
        if (mask != null) {
            mask[i]
        } else {
            val x = i % width
            val y = i / width
            x in width * 15 / 100 until width * 85 / 100 && y in height * 15 / 100 until height * 85 / 100
        }
    }
    fun l(x: Int, y: Int) = light[y * width + x]
    fun deepInside(x: Int, y: Int): Boolean {
        // Skip pixels near the item's outline, where the background would make fake edges
        for (dy in -2..2 step 2) for (dx in -2..2 step 2) {
            if (!inside[(y + dy) * width + (x + dx)]) return false
        }
        return true
    }

    // Strength of edges in each direction, in 12 steps of 15 degrees (0..180)
    val directions = DoubleArray(12)
    var area = 0
    var edges = 0
    var sumCos = 0.0
    var sumSin = 0.0
    var sumStrength = 0.0
    for (y in 2 until height - 2) {
        for (x in 2 until width - 2) {
            if (!deepInside(x, y)) continue
            area++
            // Sobel filter: how fast the brightness changes left-right (gx) and up-down (gy)
            val gx = (l(x + 1, y - 1) + 2 * l(x + 1, y) + l(x + 1, y + 1)) - (l(x - 1, y - 1) + 2 * l(x - 1, y) + l(x - 1, y + 1))
            val gy = (l(x - 1, y + 1) + 2 * l(x, y + 1) + l(x + 1, y + 1)) - (l(x - 1, y - 1) + 2 * l(x, y - 1) + l(x + 1, y - 1))
            val strength = hypot(gx, gy).toDouble()
            if (strength < EDGE_STRENGTH) continue   // soft shading from folds and light doesn't count
            edges++
            val angle = atan2(gy, gx).toDouble()     // -180..180 degrees, but 10 and 190 are the same line
            sumCos += strength * cos(2 * angle)
            sumSin += strength * sin(2 * angle)
            sumStrength += strength
            val degrees = ((Math.toDegrees(angle) % 180) + 180) % 180
            directions[(degrees / 15).toInt() % 12] += strength
        }
    }

    if (area < 50 || edges.toDouble() / area < 0.05) return Pattern.SOLID

    // 1.0 = every edge faces the same way, 0.0 = all directions equally
    val sameWay = hypot(sumCos, sumSin) / sumStrength
    if (sameWay > 0.5) return Pattern.STRIPES

    // Look for two main directions at a right angle (75..105 degrees apart)
    val smooth = DoubleArray(12) { i -> directions[(i + 11) % 12] + directions[i] + directions[(i + 1) % 12] }
    val first = smooth.indices.maxBy { smooth[it] }
    val second = (5..7).map { (first + it) % 12 }.maxBy { smooth[it] }
    // Share of all edges near those two directions (half the circle, so 0.5 would be random)
    val inTwoDirections = (smooth[first] + smooth[second]) / directions.sum()
    if (smooth[second] >= 0.5 * smooth[first] && inTwoDirections >= 0.7) return Pattern.CHECKS

    return Pattern.PRINT
}

/** How sharp a brightness change must be to count as an edge (Sobel scale; about a 20/255 step). */
private const val EDGE_STRENGTH = 80.0
