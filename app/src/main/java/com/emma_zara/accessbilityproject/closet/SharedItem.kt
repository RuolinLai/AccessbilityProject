package com.emma_zara.accessbilityproject.closet

import android.content.Intent
import android.net.Uri
import androidx.core.content.IntentCompat

/**
 * Something shared to the app from another app with the "Share" button,
 * like a product link from a shopping app or browser, or a picture.
 * It opens the shopping check.
 */
sealed interface SharedItem {
    data class Link(val text: String) : SharedItem
    data class Image(val uri: Uri) : SharedItem

    companion object {
        fun from(intent: Intent?): SharedItem? {
            if (intent?.action != Intent.ACTION_SEND) return null
            val type = intent.type.orEmpty()
            return when {
                type.startsWith("image/") ->
                    IntentCompat.getParcelableExtra(intent, Intent.EXTRA_STREAM, Uri::class.java)?.let { Image(it) }
                else -> intent.getStringExtra(Intent.EXTRA_TEXT)?.takeIf { it.isNotBlank() }?.let { Link(it) }
            }
        }
    }
}
