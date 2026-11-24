package com.emma_zara.accessbilityproject.ui.screens.closet

import com.emma_zara.accessbilityproject.ui.components.LocalBottomBarSpace
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.emma_zara.accessbilityproject.closet.ClosetRepository
import com.emma_zara.accessbilityproject.closet.findMatches
import com.emma_zara.accessbilityproject.ui.components.AText
import com.emma_zara.accessbilityproject.ui.components.SectionCard

/** Things you're thinking of buying, each with how many closet items it would go with. */
@Composable
fun WishListScreen(repo: ClosetRepository, onBack: () -> Unit, onOpen: (String) -> Unit, onShop: () -> Unit) {
    val wishes = repo.wishList.sortedByDescending { it.addedAt }

    Column(
        Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(bottom = LocalBottomBarSpace.current)
            .padding(horizontal = 16.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        BackHeader("Wish list", onBack)

        SectionCard {
            AText(
                "Things you saved from a shopping check. Open one to see what it goes with, " +
                    "or to move it into your closet once you buy it.",
                style = MaterialTheme.typography.bodyLarge,
            )
            FilledTonalButton(onClick = onShop, modifier = Modifier.fillMaxWidth().heightIn(min = 56.dp)) {
                AText("New shopping check")
            }
        }

        if (wishes.isEmpty()) {
            SectionCard { AText("Your wish list is empty.", style = MaterialTheme.typography.bodyLarge) }
        } else {
            SectionCard(title = if (wishes.size == 1) "1 item" else "${wishes.size} items") {
                wishes.forEachIndexed { i, item ->
                    if (i > 0) HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                    val count = findMatches(item, repo.items, repo.suggestFrom).size
                    Column {
                        ItemRow(repo, item) { onOpen(item.id) }
                        AText(
                            when (count) {
                                0 -> "Doesn't go with anything in your closet yet."
                                1 -> "Goes with 1 item in your closet."
                                else -> "Goes with $count items in your closet."
                            },
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(start = 76.dp),
                        )
                    }
                }
            }
        }
        Spacer(Modifier.size(8.dp))
    }
}
