@file:Suppress("MagicNumber") // G.711 µ-law is defined by its bit layout; the numbers are the spec.

package com.carmelitasoftworks.motointercom.core.nearby

import kotlin.math.abs

/**
 * G.711 µ-law: 16-bit PCM to 8-bit samples and back. Halves the bandwidth with
 * no codec library, and a hands-free headset carries narrowband voice anyway.
 */
internal object MuLaw {
    private const val BIAS = 0x84
    private const val CLIP = 32_635
    private const val SIGN_BIT = 0x80
    private const val SEGMENT_SHIFT = 4
    private const val QUANT_MASK = 0x0F
    private const val SEGMENT_MASK = 0x70
    private const val BYTE_MASK = 0xFF

    private val decodeTable = ShortArray(256) { decodeOne(it.toByte()) }

    fun encode(pcm: ShortArray, count: Int = pcm.size): ByteArray = ByteArray(count) { encodeOne(pcm[it]) }

    fun decode(data: ByteArray, offset: Int = 0, length: Int = data.size - offset): ShortArray =
        ShortArray(length) { decodeTable[data[offset + it].toInt() and BYTE_MASK] }

    private fun encodeOne(sample: Short): Byte {
        val value = sample.toInt()
        val sign = if (value < 0) SIGN_BIT else 0
        val magnitude = (abs(value).coerceAtMost(CLIP)) + BIAS
        var segment = 7
        var mask = 0x4000
        while (segment > 0 && magnitude and mask == 0) {
            segment--
            mask = mask shr 1
        }
        val mantissa = (magnitude shr (segment + 3)) and QUANT_MASK
        return (sign or (segment shl SEGMENT_SHIFT) or mantissa).inv().toByte()
    }

    private fun decodeOne(encoded: Byte): Short {
        val value = encoded.toInt().inv() and BYTE_MASK
        val segment = (value and SEGMENT_MASK) shr SEGMENT_SHIFT
        val magnitude = (((value and QUANT_MASK) shl 3) + BIAS) shl segment
        val sample = magnitude - BIAS
        return (if (value and SIGN_BIT != 0) -sample else sample).toShort()
    }
}
