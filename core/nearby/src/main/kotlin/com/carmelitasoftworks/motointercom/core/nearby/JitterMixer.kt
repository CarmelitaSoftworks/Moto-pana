package com.carmelitasoftworks.motointercom.core.nearby

/**
 * Per-rider jitter buffers mixed into one playback stream. Frames arrive in
 * bursts over the radio; each rider plays after a short prefill and the queue is
 * capped so delay never builds up. Thread-safe: radio thread offers, playback thread mixes.
 */
internal class JitterMixer(
    private val frameSize: Int,
    private val prefillFrames: Int = 2,
    private val maxQueuedFrames: Int = 8,
) {
    private class Buffer {
        val frames = ArrayDeque<ShortArray>()
        var playing = false
    }

    private val buffers = HashMap<String, Buffer>()
    private val muted = HashSet<String>()

    @Synchronized
    fun offer(riderId: String, frame: ShortArray) {
        if (riderId in muted || frame.size != frameSize) return
        val buffer = buffers.getOrPut(riderId) { Buffer() }
        buffer.frames.addLast(frame)
        while (buffer.frames.size > maxQueuedFrames) buffer.frames.removeFirst()
    }

    @Synchronized
    fun setMuted(riderId: String, mute: Boolean) {
        if (mute) {
            muted += riderId
            buffers.remove(riderId)
        } else {
            muted -= riderId
        }
    }

    @Synchronized
    fun remove(riderId: String) {
        buffers.remove(riderId)
    }

    @Synchronized
    fun clear() {
        buffers.clear()
        muted.clear()
    }

    /** Mixes the next frame of every rider into [out]; returns false when nobody is talking. */
    @Synchronized
    fun mixNext(out: ShortArray): Boolean {
        val mix = IntArray(frameSize)
        var any = false
        for (buffer in buffers.values) {
            if (!buffer.playing && buffer.frames.size >= prefillFrames) buffer.playing = true
            val frame = if (buffer.playing) buffer.frames.removeFirstOrNull() else null
            if (frame != null) {
                any = true
                for (i in 0 until frameSize) mix[i] += frame[i]
            } else {
                // Ran dry: wait for a fresh prefill instead of stuttering frame by frame.
                buffer.playing = false
            }
        }
        for (i in 0 until frameSize) {
            out[i] = mix[i].coerceIn(Short.MIN_VALUE.toInt(), Short.MAX_VALUE.toInt()).toShort()
        }
        return any
    }
}
