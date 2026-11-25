package com.kaisou.app.features.title.data

// Coroutines
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
// JSON parsing
import org.json.JSONArray
import org.json.JSONObject
// Project resources
import com.kaisou.app.core.cache.AppStore

// The openings and endings of an anime as MyAnimeList lists them through Jikan, each a line like 1: "Song" by Artist (eps 1-12)
object ThemeClient {
    // Where themes are kept, how long, the empty answer, and the labels of an opening and an ending
    private const val StoreSpace = "themes2"
    private const val StoreMillis = 24L * 60L * 60L * 1000L
    private const val EmptyAnswer = "{\"data\":{}}"
    private const val OpeningLabel = "OP"
    private const val EndingLabel = "ED"

    // The line of a theme: its number, the song between quotes, who sings it and, when there are, the episodes it plays in
    private val Line = Regex("""^#?\s*([A-Za-z]?\d*)\s*:?\s*"(.+)"\s+by\s+(.+?)(?:\s*\(eps?\s*([^()]*)\))?\s*$""")

    // The themes, the openings first, empty when MyAnimeList lists none, null when it could not be reached, which is not kept
    suspend fun themes(malId: Int): List<TitleTheme>? = withContext(Dispatchers.IO) {
        val key = malId.toString()
        val body = AppStore.get(StoreSpace, key, StoreMillis) ?: try {
            val fetched = JikanClient.get("/anime/$malId/full") ?: EmptyAnswer
            AppStore.put(StoreSpace, key, fetched)
            fetched
        } catch (_: Exception) {
            return@withContext null
        }
        runCatching { parse(body) }.getOrNull()
    }

    // Reads the openings and endings from the answer of Jikan
    internal fun parse(body: String): List<TitleTheme> {
        val theme = JSONObject(body).optJSONObject("data")?.optJSONObject("theme") ?: return emptyList()
        return lines(theme.optJSONArray("openings")).mapNotNull { line(it, OpeningLabel) } +
            lines(theme.optJSONArray("endings")).mapNotNull { line(it, EndingLabel) }
    }

    // Turns a list of JSON texts into lines
    private fun lines(list: JSONArray?): List<String> = if (list == null) emptyList() else (0 until list.length()).map { list.optString(it) }

    // A line that does not follow the form is kept whole as the song, so nothing that MyAnimeList lists is lost
    private fun line(text: String, kind: String): TitleTheme? {
        val clean = text.trim().takeIf { it.isNotEmpty() } ?: return null
        val match = Line.find(clean) ?: return TitleTheme(kind, clean, null, null)
        val (number, song, artist, episodes) = match.destructured
        return TitleTheme(kind + number, song.trim(), artist.trim().takeIf { it.isNotEmpty() }, episodes.trim().takeIf { it.isNotEmpty() })
    }
}
