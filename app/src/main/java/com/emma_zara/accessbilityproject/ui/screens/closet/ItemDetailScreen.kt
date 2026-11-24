package com.emma_zara.accessbilityproject.ui.screens.closet

import com.emma_zara.accessbilityproject.ui.components.LocalBottomBarSpace
import android.content.ActivityNotFoundException
import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.emma_zara.accessbilityproject.R
import com.emma_zara.accessbilityproject.closet.ClosetRepository
import com.emma_zara.accessbilityproject.closet.ClothingItem
import com.emma_zara.accessbilityproject.closet.Match
import com.emma_zara.accessbilityproject.closet.findMatches
import com.emma_zara.accessbilityproject.closet.homeGuessed
import androidx.compose.ui.graphics.Color
import com.emma_zara.accessbilityproject.closet.patternLabel
import com.emma_zara.accessbilityproject.closet.typeLabel
import com.emma_zara.accessbilityproject.color.ColorAnalysis
import com.emma_zara.accessbilityproject.color.ColorNames
import com.emma_zara.accessbilityproject.color.RgbColor
import com.emma_zara.accessbilityproject.color.analyzeColor
import com.emma_zara.accessbilityproject.color.looksAlike
import com.emma_zara.accessbilityproject.ui.components.AText
import com.emma_zara.accessbilityproject.ui.components.ExpandableCard
import com.emma_zara.accessbilityproject.ui.components.ItemTags
import com.emma_zara.accessbilityproject.ui.components.RoundIconButton
import com.emma_zara.accessbilityproject.ui.components.Tag
import com.emma_zara.accessbilityproject.ui.components.TagKind
import com.emma_zara.accessbilityproject.ui.components.Pill
import com.emma_zara.accessbilityproject.ui.components.SectionCard
import com.emma_zara.accessbilityproject.ui.components.Swatch
import com.emma_zara.accessbilityproject.ui.components.colorVisionHint
import com.emma_zara.accessbilityproject.ui.components.rememberSpeaker
import com.emma_zara.accessbilityproject.ui.components.toComposeColor
import com.emma_zara.accessbilityproject.ui.theme.LocalColorVision
import com.emma_zara.accessbilityproject.ui.theme.LocalHighContrast

/**
 * One item: its photo and color, what it goes with, and (folded away) its details to edit.
 * The most useful things are at the top; editing is at the bottom because it's needed less often.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun ItemDetailScreen(
    repo: ClosetRepository,
    itemId: String,
    onBack: () -> Unit,
    onOpen: (String) -> Unit,
    onOutfits: (String) -> Unit,
) {
    val item = repo.find(itemId)
    // The item was just deleted (this page is sliding away): show nothing
    if (item == null) return
    val analysis = analyzeColor(item.color)
    val matches = findMatches(item, repo.items, repo.suggestFrom)
    var confirmDelete by remember { mutableStateOf(false) }
    var showPhoto by remember { mutableStateOf(false) }
    val context = LocalContext.current
    val vision = LocalColorVision.current
    // Closet items that may look like the same color to the user, but aren't
    val mixUps = repo.items.filter { it.id != item.id && looksAlike(item.color, it.color, vision) }

    Column(
        Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(bottom = LocalBottomBarSpace.current)
            .padding(horizontal = 16.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        BackHeader(item.name, onBack) {
            RoundIconButton(R.drawable.ic_delete, "Delete ${item.name}", onClick = { confirmDelete = true })
        }

        // Cut to fit here; tap to see the whole photo
        Box {
            PhotoThumbnail(
                repo.photoFor(item),
                size = 1000,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(320.dp)
                    .clip(RoundedCornerShape(28.dp))
                    .clickable(role = Role.Button, onClickLabel = "See the whole photo") { showPhoto = true }
                    .semantics { contentDescription = "Photo of ${item.name}" },
            )
            Surface(
                shape = RoundedCornerShape(50),
                color = Color.Black.copy(alpha = 0.55f),
                modifier = Modifier.align(Alignment.BottomEnd).padding(12.dp).clearAndSetSemantics {},
            ) {
                AText(
                    "Tap to see it all",
                    style = MaterialTheme.typography.labelMedium,
                    color = Color.White,
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                )
            }
        }
        if (showPhoto) FullPhotoViewer(repo.photoFor(item), item.name) { showPhoto = false }

        ColorSummary(item, analysis)

        // What to do next
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            if (item.wished) {
                Button(onClick = { repo.markBought(item) }, modifier = Modifier.fillMaxWidth().heightIn(min = 56.dp)) {
                    AText("I bought it: move to closet")
                }
            }
            val outfitsButton: @Composable () -> Unit = { AText("Outfit ideas with this") }
            if (item.wished) {
                OutlinedButton(onClick = { onOutfits(item.id) }, modifier = Modifier.fillMaxWidth().heightIn(min = 56.dp)) { outfitsButton() }
            } else {
                Button(onClick = { onOutfits(item.id) }, modifier = Modifier.fillMaxWidth().heightIn(min = 56.dp)) { outfitsButton() }
            }
            item.link?.let { url ->
                val host = Uri.parse(url).host?.removePrefix("www.") ?: "the website"
                OutlinedButton(
                    onClick = {
                        try {
                            context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url)))
                        } catch (e: ActivityNotFoundException) {
                            Toast.makeText(context, "No browser found to open the link.", Toast.LENGTH_LONG).show()
                        }
                    },
                    modifier = Modifier.fillMaxWidth().heightIn(min = 56.dp),
                ) { AText("Open on $host") }
            }
        }

        WearItWith(repo, item, matches, onOpen)

        if (mixUps.isNotEmpty()) {
            SectionCard(title = "Easy to mix up") {
                AText(
                    "With your color vision, these may look like the same color as ${item.name}, but they're really different:",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                mixUps.forEach { other ->
                    Column {
                        ItemRow(repo, other) { onOpen(other.id) }
                        AText(
                            "${ColorNames.nearest(item.color).name} vs ${ColorNames.nearest(other.color).name}",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(start = 76.dp),
                        )
                    }
                }
            }
        }

        ShoppingColors(analysis)

        ExpandableCard(
            title = "Edit details",
            summary = detailsSummary(item),
        ) {
            AText("Type", style = MaterialTheme.typography.titleMedium)
            TypeChips(repo, item.category, item.typeName) { c, t -> repo.update(item.copy(category = c, typeName = t)) }
            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
            AText("Pattern", style = MaterialTheme.typography.titleMedium)
            PatternChips(repo, item.pattern, item.patternName) { p, n -> repo.update(item.copy(pattern = p, patternName = n)) }
            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
            AText("When to wear it", style = MaterialTheme.typography.titleMedium)
            AText(
                "Helps the Outfits tab suggest it at the right time. Leave it all off if it's fine for anything.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            if (item.homeGuessed) {
                AText(
                    "This looks like something you wear at home (from its name), so it's only paired with other " +
                        "home clothes. Choose occasions below to change that.",
                    style = MaterialTheme.typography.bodyMedium,
                )
            }
            WhenToWearChips(item.occasions, item.warmth) { o, w -> repo.update(item.copy(occasions = o, warmth = w)) }
            if (item.extraColors.isNotEmpty()) {
                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                AText("Other colors on it", style = MaterialTheme.typography.titleMedium)
                AText(
                    "These are checked too when matching. Remove one if it's really the background or a shadow.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                item.extraColors.forEach { c ->
                    val colorName = ColorNames.nearest(c).name
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Swatch(c, Modifier.size(40.dp), corner = 10)
                        Spacer(Modifier.width(12.dp))
                        AText(colorName, style = MaterialTheme.typography.bodyLarge, modifier = Modifier.weight(1f))
                        TextButton(
                            onClick = { repo.update(item.copy(extraColors = item.extraColors - c)) },
                            modifier = Modifier.semantics { contentDescription = "Remove $colorName" },
                        ) { AText("Remove") }
                    }
                }
            }
        }
        Spacer(Modifier.size(8.dp))
    }

    if (confirmDelete) {
        AlertDialog(
            onDismissRequest = { confirmDelete = false },
            title = { AText("Delete ${item.name}?") },
            text = { AText("This removes it and its photo from your ${if (item.wished) "wish list" else "closet"}.") },
            confirmButton = {
                TextButton(onClick = {
                    confirmDelete = false
                    repo.delete(item)
                    onBack()
                }) { AText("Delete") }
            },
            dismissButton = { TextButton(onClick = { confirmDelete = false }) { AText("Cancel") } },
        )
    }
}

/** Like "Shirt · Plain · School, Formal · Any weather", shown on the closed "Edit details" card. */
private fun detailsSummary(item: ClothingItem): String = listOf(
    item.typeLabel,
    item.patternLabel,
    when {
        item.homeGuessed -> "Home"
        item.occasions.isEmpty() -> "Any occasion"
        else -> item.occasions.sortedBy { it.ordinal }.joinToString { it.label }
    },
    item.warmth?.label ?: "Any weather",
).joinToString(" · ")

/** The item's color at a glance: a big dot, the name, the key facts, and what to know about it. */
@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun ColorSummary(item: ClothingItem, analysis: ColorAnalysis) {
    val vision = LocalColorVision.current
    val seenAs = colorVisionHint(analysis, vision)
    val speaker = rememberSpeaker()
    val colorNames = (listOf(item.color) + item.extraColors).map { ColorNames.nearest(it).name }.distinct()
    SectionCard {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.semantics(mergeDescendants = true) {
                contentDescription = "Color: ${colorNames.joinToString(" and ")}. ${analysis.lightness.label}, " +
                    "${analysis.saturation.label}, ${analysis.temperature.label}. Hex ${item.color.hex}"
            },
        ) {
            Box(Modifier.size(64.dp)) {
                ColorDisc(item.color, Modifier.size(64.dp))
                // Other colors (like the white in stripes) as a small dot on the edge
                item.extraColors.firstOrNull()?.let { ColorDisc(it, Modifier.size(26.dp).align(Alignment.BottomEnd)) }
            }
            Spacer(Modifier.width(16.dp))
            Column(Modifier.weight(1f)) {
                AText(colorNames.joinToString(" & "), style = MaterialTheme.typography.headlineSmall)
                AText(item.color.hex, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
        // About the color (gray labels), then about the piece (colored by kind)
        FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Tag(analysis.lightness.label, TagKind.COLOR)
            Tag(analysis.saturation.label, TagKind.COLOR)
            Tag(analysis.temperature.label, TagKind.COLOR)
        }
        ItemTags(item, showPlain = true)
        AText(analysis.description, style = MaterialTheme.typography.bodyLarge)
        seenAs?.let {
            AText(it, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        TextButton(
            onClick = { speaker.speak(listOfNotNull("${item.name}.", analysis.description, seenAs).joinToString(" ")) },
            contentPadding = PaddingValues(horizontal = 4.dp),
            modifier = Modifier.heightIn(min = 48.dp),
        ) {
            Icon(painterResource(R.drawable.ic_volume), contentDescription = null)
            Spacer(Modifier.width(8.dp))
            AText("Read aloud")
        }
    }
}

@Composable
private fun ColorDisc(color: RgbColor, modifier: Modifier) {
    Box(
        modifier
            .clip(CircleShape)
            .background(color.toComposeColor())
            .border(3.dp, MaterialTheme.colorScheme.surfaceContainer, CircleShape)
            .border(4.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.4f), CircleShape),
    )
}

/** Closet pieces that go with this one, as cards you can swipe through sideways. */
@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun WearItWith(repo: ClosetRepository, item: ClothingItem, matches: List<Match>, onOpen: (String) -> Unit) {
    var choosing by remember { mutableStateOf(false) }
    SectionCard(title = if (item.wished) "Would go with" else "Wear it with") {
        AText(
            when {
                matches.isEmpty() -> when {
                    repo.items.count { it.id != item.id } == 0 -> "Add more clothes to see what goes with this."
                    repo.suggestFrom.none { it != item.category } -> "Choose at least one category below."
                    else -> "Nothing in your closet matches by color yet. Neutrals like white, black, gray, beige or navy go with almost anything."
                }
                matches.size == 1 -> "1 piece in your closet. Swipe or tap to open."
                else -> "${matches.size} pieces in your closet. Swipe sideways to see them all."
            },
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        if (item.pattern.busy) {
            AText(
                "It has a pattern (${item.patternLabel.lowercase()}), so only plain pieces are shown. Two patterns can clash.",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        if (matches.isNotEmpty()) {
            LazyRow(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                items(matches, key = { it.item.id }) { m -> MatchTile(repo, item, m) { onOpen(m.item.id) } }
            }
        }
        TextButton(onClick = { choosing = !choosing }, modifier = Modifier.heightIn(min = 48.dp)) {
            AText(if (choosing) "Done choosing categories" else "Choose categories (${repo.suggestFrom.count { it != item.category }} on)")
        }
        if (choosing) SuggestFromChips(repo, hide = item.category)
    }
}

/** One matching piece: its photo, name, and how it matches (and a warning if needed). */
@Composable
private fun MatchTile(repo: ClosetRepository, base: ClothingItem, match: Match, onClick: () -> Unit) {
    val vision = LocalColorVision.current
    val warning = when {
        looksAlike(base.color, match.item.color, vision) -> "May look like the same color to you"
        match.lowContrast -> "Similar lightness: may blur together"
        else -> null
    }
    Column(
        Modifier
            .width(140.dp)
            .clip(RoundedCornerShape(16.dp))
            .clickable(role = Role.Button, onClickLabel = "Open ${match.item.name}", onClick = onClick)
            .semantics(mergeDescendants = true) {},
        verticalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        Box {
            PhotoThumbnail(
                repo.photoFor(match.item),
                Modifier
                    .size(140.dp)
                    .clip(RoundedCornerShape(16.dp))
                    .clearAndSetSemantics {},
            )
            Surface(
                shape = RoundedCornerShape(50),
                color = MaterialTheme.colorScheme.surfaceContainer,
                border = if (LocalHighContrast.current) BorderStroke(1.5.dp, MaterialTheme.colorScheme.outline) else null,
                modifier = Modifier.align(Alignment.TopStart).padding(8.dp),
            ) {
                AText(
                    match.type.label,
                    style = MaterialTheme.typography.labelMedium,
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                )
            }
        }
        AText(match.item.name, style = MaterialTheme.typography.titleSmall, maxLines = 2, modifier = Modifier.padding(horizontal = 4.dp))
        AText(
            ColorNames.nearest(match.item.color).name,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(horizontal = 4.dp),
        )
        warning?.let {
            AText(
                "Note: $it",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(horizontal = 4.dp),
            )
        }
    }
}

/** Colors that would go with this piece, to look for when shopping. Folded away until opened. */
@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun ShoppingColors(analysis: ColorAnalysis) {
    val options = listOf(
        "Opposite" to analysis.complementary,
        "Neighbor" to analysis.analogous[0],
        "Neighbor" to analysis.analogous[1],
        "Triangle" to analysis.triadic[0],
        "Triangle" to analysis.triadic[1],
    )
    ExpandableCard(
        title = "Colors to shop for",
        summary = options.map { ColorNames.nearest(it.second).name }.distinct().joinToString(", "),
    ) {
        AText(
            "Opposite colors stand out, neighbors look calm, and triangle colors look playful.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        FlowRow(horizontalArrangement = Arrangement.spacedBy(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            options.forEach { (kind, c) ->
                Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.semantics(mergeDescendants = true) {}) {
                    ColorDisc(c, Modifier.size(52.dp))
                    AText(ColorNames.nearest(c).name, style = MaterialTheme.typography.labelLarge)
                    AText(kind, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        }
    }
}
