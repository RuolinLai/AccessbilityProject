package com.emma_zara.accessbilityproject.ui.screens.closet

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.emma_zara.accessbilityproject.R
import com.emma_zara.accessbilityproject.closet.ClosetRepository
import com.emma_zara.accessbilityproject.closet.findMatches
import com.emma_zara.accessbilityproject.closet.patternLabel
import com.emma_zara.accessbilityproject.closet.typeLabel
import com.emma_zara.accessbilityproject.color.analyzeColor
import com.emma_zara.accessbilityproject.ui.components.AText
import com.emma_zara.accessbilityproject.ui.components.ColorHeroCard
import com.emma_zara.accessbilityproject.ui.components.MatchingColorsCard
import com.emma_zara.accessbilityproject.ui.components.Pill
import com.emma_zara.accessbilityproject.ui.components.SectionCard

/** One item: its photo, color details, and what else in the closet goes with it. */
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

        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Pill(item.typeLabel)
            Pill(item.patternLabel)
        }
        ColorHeroCard(analysis)

        SectionCard(title = "Type") {
            TypeChips(repo, item.category, item.typeName) { c, t -> repo.update(item.copy(category = c, typeName = t)) }
        }

        SectionCard(title = "Pattern") {
            AText("Wrong? Choose the right one.", style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant)
            PatternChips(repo, item.pattern, item.patternName) { p, n -> repo.update(item.copy(pattern = p, patternName = n)) }
        }

        Button(
            onClick = { onOutfits(item.id) },
            modifier = Modifier.fillMaxWidth().heightIn(min = 56.dp),
        ) { AText("Outfit ideas with this") }

        SectionCard(title = "Wear it with") {
            SuggestFromChips(repo, hide = item.category)
            if (item.pattern.busy) {
                AText(
                    "This has a busy pattern (${item.patternLabel.lowercase()}), so only plain pieces are suggested. " +
                        "Two patterns together can clash.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            if (matches.isEmpty()) {
                AText(
                    when {
                        repo.items.size <= 1 -> "Add more clothes to see what goes with this."
                        repo.suggestFrom.none { it != item.category } -> "Choose at least one category above."
                        else -> "Nothing in your closet matches by color yet. " +
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
                        list.forEach { ItemRow(repo, it.item) { onOpen(it.item.id) } }
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
