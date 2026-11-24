package com.emma_zara.accessbilityproject.ui.screens.closet

import android.graphics.Bitmap
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.emma_zara.accessbilityproject.R
import com.emma_zara.accessbilityproject.closet.Category
import com.emma_zara.accessbilityproject.closet.ClosetRepository
import com.emma_zara.accessbilityproject.closet.ClothingItem
import com.emma_zara.accessbilityproject.closet.Match
import com.emma_zara.accessbilityproject.closet.summary
import com.emma_zara.accessbilityproject.ui.components.AText
import com.emma_zara.accessbilityproject.color.ColorNames
import com.emma_zara.accessbilityproject.color.looksAlike
import com.emma_zara.accessbilityproject.ui.theme.LocalColorVision

/** Id used for a new item that isn't saved in the closet yet (shopping check). */
const val NEW_ITEM_ID = "new-item"

/**
 * Chips to choose which kinds of items the app suggests (for example only shoes and bags).
 * The choice is saved, so it's the same on every screen.
 * Chosen chips show a check mark, so you don't have to tell them apart by color.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun SuggestFromChips(repo: ClosetRepository, hide: Category? = null) {
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        AText("Suggest from:", style = MaterialTheme.typography.titleMedium)
        FlowRow(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            Category.entries.filter { it != hide }.forEach { c ->
                val on = c in repo.suggestFrom
                FilterChip(
                    selected = on,
                    onClick = { repo.setSuggestFrom(c, !on) },
                    label = { AText(c.label) },
                    leadingIcon = if (on) {
                        { Icon(painterResource(R.drawable.ic_check), null, Modifier.size(FilterChipDefaults.IconSize)) }
                    } else {
                        null
                    },
                )
            }
        }
    }
}

/** One item in a list: photo, name and a short description. Tappable if [onClick] is given. */
@Composable
fun ItemRow(repo: ClosetRepository, item: ClothingItem, newPhoto: Bitmap? = null, onClick: (() -> Unit)? = null) {
    val clickable = if (onClick != null) {
        Modifier.clickable(role = Role.Button, onClickLabel = "Open ${item.name}", onClick = onClick)
    } else {
        Modifier
    }
    Row(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .then(clickable)
            .padding(vertical = 4.dp)
            .semantics(mergeDescendants = true) {},
        verticalAlignment = Alignment.CenterVertically,
    ) {
        val photoModifier = Modifier
            .size(64.dp)
            .clip(RoundedCornerShape(12.dp))
        if (newPhoto != null) {
            val image = remember(newPhoto) { newPhoto.asImageBitmap() }
            Image(image, contentDescription = null, contentScale = ContentScale.Crop, modifier = photoModifier)
        } else {
            PhotoThumbnail(repo.photoFor(item), photoModifier)
        }
        Spacer(Modifier.width(12.dp))
        Column(Modifier.weight(1f).widthIn(min = 0.dp)) {
            AText(item.name, style = MaterialTheme.typography.titleMedium)
            AText(
                item.summary(),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

/**
 * An item that goes with [base], with short notes under it: why it matches ([reason]),
 * whether the two may blur together, and whether they may look alike with the user's color vision.
 */
@Composable
fun MatchRow(repo: ClosetRepository, base: ClothingItem, match: Match, reason: String? = null, onClick: (() -> Unit)? = null) {
    val vision = LocalColorVision.current
    val notes = listOfNotNull(
        reason,
        if (match.lowContrast) "About as light or dark as each other, so they may blur together." else null,
        if (looksAlike(base.color, match.item.color, vision)) {
            "May look like the same color to you, but it's ${ColorNames.nearest(match.item.color).name}."
        } else {
            null
        },
    )
    Column {
        ItemRow(repo, match.item, onClick = onClick)
        notes.forEach {
            AText(
                it,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(start = 76.dp),
            )
        }
    }
}
