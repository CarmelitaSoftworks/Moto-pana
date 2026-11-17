package com.carmelitasoftworks.motointercom.core.nearby

import kotlin.math.sqrt

/**
 * Decides per frame whether the rider is talking, so silence is not sent
 * ("voz abierta con VAD", CLAUDE.md §4). Energy threshold that adapts to the
 * background noise floor, plus a hangover so word endings are not clipped.
 */
internal class VoiceActivityDetector(
    private val hangoverFrames: Int = 8,
    private val minThreshold: Double = 400.0,
    private val noiseMultiplier: Double = 3.0,
) {
    private var noiseFloor = minThreshold / noiseMultiplier
    private var framesSinceVoice = Int.MAX_VALUE

    /** Feeds one frame; returns true while it (or the recent past) contains speech. */
    fun isSpeech(frame: ShortArray, count: Int = frame.size): Boolean {
        val level = rms(frame, count)
        val threshold = maxOf(minThreshold, noiseFloor * noiseMultiplier)
        if (level > threshold) {
            framesSinceVoice = 0
            // Creep up very slowly even "during speech": real speech pauses every few seconds,
            // steady wind does not, so constant noise eventually stops counting as voice.
            noiseFloor += (level - noiseFloor) * NOISE_ADAPTATION_LOUD
        } else {
            noiseFloor += (level - noiseFloor) * NOISE_ADAPTATION_QUIET
            if (framesSinceVoice != Int.MAX_VALUE) framesSinceVoice++
        }
        return framesSinceVoice <= hangoverFrames
    }

    private fun rms(frame: ShortArray, count: Int): Double {
        if (count == 0) return 0.0
        var sum = 0.0
        for (i in 0 until count) {
            val sample = frame[i].toDouble()
            sum += sample * sample
        }
        return sqrt(sum / count)
    }

    private companion object {
        const val NOISE_ADAPTATION_QUIET = 0.05
        const val NOISE_ADAPTATION_LOUD = 0.005
    }
}
