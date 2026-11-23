package com.kaisou.app.features.title.data

// JSON parsing
import org.json.JSONObject
// Coroutines
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
// Java networking and time
import java.io.IOException
import java.net.HttpURLConnection
import java.net.URL
import java.time.LocalDate
// Project resources
import com.kaisou.app.core.net.Gates
import com.kaisou.app.core.net.TooManyRequests

// The score IMDb has for each episode of a series, from the same open door its own site uses to show them, the GraphQL one, which answers a plain
// request and gives all the episodes of a series in a page or a few, each with its score, the day it aired and its season. It is not a door
// IMDb documents, so it can change, and then the other sources are used
object ImdbClient {
    private const val ApiUrl = "https://api.graphql.imdb.com/"
    private const val TimeoutMillis = 15_000
    private const val PageSize = 250
    private const val MaxPages = 8
    // The fewest people that can have scored an episode for the score to be taken, since a score of one person is not what people think of it
    private const val MinVotes = 3

    internal const val Query = """query(${'$'}id:ID!,${'$'}after:ID){title(id:${'$'}id){episodes{episodes(first:$PageSize,after:${'$'}after){pageInfo{hasNextPage endCursor} edges{node{releaseDate{year month day} ratingsSummary{aggregateRating voteCount} series{episodeNumber{episodeNumber seasonNumber}}}}}}}}"""

    // An episode of a season of IMDb: its number in the season, the day it aired as 2026-10-03 and its score from 0 to 10
    class Episode(val season: Int, val number: Int, val released: String?, val rating: Float)

    // A page of the answer: the episodes that have a score, whether there is another page and where it begins
    class Page(val episodes: List<Episode>, val hasNext: Boolean, val cursor: String?)

    // The episodes of all the seasons of a series that have a score, and whether all the pages came. Null when not even the first did. Throws when
    // IMDb could not be reached
    suspend fun episodes(imdbId: String): Pair<List<Episode>, Boolean>? = withContext(Dispatchers.IO) {
        val found = ArrayList<Episode>()
        var cursor: String? = null
        repeat(MaxPages) {
            val page = post(imdbId, cursor)?.let(::parsePage) ?: return@withContext if (found.isEmpty()) null else found to false
            found += page.episodes
            if (!page.hasNext || page.cursor == null) return@withContext found to true
            cursor = page.cursor
        }
        found to false
    }

    private fun post(imdbId: String, after: String?): String? = Gates.Imdb.run {
        val connection = URL(ApiUrl).openConnection() as HttpURLConnection
        try {
            connection.requestMethod = "POST"
            connection.connectTimeout = TimeoutMillis
            connection.readTimeout = TimeoutMillis
            connection.doOutput = true
            connection.setRequestProperty("Content-Type", "application/json")
            connection.setRequestProperty("Accept", "application/json")
            connection.setRequestProperty("x-imdb-client-name", "imdb-web-next")
            connection.setRequestProperty("x-imdb-user-country", "US")
            connection.setRequestProperty("Referer", "https://www.imdb.com/")
            val variables = JSONObject().put("id", imdbId).put("after", after ?: JSONObject.NULL)
            connection.outputStream.use { it.write(JSONObject().put("query", Query).put("variables", variables).toString().toByteArray(Charsets.UTF_8)) }
            when (connection.responseCode) {
                HttpURLConnection.HTTP_OK -> connection.inputStream.bufferedReader(Charsets.UTF_8).readText()
                TooManyRequests.Code -> throw TooManyRequests.of(connection)
                else -> throw IOException("IMDb answered ${connection.responseCode}")
            }
        } finally {
            connection.disconnect()
        }
    }

    // A page from the text of the answer, the ones nobody scored enough are left out. Null when the answer is not one of episodes
    internal fun parsePage(text: String): Page? =
        runCatching {
            val episodes = JSONObject(text).getJSONObject("data").getJSONObject("title").getJSONObject("episodes").getJSONObject("episodes")
            val edges = episodes.getJSONArray("edges")
            val info = episodes.optJSONObject("pageInfo")
            Page(
                episodes = (0 until edges.length()).mapNotNull { index -> episode(edges.getJSONObject(index).getJSONObject("node")) },
                hasNext = info?.optBoolean("hasNextPage") ?: false,
                cursor = info?.takeIf { !it.isNull("endCursor") }?.getString("endCursor")
            )
        }.getOrNull()

    private fun episode(node: JSONObject): Episode? {
        val numbers = node.optJSONObject("series")?.optJSONObject("episodeNumber") ?: return null
        val summary = node.optJSONObject("ratingsSummary") ?: return null
        if (summary.isNull("aggregateRating") || summary.optInt("voteCount") < MinVotes) return null
        val rating = summary.optDouble("aggregateRating", 0.0).toFloat()
        if (rating <= 0f) return null
        return Episode(numbers.optInt("seasonNumber"), numbers.optInt("episodeNumber"), day(node.optJSONObject("releaseDate")), rating)
    }

    // The day of an episode as 2026-10-03 from the year, the month and the day IMDb gives, null when it is not known
    private fun day(date: JSONObject?): String? =
        date?.let { runCatching { LocalDate.of(it.getInt("year"), it.getInt("month"), it.getInt("day")).toString() }.getOrNull() }
}
