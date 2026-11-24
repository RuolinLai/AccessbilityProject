package com.emma_zara.accessbilityproject.ui.screens.closet

import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.Spacer
import com.emma_zara.accessbilityproject.ui.components.RoundIconButton
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.emma_zara.accessbilityproject.R
import com.emma_zara.accessbilityproject.closet.Photos
import com.emma_zara.accessbilityproject.ui.components.AText
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File

/** Shows a saved photo. It loads in the background so the screen doesn't freeze. */
@Composable
fun PhotoThumbnail(
    file: File,
    modifier: Modifier = Modifier,
    size: Int = 400,
    contentScale: ContentScale = ContentScale.Crop,
    background: Color = MaterialTheme.colorScheme.surfaceContainerHigh,
) {
    val bitmap: ImageBitmap? by produceState(Photos.cachedThumbnail(file, size), file, size) {
        // When the file changes (the same spot now shows another item), never keep showing the old photo
        value = Photos.cachedThumbnail(file, size) ?: withContext(Dispatchers.IO) { Photos.loadThumbnail(file, size) }
    }
    Box(modifier.background(background)) {
        bitmap?.let {
            Image(it, contentDescription = null, contentScale = contentScale, modifier = Modifier.fillMaxSize())
        }
    }
}

/** Title bar with a back button, for screens inside the closet. */
@Composable
fun BackHeader(title: String, onBack: () -> Unit, actions: @Composable RowScope.() -> Unit = {}) {
    Row(
        Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        RoundIconButton(R.drawable.ic_arrow_back, "Back", onBack)
        Spacer(Modifier.width(12.dp))
        AText(
            title,
            style = MaterialTheme.typography.headlineSmall,
            modifier = Modifier
                .weight(1f)
                .semantics { heading() },
        )
        actions()
    }
}
