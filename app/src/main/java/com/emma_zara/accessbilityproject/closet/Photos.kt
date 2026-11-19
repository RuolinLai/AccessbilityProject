package com.emma_zara.accessbilityproject.closet

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.ImageDecoder
import android.net.Uri
import android.os.Build
import android.util.LruCache
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import com.emma_zara.accessbilityproject.color.RgbColor
import com.emma_zara.accessbilityproject.color.dominantColors
import java.io.File
import kotlin.math.max

/** Helpers for loading, shrinking and reading colors from photos. */
object Photos {

    /** Loads a photo from the camera or gallery, shrunk so its longest side is at most [maxSize] pixels. */
    fun load(context: Context, uri: Uri, maxSize: Int = 1280): Bitmap? = try {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            // ImageDecoder also turns the photo the right way up automatically
            ImageDecoder.decodeBitmap(ImageDecoder.createSource(context.contentResolver, uri)) { decoder, info, _ ->
                val longest = max(info.size.width, info.size.height)
                if (longest > maxSize) {
                    val scale = maxSize.toFloat() / longest
                    decoder.setTargetSize((info.size.width * scale).toInt(), (info.size.height * scale).toInt())
                }
                decoder.allocator = ImageDecoder.ALLOCATOR_SOFTWARE // so we can read pixels
            }
        } else {
            val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
            context.contentResolver.openInputStream(uri)?.use { BitmapFactory.decodeStream(it, null, bounds) }
            val options = BitmapFactory.Options().apply {
                inSampleSize = sampleSize(max(bounds.outWidth, bounds.outHeight), maxSize)
            }
            context.contentResolver.openInputStream(uri)?.use { BitmapFactory.decodeStream(it, null, options) }
        }
    } catch (e: Exception) {
        null
    }

    /** The main colors in the photo (most common first). */
    fun mainColors(bitmap: Bitmap): List<RgbColor> {
        val small = Bitmap.createScaledBitmap(bitmap, 64, 64, true)
        val pixels = IntArray(64 * 64)
        small.getPixels(pixels, 0, 64, 0, 0, 64, 64)
        return dominantColors(pixels, 64, 64)
    }

    /** The color at one point of the photo (averaged over a small area to reduce noise). */
    fun colorAt(bitmap: Bitmap, x: Int, y: Int): RgbColor {
        val radius = max(2, bitmap.width / 100)
        var r = 0L
        var g = 0L
        var b = 0L
        var n = 0
        for (yy in (y - radius)..(y + radius)) {
            for (xx in (x - radius)..(x + radius)) {
                if (xx !in 0 until bitmap.width || yy !in 0 until bitmap.height) continue
                val p = bitmap.getPixel(xx, yy)
                r += (p shr 16) and 0xFF
                g += (p shr 8) and 0xFF
                b += p and 0xFF
                n++
            }
        }
        return RgbColor((r / n).toInt(), (g / n).toInt(), (b / n).toInt())
    }

    // Small thumbnails are cached in memory so scrolling the closet is smooth
    private val thumbCache = LruCache<String, ImageBitmap>(60)

    fun cachedThumbnail(file: File, size: Int = 400): ImageBitmap? = thumbCache.get("${file.path}@$size")

    /** Loads a small version of a saved photo. Call this off the main thread. */
    fun loadThumbnail(file: File, size: Int = 400): ImageBitmap? {
        val key = "${file.path}@$size"
        thumbCache.get(key)?.let { return it }
        val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        BitmapFactory.decodeFile(file.path, bounds)
        if (bounds.outWidth <= 0) return null
        val options = BitmapFactory.Options().apply { inSampleSize = sampleSize(max(bounds.outWidth, bounds.outHeight), size) }
        val bitmap = BitmapFactory.decodeFile(file.path, options)?.asImageBitmap() ?: return null
        thumbCache.put(key, bitmap)
        return bitmap
    }

    private fun sampleSize(longest: Int, target: Int): Int {
        var sample = 1
        while (longest / (sample * 2) >= target) sample *= 2
        return sample
    }
}
