package com.emma_zara.accessbilityproject.ui.screens.closet

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.dp
import com.emma_zara.accessbilityproject.closet.Occasion
import com.emma_zara.accessbilityproject.closet.Warmth
import com.emma_zara.accessbilityproject.ui.components.AText
import com.emma_zara.accessbilityproject.ui.components.SectionCard

/**
 * Choose where a piece is good to wear (any number of occasions) and what weather it's for.
 * Used by the outfit recommendations. Leaving everything off means "fine for anything".
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun WhenToWearCard(
    occasions: Set<Occasion>,
    warmth: Warmth?,
    title: String = "When to wear it",
    onChange: (Set<Occasion>, Warmth?) -> Unit,
) {
    SectionCard(title = title) {
        AText(
            "Helps the Outfits tab suggest it at the right time. Leave it all off if it's fine for anything.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        WhenToWearChips(occasions, warmth, onChange)
    }
}

/** The occasion and weather chips, without a card around them. */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun WhenToWearChips(occasions: Set<Occasion>, warmth: Warmth?, onChange: (Set<Occasion>, Warmth?) -> Unit) {
    androidx.compose.foundation.layout.Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        AText("Good for (choose any):", style = MaterialTheme.typography.titleMedium)
        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Occasion.entries.forEach { o ->
                val on = o in occasions
                ChoiceChip(o.label, selected = on) { onChange(if (on) occasions - o else occasions + o, warmth) }
            }
        }
        AText("Weather:", style = MaterialTheme.typography.titleMedium)
        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            ChoiceChip("Any weather", selected = warmth == null) { onChange(occasions, null) }
            Warmth.entries.forEach { w ->
                ChoiceChip(w.label, selected = warmth == w, description = w.description) { onChange(occasions, w) }
            }
        }
        warmth?.let {
            AText(it.description + ".", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}
