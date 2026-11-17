package com.carmelitasoftworks.motointercom.core.rtc

import java.io.IOException
import java.net.HttpURLConnection
import java.net.URL
import javax.inject.Inject
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONException
import org.json.JSONObject

/**
 * Gets tokens from LiveKit Cloud's development token server, so the MVP works
 * without our own backend. Development only: anyone with the id can get a token.
 * The Cloudflare Worker (work order step 2) replaces it.
 */
class DevTokenServerProvider internal constructor(
    private val tokenServerId: String,
    private val endpoint: String,
) : TokenProvider {
    @Inject constructor() : this(BuildConfig.TOKEN_SERVER_ID, ENDPOINT)

    override suspend fun fetch(roomName: String, participantName: String): ConnectionDetails {
        if (tokenServerId.isBlank()) throw TokenException(TokenException.Reason.NOT_CONFIGURED)
        val body = JSONObject()
            .put("roomName", roomName)
            .put("participantName", participantName)
            .toString()
        val (status, response) = withContext(Dispatchers.IO) { post(body) }
        if (status !in HTTP_SUCCESS) throw TokenException(TokenException.Reason.REJECTED)
        return parse(response)
    }

    private fun post(body: String): Pair<Int, String> = try {
        val connection = URL(endpoint).openConnection() as HttpURLConnection
        try {
            connection.requestMethod = "POST"
            connection.connectTimeout = TIMEOUT_MS
            connection.readTimeout = TIMEOUT_MS
            connection.doOutput = true
            connection.setRequestProperty("Content-Type", "application/json")
            connection.setRequestProperty("X-Sandbox-ID", tokenServerId)
            connection.outputStream.use { it.write(body.toByteArray()) }
            val status = connection.responseCode
            val stream = if (status in HTTP_SUCCESS) connection.inputStream else connection.errorStream
            status to (stream?.bufferedReader()?.use { it.readText() }.orEmpty())
        } finally {
            connection.disconnect()
        }
    } catch (e: IOException) {
        throw TokenException(TokenException.Reason.NETWORK, e)
    }

    internal companion object {
        const val ENDPOINT = "https://cloud-api.livekit.io/api/sandbox/connection-details"
        private const val TIMEOUT_MS = 10_000
        private val HTTP_SUCCESS = 200..299

        /** Accepts both the camelCase and snake_case field names LiveKit has used. */
        fun parse(response: String): ConnectionDetails = try {
            val json = JSONObject(response)
            val url = json.optString("serverUrl").ifBlank { json.optString("server_url") }
            val token = json.optString("participantToken").ifBlank { json.optString("participant_token") }
            if (url.isBlank() || token.isBlank()) throw TokenException(TokenException.Reason.REJECTED)
            ConnectionDetails(serverUrl = url, token = token)
        } catch (e: JSONException) {
            throw TokenException(TokenException.Reason.REJECTED, e)
        }
    }
}
