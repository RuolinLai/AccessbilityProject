package com.emma_zara.accessbilityproject.ui.screens.closet

import com.emma_zara.accessbilityproject.ui.components.LocalBottomBarSpace
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.emma_zara.accessbilityproject.closet.ClosetRepository
import com.emma_zara.accessbilityproject.closet.suggestOutfits
import com.emma_zara.accessbilityproject.ui.components.AText
import com.emma_zara.accessbilityproject.ui.components.SectionCard
import com.emma_zara.accessbilityproject.ui.components.popIn

/**
 * Whole outfits made from the closet. If [anchorId] is given, every outfit is built around that item.
 */
@Composable
fun OutfitsScreen(repo: ClosetRepository, anchorId: String?, onBack: () -> Unit, onOpen: (String) -> Unit) {
    val anchor = anchorId?.let { repo.find(it) }
    // The item was just deleted (this page is sliding away): show nothing
    if (anchorId != null && anchor == null) return
    val outfits = remember(repo.items, repo.suggestFrom, anchor) { suggestOutfits(repo.items, repo.suggestFrom, anchor) }

    Column(
        Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(bottom = LocalBottomBarSpace.current)
            .padding(horizontal = 16.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        BackHeader(if (anchor != null) "Outfits with ${anchor.name}" else "Outfit ideas", onBack)

        SectionCard {
            AText(
                "Each outfit has a top with pants or a skirt, or a dress. Shoes, a bag and jewellery are " +
                    "added when one goes with everything else. Every piece matches every other piece, " +
                    "and there's never more than one pattern.",
                style = MaterialTheme.typography.bodyLarge,
            )
            SuggestFromChips(repo, hide = anchor?.category)
        }

        if (outfits.isEmpty()) {
            SectionCard(title = "No outfits yet") {
                AText(
                    "To make an outfit, the closet needs a top and pants or a skirt (or a dress) whose colors " +
                        "go together. Try adding more clothes, especially neutrals like white, black, gray, " +
                        "beige or navy, or choose more categories above.",
                    style = MaterialTheme.typography.bodyLarge,
                )
            }
        } else {
            outfits.forEachIndexed { i, outfit ->
                OutfitCard(repo, outfit, title = "Outfit ${i + 1}", modifier = Modifier.popIn(i), onOpen = onOpen)
            }
        }
        Spacer(Modifier.size(8.dp))
    }
}
