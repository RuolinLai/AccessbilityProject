package com.emma_zara.accessbilityproject.ui.screens.closet

import com.emma_zara.accessbilityproject.ui.components.LocalBottomBarSpace
import android.Manifest
import android.content.ActivityNotFoundException
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.net.Uri
import android.util.Log
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
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
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import android.content.ClipboardManager
import android.content.Context
import com.emma_zara.accessbilityproject.closet.LinkProblem
import com.emma_zara.accessbilityproject.closet.Occasion
import com.emma_zara.accessbilityproject.closet.Warmth
import com.emma_zara.accessbilityproject.closet.ProductLink
import com.emma_zara.accessbilityproject.closet.SharedItem
import com.emma_zara.accessbilityproject.ui.components.TappablePhoto
import com.emma_zara.accessbilityproject.ui.components.rememberPhotoPicker
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
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

/**
 * Add a piece of clothing: take or choose a photo, and the app finds the item's color (and, if it can,
 * its category) by itself. The color is always shown as a name, so you don't need to see colors to use it.
 *
 * With [shopping] = true this is the "shopping check": take a photo of something in a store and see
 * what it goes with in your closet before you buy it. It is only saved if you choose to.
 *
 * The photo can also come from a shopping website link (pasted here, or [shared] from another app).
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun AddItemScreen(
    repo: ClosetRepository,
    onBack: () -> Unit,
    onSaved: (String) -> Unit,
    shopping: Boolean = false,
    shared: SharedItem? = null,
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    var photo by remember { mutableStateOf<Bitmap?>(null) }
    var loading by remember { mutableStateOf(false) }
    var loadingText by remember { mutableStateOf("") }
    var linkText by remember { mutableStateOf("") }
    // The product page the photo came from, saved with the item so it can be opened again
    var link by remember { mutableStateOf<String?>(null) }
    var error by remember { mutableStateOf<String?>(null) }
    var detectedColors by remember { mutableStateOf<List<RgbColor>>(emptyList()) }
    var color by remember { mutableStateOf<RgbColor?>(null) }
    // Which of the other found colors are really on the item (like the white in stripes)
    var extraOn by remember { mutableStateOf<Set<RgbColor>>(emptySet()) }
    var tapPoint by remember { mutableStateOf<Offset?>(null) }
    var category by remember { mutableStateOf<Category?>(null) }
    var foundItem by remember { mutableStateOf(true) }
    var guessedCategory by remember { mutableStateOf<Category?>(null) }
    var pattern by remember { mutableStateOf(Pattern.SOLID) }
    var typeName by remember { mutableStateOf<String?>(null) }      // the user's own type, if chosen
    var patternName by remember { mutableStateOf<String?>(null) }   // the user's own pattern, if chosen
    var name by remember { mutableStateOf("") }
    var occasions by remember { mutableStateOf<Set<Occasion>>(emptySet()) }
    var warmth by remember { mutableStateOf<Warmth?>(null) }

    // A website that only works in a browser: (address, why it was opened). Shows the app's browser.
    var browser by remember { mutableStateOf<Pair<String, String?>?>(null) }

    // Loads the photo in the background (with [load]), then finds the item, its colors and its category.
    // [fromLink] and [title] are set when the photo came from a shopping website.
    fun scanPhoto(load: () -> Bitmap?, fromLink: String? = null, title: String? = null) {
        loading = true
        loadingText = "Finding the item and its color…"
        error = null
        scope.launch {
            val result = withContext(Dispatchers.IO) {
                try {
                    load()?.let { it to ItemScanner.scan(it) }
                } catch (e: Throwable) {
                    // Never crash on a strange photo (for example one that is too big for memory)
                    Log.e("AddItemScreen", "Couldn't use photo", e)
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
                // Start with the other colors turned on if the item was cut out cleanly (so they aren't
                // the background), skipping shades with the same name as the main color (often just shadows)
                val mainName = color?.let { ColorNames.nearest(it).name }
                extraOn = if (scan.foundItem) {
                    scan.colors.drop(1).filter { ColorNames.nearest(it).name != mainName }.toSet()
                } else {
                    emptySet()
                }
                foundItem = scan.foundItem
                guessedCategory = scan.category
                category = scan.category
                pattern = scan.pattern
                typeName = null
                patternName = null
                tapPoint = null
                link = fromLink
                if (title != null) name = title
            }
        }
    }

    fun usePhoto(uri: Uri, fromLink: String? = null, title: String? = null) =
        scanPhoto({ Photos.load(context, uri) }, fromLink, title)

    // The user tapped "Use this product" in the app's browser: download the product photo, or if that
    // doesn't work, use the picture of what was on the screen
    fun usePick(pick: BrowserPick) {
        browser = null
        scanPhoto(
            load = {
                val file = pick.imageUrl?.let { url ->
                    runCatching {
                        ProductLink.downloadImage(url, File(context.cacheDir, "shop"), pick.userAgent, pick.cookies)
                    }.getOrNull()
                }
                file?.let { Photos.load(context, Uri.fromFile(it)) } ?: pick.screenshot
            },
            fromLink = pick.pageUrl,
            title = pick.title,
        )
    }

    // Downloads the product page, finds its photo, then uses it like a photo from the gallery
    fun useLink(text: String) {
        val url = ProductLink.findUrl(text)
        if (url == null) {
            error = "That doesn't look like a link. Copy the link from the shopping website or app and paste it here."
            return
        }
        loading = true
        loadingText = "Getting the photo from the website…"
        error = null
        scope.launch {
            val result = withContext(Dispatchers.IO) {
                try {
                    Result.success(ProductLink.fetch(url, File(context.cacheDir, "shop")))
                } catch (e: Throwable) {
                    if (e !is LinkProblem) Log.e("AddItemScreen", "Couldn't use link $url", e)
                    Result.failure(e)
                }
            }
            result.onSuccess { page ->
                usePhoto(Uri.fromFile(page.imageFile), fromLink = page.pageUrl, title = page.title)
            }.onFailure { e ->
                loading = false
                val browserUrl = (e as? LinkProblem)?.browserUrl
                if (browserUrl != null) {
                    browser = browserUrl to e.message
                } else {
                    error = (e as? LinkProblem)?.message
                        ?: "Couldn't get a photo from that link. Save the picture and choose it from the gallery instead."
                }
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
    fun takePhoto() {
        val granted = ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) ==
            PackageManager.PERMISSION_GRANTED
        if (granted) launchCamera() else cameraPermission.launch(Manifest.permission.CAMERA)
    }
    val choosePhoto = rememberPhotoPicker(
        onPicked = { usePhoto(it) },
        onError = { error = "$it Try taking a photo instead." },
    )

    // Something shared from another app (a product link or a picture): use it right away
    LaunchedEffect(shared) {
        when (shared) {
            is SharedItem.Link -> {
                linkText = shared.text
                useLink(shared.text)
            }
            is SharedItem.Image -> usePhoto(shared.uri)
            null -> Unit
        }
    }

    browser?.let { (url, message) ->
        ProductBrowser(url, message, onClose = { browser = null }, onPick = ::usePick)
        return
    }

    Column(
        Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(bottom = LocalBottomBarSpace.current)
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
                    loadingText,
                    style = MaterialTheme.typography.bodyLarge,
                    modifier = Modifier.semantics { liveRegion = LiveRegionMode.Polite },
                )
            }

            currentPhoto == null -> Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                SectionCard(title = if (shopping) "Thinking of buying something?" else "Step 1: Add a photo") {
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
                    OutlinedButton(onClick = choosePhoto, modifier = Modifier.fillMaxWidth().heightIn(min = 56.dp)) {
                        Icon(painterResource(R.drawable.ic_image), contentDescription = null)
                        Spacer(Modifier.width(8.dp))
                        AText("Choose from gallery")
                    }
                }
                LinkCard(
                    text = linkText,
                    onTextChange = { linkText = it },
                    onGo = { useLink(linkText) },
                    onPaste = {
                        val pasted = pasteText(context)
                        if (pasted.isNullOrBlank()) {
                            error = "There's nothing to paste. Copy the product's link first."
                        } else {
                            linkText = pasted
                            useLink(pasted)
                        }
                    },
                )
            }

            else -> {
                SectionCard(title = "Photo") {
                    TappablePhoto(
                        bitmap = currentPhoto,
                        tap = tapPoint,
                        description = "Photo of the item. Tap to pick a color.",
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
                    link?.let { url ->
                        AText(
                            "From ${Uri.parse(url).host?.removePrefix("www.") ?: "a website"}",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                    FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        TextButton(onClick = ::takePhoto) { AText("Retake photo") }
                        TextButton(onClick = choosePhoto) { AText("Choose another") }
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
                    val others = detectedColors.filter { it != color }
                    if (others.isNotEmpty()) {
                        AText(
                            "Other colors on it: turn on the ones that are really part of the item (like stripes " +
                                "or a print), and off for the background or shadows. They're checked when matching.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                        FlowRow(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalArrangement = Arrangement.spacedBy(4.dp),
                        ) {
                            others.forEach { c ->
                                val on = c in extraOn
                                FilterChip(
                                    selected = on,
                                    onClick = { extraOn = if (on) extraOn - c else extraOn + c },
                                    label = { AText(ColorNames.nearest(c).name) },
                                    leadingIcon = {
                                        Box(
                                            Modifier
                                                .size(FilterChipDefaults.IconSize)
                                                .clip(RoundedCornerShape(4.dp))
                                                .background(c.toComposeColor())
                                                .border(1.dp, MaterialTheme.colorScheme.outline, RoundedCornerShape(4.dp)),
                                        )
                                    },
                                    trailingIcon = if (on) {
                                        { Icon(painterResource(R.drawable.ic_check), null, Modifier.size(FilterChipDefaults.IconSize)) }
                                    } else {
                                        null
                                    },
                                )
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

                val extras = detectedColors.filter { it != color && it in extraOn }
                val defaultName = "${color?.let { ColorNames.nearest(it).name } ?: ""} ${typeName ?: category?.singular ?: "item"}".trim()

                val c = color
                val cat = category
                if (shopping && c != null && cat != null) {
                    val newItem = ClothingItem(
                        NEW_ITEM_ID, "This new ${(typeName ?: cat.singular).lowercase()}", cat, c, "", 0,
                        pattern, typeName, patternName, extraColors = extras,
                    )
                    ShoppingResults(repo, newItem, currentPhoto)
                }
                WhenToWearCard(occasions, warmth, title = "When to wear it (optional)") { o, w ->
                    occasions = o
                    warmth = w
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
                        val item = repo.add(
                            name.trim().ifEmpty { defaultName }, category!!, color!!, pattern, currentPhoto,
                            typeName, patternName, extraColors = extras, link = link,
                            occasions = occasions, warmth = warmth,
                        )
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
                if (shopping && ready) {
                    OutlinedButton(
                        onClick = {
                            val item = repo.add(
                                name.trim().ifEmpty { defaultName }, category!!, color!!, pattern, currentPhoto,
                                typeName, patternName, wished = true, extraColors = extras, link = link,
                                occasions = occasions, warmth = warmth,
                            )
                            onSaved(item.id)
                        },
                        modifier = Modifier.fillMaxWidth().heightIn(min = 56.dp),
                    ) { AText("Not yet: save to wish list") }
                }
                Spacer(Modifier.size(8.dp))
            }
        }
    }
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
        matches.forEach { m -> MatchRow(repo, newItem, m, reason = "${m.type.label}: ${m.type.reason}") }
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

/** Paste a link to a product on a shopping website, and the app gets its photo. */
@Composable
private fun LinkCard(text: String, onTextChange: (String) -> Unit, onGo: () -> Unit, onPaste: () -> Unit) {
    SectionCard(title = "Or use a shopping website") {
        AText(
            "Copy the link to the product on the website or in the shopping app, then paste it here. " +
                "You can also press Share on a product and choose Color Closet.",
            style = MaterialTheme.typography.bodyLarge,
        )
        OutlinedTextField(
            value = text,
            onValueChange = onTextChange,
            label = { AText("Product link") },
            placeholder = { AText("https://…") },
            singleLine = true,
            textStyle = MaterialTheme.typography.bodyLarge,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Uri, imeAction = ImeAction.Go),
            keyboardActions = KeyboardActions(onGo = { if (text.isNotBlank()) onGo() }),
            modifier = Modifier.fillMaxWidth(),
        )
        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            OutlinedButton(onClick = onPaste, modifier = Modifier.heightIn(min = 56.dp)) { AText("Paste link") }
            Button(onClick = onGo, enabled = text.isNotBlank(), modifier = Modifier.heightIn(min = 56.dp)) {
                AText("Get the photo")
            }
        }
    }
}

/** The text the user copied last, or null. */
private fun pasteText(context: Context): String? {
    val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as? ClipboardManager ?: return null
    val clip = clipboard.primaryClip ?: return null
    if (clip.itemCount == 0) return null
    return clip.getItemAt(0).coerceToText(context)?.toString()
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
