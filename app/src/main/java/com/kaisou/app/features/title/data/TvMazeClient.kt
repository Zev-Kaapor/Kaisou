package com.kaisou.app.features.title.data

// JSON parsing
import org.json.JSONArray
import org.json.JSONObject
// Project resources
import com.kaisou.app.core.net.Gates
import com.kaisou.app.core.net.HttpText

// Asks TVmaze, which has a score for each episode of a series and needs no key. The series is found by the id of IMDb or the one of TheTVDB,
// which ani.zip gives
object TvMazeClient {
    private const val ApiUrl = "https://api.tvmaze.com"

    // An episode of a season of TVmaze: its number in the season, the day it aired as 2026-10-03 and its score from 0 to 10
    class Episode(val season: Int, val number: Int, val aired: String?, val rating: Float)

    // The id of the series on TVmaze, null when it does not know it. The id of IMDb is tried first and then the one of TheTVDB. Throws when it
    // could not be asked. Blocks on the network
    fun showId(imdbId: String?, tvdbId: Int?): Int? {
        val queries = listOfNotNull(imdbId?.let { "imdb=$it" }, tvdbId?.let { "thetvdb=$it" })
        for (query in queries) {
            val text = Gates.TvMaze.run { HttpText.get("$ApiUrl/lookup/shows?$query") } ?: continue
            parseShow(text)?.let { return it }
        }
        return null
    }

    // The episodes of the series that have a score, null when TVmaze does not know the series. Throws when it could not be asked. Blocks on the
    // network
    fun episodes(showId: Int): List<Episode>? {
        val text = Gates.TvMaze.run { HttpText.get("$ApiUrl/shows/$showId/episodes") } ?: return null
        return parseEpisodes(text)
    }

    internal fun parseShow(text: String): Int? = runCatching { JSONObject(text).optInt("id").takeIf { it > 0 } }.getOrNull()

    // The episodes of the answer with a number and a score, the specials, which have no number, and the ones nobody scored are left out. Null
    // when the answer is not a list
    internal fun parseEpisodes(text: String): List<Episode>? =
        runCatching {
            val list = JSONArray(text)
            (0 until list.length()).mapNotNull { index ->
                val item = list.getJSONObject(index)
                if (item.isNull("number")) return@mapNotNull null
                val rating = item.optJSONObject("rating")?.takeIf { !it.isNull("average") }?.optDouble("average", 0.0)?.toFloat()?.takeIf { it > 0f } ?: return@mapNotNull null
                Episode(item.optInt("season"), item.getInt("number"), item.optString("airdate").takeIf { it.isNotBlank() }, rating)
            }
        }.getOrNull()
}
