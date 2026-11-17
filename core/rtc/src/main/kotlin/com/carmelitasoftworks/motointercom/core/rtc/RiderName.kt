package com.carmelitasoftworks.motointercom.core.rtc

/**
 * LiveKit identities must be unique, but two riders can share a name. We join
 * as "Name~xxxx" and show only "Name".
 */
object RiderName {
    private const val SEPARATOR = '~'

    fun unique(displayName: String, suffix: String): String = "${displayName.trim()}$SEPARATOR$suffix"

    fun display(participantName: String): String = participantName.substringBeforeLast(SEPARATOR).ifBlank { "?" }
}
