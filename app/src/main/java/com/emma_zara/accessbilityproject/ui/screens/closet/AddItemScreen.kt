package com.emma_zara.accessbilityproject.ui.screens.closet

import android.Manifest
import android.content.ActivityNotFoundException
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import com.emma_zara.accessbilityproject.R
import com.emma_zara.accessbilityproject.closet.Category
import com.emma_zara.accessbilityproject.closet.ClosetRepository
import com.emma_zara.accessbilityproject.closet.Photos
import com.emma_zara.accessbilityproject.color.ColorNames
import com.emma_zara.accessbilityproject.color.RgbColor
import com.emma_zara.accessbilityproject.color.analyzeColor
import com.emma_zara.accessbilityproject.ui.components.AText
import com.emma_zara.accessbilityproject.ui.components.ColorHeroCard
import com.emma_zara.accessbilityproject.ui.components.SectionCard
import com.emma_zara.accessbilityproject.ui.components.toComposeColor
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import kotlin.math.min

/**
 * Add a piece of clothing: take or choose a photo, check its color, pick a category, save.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun AddItemScreen(repo: ClosetRepository, onBack: () -> Unit, onSaved: (String) -> Unit) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    var photo by remember { mutableStateOf<Bitmap?>(null) }
    var loading by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }
    var detectedColors by remember { mutableStateOf<List<RgbColor>>(emptyList()) }
    var color by remember { mutableStateOf<RgbColor?>(null) }
    var tapPoint by remember { mutableStateOf<Offset?>(null) }
    var category by remember { mutableStateOf<Category?>(null) }
    var name by remember { mutableStateOf("") }

    // Loads the photo in the background, then finds its main colors
    fun usePhoto(uri: Uri) {
        loading = true
        error = null
        scope.launch {
            val result = withContext(Dispatchers.IO) {
                Photos.load(context, uri)?.let { it to Photos.mainColors(it) }
            }
            loading = false
            if (result == null) {
                error = "Couldn't open that photo. Please try another one."
            } else {
                photo = result.first
                detectedColors = result.second
                color = result.second.firstOrNull()
                tapPoint = null
            }
        }
    }

    // The camera app saves the new photo into this file
    val cameraUri = remember {
        val dir = File(context.cacheDir, "camera").apply { mkdirs() }
        FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", File(dir, "new_photo.jpg"))
    }
    val takePicture = rememberLauncherForActivityResult(ActivityResultContracts.TakePicture()) { ok ->
        if (ok) usePhoto(cameraUri)
    }
    fun launchCamera() {
        try {
            takePicture.launch(cameraUri)
        } catch (e: ActivityNotFoundException) {
            error = "No camera app found. Try choosing from the gallery instead."
        }
    }
    val cameraPermission = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        if (granted) launchCamera() else error = "The camera permission is needed to take a photo."
    }
    val pickImage = rememberLauncherForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri ->
        if (uri != null) usePhoto(uri)
    }

    fun takePhoto() {
        val granted = ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) ==
            PackageManager.PERMISSION_GRANTED
        if (granted) launchCamera() else cameraPermission.launch(Manifest.permission.CAMERA)
    }
    fun choosePhoto() = pickImage.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))

    Column(
        Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 16.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        BackHeader("Add clothes", onBack)

        error?.let {
            SectionCard { AText(it, style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.error) }
        }

        val currentPhoto = photo
        when {
            loading -> Box(Modifier.fillMaxWidth().padding(48.dp), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(Modifier.semantics { contentDescription = "Loading photo" })
            }

            currentPhoto == null -> SectionCard(title = "Step 1: Add a photo") {
                AText(
                    "Lay the item flat or hang it up, on a plain background, in good light. " +
                        "Try to fill the photo with the item.",
                    style = MaterialTheme.typography.bodyLarge,
                )
                Button(onClick = ::takePhoto, modifier = Modifier.fillMaxWidth().heightIn(min = 56.dp)) {
                    Icon(painterResource(R.drawable.ic_camera), contentDescription = null)
                    Spacer(Modifier.width(8.dp))
                    AText("Take a photo")
                }
                OutlinedButton(onClick = ::choosePhoto, modifier = Modifier.fillMaxWidth().heightIn(min = 56.dp)) {
                    Icon(painterResource(R.drawable.ic_image), contentDescription = null)
                    Spacer(Modifier.width(8.dp))
                    AText("Choose from gallery")
                }
            }

            else -> {
                SectionCard(title = "Photo") {
                    TappablePhoto(
                        bitmap = currentPhoto,
                        tapPoint = tapPoint,
                        onTap = { point, picked ->
                            tapPoint = point
                            color = picked
                        },
                    )
                    AText(
                        "Tap the item in the photo to pick its exact color.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        TextButton(onClick = ::takePhoto) { AText("Retake photo") }
                        TextButton(onClick = ::choosePhoto) { AText("Choose another") }
                    }
                }

                SectionCard(title = "Step 2: Check the color") {
                    AText("Colors found in the photo:", style = MaterialTheme.typography.bodyLarge)
                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp),
                    ) {
                        detectedColors.forEach { c ->
                            ColorChoice(c, selected = c == color) {
                                color = c
                                tapPoint = null
                            }
                        }
                    }
                }
                color?.let { ColorHeroCard(analyzeColor(it), showDescription = false) }

                SectionCard(title = "Step 3: Category") {
                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(4.dp),
                    ) {
                        Category.entries.forEach { c ->
                            FilterChip(selected = category == c, onClick = { category = c }, label = { AText(c.label) })
                        }
                    }
                }

                val defaultName = "${color?.let { ColorNames.nearest(it).name } ?: ""} ${category?.singular ?: "item"}".trim()
                SectionCard(title = "Name (optional)") {
                    OutlinedTextField(
                        value = name,
                        onValueChange = { name = it },
                        placeholder = { AText(defaultName) },
                        singleLine = true,
                        textStyle = MaterialTheme.typography.bodyLarge,
                        modifier = Modifier.fillMaxWidth(),
                    )
                }

                val ready = category != null && color != null
                Button(
                    onClick = {
                        val item = repo.add(name.trim().ifEmpty { defaultName }, category!!, color!!, currentPhoto)
                        onSaved(item.id)
                    },
                    enabled = ready,
                    modifier = Modifier.fillMaxWidth().heightIn(min = 56.dp),
                ) {
                    AText(if (ready) "Save to closet" else "Choose a category to save")
                }
                Spacer(Modifier.size(8.dp))
            }
        }
    }
}

/** The photo, sized to fit. Tapping it reads the color under your finger. */
@Composable
private fun TappablePhoto(bitmap: Bitmap, tapPoint: Offset?, onTap: (Offset, RgbColor) -> Unit) {
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
                        val x = (pos.x / size.width * bitmap.width).toInt().coerceIn(0, bitmap.width - 1)
                        val y = (pos.y / size.height * bitmap.height).toInt().coerceIn(0, bitmap.height - 1)
                        onTap(pos, Photos.colorAt(bitmap, x, y))
                    }
                }
                .semantics { contentDescription = "Photo of the item. Tap to pick a color." },
        ) {
            Image(image, contentDescription = null, contentScale = ContentScale.FillBounds, modifier = Modifier.fillMaxSize())
            if (tapPoint != null) {
                Canvas(Modifier.fillMaxSize()) {
                    drawCircle(Color.Black, 20.dp.toPx(), tapPoint, style = Stroke(5.dp.toPx()))
                    drawCircle(Color.White, 20.dp.toPx(), tapPoint, style = Stroke(2.5.dp.toPx()))
                }
            }
        }
    }
}

/** A color the user can choose, with its name underneath. */
@Composable
private fun ColorChoice(color: RgbColor, selected: Boolean, onClick: () -> Unit) {
    val name = ColorNames.nearest(color).name
    Column(
        Modifier
            .clip(RoundedCornerShape(12.dp))
            .selectable(selected = selected, role = Role.RadioButton, onClick = onClick)
            .padding(4.dp)
            .semantics(mergeDescendants = true) {},
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Box(
            Modifier
                .size(64.dp)
                .clip(RoundedCornerShape(14.dp))
                .border(
                    if (selected) 4.dp else 1.5.dp,
                    if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outline,
                    RoundedCornerShape(14.dp),
                )
                .padding(if (selected) 6.dp else 0.dp)
                .clip(RoundedCornerShape(10.dp))
                .background(color.toComposeColor()),
        )
        AText(name, style = MaterialTheme.typography.labelLarge)
    }
}
