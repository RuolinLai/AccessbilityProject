package com.emma_zara.accessbilityproject.ui.screens

import android.Manifest
import android.content.pm.PackageManager
import android.util.Log
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageProxy
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.emma_zara.accessbilityproject.R
import com.emma_zara.accessbilityproject.color.RgbColor
import com.emma_zara.accessbilityproject.color.analyzeColor
import com.emma_zara.accessbilityproject.ui.components.AText
import com.emma_zara.accessbilityproject.ui.components.ColorHeroCard
import com.emma_zara.accessbilityproject.ui.components.CompositionCard
import com.emma_zara.accessbilityproject.ui.components.MatchingColorsCard
import com.emma_zara.accessbilityproject.ui.components.ScreenTitle
import com.emma_zara.accessbilityproject.ui.components.SectionCard
import com.emma_zara.accessbilityproject.ui.components.toComposeColor

/**
 * Color identifier: shows the camera, and describes the color under the circle in the middle.
 */
@Composable
fun IdentifyScreen(
    color: RgbColor,
    onColorChange: (RgbColor) -> Unit,
    onOpenInWheel: () -> Unit,
) {
    val context = LocalContext.current
    var hasPermission by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED,
        )
    }
    val permissionLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) {
        hasPermission = it
    }
    var frozen by remember { mutableStateOf(false) }
    var cameraError by remember { mutableStateOf<String?>(null) }
    val analysis = analyzeColor(color)

    Column(
        Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        ScreenTitle("Identify a color", "Point the circle at something.")

        when {
            !hasPermission -> SectionCard(title = "Camera needed") {
                AText(
                    "The app looks through your camera to tell you what color something is. " +
                        "Nothing is saved or sent anywhere.",
                    style = MaterialTheme.typography.bodyLarge,
                )
                Button(onClick = { permissionLauncher.launch(Manifest.permission.CAMERA) }) {
                    Icon(painterResource(R.drawable.ic_camera), contentDescription = null)
                    Spacer(Modifier.width(8.dp))
                    AText("Allow camera")
                }
            }

            cameraError != null -> SectionCard(title = "Camera problem") {
                AText(cameraError ?: "", style = MaterialTheme.typography.bodyLarge)
            }

            else -> CameraCard(
                frozen = frozen,
                color = color,
                onToggleFreeze = { frozen = !frozen },
                onColor = onColorChange,
                onError = { cameraError = it },
            )
        }

        // When frozen, screen readers announce the result
        Box(
            Modifier.semantics {
                if (frozen) liveRegion = LiveRegionMode.Polite
            },
        ) {
            ColorHeroCard(analysis)
        }

        OutlinedButton(onClick = onOpenInWheel, modifier = Modifier.fillMaxWidth()) {
            Icon(painterResource(R.drawable.ic_palette), contentDescription = null)
            Spacer(Modifier.width(8.dp))
            AText("Adjust on the color wheel")
        }

        MatchingColorsCard(analysis, onColorClick = { frozen = true; onColorChange(it) })
        CompositionCard(analysis)
    }
}

@Composable
private fun CameraCard(
    frozen: Boolean,
    color: RgbColor,
    onToggleFreeze: () -> Unit,
    onColor: (RgbColor) -> Unit,
    onError: (String) -> Unit,
) {
    Box(
        Modifier
            .fillMaxWidth()
            .aspectRatio(4f / 3f)
            .clip(RoundedCornerShape(24.dp))
            .background(Color.Black)
            .border(2.dp, MaterialTheme.colorScheme.outline, RoundedCornerShape(24.dp)),
    ) {
        CameraColorPreview(
            frozen = frozen,
            onColor = onColor,
            onError = onError,
            modifier = Modifier
                .fillMaxSize()
                .semantics { contentDescription = "Camera view" },
        )
        Crosshair(color, Modifier.align(Alignment.Center))

        // "Live" / "Frozen" label in the top-left corner
        Row(
            Modifier
                .align(Alignment.TopStart)
                .padding(12.dp)
                .clip(RoundedCornerShape(50))
                .background(Color.Black.copy(alpha = 0.65f))
                .padding(horizontal = 12.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(
                Modifier
                    .size(10.dp)
                    .clip(CircleShape)
                    .background(if (frozen) Color(0xFF64B5F6) else Color(0xFFEF5350)),
            )
            Spacer(Modifier.width(8.dp))
            AText(if (frozen) "Frozen" else "Live", color = Color.White, style = MaterialTheme.typography.labelLarge)
        }

        // Freeze / resume button in the bottom-right corner
        FilledIconButton(
            onClick = onToggleFreeze,
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(12.dp)
                .size(64.dp),
            colors = IconButtonDefaults.filledIconButtonColors(
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary,
            ),
        ) {
            Icon(
                painterResource(if (frozen) R.drawable.ic_play else R.drawable.ic_pause),
                contentDescription = if (frozen) "Resume camera" else "Freeze color",
                modifier = Modifier.size(32.dp),
            )
        }
    }
}

/** A ring in the middle of the camera, filled with the color it sees (black + white edge shows on anything). */
@Composable
private fun Crosshair(color: RgbColor, modifier: Modifier = Modifier) {
    val fill = color.toComposeColor()
    Canvas(modifier.size(72.dp)) {
        val r = 26.dp.toPx()
        drawCircle(Color.Black, radius = r + 3.dp.toPx(), style = Stroke(3.dp.toPx()))
        drawCircle(Color.White, radius = r, style = Stroke(4.dp.toPx()))
        // Small swatch dot in the center showing the detected color
        drawCircle(Color.White, radius = 9.dp.toPx())
        drawCircle(fill, radius = 7.dp.toPx())
    }
}

/** Shows the live camera and reports the average color in the middle of each frame. */
@Composable
private fun CameraColorPreview(
    frozen: Boolean,
    onColor: (RgbColor) -> Unit,
    onError: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val currentFrozen by rememberUpdatedState(frozen)
    val currentOnColor by rememberUpdatedState(onColor)
    val currentOnError by rememberUpdatedState(onError)
    val previewView = remember {
        PreviewView(context).apply { scaleType = PreviewView.ScaleType.FILL_CENTER }
    }

    DisposableEffect(lifecycleOwner) {
        val mainExecutor = ContextCompat.getMainExecutor(context)
        val providerFuture = ProcessCameraProvider.getInstance(context)
        var disposed = false

        providerFuture.addListener({
            if (disposed) return@addListener
            try {
                val provider = providerFuture.get()
                val preview = Preview.Builder().build()
                preview.setSurfaceProvider(previewView.surfaceProvider)

                val analysis = ImageAnalysis.Builder()
                    .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                    .setOutputImageFormat(ImageAnalysis.OUTPUT_IMAGE_FORMAT_RGBA_8888)
                    .build()

                var lastUpdate = 0L
                analysis.setAnalyzer(mainExecutor) { image ->
                    val now = System.currentTimeMillis()
                    // Update 4 times per second so the text isn't flickering
                    if (!currentFrozen && now - lastUpdate > 250) {
                        lastUpdate = now
                        currentOnColor(averageCenterColor(image))
                    }
                    image.close()
                }

                val selector = if (provider.hasCamera(CameraSelector.DEFAULT_BACK_CAMERA)) {
                    CameraSelector.DEFAULT_BACK_CAMERA
                } else {
                    CameraSelector.DEFAULT_FRONT_CAMERA
                }
                provider.unbindAll()
                provider.bindToLifecycle(lifecycleOwner, selector, preview, analysis)
            } catch (e: Exception) {
                Log.e("IdentifyScreen", "Camera failed", e)
                currentOnError(e.message ?: "No camera found")
            }
        }, mainExecutor)

        onDispose {
            disposed = true
            if (providerFuture.isDone) providerFuture.get().unbindAll()
        }
    }

    AndroidView(factory = { previewView }, modifier = modifier)
}

/** Averages a small square of pixels in the center of the camera frame (less noisy than 1 pixel). */
private fun averageCenterColor(image: ImageProxy): RgbColor {
    val plane = image.planes[0]
    val buffer = plane.buffer
    val rowStride = plane.rowStride
    val pixelStride = plane.pixelStride   // 4 bytes per pixel: R, G, B, A
    val cx = image.width / 2
    val cy = image.height / 2
    val radius = 5

    var r = 0L
    var g = 0L
    var b = 0L
    var count = 0
    for (y in (cy - radius)..(cy + radius)) {
        for (x in (cx - radius)..(cx + radius)) {
            val i = y * rowStride + x * pixelStride
            r += buffer.get(i).toInt() and 0xFF
            g += buffer.get(i + 1).toInt() and 0xFF
            b += buffer.get(i + 2).toInt() and 0xFF
            count++
        }
    }
    return RgbColor((r / count).toInt(), (g / count).toInt(), (b / count).toInt())
}
