package com.kaisou.app.core.auth

// JSON parsing
import org.json.JSONObject
// Java utilities
import java.util.Base64

// The token AniList shows for copying, only used when it was made for the client of this app
object PinToken {
    // The parts of a token and the one that has its body
    private const val Parts = 3
    private const val BodyPart = 1

    // Tells whether the text is a token made for this client
    fun isFor(text: String, clientId: String): Boolean {
        if (clientId.isEmpty()) return false
        val parts = text.trim().split('.')
        if (parts.size != Parts) return false
        return try {
            val body = String(Base64.getUrlDecoder().decode(parts[BodyPart]), Charsets.UTF_8)
            JSONObject(body).optString("aud") == clientId
        } catch (_: Exception) {
            false
        }
    }
}
