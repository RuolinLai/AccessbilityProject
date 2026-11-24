package com.emma_zara.accessbilityproject.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.snap
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
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
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.unit.dp
import com.emma_zara.accessbilityproject.R
import com.emma_zara.accessbilityproject.ui.theme.LocalHighContrast
import com.emma_zara.accessbilityproject.ui.theme.LocalMotion

/**
 * A card that opens and closes when you tap its title. Closed, it shows a one-line [summary],
 * so the page stays short but you can still see what's inside.
 */
@Composable
fun ExpandableCard(
    title: String,
    summary: String?,
    modifier: Modifier = Modifier,
    startOpen: Boolean = false,
    content: @Composable ColumnScope.() -> Unit,
) {
    var open by rememberSaveable(title) { mutableStateOf(startOpen) }
    val motion = LocalMotion.current
    val turn by animateFloatAsState(if (open) 180f else 0f, if (motion) tween(200) else snap(), label = "chevron")
    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        color = MaterialTheme.colorScheme.surfaceContainer,
        border = panelBorder(),
    ) {
        Column {
            Row(
                Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(20.dp))
                    .clickable(role = Role.Button, onClickLabel = if (open) "Close" else "Open") { open = !open }
                    .heightIn(min = 64.dp)
                    .padding(horizontal = 20.dp, vertical = 14.dp)
                    .semantics(mergeDescendants = true) { stateDescription = if (open) "Open" else "Closed" },
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column(Modifier.weight(1f)) {
                    AText(title, style = MaterialTheme.typography.titleLarge, modifier = Modifier.semantics { heading() })
                    if (summary != null && !open) {
                        AText(summary, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
                Spacer(Modifier.width(8.dp))
                Icon(painterResource(R.drawable.ic_expand), contentDescription = null, modifier = Modifier.rotate(turn))
            }
            AnimatedVisibility(
                visible = open,
                enter = if (motion) expandVertically() + fadeIn() else EnterTransition.None,
                exit = if (motion) shrinkVertically() + fadeOut() else ExitTransition.None,
            ) {
                Column(
                    Modifier.padding(start = 20.dp, end = 20.dp, bottom = 20.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                    content = content,
                )
            }
        }
    }
}
