package com.carmelitasoftworks.motointercom.core.nearby

import android.annotation.SuppressLint
import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioRecord
import android.media.AudioTrack
import android.media.MediaRecorder
import android.media.audiofx.AcousticEchoCanceler
import android.media.audiofx.AudioEffect
import android.media.audiofx.NoiseSuppressor
import com.carmelitasoftworks.motointercom.core.audio.SoundEffects
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

/**
 * Microphone in, speaker out, for the nearby mode: 16 kHz mono frames of
 * [FRAME_SAMPLES]. Captured frames that contain speech go to [onFrame]; incoming
 * frames are mixed by [mixer] and played through the rider's sound settings.
 */
internal class VoicePipeline(
    private val soundEffects: SoundEffects,
    private val onFrame: (ShortArray) -> Unit,
    private val onSpeaking: (Boolean) -> Unit,
) {
    val mixer = JitterMixer(frameSize = FRAME_SAMPLES)

    @Volatile var transmitting = false

    private var scope: CoroutineScope? = null

    fun start() {
        if (scope != null) return
        val newScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
        scope = newScope
        newScope.launch { capture() }
        newScope.launch { play(newScope) }
    }

    fun stop() {
        scope?.cancel()
        scope = null
        mixer.clear()
        onSpeaking(false)
    }

    @SuppressLint("MissingPermission") // The ride only starts after RECORD_AUDIO was granted.
    private suspend fun capture() {
        val minBuffer = AudioRecord.getMinBufferSize(SAMPLE_RATE, AudioFormat.CHANNEL_IN_MONO, ENCODING)
        val record = AudioRecord(
            MediaRecorder.AudioSource.VOICE_COMMUNICATION,
            SAMPLE_RATE,
            AudioFormat.CHANNEL_IN_MONO,
            ENCODING,
            maxOf(minBuffer, FRAME_BYTES * RECORD_BUFFER_FRAMES),
        )
        val effects = listOfNotNull(
            effectOrNull { AcousticEchoCanceler.create(record.audioSessionId) },
            effectOrNull { NoiseSuppressor.create(record.audioSessionId) },
        )
        val vad = VoiceActivityDetector()
        var wasSpeaking = false
        try {
            if (record.state != AudioRecord.STATE_INITIALIZED) return
            record.startRecording()
            val frame = ShortArray(FRAME_SAMPLES)
            while (kotlinx.coroutines.currentCoroutineContext().isActive) {
                if (!readFully(record, frame)) break
                val speaking = transmitting && vad.isSpeech(frame)
                if (speaking) onFrame(frame.copyOf())
                if (speaking != wasSpeaking) {
                    wasSpeaking = speaking
                    onSpeaking(speaking)
                }
            }
        } finally {
            effects.forEach { it.release() }
            record.release()
        }
    }

    private fun readFully(record: AudioRecord, frame: ShortArray): Boolean {
        var read = 0
        while (read < frame.size) {
            val n = record.read(frame, read, frame.size - read)
            if (n < 0) return false
            read += n
        }
        return true
    }

    private suspend fun play(scope: CoroutineScope) {
        val minBuffer = AudioTrack.getMinBufferSize(SAMPLE_RATE, AudioFormat.CHANNEL_OUT_MONO, ENCODING)
        val track = AudioTrack.Builder()
            .setAudioAttributes(
                AudioAttributes.Builder()
                    .setUsage(AudioAttributes.USAGE_VOICE_COMMUNICATION)
                    .setContentType(AudioAttributes.CONTENT_TYPE_SPEECH)
                    .build(),
            )
            .setAudioFormat(
                AudioFormat.Builder()
                    .setSampleRate(SAMPLE_RATE)
                    .setEncoding(ENCODING)
                    .setChannelMask(AudioFormat.CHANNEL_OUT_MONO)
                    .build(),
            )
            .setBufferSizeInBytes(maxOf(minBuffer, FRAME_BYTES * PLAY_BUFFER_FRAMES))
            .setTransferMode(AudioTrack.MODE_STREAM)
            .build()
        // Equalizer and volume boost from Ajustes > Sonido apply to our own track here.
        val effects: Job = scope.launch { soundEffects.applyTo(track.audioSessionId) }
        val out = ShortArray(FRAME_SAMPLES)
        try {
            track.play()
            while (kotlinx.coroutines.currentCoroutineContext().isActive) {
                if (!mixer.mixNext(out)) out.fill(0)
                // Blocking write paces the loop at real time.
                if (track.write(out, 0, out.size) < 0) break
            }
        } finally {
            effects.cancel()
            track.release()
        }
    }

    private fun <T : AudioEffect> effectOrNull(create: () -> T?): T? {
        val effect = runCatching(create).getOrNull() ?: return null
        return runCatching { effect.apply { enabled = true } }.onFailure { effect.release() }.getOrNull()
    }

    companion object {
        const val SAMPLE_RATE = 16_000

        /** 40 ms: few enough packets for the radio, short enough for conversation. */
        const val FRAME_SAMPLES = 640
        private const val ENCODING = AudioFormat.ENCODING_PCM_16BIT
        private const val FRAME_BYTES = FRAME_SAMPLES * 2
        private const val RECORD_BUFFER_FRAMES = 4
        private const val PLAY_BUFFER_FRAMES = 2
    }
}
