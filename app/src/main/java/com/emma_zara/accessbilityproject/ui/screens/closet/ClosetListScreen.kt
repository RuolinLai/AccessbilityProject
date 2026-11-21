package com.emma_zara.accessbilityproject.ui.screens.closet

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.FilterChip
import androidx.compose.foundation.layout.heightIn
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.emma_zara.accessbilityproject.R
import com.emma_zara.accessbilityproject.closet.Category
import com.emma_zara.accessbilityproject.closet.ClosetRepository
import com.emma_zara.accessbilityproject.closet.ClothingItem
import com.emma_zara.accessbilityproject.closet.SortOrder
import com.emma_zara.accessbilityproject.closet.sortItems
import com.emma_zara.accessbilityproject.closet.summary
import com.emma_zara.accessbilityproject.color.ColorNames
import com.emma_zara.accessbilityproject.ui.components.AText
import com.emma_zara.accessbilityproject.ui.components.ScreenTitle
import com.emma_zara.accessbilityproject.ui.components.SectionCard
import com.emma_zara.accessbilityproject.ui.components.SwingingHanger
import com.emma_zara.accessbilityproject.ui.components.popIn
import com.emma_zara.accessbilityproject.ui.theme.LocalMotion
import com.emma_zara.accessbilityproject.ui.components.toComposeColor
import com.emma_zara.accessbilityproject.ui.theme.LocalHighContrast

@Composable
fun ClosetListScreen(
    repo: ClosetRepository,
    onAdd: () -> Unit,
    onOpen: (String) -> Unit,
    onOutfits: () -> Unit,
    onShop: () -> Unit,
) {
    val items = repo.items
    // null = show all, otherwise a built-in category's name or "own:" + the user's own type
    var filter by rememberSaveable { mutableStateOf<String?>(null) }
    var sort by rememberSaveable { mutableStateOf(SortOrder.NEWEST) }
    fun ClothingItem.filterKey() = typeName?.let { "own:$it" } ?: category.name
    val shown = sortItems(items.filter { filter == null || it.filterKey() == filter }, sort)
    val filterLabel = filter?.let { f -> Category.entries.firstOrNull { it.name == f }?.label ?: f.removePrefix("own:") }
    val motion = LocalMotion.current

    Box(Modifier.fillMaxSize()) {
        LazyVerticalGrid(
            columns = GridCells.Adaptive(minSize = 160.dp),
            contentPadding = PaddingValues(start = 16.dp, end = 16.dp, top = 12.dp, bottom = 104.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
            modifier = Modifier.fillMaxSize(),
        ) {
            item(span = { GridItemSpan(maxLineSpan) }) {
                ScreenTitle("My closet", if (items.size == 1) "1 item" else "${items.size} items")
            }

            if (items.isNotEmpty()) {
                item(span = { GridItemSpan(maxLineSpan) }) {
                    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        FilledTonalButton(onClick = onOutfits, modifier = Modifier.weight(1f).heightIn(min = 56.dp)) {
                            AText("Outfit ideas", textAlign = TextAlign.Center)
                        }
                        FilledTonalButton(onClick = onShop, modifier = Modifier.weight(1f).heightIn(min = 56.dp)) {
                            AText("Shopping check", textAlign = TextAlign.Center)
                        }
                    }
                }
                item(span = { GridItemSpan(maxLineSpan) }) {
                    ChipRow("Show") {
                        FilterChip(
                            selected = filter == null,
                            onClick = { filter = null },
                            label = { AText("All") },
                        )
                        Category.entries.forEach { c ->
                            val count = items.count { it.filterKey() == c.name }
                            FilterChip(
                                selected = filter == c.name,
                                onClick = { filter = c.name },
                                label = { AText("${c.label} ($count)") },
                            )
                        }
                        repo.customTypes.forEach { t ->
                            val key = "own:${t.name}"
                            FilterChip(
                                selected = filter == key,
                                onClick = { filter = key },
                                label = { AText("${t.name} (${items.count { it.filterKey() == key }})") },
                            )
                        }
                    }
                }
                item(span = { GridItemSpan(maxLineSpan) }) {
                    ChipRow("Sort by") {
                        SortOrder.entries.forEach { s ->
                            FilterChip(selected = sort == s, onClick = { sort = s }, label = { AText(s.label) })
                        }
                    }
                }
            }

            when {
                items.isEmpty() -> item(span = { GridItemSpan(maxLineSpan) }) { EmptyCloset(onAdd) }
                shown.isEmpty() -> item(span = { GridItemSpan(maxLineSpan) }) {
                    SectionCard {
                        AText("No ${filterLabel?.lowercase()} yet.", style = MaterialTheme.typography.bodyLarge)
                    }
                }
                else -> itemsIndexed(shown, key = { _, it -> it.id }) { index, item ->
                    // Cards pop in one by one, and glide to their new place when you change the sort
                    val move = if (motion) Modifier.animateItem() else Modifier
                    ItemCard(repo, item, move.popIn(index)) { onOpen(item.id) }
                }
            }
        }

        if (items.isNotEmpty()) {
            ExtendedFloatingActionButton(
                onClick = onAdd,
                icon = { Icon(painterResource(R.drawable.ic_add), contentDescription = null) },
                text = { AText("Add clothes") },
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(16.dp),
            )
        }
    }
}

/** A label and a row of chips you can scroll sideways. */
@Composable
private fun ChipRow(label: String, chips: @Composable () -> Unit) {
    Column {
        AText(label, style = MaterialTheme.typography.titleMedium)
        Row(
            Modifier
                .fillMaxWidth()
                .horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) { chips() }
    }
}

@Composable
private fun EmptyCloset(onAdd: () -> Unit) {
    SectionCard {
        SwingingHanger(Modifier.align(Alignment.CenterHorizontally).padding(vertical = 8.dp))
        AText(
            "Your closet is empty",
            style = MaterialTheme.typography.titleLarge,
            modifier = Modifier.semantics { heading() },
        )
        AText(
            "Take photos of your clothes, shoes, bags and jewellery. " +
                "The app finds each item's color and shows you what goes well together.",
            style = MaterialTheme.typography.bodyLarge,
        )
        Button(onClick = onAdd, modifier = Modifier.fillMaxWidth()) {
            Icon(painterResource(R.drawable.ic_add), contentDescription = null)
            Spacer(Modifier.width(8.dp))
            AText("Add your first item")
        }
    }
}

@Composable
private fun ItemCard(repo: ClosetRepository, item: ClothingItem, modifier: Modifier = Modifier, onClick: () -> Unit) {
    val colorName = ColorNames.nearest(item.color).name
    val highContrast = LocalHighContrast.current
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(20.dp),
        color = MaterialTheme.colorScheme.surfaceContainer,
        border = if (highContrast) BorderStroke(2.dp, MaterialTheme.colorScheme.outline) else null,
        modifier = modifier.semantics(mergeDescendants = true) {
            contentDescription = "${item.name}. ${item.summary()}"
        },
    ) {
        Column {
            PhotoThumbnail(
                repo.photoFor(item),
                Modifier
                    .fillMaxWidth()
                    .aspectRatio(1f)
                    .clip(RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp)),
            )
            Column(Modifier.padding(12.dp)) {
                AText(item.name, style = MaterialTheme.typography.titleMedium, maxLines = 2)
                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(top = 4.dp)) {
                    Box(
                        Modifier
                            .size(16.dp)
                            .clip(CircleShape)
                            .background(item.color.toComposeColor())
                            .border(1.dp, MaterialTheme.colorScheme.outline, CircleShape)
                            .clearAndSetSemantics {},
                    )
                    Spacer(Modifier.width(6.dp))
                    AText(
                        item.summary(),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        }
    }
}
