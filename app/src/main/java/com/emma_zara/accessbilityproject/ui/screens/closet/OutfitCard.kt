package com.emma_zara.accessbilityproject.ui.screens.closet

import com.emma_zara.accessbilityproject.ui.components.panelBorder
import android.graphics.Bitmap
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.emma_zara.accessbilityproject.R
import com.emma_zara.accessbilityproject.closet.Category
import com.emma_zara.accessbilityproject.closet.ClosetRepository
import com.emma_zara.accessbilityproject.closet.ClothingItem
import com.emma_zara.accessbilityproject.closet.Outfit
import com.emma_zara.accessbilityproject.closet.reasons
import com.emma_zara.accessbilityproject.closet.typeLabel
import com.emma_zara.accessbilityproject.color.ColorNames
import com.emma_zara.accessbilityproject.ui.components.AText
import com.emma_zara.accessbilityproject.ui.components.toComposeColor
import com.emma_zara.accessbilityproject.ui.theme.LocalColorVision
import com.emma_zara.accessbilityproject.ui.theme.LocalHighContrast

private val MainCategories = setOf(Category.SHIRTS, Category.PANTS, Category.SKIRTS, Category.DRESSES)

/**
 * A whole outfit, laid out like clothes on a bed: the top and bottom (or dress) big on the left,
 * shoes, bag and jewellery smaller on the right. Under it, the outfit's colors and why it works.
 * [newPhoto] is the photo of the new (not saved) item, if the outfit has one.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun OutfitCard(
    repo: ClosetRepository,
    outfit: Outfit,
    title: String,
    modifier: Modifier = Modifier,
    newPhoto: Bitmap? = null,
    onOpen: ((String) -> Unit)? = null,
    /** Reasons shown before the color reasons, like why it suits the weather. */
    extraReasons: List<String> = emptyList(),
) {
    val reasons = extraReasons + outfit.reasons(LocalColorVision.current)
    var showAll by rememberSaveable(title, outfit.items.map { it.id }) { mutableStateOf(false) }
    val main = outfit.items.filter { it.category in MainCategories }
    val extras = outfit.items.filter { it.category !in MainCategories }

    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(24.dp),
        color = MaterialTheme.colorScheme.surfaceContainer,
        border = panelBorder(),
    ) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                AText(
                    title,
                    style = MaterialTheme.typography.titleLarge,
                    modifier = Modifier.weight(1f).semantics { heading() },
                )
                ColorDots(outfit.items)
            }

            // The picture of the outfit. Screen readers skip it: the names below say the same thing.
            Row(
                Modifier
                    .fillMaxWidth()
                    .height(if (extras.isEmpty()) 200.dp else 280.dp)
                    .clearAndSetSemantics {},
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                if (extras.isEmpty()) {
                    main.forEach { PieceTile(repo, it, newPhoto, onOpen, Modifier.weight(1f).fillMaxHeight()) }
                } else {
                    Column(Modifier.weight(1.5f).fillMaxHeight(), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        main.forEach { PieceTile(repo, it, newPhoto, onOpen, Modifier.weight(1f).fillMaxWidth()) }
                    }
                    Column(Modifier.weight(1f).fillMaxHeight(), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        extras.forEach { PieceTile(repo, it, newPhoto, onOpen, Modifier.weight(1f).fillMaxWidth()) }
                        // Keep the small pictures square-ish when there are only one or two
                        repeat(3 - extras.size) { Spacer(Modifier.weight(1f)) }
                    }
                }
            }

            // The pieces, by name (tap one to open it)
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                outfit.items.forEach { PieceChip(it, onOpen) }
            }

            if (reasons.isNotEmpty()) {
                Surface(shape = RoundedCornerShape(16.dp), color = MaterialTheme.colorScheme.surfaceContainerHigh) {
                    Column(Modifier.fillMaxWidth().padding(14.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        AText("Why it works", style = MaterialTheme.typography.titleSmall)
                        (if (showAll) reasons else reasons.take(1)).forEach { ReasonLine(it) }
                        if (reasons.size > 1) {
                            TextButton(onClick = { showAll = !showAll }, modifier = Modifier.heightIn(min = 48.dp)) {
                                AText(if (showAll) "Show less" else "Show all ${reasons.size} reasons")
                                Spacer(Modifier.width(4.dp))
                                Icon(
                                    painterResource(R.drawable.ic_expand),
                                    contentDescription = null,
                                    modifier = Modifier.rotate(if (showAll) 180f else 0f),
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

/** A small row of the outfit's colors, like a paint palette. */
@Composable
private fun ColorDots(items: List<ClothingItem>) {
    val colors = items.map { it.color }.distinct()
    // Each dot overlaps the one before it a little
    Box(Modifier.width((16 * colors.size + 6).dp).height(22.dp).clearAndSetSemantics {}) {
        colors.forEachIndexed { i, c ->
            Box(
                Modifier
                    .padding(start = (16 * i).dp)
                    .size(22.dp)
                    .clip(CircleShape)
                    .background(c.toComposeColor())
                    .border(2.dp, MaterialTheme.colorScheme.surfaceContainer, CircleShape),
            )
        }
    }
}

/** One piece's photo in the outfit picture. */
@Composable
private fun PieceTile(
    repo: ClosetRepository,
    item: ClothingItem,
    newPhoto: Bitmap?,
    onOpen: ((String) -> Unit)?,
    modifier: Modifier,
) {
    val isNew = item.id == NEW_ITEM_ID
    val click = if (onOpen != null && !isNew) Modifier.clickable { onOpen(item.id) } else Modifier
    Box(
        modifier
            .clip(RoundedCornerShape(16.dp))
            .background(MaterialTheme.colorScheme.surfaceContainerHigh)
            .then(click),
    ) {
        if (isNew && newPhoto != null) {
            val image = remember(newPhoto) { newPhoto.asImageBitmap() }
            Image(image, contentDescription = null, contentScale = ContentScale.Crop, modifier = Modifier.fillMaxSize())
        } else {
            PhotoThumbnail(repo.photoFor(item), Modifier.fillMaxSize())
        }
        if (isNew) {
            Surface(
                shape = RoundedCornerShape(50),
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.align(Alignment.TopStart).padding(8.dp),
            ) {
                AText(
                    "New",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onPrimary,
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                )
            }
        }
    }
}

/** A piece's name with a dot of its color, like "● Navy Shirt". */
@Composable
private fun PieceChip(item: ClothingItem, onOpen: ((String) -> Unit)?) {
    val isNew = item.id == NEW_ITEM_ID
    val click = if (onOpen != null && !isNew) {
        Modifier.clickable(role = Role.Button, onClickLabel = "Open ${item.name}") { onOpen(item.id) }
    } else {
        Modifier
    }
    Surface(
        shape = RoundedCornerShape(50),
        color = MaterialTheme.colorScheme.surfaceContainerHigh,
        border = if (LocalHighContrast.current) BorderStroke(1.5.dp, MaterialTheme.colorScheme.outline) else null,
    ) {
        Row(
            Modifier
                .then(click)
                .heightIn(min = 40.dp)
                .padding(start = 8.dp, end = 14.dp)
                .semantics(mergeDescendants = true) {},
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(
                Modifier
                    .size(18.dp)
                    .clip(CircleShape)
                    .background(item.color.toComposeColor())
                    .border(1.dp, MaterialTheme.colorScheme.outline, CircleShape),
            )
            Spacer(Modifier.width(8.dp))
            Column {
                AText(item.name, style = MaterialTheme.typography.labelLarge)
                AText(
                    "${ColorNames.nearest(item.color).name} ${item.typeLabel.lowercase()}",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

@Composable
private fun ColumnScope.ReasonLine(text: String) {
    Row {
        AText("•", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.primary)
        Spacer(Modifier.width(8.dp))
        AText(text, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurface)
    }
}
