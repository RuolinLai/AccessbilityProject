package com.emma_zara.accessbilityproject.color

/**
 * Finds the main colors in a picture.
 *
 * How it works:
 * 1. Only look at the middle part of the photo (the clothes are usually in the middle,
 *    the background is around the edges).
 * 2. Put every pixel into a "bucket" of similar colors (8 levels per channel = 512 buckets).
 * 3. The bucket with the most pixels is the main color. We return the average color
 *    of the biggest buckets, skipping ones that look almost the same as one already chosen.
 *
 * @param pixels ARGB pixels, row by row (like Android's Bitmap.getPixels)
 */
fun dominantColors(pixels: IntArray, width: Int, height: Int, count: Int = 4): List<RgbColor> {
    val counts = IntArray(512)
    val sumR = LongArray(512)
    val sumG = LongArray(512)
    val sumB = LongArray(512)

    val x0 = width * 15 / 100
    val x1 = width * 85 / 100
    val y0 = height * 15 / 100
    val y1 = height * 85 / 100

    for (y in y0 until y1) {
        for (x in x0 until x1) {
            val p = pixels[y * width + x]
            val r = (p shr 16) and 0xFF
            val g = (p shr 8) and 0xFF
            val b = p and 0xFF
            val bucket = (r shr 5) * 64 + (g shr 5) * 8 + (b shr 5)
            counts[bucket]++
            sumR[bucket] += r.toLong()
            sumG[bucket] += g.toLong()
            sumB[bucket] += b.toLong()
        }
    }

    val result = mutableListOf<RgbColor>()
    for (bucket in counts.indices.sortedByDescending { counts[it] }) {
        val n = counts[bucket]
        if (n == 0 || result.size >= count) break
        val color = RgbColor((sumR[bucket] / n).toInt(), (sumG[bucket] / n).toInt(), (sumB[bucket] / n).toInt())
        val lab = color.toLab()
        // Skip colors that look nearly the same as one we already have
        if (result.none { deltaE(it.toLab(), lab) < 15.0 }) result.add(color)
    }
    return result
}
