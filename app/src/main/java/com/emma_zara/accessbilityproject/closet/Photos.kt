package com.emma_zara.accessbilityproject.closet

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.ColorSpace
import android.graphics.ImageDecoder
import android.graphics.Matrix
import android.media.ExifInterface
import android.net.Uri
import android.os.Build
import android.util.Log
import android.util.LruCache
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import com.emma_zara.accessbilityproject.color.RgbColor
import java.io.File
import kotlin.math.max

/** Helpers for loading, shrinking and reading colors from photos. */
object Photos {

    /**
     * Loads a photo from the camera or gallery, shrunk so its longest side is at most [maxSize] pixels.
     * Gallery photos can be HEIC or 10-bit HDR (camera photos are always plain JPEG), so every photo
     * is turned into a normal 8-bit sRGB picture, and if one way of reading it fails we try another.
     */
    fun load(context: Context, uri: Uri, maxSize: Int = 1280): Bitmap? {
        val decoded = (if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) withImageDecoder(context, uri, maxSize) else null)
            ?: withBitmapFactory(context, uri, maxSize)
            ?: return null
        return if (decoded.config == Bitmap.Config.ARGB_8888) decoded else decoded.copy(Bitmap.Config.ARGB_8888, false)
    }

    private fun withImageDecoder(context: Context, uri: Uri, maxSize: Int): Bitmap? = try {
        // ImageDecoder also turns the photo the right way up automatically
        ImageDecoder.decodeBitmap(ImageDecoder.createSource(context.contentResolver, uri)) { decoder, info, _ ->
            val longest = max(info.size.width, info.size.height)
            if (longest > maxSize) {
                val scale = maxSize.toFloat() / longest
                decoder.setTargetSize(max(1, (info.size.width * scale).toInt()), max(1, (info.size.height * scale).toInt()))
            }
            decoder.allocator = ImageDecoder.ALLOCATOR_SOFTWARE // so we can read pixels
            decoder.setTargetColorSpace(ColorSpace.get(ColorSpace.Named.SRGB)) // no HDR / wide color
        }
    } catch (e: Throwable) {
        Log.w("Photos", "ImageDecoder couldn't read $uri, trying BitmapFactory", e)
        null
    }

    private fun withBitmapFactory(context: Context, uri: Uri, maxSize: Int): Bitmap? = try {
        val resolver = context.contentResolver
        val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        resolver.openInputStream(uri)?.use { BitmapFactory.decodeStream(it, null, bounds) }
        val options = BitmapFactory.Options().apply {
            inSampleSize = sampleSize(max(bounds.outWidth, bounds.outHeight), maxSize)
            inPreferredConfig = Bitmap.Config.ARGB_8888
        }
        val bitmap = resolver.openInputStream(uri)?.use { BitmapFactory.decodeStream(it, null, options) }
        // BitmapFactory doesn't turn photos the right way up, so read the rotation from the photo's info
        val orientation = resolver.openInputStream(uri)?.use {
            ExifInterface(it).getAttributeInt(ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_NORMAL)
        }
        val degrees = when (orientation) {
            ExifInterface.ORIENTATION_ROTATE_90 -> 90
            ExifInterface.ORIENTATION_ROTATE_180 -> 180
            ExifInterface.ORIENTATION_ROTATE_270 -> 270
            else -> 0
        }
        if (bitmap != null && degrees != 0) {
            Bitmap.createBitmap(bitmap, 0, 0, bitmap.width, bitmap.height, Matrix().apply { postRotate(degrees.toFloat()) }, true)
        } else {
            bitmap
        }
    } catch (e: Throwable) {
        Log.e("Photos", "Couldn't read photo $uri", e)
        null
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
