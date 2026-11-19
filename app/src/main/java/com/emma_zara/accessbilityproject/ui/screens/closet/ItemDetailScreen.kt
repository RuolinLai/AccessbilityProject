package com.emma_zara.accessbilityproject.ui.screens.closet

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.emma_zara.accessbilityproject.R
import com.emma_zara.accessbilityproject.closet.ClosetRepository
import com.emma_zara.accessbilityproject.closet.Match
import com.emma_zara.accessbilityproject.closet.findMatches
import com.emma_zara.accessbilityproject.color.ColorNames
import com.emma_zara.accessbilityproject.color.analyzeColor
import com.emma_zara.accessbilityproject.ui.components.AText
import com.emma_zara.accessbilityproject.ui.components.ColorHeroCard
import com.emma_zara.accessbilityproject.ui.components.MatchingColorsCard
import com.emma_zara.accessbilityproject.ui.components.Pill
import com.emma_zara.accessbilityproject.ui.components.SectionCard

/** One item: its photo, color details, and what else in the closet goes with it. */
@Composable
fun ItemDetailScreen(repo: ClosetRepository, itemId: String, onBack: () -> Unit, onOpen: (String) -> Unit) {
    val item = repo.find(itemId)
    if (item == null) {
        // The item was deleted: go back to the list
        LaunchedEffect(Unit) { onBack() }
        return
    }
    val analysis = analyzeColor(item.color)
    val matches = findMatches(item, repo.items)
    var confirmDelete by remember { mutableStateOf(false) }

    Column(
        Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 16.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        BackHeader(item.name, onBack) {
            IconButton(onClick = { confirmDelete = true }, modifier = Modifier.size(56.dp)) {
                Icon(painterResource(R.drawable.ic_delete), contentDescription = "Delete ${item.name}")
            }
        }

        PhotoThumbnail(
            repo.photoFor(item),
            size = 1000,
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(1f)
                .clip(RoundedCornerShape(24.dp))
                .semantics { contentDescription = "Photo of ${item.name}" },
        )

        Pill(item.category.label)
        ColorHeroCard(analysis)

        SectionCard(title = "Wear it with") {
            if (matches.isEmpty()) {
                AText(
                    if (repo.items.size <= 1) {
                        "Add more clothes to see what goes with this."
                    } else {
                        "Nothing in your closet matches by color yet. " +
                            "Neutrals like white, black, gray, beige or navy go with almost anything."
                    },
                    style = MaterialTheme.typography.bodyLarge,
                )
            } else {
                // Group the matches by reason: Neutral, Same color family, ...
                val groups = matches.groupBy { it.type }
                groups.entries.forEachIndexed { index, (type, list) ->
                    if (index > 0) HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        AText(type.label, style = MaterialTheme.typography.titleMedium)
                        AText(
                            type.reason,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                        list.forEach { MatchRow(repo, it) { onOpen(it.item.id) } }
                    }
                }
            }
        }

        // Colors to look for when shopping
        MatchingColorsCard(analysis, title = "Colors to look for when shopping")
        Spacer(Modifier.size(8.dp))
    }

    if (confirmDelete) {
        AlertDialog(
            onDismissRequest = { confirmDelete = false },
            title = { AText("Delete ${item.name}?") },
            text = { AText("This removes it and its photo from your closet.") },
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

@Composable
private fun MatchRow(repo: ClosetRepository, match: Match, onClick: () -> Unit) {
    val other = match.item
    Row(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .clickable(role = Role.Button, onClickLabel = "Open ${other.name}", onClick = onClick)
            .padding(vertical = 4.dp)
            .semantics(mergeDescendants = true) {},
        verticalAlignment = Alignment.CenterVertically,
    ) {
        PhotoThumbnail(
            repo.photoFor(other),
            Modifier
                .size(64.dp)
                .clip(RoundedCornerShape(12.dp)),
        )
        Spacer(Modifier.width(12.dp))
        Column(Modifier.weight(1f).widthIn(min = 0.dp)) {
            AText(other.name, style = MaterialTheme.typography.titleMedium)
            AText(
                "${ColorNames.nearest(other.color).name} · ${other.category.singular}",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}
