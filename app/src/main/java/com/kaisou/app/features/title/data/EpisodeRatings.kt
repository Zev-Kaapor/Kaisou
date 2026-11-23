package com.kaisou.app.features.title.data

// Android logging
import android.util.Log
// Coroutines
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
// JSON parsing
import org.json.JSONArray
import org.json.JSONObject
// Project resources
import com.kaisou.app.core.cache.AppStore

// What people score each episode, from 0 to 10, by the number of the episode. Each episode takes the first source that has a score for it:
// IMDb, through the open door its own site uses, then MyAnimeList, through Jikan, then TVmaze, then TMDB. AniList has no score for
// episodes. An episode none of them scored shows none, since the one ani.zip has comes from TheTVDB and is far from what people think of the
// anime
object EpisodeRatings {
    private const val Tag = "EpisodeRatings"
    private const val ImdbSpace = "imdb-ratings2"
    private const val MalSpace = "mal-ratings1"
    private const val TvMazeSpace = "tvmaze-ratings1"
    private const val StoreMillis = 7L * 24L * 60L * 60L * 1000L
    private const val MaxPages = 6
    // MyAnimeList scores an episode from 0 to 5
    private const val MalScale = 2f

    // The scores as the sources give them: each time one answers, what is known is given to the one that asked, so a slow one does not hold the
    // others back. An episode shows the score of the first source that has it by then, and changes to
    // the one of a better source if it comes after
    suspend fun rate(found: EpisodeSet, language: String, region: String, onScores: suspend (Map<Int, Float>) -> Unit) {
        val sources = listOf<Pair<String, suspend () -> Map<Int, Float>>>(
            "IMDb" to { imdb(found) },
            "MyAnimeList" to { mal(found) },
            "TVmaze" to { tvMaze(found) },
            "TMDB" to { EpisodeTranslator.votes(found, language, region) }
        )
        val answers = arrayOfNulls<Map<Int, Float>>(sources.size)
        val turn = Mutex()
        coroutineScope {
            sources.forEachIndexed { index, (name, source) ->
                launch {
                    // A source that fails is left out, and the log says which and why
                    val scores = runCatching { source() }.onFailure { Log.w(Tag, "The scores of $name failed", it) }.getOrDefault(emptyMap())
                    Log.i(Tag, "$name gave ${scores.size} scores for ${found.episodes.size} episodes")
                    turn.withLock {
                        answers[index] = scores
                        onScores(merge(answers.map { it.orEmpty() }))
                    }
                }
            }
        }
    }

    // The scores of all the sources in one, each episode with the one of the first source that has it
    internal fun merge(sources: List<Map<Int, Float>>): Map<Int, Float> {
        val merged = LinkedHashMap<Int, Float>()
        sources.forEach { source -> source.forEach { (number, score) -> merged.putIfAbsent(number, score) } }
        return merged
    }

    // The scores of IMDb, matched to the episodes by the day they aired since IMDb numbers the seasons its own way. What was found is kept for
    // a week when all the seasons came, and when some did not what came is used and not kept
    private suspend fun imdb(found: EpisodeSet): Map<Int, Float> {
        val id = found.imdbId
        if (id == null) {
            Log.i(Tag, "IMDb: the title has no id of IMDb")
            return emptyMap()
        }
        val episodes = imdbEpisodes(id)
        val pairs = DayMatcher.pair(found.episodes, episodes, day = { it.released }, order = { it.season * SeasonStride + it.number })
        Log.i(Tag, "IMDb $id: ${episodes.size} episodes with a score, ${pairs.size} matched by the day")
        return pairs.mapValues { it.value.rating }
    }

    private suspend fun imdbEpisodes(id: String): List<ImdbClient.Episode> {
        withContext(Dispatchers.IO) { AppStore.get(ImdbSpace, id, StoreMillis) }?.let(::readImdb)?.let { return it }
        val (episodes, complete) = ImdbClient.episodes(id) ?: run {
            Log.w(Tag, "IMDb $id: no page of episodes came")
            return emptyList()
        }
        if (complete && episodes.isNotEmpty()) withContext(Dispatchers.IO) { AppStore.put(ImdbSpace, id, writeImdb(episodes)) }
        return episodes
    }

    // The scores of MyAnimeList, which numbers the episodes of a title as ani.zip does, so they are taken by the number. A score is from 0 to 5
    private suspend fun mal(found: EpisodeSet): Map<Int, Float> {
        val id = found.malId
        if (id == null) {
            Log.i(Tag, "MyAnimeList: the title has no id of MyAnimeList")
            return emptyMap()
        }
        return withContext(Dispatchers.IO) {
            AppStore.get(MalSpace, id.toString(), StoreMillis)?.let(::readMal)?.let { return@withContext it }
            val scores = LinkedHashMap<Int, Float>()
            var complete = false
            for (page in 1..MaxPages) {
                val text = JikanClient.get("/anime/$id/episodes?page=$page") ?: break
                val answer = parseMal(text) ?: break
                scores += answer.first
                if (!answer.second) {
                    complete = true
                    break
                }
            }
            if (complete) AppStore.put(MalSpace, id.toString(), writeMal(scores))
            scores
        }
    }

    // The scores of TVmaze, matched to the episodes by the day they aired since it numbers the seasons its own way. What was found is kept for a
    // week, and nothing is kept when the series is not known or could not be asked
    private suspend fun tvMaze(found: EpisodeSet): Map<Int, Float> {
        val key = found.imdbId ?: found.tvdbId?.let { "tvdb$it" }
        if (key == null) {
            Log.i(Tag, "TVmaze: the title has no id of IMDb or of TheTVDB")
            return emptyMap()
        }
        val episodes = withContext(Dispatchers.IO) {
            AppStore.get(TvMazeSpace, key, StoreMillis)?.let(::readTvMaze)?.let { return@withContext it }
            val show = TvMazeClient.showId(found.imdbId, found.tvdbId) ?: return@withContext emptyList()
            val asked = TvMazeClient.episodes(show) ?: return@withContext emptyList()
            AppStore.put(TvMazeSpace, key, writeTvMaze(asked))
            asked
        }
        return DayMatcher.pair(found.episodes, episodes, day = { it.aired }, order = { it.season * SeasonStride + it.number }).mapValues { it.value.rating }
    }

    // The scores of an answer of Jikan for the episodes of an anime, by number and from 0 to 10, and whether there is another page. Null when
    // the answer is not one
    internal fun parseMal(text: String): Pair<Map<Int, Float>, Boolean>? =
        runCatching {
            val json = JSONObject(text)
            val list = json.getJSONArray("data")
            val scores = LinkedHashMap<Int, Float>()
            for (index in 0 until list.length()) {
                val item = list.getJSONObject(index)
                val score = if (item.isNull("score")) 0.0 else item.optDouble("score", 0.0)
                if (score > 0.0) scores[item.getInt("mal_id")] = score.toFloat() * MalScale
            }
            scores to (json.optJSONObject("pagination")?.optBoolean("has_next_page") ?: false)
        }.getOrNull()

    private fun writeImdb(list: List<ImdbClient.Episode>): String = JSONArray().apply {
        list.forEach { put(JSONArray().put(it.season).put(it.number).put(it.released ?: "").put(it.rating.toDouble())) }
    }.toString()

    private fun readImdb(text: String): List<ImdbClient.Episode>? =
        try {
            val list = JSONArray(text)
            (0 until list.length()).map { index ->
                val item = list.getJSONArray(index)
                ImdbClient.Episode(item.getInt(0), item.getInt(1), item.getString(2).takeIf { it.isNotEmpty() }, item.getDouble(3).toFloat())
            }
        } catch (_: Exception) {
            null
        }

    private fun writeTvMaze(list: List<TvMazeClient.Episode>): String = JSONArray().apply {
        list.forEach { put(JSONArray().put(it.season).put(it.number).put(it.aired ?: "").put(it.rating.toDouble())) }
    }.toString()

    private fun readTvMaze(text: String): List<TvMazeClient.Episode>? =
        try {
            val list = JSONArray(text)
            (0 until list.length()).map { index ->
                val item = list.getJSONArray(index)
                TvMazeClient.Episode(item.getInt(0), item.getInt(1), item.getString(2).takeIf { it.isNotEmpty() }, item.getDouble(3).toFloat())
            }
        } catch (_: Exception) {
            null
        }

    private fun writeMal(scores: Map<Int, Float>): String = scores.entries.joinToString(",") { "${it.key}=${it.value}" }

    private fun readMal(text: String): Map<Int, Float>? =
        try {
            if (text.isBlank()) emptyMap() else text.split(",").associate { it.substringBefore("=").toInt() to it.substringAfter("=").toFloat() }
        } catch (_: Exception) {
            null
        }

    // How many numbers a season takes in the order of the episodes of a day, which is by season first
    private const val SeasonStride = 10_000
}
