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

// The score IMDb has for each episode of a series, from the open GraphQL door its own site uses, which can change
object ImdbClient {
    // Address of the API, the time to wait, the size of a page and how many pages are read
    private const val ApiUrl = "https://api.graphql.imdb.com/"
    private const val TimeoutMillis = 15_000
    private const val PageSize = 250
    private const val MaxPages = 8

    // The fewest people that can have scored an episode for the score to be taken
    private const val MinVotes = 3

    // The seconds in a minute, to give the length of an episode in minutes
    private const val SecondsPerMinute = 60

    // The GraphQL query of the episodes of a series
    internal const val Query = """query(${'$'}id:ID!,${'$'}after:ID){title(id:${'$'}id){episodes{episodes(first:$PageSize,after:${'$'}after){pageInfo{hasNextPage endCursor} edges{node{releaseDate{year month day} ratingsSummary{aggregateRating voteCount} series{episodeNumber{episodeNumber seasonNumber}}}}}}}}"""

    // The GraphQL query of what each episode of a series is: its name, text, picture, day and length
    internal const val DetailsQuery = """query(${'$'}id:ID!,${'$'}after:ID){title(id:${'$'}id){episodes{episodes(first:$PageSize,after:${'$'}after){pageInfo{hasNextPage endCursor} edges{node{titleText{text} plot{plotText{plainText}} primaryImage{url} releaseDate{year month day} runtime{seconds} series{episodeNumber{episodeNumber seasonNumber}}}}}}}}"""

    // An episode of a season of IMDb: its number in the season, the day it aired as 2026-10-03 and its score from 0 to 10
    class Episode(val season: Int, val number: Int, val released: String?, val rating: Float)

    // What IMDb says an episode is, any of it may be missing: the name, the text, the picture, the day and the minutes
    class Info(val season: Int, val number: Int, val released: String?, val name: String?, val plot: String?, val image: String?, val minutes: Int?)

    // A page of the details of the episodes, whether there is another page and where it begins
    class DetailsPage(val episodes: List<Info>, val hasNext: Boolean, val cursor: String?)

    // What IMDb says of every episode of every season, null when not even the first page came, throws when unreachable
    suspend fun details(imdbId: String): List<Info>? = withContext(Dispatchers.IO) {
        val found = ArrayList<Info>()
        var cursor: String? = null
        repeat(MaxPages) {
            val page = post(imdbId, cursor, DetailsQuery)?.let(::parseDetails) ?: return@withContext if (found.isEmpty()) null else found
            found += page.episodes
            if (!page.hasNext || page.cursor == null) return@withContext found
            cursor = page.cursor
        }
        found
    }

    // A page of details from the text of the answer, null when the answer is not one of episodes
    internal fun parseDetails(text: String): DetailsPage? =
        runCatching {
            val episodes = JSONObject(text).getJSONObject("data").getJSONObject("title").getJSONObject("episodes").getJSONObject("episodes")
            val edges = episodes.getJSONArray("edges")
            val info = episodes.optJSONObject("pageInfo")
            DetailsPage(
                episodes = (0 until edges.length()).mapNotNull { index -> info(edges.getJSONObject(index).getJSONObject("node")) },
                hasNext = info?.optBoolean("hasNextPage") ?: false,
                cursor = info?.takeIf { !it.isNull("endCursor") }?.getString("endCursor")
            )
        }.getOrNull()

    // Reads what an episode is, null when it does not say its season and number
    private fun info(node: JSONObject): Info? {
        val numbers = node.optJSONObject("series")?.optJSONObject("episodeNumber") ?: return null
        val season = numbers.optInt("seasonNumber")
        val number = numbers.optInt("episodeNumber")
        if (season <= 0 || number <= 0) return null
        return Info(
            season = season,
            number = number,
            released = day(node.optJSONObject("releaseDate")),
            name = node.optJSONObject("titleText")?.let { if (it.isNull("text")) null else it.optString("text") }?.trim()?.takeIf { it.isNotEmpty() },
            plot = node.optJSONObject("plot")?.optJSONObject("plotText")?.let { if (it.isNull("plainText")) null else it.optString("plainText") }?.trim()?.takeIf { it.isNotEmpty() },
            image = node.optJSONObject("primaryImage")?.let { if (it.isNull("url")) null else it.optString("url") }?.takeIf { it.isNotBlank() },
            minutes = node.optJSONObject("runtime")?.optInt("seconds")?.takeIf { it > 0 }?.let { (it + SecondsPerMinute / 2) / SecondsPerMinute }
        )
    }

    // A page of the answer: the episodes that have a score, whether there is another page and where it begins
    class Page(val episodes: List<Episode>, val hasNext: Boolean, val cursor: String?)

    // The scored episodes of all the seasons and whether all the pages came, null when not even the first did, throws when unreachable
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

    // Asks IMDb for a page of the episodes, with the scores or with the details
    private fun post(imdbId: String, after: String?, query: String = Query): String? = Gates.Imdb.run {
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
            connection.outputStream.use { it.write(JSONObject().put("query", query).put("variables", variables).toString().toByteArray(Charsets.UTF_8)) }
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

    // Reads an episode with its season, number, day and score
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
