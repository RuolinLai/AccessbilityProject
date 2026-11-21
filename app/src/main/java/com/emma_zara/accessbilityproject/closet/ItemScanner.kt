package com.emma_zara.accessbilityproject.closet

import android.graphics.Bitmap
import com.emma_zara.accessbilityproject.color.RgbColor
import com.emma_zara.accessbilityproject.color.dominantColors
import com.google.android.gms.tasks.Task
import com.google.android.gms.tasks.Tasks
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.label.ImageLabeling
import com.google.mlkit.vision.label.defaults.ImageLabelerOptions
import com.google.mlkit.vision.segmentation.subject.SubjectSegmentation
import com.google.mlkit.vision.segmentation.subject.SubjectSegmenterOptions
import java.util.concurrent.TimeUnit
import kotlin.math.max
import kotlin.math.roundToInt

/** What the app worked out by itself from a photo of clothes. */
data class ScanResult(
    /** Main colors of the item, most common first. */
    val colors: List<RgbColor>,
    /** True if the item was cut out from the background, so the colors are only from the item. */
    val foundItem: Boolean,
    /** Best guess of the category, or null if the app isn't sure. */
    val category: Category?,
    /** Best guess of the pattern on the fabric. */
    val pattern: Pattern,
)

/**
 * Looks at a clothing photo automatically, so people who can't tell colors apart don't have to.
 * Uses Google ML Kit, which runs on the phone (no internet, nothing is uploaded).
 */
object ItemScanner {

    /** Call this off the main thread: it waits for ML Kit to finish. */
    fun scan(photo: Bitmap): ScanResult {
        // A small copy is plenty for colors and much faster to process
        val small = scaleDown(photo, 256)
        val w = small.width
        val h = small.height
        val pixels = IntArray(w * h)
        small.getPixels(pixels, 0, w, 0, 0, w, h)

        val mask = findItem(small)
        val itemColors = mask?.let { dominantColors(pixels, w, h, mask = it) }.orEmpty()
        val foundItem = itemColors.isNotEmpty()

        return ScanResult(
            // If the item couldn't be found, fall back to the middle of the photo
            colors = if (foundItem) itemColors else dominantColors(pixels, w, h),
            foundItem = foundItem,
            category = guessCategory(photo),
            pattern = detectPattern(pixels, w, h, if (foundItem) mask else null),
        )
    }

    /**
     * Returns true for each pixel that belongs to the item (not the background),
     * or null if ML Kit isn't available or found nothing.
     */
    private fun findItem(bitmap: Bitmap): BooleanArray? {
        // Still downloading, or it can never work on this phone: don't wait for it
        when (val status = BackgroundRemover.status) {
            is BackgroundRemover.Status.Downloading, BackgroundRemover.Status.WaitingForInternet -> return null
            is BackgroundRemover.Status.Unavailable -> if (!status.canRetry) return null
            else -> Unit
        }
        return cutOut(bitmap)
    }

    private fun cutOut(bitmap: Bitmap): BooleanArray? = try {
        val segmenter = SubjectSegmentation.getClient(
            SubjectSegmenterOptions.Builder().enableForegroundConfidenceMask().build(),
        )
        val result = try {
            await(segmenter.process(InputImage.fromBitmap(bitmap, 0)))
        } finally {
            segmenter.close()
        }
        BackgroundRemover.markReady()
        val confidence = result.foregroundConfidenceMask
        val size = bitmap.width * bitmap.height
        if (confidence == null || confidence.remaining() < size) {
            null
        } else {
            val mask = BooleanArray(size) { confidence.get(it) > 0.5f }
            // Less than 3% of the photo is probably a mistake, not the item
            if (mask.count { it } < size * 0.03) null else mask
        }
    } catch (e: Exception) {
        // For example: Google Play services missing, or the model is still downloading
        null
    }

    /** Asks ML Kit what's in the photo and turns that into one of our categories. */
    private fun guessCategory(bitmap: Bitmap): Category? = try {
        val labeler = ImageLabeling.getClient(
            ImageLabelerOptions.Builder().setConfidenceThreshold(0.6f).build(),
        )
        val labels = try {
            await(labeler.process(InputImage.fromBitmap(bitmap, 0)))
        } finally {
            labeler.close()
        }
        labels.sortedByDescending { it.confidence }.firstNotNullOfOrNull { categoryForLabel(it.text) }
    } catch (e: Exception) {
        null
    }

    private fun <T> await(task: Task<T>): T = Tasks.await(task, 10, TimeUnit.SECONDS)

    private fun scaleDown(bitmap: Bitmap, maxSize: Int): Bitmap {
        val longest = max(bitmap.width, bitmap.height)
        if (longest <= maxSize) return bitmap
        val scale = maxSize.toFloat() / longest
        return Bitmap.createScaledBitmap(
            bitmap,
            max(1, (bitmap.width * scale).roundToInt()),
            max(1, (bitmap.height * scale).roundToInt()),
            true,
        )
    }
}

/** Words ML Kit might use for each category. Checked in this order. */
private val categoryWords: List<Pair<Category, List<String>>> = listOf(
    Category.DRESSES to listOf("dress", "gown"),
    Category.SKIRTS to listOf("skirt", "miniskirt"),
    Category.PANTS to listOf("jeans", "pants", "trousers", "shorts", "leggings"),
    Category.SHOES to listOf("shoe", "shoes", "sneakers", "boot", "boots", "sandal", "sandals", "high heels", "footwear"),
    Category.BAGS to listOf("bag", "handbag", "backpack", "purse", "wallet", "luggage"),
    Category.JEWELLERY to listOf(
        "jewelry", "jewellery", "necklace", "ring", "earring", "earrings", "bracelet", "pendant", "brooch",
    ),
    Category.SHIRTS to listOf(
        "shirt", "t shirt", "blouse", "jersey", "sweater", "sweatshirt", "hoodie", "top", "jacket", "coat",
    ),
)

/** Turns a label like "T-shirt" or "Jeans" into a category, or null if it isn't clothing. */
fun categoryForLabel(label: String): Category? {
    // Compare whole words only, so "earring" doesn't count as "ring"
    val words = " " + label.lowercase().replace(Regex("[^a-z]+"), " ").trim() + " "
    return categoryWords.firstOrNull { (_, keys) -> keys.any { words.contains(" $it ") } }?.first
}
