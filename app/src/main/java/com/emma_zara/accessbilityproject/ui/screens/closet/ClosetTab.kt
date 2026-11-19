package com.emma_zara.accessbilityproject.ui.screens.closet

import androidx.activity.compose.BackHandler
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.remember
import com.emma_zara.accessbilityproject.closet.ClosetRepository

/** The pages inside the Closet tab. */
sealed interface ClosetPage {
    data object List : ClosetPage
    data object Add : ClosetPage
    data class Detail(val id: String) : ClosetPage
}

/**
 * The Closet tab. Keeps a "back stack" of pages so the phone's back button
 * goes to the previous page (for example from an item back to the list).
 */
@Composable
fun ClosetTab(repo: ClosetRepository) {
    val stack = remember { mutableStateListOf<ClosetPage>(ClosetPage.List) }
    fun open(page: ClosetPage) = stack.add(page)
    fun back() {
        if (stack.size > 1) stack.removeAt(stack.lastIndex)
    }

    BackHandler(enabled = stack.size > 1) { back() }

    when (val page = stack.last()) {
        ClosetPage.List -> ClosetListScreen(
            repo = repo,
            onAdd = { open(ClosetPage.Add) },
            onOpen = { open(ClosetPage.Detail(it)) },
        )

        ClosetPage.Add -> AddItemScreen(
            repo = repo,
            onBack = ::back,
            onSaved = { id ->
                back()                          // close the "Add" page...
                open(ClosetPage.Detail(id))     // ...and show the new item
            },
        )

        is ClosetPage.Detail -> ItemDetailScreen(
            repo = repo,
            itemId = page.id,
            onBack = ::back,
            onOpen = { open(ClosetPage.Detail(it)) },
        )
    }
}
