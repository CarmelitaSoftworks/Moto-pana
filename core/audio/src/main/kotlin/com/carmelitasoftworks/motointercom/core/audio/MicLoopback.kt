package com.carmelitasoftworks.motointercom.core.audio

import android.annotation.SuppressLint
import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioRecord
import android.media.AudioTrack
import android.media.MediaRecorder
import javax.inject.Inject
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.isActive
import kotlinx.coroutines.withContext

/**
 * Plays the microphone back to the rider so a headset can be checked without
 * a second person. Runs until the calling coroutine is cancelled.
 */
fun interface MicLoopback {
    /**
     * Requires the RECORD_AUDIO permission and an [AudioRouter] that was started.
     * Audio is only played while [shouldPlay] returns true, so callers can mute
     * it on the loudspeaker, where it would feed back into the mic.
     */
    suspend fun run(shouldPlay: () -> Boolean)
}

class AndroidMicLoopback @Inject constructor() : MicLoopback {
    @SuppressLint("MissingPermission") // Documented precondition of MicLoopback.run.
    override suspend fun run(shouldPlay: () -> Boolean) = withContext(Dispatchers.IO) {
        val recordBuffer = AudioRecord.getMinBufferSize(SAMPLE_RATE, AudioFormat.CHANNEL_IN_MONO, ENCODING)
        val trackBuffer = AudioTrack.getMinBufferSize(SAMPLE_RATE, AudioFormat.CHANNEL_OUT_MONO, ENCODING)
        val record = AudioRecord(
            MediaRecorder.AudioSource.VOICE_COMMUNICATION,
            SAMPLE_RATE,
            AudioFormat.CHANNEL_IN_MONO,
            ENCODING,
            recordBuffer * 2,
        )
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
            .setBufferSizeInBytes(trackBuffer * 2)
            .setTransferMode(AudioTrack.MODE_STREAM)
            .build()
        try {
            if (record.state != AudioRecord.STATE_INITIALIZED) return@withContext
            record.startRecording()
            track.play()
            val samples = ShortArray(recordBuffer / 2)
            while (isActive) {
                val read = record.read(samples, 0, samples.size)
                if (read < 0) break
                if (read > 0 && shouldPlay()) track.write(samples, 0, read)
            }
        } finally {
            record.release()
            track.release()
        }
    }

    private companion object {
        // 16 kHz mono is what a hands-free (HFP) link carries at best.
        const val SAMPLE_RATE = 16_000
        const val ENCODING = AudioFormat.ENCODING_PCM_16BIT
    }
}
