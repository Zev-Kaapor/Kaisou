package com.kaisou.app.core.auth

// JSON parsing
import org.json.JSONObject
// Java utilities
import java.util.Base64

// The access token that AniList shows on its page for copying, a JWT whose body says which API client it was made for. It is only
// used when it was made for the client of this app, so a token of another service that happens to be in the clipboard is never sent
object PinToken {
    private const val Parts = 3
    private const val BodyPart = 1

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
