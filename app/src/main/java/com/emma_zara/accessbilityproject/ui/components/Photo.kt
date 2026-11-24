package com.emma_zara.accessbilityproject.ui.components

import android.content.ActivityNotFoundException
import android.graphics.Bitmap
import android.net.Uri
import android.os.Build
import android.os.ext.SdkExtensions
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.emma_zara.accessbilityproject.closet.Photos
import com.emma_zara.accessbilityproject.color.RgbColor
import kotlin.math.min

/**
 * Returns a function that opens the phone's gallery to choose one picture.
 * [onPicked] gets the chosen picture; [onError] gets a message if no gallery app could be opened.
 */
@Composable
fun rememberPhotoPicker(onPicked: (Uri) -> Unit, onError: (String) -> Unit): () -> Unit {
    val pickImage = rememberLauncherForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri ->
        if (uri != null) onPicked(uri)
    }
    // Older phones (like the Galaxy S9+ on Android 10) don't have Android's photo picker. The library would
    // then use Google's backup picker, which on some phones doesn't open at all. So there we open the
    // phone's own file chooser / gallery instead.
    val getImage = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        if (uri != null) onPicked(uri)
    }
    return {
        try {
            if (hasSystemPhotoPicker()) {
                pickImage.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
            } else {
                getImage.launch("image/*")
            }
        } catch (e: ActivityNotFoundException) {
            onError("No gallery app found.")
        }
    }
}

/**
 * True only if Android's own photo picker is built in: Android 13+, or Android 11/12 with the updated
 * system component. (The library's isPhotoPickerAvailable() also says yes for Google's backup picker,
 * which is the one that didn't open on the Galaxy S9+.)
 */
private fun hasSystemPhotoPicker(): Boolean {
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) return true
    return Build.VERSION.SDK_INT >= Build.VERSION_CODES.R &&
        SdkExtensions.getExtensionVersion(Build.VERSION_CODES.R) >= 2
}

/**
 * The photo, sized to fit. Tapping it reads the color under your finger.
 * [tap] is where the circle is drawn, as a fraction of the photo's width and height (0 to 1), or null for none.
 */
@Composable
fun TappablePhoto(
    bitmap: Bitmap,
    tap: Offset?,
    description: String,
    onTap: (Offset, RgbColor) -> Unit,
) {
    val image = remember(bitmap) { bitmap.asImageBitmap() }
    val ratio = bitmap.width.toFloat() / bitmap.height
    BoxWithConstraints(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
        // Fit inside the card width and at most 420dp tall, keeping the photo's shape
        val height = min(maxWidth.value / ratio, 420f).dp
        val width = (height.value * ratio).dp
        Box(
            Modifier
                .size(width, height)
                .clip(RoundedCornerShape(16.dp))
                .pointerInput(bitmap) {
                    detectTapGestures { pos ->
                        val fx = (pos.x / size.width).coerceIn(0f, 1f)
                        val fy = (pos.y / size.height).coerceIn(0f, 1f)
                        onTap(Offset(fx, fy), colorAtFraction(bitmap, fx, fy))
                    }
                }
                .semantics { contentDescription = description },
        ) {
            Image(image, contentDescription = null, contentScale = ContentScale.FillBounds, modifier = Modifier.fillMaxSize())
            if (tap != null) {
                Canvas(Modifier.fillMaxSize()) {
                    val center = Offset(tap.x * size.width, tap.y * size.height)
                    drawCircle(Color.Black, 20.dp.toPx(), center, style = Stroke(5.dp.toPx()))
                    drawCircle(Color.White, 20.dp.toPx(), center, style = Stroke(2.5.dp.toPx()))
                }
            }
        }
    }
}

/** The color at a point given as a fraction of the photo's width and height (0 to 1). */
fun colorAtFraction(bitmap: Bitmap, fx: Float, fy: Float): RgbColor =
    Photos.colorAt(
        bitmap,
        (fx * bitmap.width).toInt().coerceIn(0, bitmap.width - 1),
        (fy * bitmap.height).toInt().coerceIn(0, bitmap.height - 1),
    )
