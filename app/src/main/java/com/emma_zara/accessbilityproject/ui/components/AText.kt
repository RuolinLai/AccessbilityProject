package com.emma_zara.accessbilityproject.ui.components

import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.style.TextAlign
import com.emma_zara.accessbilityproject.ui.theme.LocalTextCasing

/**
 * "Accessible Text": same as Text, but follows the user's letter-casing setting
 * (Normal / UPPERCASE / lowercase). Use this instead of Text everywhere in the app.
 */
@Composable
fun AText(
    text: String,
    modifier: Modifier = Modifier,
    style: TextStyle = LocalTextStyle.current,
    color: Color = Color.Unspecified,
    textAlign: TextAlign? = null,
) {
    Text(
        text = LocalTextCasing.current.apply(text),
        modifier = modifier,
        style = style,
        color = color,
        textAlign = textAlign,
    )
}
