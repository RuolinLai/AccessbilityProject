package com.emma_zara.accessbilityproject.ui.components

import android.content.Context
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import com.emma_zara.accessbilityproject.ui.theme.LocalVibrate
import kotlinx.coroutines.delay

/** A short buzz, strong enough to notice in your hand. Does nothing on phones without a vibration motor. */
fun buzz(context: Context) {
    val vibrator = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
        context.getSystemService(VibratorManager::class.java)?.defaultVibrator
    } else {
        context.getSystemService(Vibrator::class.java)
    } ?: return
    if (!vibrator.hasVibrator()) return
    vibrator.vibrate(VibrationEffect.createOneShot(40, VibrationEffect.DEFAULT_AMPLITUDE))
}

/**
 * Buzzes when [colorName] changes (if vibration is on in Settings), so you can feel that the color is
 * different without looking. It waits until the name stays the same for a moment, so a shaky camera
 * jumping between two colors doesn't buzz again and again. Nothing happens when the screen first opens.
 */
@Composable
fun VibrateOnColorChange(colorName: String) {
    val context = LocalContext.current
    val on = LocalVibrate.current
    var lastName by remember { mutableStateOf(colorName) }
    LaunchedEffect(colorName, on) {
        if (!on) lastName = colorName
        if (!on || colorName == lastName) return@LaunchedEffect
        delay(250)
        lastName = colorName
        buzz(context)
    }
}
