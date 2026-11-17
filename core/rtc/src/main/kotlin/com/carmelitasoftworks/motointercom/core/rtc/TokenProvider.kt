package com.carmelitasoftworks.motointercom.core.rtc

/** Where and how to connect to one group's room. */
data class ConnectionDetails(val serverUrl: String, val token: String)

fun interface TokenProvider {
    /** Gets a token for [roomName] as [participantName]; throws [TokenException] on failure. */
    suspend fun fetch(roomName: String, participantName: String): ConnectionDetails
}

class TokenException(val reason: Reason, cause: Throwable? = null) : Exception(reason.name, cause) {
    enum class Reason {
        /** The app was built without a token server id. */
        NOT_CONFIGURED,

        /** No network, timeout, or the server could not be reached. */
        NETWORK,

        /** The server answered, but not with usable connection details. */
        REJECTED,
    }
}
