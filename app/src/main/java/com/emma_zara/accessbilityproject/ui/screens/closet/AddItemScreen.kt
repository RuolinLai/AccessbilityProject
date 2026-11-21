package com.emma_zara.accessbilityproject.ui.screens.closet

import android.Manifest
import android.content.ActivityNotFoundException
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.net.Uri
import android.os.Build
import android.os.ext.SdkExtensions
import android.util.Log
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
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
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
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import com.emma_zara.accessbilityproject.R
import com.emma_zara.accessbilityproject.closet.Category
import com.emma_zara.accessbilityproject.closet.ClosetRepository
import com.emma_zara.accessbilityproject.closet.BackgroundRemover
import com.emma_zara.accessbilityproject.closet.ClothingItem
import com.emma_zara.accessbilityproject.closet.ItemScanner
import com.emma_zara.accessbilityproject.closet.Pattern
import com.emma_zara.accessbilityproject.closet.findMatches
import com.emma_zara.accessbilityproject.closet.suggestOutfits
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
 * Add a piece of clothing: take or choose a photo, and the app finds the item's color (and, if it can,
 * its category) by itself. The color is always shown as a name, so you don't need to see colors to use it.
 *
 * With [shopping] = true this is the "shopping check": take a photo of something in a store and see
 * what it goes with in your closet before you buy it. It is only saved if you choose to.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun AddItemScreen(repo: ClosetRepository, onBack: () -> Unit, onSaved: (String) -> Unit, shopping: Boolean = false) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    var photo by remember { mutableStateOf<Bitmap?>(null) }
    var loading by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }
    var detectedColors by remember { mutableStateOf<List<RgbColor>>(emptyList()) }
    var color by remember { mutableStateOf<RgbColor?>(null) }
    var tapPoint by remember { mutableStateOf<Offset?>(null) }
    var category by remember { mutableStateOf<Category?>(null) }
    var foundItem by remember { mutableStateOf(true) }
    var guessedCategory by remember { mutableStateOf<Category?>(null) }
    var pattern by remember { mutableStateOf(Pattern.SOLID) }
    var typeName by remember { mutableStateOf<String?>(null) }      // the user's own type, if chosen
    var patternName by remember { mutableStateOf<String?>(null) }   // the user's own pattern, if chosen
    var name by remember { mutableStateOf("") }

    // Loads the photo in the background, then finds the item, its colors and its category
    fun usePhoto(uri: Uri) {
        loading = true
        error = null
        scope.launch {
            val result = withContext(Dispatchers.IO) {
                try {
                    Photos.load(context, uri)?.let { it to ItemScanner.scan(it) }
                } catch (e: Throwable) {
                    // Never crash on a strange photo (for example one that is too big for memory)
                    Log.e("AddItemScreen", "Couldn't use photo $uri", e)
                    null
                }
            }
            loading = false
            if (result == null) {
                error = "Couldn't open that photo. Please try another one."
            } else {
                val scan = result.second
                photo = result.first
                detectedColors = scan.colors
                color = scan.colors.firstOrNull()
                foundItem = scan.foundItem
                guessedCategory = scan.category
                category = scan.category
                pattern = scan.pattern
                typeName = null
                patternName = null
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
    // Older phones (like the Galaxy S9+ on Android 10) don't have Android's photo picker. The library would
    // then use Google's backup picker, which on some phones doesn't open at all. So there we open the
    // phone's own file chooser / gallery instead.
    val getImage = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        if (uri != null) usePhoto(uri)
    }
    fun choosePhoto() {
        try {
            if (hasSystemPhotoPicker()) {
                pickImage.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
            } else {
                getImage.launch("image/*")
            }
        } catch (e: ActivityNotFoundException) {
            error = "No gallery app found. Try taking a photo instead."
        }
    }

    Column(
        Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 16.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        BackHeader(if (shopping) "Shopping check" else "Add clothes", onBack)

        BackgroundRemoverStatus()

        error?.let {
            SectionCard { AText(it, style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.error) }
        }

        val currentPhoto = photo
        when {
            loading -> Column(
                Modifier.fillMaxWidth().padding(48.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(16.dp),
            ) {
                CircularProgressIndicator()
                AText(
                    "Finding the item and its color…",
                    style = MaterialTheme.typography.bodyLarge,
                    modifier = Modifier.semantics { liveRegion = LiveRegionMode.Polite },
                )
            }

            currentPhoto == null -> SectionCard(title = if (shopping) "Thinking of buying something?" else "Step 1: Add a photo") {
                AText(
                    if (shopping) {
                        "Take a photo of it in the store. The app reads its color and pattern and shows " +
                            "what it goes with in your closet. Nothing is saved unless you choose to."
                    } else {
                        "Lay the item flat or hang it up, on a plain background, in good light. " +
                            "Try to fill the photo with the item."
                    },
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
                        "Optional: tap a spot on the item to read the color right there.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        TextButton(onClick = ::takePhoto) { AText("Retake photo") }
                        TextButton(onClick = ::choosePhoto) { AText("Choose another") }
                    }
                }

                SectionCard(title = "Color") {
                    val colorName = color?.let { ColorNames.nearest(it).name } ?: "unknown"
                    // Read out loud by screen readers whenever the color changes
                    AText(
                        when {
                            foundItem -> "Found the item in the photo. Its color is $colorName."
                            BackgroundRemover.status != BackgroundRemover.Status.Ready ->
                                "The background remover isn't ready yet (see above), so this color is from the " +
                                    "middle of the photo and might include the background: $colorName."
                            else -> "Couldn't separate the item from the background, so this might be the background's " +
                                "color: $colorName. For a better result, use a plain background and fill the photo with the item."
                        },
                        style = MaterialTheme.typography.bodyLarge,
                        color = if (foundItem) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.error,
                        modifier = Modifier.semantics { liveRegion = LiveRegionMode.Polite },
                    )
                    if (detectedColors.size > 1) {
                        AText(
                            "All colors on the item, most common first. Choose another if it has several colors:",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
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

                SectionCard(title = "Category") {
                    AText(
                        guessedCategory?.let { "This looks like ${it.label.lowercase()}. Change it if that's wrong." }
                            ?: "Choose what kind of item this is.",
                        style = MaterialTheme.typography.bodyLarge,
                    )
                    TypeChips(repo, category, typeName) { c, t ->
                        category = c
                        typeName = t
                    }
                }

                SectionCard(title = "Pattern") {
                    AText(
                        if (patternName != null) {
                            "Pattern: $patternName."
                        } else {
                            "This looks like: ${pattern.label.lowercase()} (${pattern.description.lowercase()}). " +
                                "Change it if that's wrong."
                        },
                        style = MaterialTheme.typography.bodyLarge,
                        modifier = Modifier.semantics { liveRegion = LiveRegionMode.Polite },
                    )
                    PatternChips(repo, pattern, patternName) { p, n ->
                        pattern = p
                        patternName = n
                    }
                }

                val defaultName = "${color?.let { ColorNames.nearest(it).name } ?: ""} ${typeName ?: category?.singular ?: "item"}".trim()

                val c = color
                val cat = category
                if (shopping && c != null && cat != null) {
                    val newItem = ClothingItem(
                        NEW_ITEM_ID, "This new ${(typeName ?: cat.singular).lowercase()}", cat, c, "", 0,
                        pattern, typeName, patternName,
                    )
                    ShoppingResults(repo, newItem, currentPhoto)
                }
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
                        val item = repo.add(name.trim().ifEmpty { defaultName }, category!!, color!!, pattern, currentPhoto, typeName, patternName)
                        onSaved(item.id)
                    },
                    enabled = ready,
                    modifier = Modifier.fillMaxWidth().heightIn(min = 56.dp),
                ) {
                    AText(
                        when {
                            !ready -> "Choose a category to save"
                            shopping -> "I bought it: add to closet"
                            else -> "Save to closet"
                        },
                    )
                }
                Spacer(Modifier.size(8.dp))
            }
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
 * Tells the user whether the background remover is ready, downloading, or can't be used, and why.
 * Shows nothing once it's ready.
 */
@Composable
private fun BackgroundRemoverStatus() {
    val context = LocalContext.current
    val status = BackgroundRemover.status
    if (status == BackgroundRemover.Status.Ready) return
    SectionCard(title = "Background remover") {
        val text = when (status) {
            BackgroundRemover.Status.Checking -> "Checking if it's ready…"
            is BackgroundRemover.Status.Downloading ->
                "Downloading it" + (status.percent?.let { " ($it%)" } ?: "") + "… This happens once and needs the internet."
            BackgroundRemover.Status.WaitingForInternet -> "Waiting for the internet to finish downloading it."
            is BackgroundRemover.Status.Unavailable -> status.reason
            BackgroundRemover.Status.Ready -> ""
        }
        AText(
            text,
            style = MaterialTheme.typography.bodyLarge,
            modifier = Modifier.semantics { liveRegion = LiveRegionMode.Polite },
        )
        AText(
            "It cuts the clothes out of the photo so the background's color isn't counted. " +
                "Until it's ready, the app reads the color from the middle of the photo.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        when (status) {
            is BackgroundRemover.Status.Downloading -> {
                val percent = status.percent
                if (percent != null) {
                    LinearProgressIndicator(progress = { percent / 100f }, modifier = Modifier.fillMaxWidth())
                } else {
                    LinearProgressIndicator(Modifier.fillMaxWidth())
                }
            }
            is BackgroundRemover.Status.Unavailable -> if (status.canRetry) {
                OutlinedButton(onClick = { BackgroundRemover.prepare(context) }) { AText("Try again") }
            }
            else -> Unit
        }
    }
}

/** Shopping check results: what the new item goes with, and whole outfits with it. */
@Composable
private fun ShoppingResults(repo: ClosetRepository, newItem: ClothingItem, photo: Bitmap) {
    val matches = findMatches(newItem, repo.items, repo.suggestFrom)
    val outfits = remember(repo.items, repo.suggestFrom, newItem) {
        suggestOutfits(repo.items, repo.suggestFrom, anchor = newItem, limit = 3)
    }

    SectionCard(title = "Goes with in your closet") {
        AText(
            when (matches.size) {
                0 -> "Nothing in your closet goes with it yet."
                1 -> "Goes with 1 item in your closet."
                else -> "Goes with ${matches.size} items in your closet."
            },
            style = MaterialTheme.typography.titleMedium,
            modifier = Modifier.semantics { liveRegion = LiveRegionMode.Polite },
        )
        SuggestFromChips(repo, hide = newItem.category)
        if (newItem.pattern.busy) {
            AText(
                "It has a pattern, so only plain pieces are counted. Two patterns together can clash.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        matches.forEach { m ->
            Column {
                ItemRow(repo, m.item)
                AText(
                    "${m.type.label}: ${m.type.reason}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(start = 76.dp),
                )
            }
        }
    }

    if (outfits.isNotEmpty()) {
        outfits.forEachIndexed { i, outfit ->
            OutfitCard(repo, outfit, title = "Outfit idea ${i + 1}", newPhoto = photo)
        }
    } else if (repo.items.isNotEmpty()) {
        SectionCard(title = "Outfit ideas") {
            AText(
                "No complete outfit with it yet: your closet needs pieces from the other parts of an outfit " +
                    "(a top, pants or a skirt, or a dress) in colors that go with it.",
                style = MaterialTheme.typography.bodyLarge,
            )
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
