package com.carmelitasoftworks.motointercom.core.service

import android.content.Context
import android.speech.tts.TextToSpeech
import java.util.Locale

/** Spoken notices in the helmet: "conexión perdida" / "reconectado" (CLAUDE.md §4.8). */
internal class VoiceAnnouncer(context: Context) {
    private var ready = false
    private val tts: TextToSpeech = TextToSpeech(context.applicationContext) { status ->
        ready = status == TextToSpeech.SUCCESS
        if (ready) tts.language = Locale("es")
    }

    fun say(text: String) {
        if (ready) tts.speak(text, TextToSpeech.QUEUE_FLUSH, null, text)
    }

    fun release() = tts.shutdown()
}
