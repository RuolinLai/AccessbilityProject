package com.emma_zara.accessbilityproject.ui.screens.closet

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.remember
import com.emma_zara.accessbilityproject.closet.ClosetRepository
import com.emma_zara.accessbilityproject.closet.SharedItem
import com.emma_zara.accessbilityproject.ui.theme.LocalMotion

/** The pages inside the Closet tab. */
sealed interface ClosetPage {
    data object List : ClosetPage
    data object Add : ClosetPage
    /** The shopping check, started with [shared] if something was shared from another app. */
    data class Shop(val shared: SharedItem? = null) : ClosetPage
    data object WishList : ClosetPage
    data class Detail(val id: String) : ClosetPage
    /** Outfit ideas, built around one item if [anchorId] is given. */
    data class Outfits(val anchorId: String?) : ClosetPage
}

/**
 * The Closet tab. Keeps a "back stack" of pages so the phone's back button
 * goes to the previous page (for example from an item back to the list).
 *
 * [incoming] is something shared from another app (like a product link): it opens the shopping check,
 * then [onIncomingHandled] is called so it isn't opened twice.
 */
@Composable
fun ClosetTab(repo: ClosetRepository, incoming: SharedItem? = null, onIncomingHandled: () -> Unit = {}) {
    val stack = remember { mutableStateListOf<ClosetPage>(ClosetPage.List) }
    var forward by remember { mutableStateOf(true) }   // which way the pages slide
    val motion = LocalMotion.current

    fun open(page: ClosetPage) {
        forward = true
        stack.add(page)
    }
    fun back() {
        forward = false
        if (stack.size > 1) stack.removeAt(stack.lastIndex)
        // Skip pages of items that were deleted
        fun gone(page: ClosetPage) = when (page) {
            is ClosetPage.Detail -> repo.find(page.id) == null
            is ClosetPage.Outfits -> page.anchorId != null && repo.find(page.anchorId) == null
            else -> false
        }
        while (stack.size > 1 && gone(stack.last())) {
            stack.removeAt(stack.lastIndex)
        }
    }

    BackHandler(enabled = stack.size > 1) { back() }

    LaunchedEffect(incoming) {
        if (incoming != null) {
            open(ClosetPage.Shop(incoming))
            onIncomingHandled()
        }
    }

    // New pages slide in from the right; going back slides them away to the right
    AnimatedContent(
        targetState = stack.last(),
        transitionSpec = {
            if (!motion) {
                EnterTransition.None togetherWith ExitTransition.None
            } else if (forward) {
                (slideInHorizontally(tween(300)) { it / 3 } + fadeIn(tween(250))) togetherWith
                    (slideOutHorizontally(tween(300)) { -it / 6 } + fadeOut(tween(200)))
            } else {
                (slideInHorizontally(tween(300)) { -it / 6 } + fadeIn(tween(250))) togetherWith
                    (slideOutHorizontally(tween(300)) { it / 3 } + fadeOut(tween(200)))
            }
        },
        label = "closetPages",
    ) { page ->
    when (page) {
        ClosetPage.List -> ClosetListScreen(
            repo = repo,
            onAdd = { open(ClosetPage.Add) },
            onOpen = { open(ClosetPage.Detail(it)) },
            onOutfits = { open(ClosetPage.Outfits(null)) },
            onShop = { open(ClosetPage.Shop()) },
            onWishList = { open(ClosetPage.WishList) },
        )

        ClosetPage.WishList -> WishListScreen(
            repo = repo,
            onBack = ::back,
            onOpen = { open(ClosetPage.Detail(it)) },
            onShop = { open(ClosetPage.Shop()) },
        )

        ClosetPage.Add, is ClosetPage.Shop -> AddItemScreen(
            repo = repo,
            shopping = page is ClosetPage.Shop,
            shared = (page as? ClosetPage.Shop)?.shared,
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
            onOutfits = { open(ClosetPage.Outfits(it)) },
        )

        is ClosetPage.Outfits -> OutfitsScreen(
            repo = repo,
            anchorId = page.anchorId,
            onBack = ::back,
            onOpen = { open(ClosetPage.Detail(it)) },
        )
    }
}
}
