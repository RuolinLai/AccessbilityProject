package com.emma_zara.accessbilityproject.ui.components

import android.content.Context
import android.speech.tts.TextToSpeech
import android.widget.Toast
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import java.util.Locale

/**
 * Reads text out loud with the phone's text-to-speech voice.
 * The voice only starts up the first time something is read, so screens that never use it stay fast.
 */
class Speaker(private val context: Context) {
    private var tts: TextToSpeech? = null
    private var ready = false
    /** Text asked for while the voice was still starting up. */
    private var waiting: String? = null

    fun speak(text: String) {
        val engine = tts
        if (engine == null) {
            waiting = text
            tts = TextToSpeech(context.applicationContext) { status -> onStarted(status) }
        } else if (ready) {
            engine.speak(text, TextToSpeech.QUEUE_FLUSH, null, "color")
        } else {
            waiting = text
        }
    }

    private fun onStarted(status: Int) {
        val engine = tts ?: return
        if (status != TextToSpeech.SUCCESS) {
            tts = null
            Toast.makeText(context, "Text-to-speech isn't available on this phone.", Toast.LENGTH_LONG).show()
            return
        }
        // The app's text is in English, so use an English voice even if the phone is set to another language
        if (engine.setLanguage(Locale.US) < TextToSpeech.LANG_AVAILABLE) engine.setLanguage(Locale.ENGLISH)
        ready = true
        waiting?.let { engine.speak(it, TextToSpeech.QUEUE_FLUSH, null, "color") }
        waiting = null
    }

    fun shutdown() {
        tts?.shutdown()
        tts = null
        ready = false
    }
}

/** A [Speaker] that is shut down when the screen goes away. */
@Composable
fun rememberSpeaker(): Speaker {
    val context = LocalContext.current
    val speaker = remember(context) { Speaker(context) }
    DisposableEffect(speaker) { onDispose { speaker.shutdown() } }
    return speaker
}
