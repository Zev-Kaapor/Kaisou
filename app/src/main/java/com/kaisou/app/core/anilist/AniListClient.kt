package com.kaisou.app.core.anilist

// Coroutines
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
// JSON parsing
import org.json.JSONObject
// Java networking
import java.net.HttpURLConnection
import java.net.URL

// The user that is signed in
data class AniViewer(
    val id: Int,
    val name: String,
    val avatar: String?
)

// What came of asking AniList who the token belongs to: the user, the token being refused, or no answer at all
sealed interface ViewerResult {
    data class Found(val viewer: AniViewer) : ViewerResult
    data object Rejected : ViewerResult
    data object Unreachable : ViewerResult
}

// Talks to the GraphQL API of AniList
class AniListClient {

    // The user the token belongs to
    suspend fun viewer(token: String): ViewerResult = withContext(Dispatchers.IO) {
        try {
            val connection = URL(ApiUrl).openConnection() as HttpURLConnection
            try {
                connection.requestMethod = "POST"
                connection.connectTimeout = TimeoutMillis
                connection.readTimeout = TimeoutMillis
                connection.doOutput = true
                connection.setRequestProperty("Content-Type", "application/json")
                connection.setRequestProperty("Accept", "application/json")
                connection.setRequestProperty("Authorization", "Bearer $token")
                connection.outputStream.use { it.write(JSONObject().put("query", ViewerQuery).toString().toByteArray()) }
                when (connection.responseCode) {
                    HttpURLConnection.HTTP_OK -> {
                        val body = connection.inputStream.bufferedReader().use { it.readText() }
                        val json = JSONObject(body).optJSONObject("data")?.optJSONObject("Viewer")
                        if (json == null) ViewerResult.Rejected else ViewerResult.Found(parseViewer(json))
                    }
                    // AniList answers a token that is not valid with a bad request
                    HttpURLConnection.HTTP_BAD_REQUEST, HttpURLConnection.HTTP_UNAUTHORIZED -> ViewerResult.Rejected
                    else -> ViewerResult.Unreachable
                }
            } finally {
                connection.disconnect()
            }
        } catch (_: Exception) {
            ViewerResult.Unreachable
        }
    }

    private fun parseViewer(json: JSONObject): AniViewer =
        AniViewer(
            id = json.getInt("id"),
            name = json.getString("name"),
            // A value that is null on the server comes as the text "null" with the plain getters
            avatar = json.optJSONObject("avatar")?.takeUnless { it.isNull("large") }?.getString("large")
        )

    private companion object {
        const val ApiUrl = "https://graphql.anilist.co"
        const val TimeoutMillis = 15_000

        const val ViewerQuery = """
            query {
              Viewer {
                id
                name
                avatar { large }
              }
            }
        """
    }
}
