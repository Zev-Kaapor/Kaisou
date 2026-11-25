package com.kaisou.app.features.title.data

// Coroutines
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.withContext
// JSON parsing
import org.json.JSONArray
// Project resources
import com.kaisou.app.core.artwork.TmdbClient
import com.kaisou.app.core.artwork.TmdbEpisode
import com.kaisou.app.core.cache.AppStore
import com.kaisou.app.core.translation.GoogleTranslator

// Puts the name and text of each episode in the language of the app from TMDB, matched by the day it aired, unmatched ones kept
object EpisodeTranslator {
    // Where the texts are kept, how long, how many seasons are read, and the first season
    private const val StoreSpace = "episode-texts2"
    private const val StoreMillis = 7L * 24L * 60L * 60L * 1000L
    private const val MaxSeasons = 30
    private const val FirstSeason = 1

    // How many descriptions go in one request of the translator, which are longer than a name
    private const val OverviewsPerRequest = 5
    private const val EnglishCode = "en"

    // How few people can have scored an episode for the score to be left out, since two votes are not what people think of it
    private const val MinVotes = 3

    // The name TMDB gives an episode it has no name for, which says less than the number does
    private val GenericName = Regex("^\\D*\\d+$")

    // The episodes with their texts in the language, unchanged when it is English, TMDB is not set or it fails
    suspend fun translate(found: EpisodeSet, language: String, region: String): List<TitleEpisode> {
        if (found.tmdbId == null || language == EnglishCode) return found.episodes
        val pairs = paired(found, tagOf(language, region))
        return found.episodes.map { episode ->
            val text = pairs[episode.number] ?: return@map episode
            val name = text.name.takeIf { it.isNotBlank() && !GenericName.matches(it) }
            val overview = text.overview.takeIf { it.isNotBlank() }
            episode.copy(title = name ?: episode.title, overview = overview ?: episode.overview, localized = name != null && (overview != null || episode.overview == null))
        }
    }

    // What the people of TMDB scored each episode, from 0 to 10, leaving out the ones too few people scored
    suspend fun votes(found: EpisodeSet, language: String, region: String): Map<Int, Float> =
        if (found.tmdbId == null) {
            emptyMap()
        } else {
            paired(found, tagOf(language, region)).filterValues { it.votes >= MinVotes && it.vote > 0f }.mapValues { it.value.vote }
        }

    // Fills the episodes TMDB did not give in the language with Google Translate, only the ones asked for, the open season
    suspend fun fill(episodes: List<TitleEpisode>, language: String): List<TitleEpisode> {
        val targets = episodes.filter { !it.localized && (it.title != null || it.overview != null) }
        if (targets.isEmpty()) return emptyList()
        return coroutineScope {
            val titles = async { GoogleTranslator.translateLines(targets.mapNotNull { it.title }, language) }
            val overviews = targets.mapNotNull { it.overview }.chunked(OverviewsPerRequest).map { part -> async { GoogleTranslator.translateLines(part, language) } }
            val titleList = titles.await().iterator()
            val overviewList = overviews.awaitAll().flatten().iterator()
            targets.map { episode ->
                episode.copy(
                    title = if (episode.title != null) titleList.next() else null,
                    overview = if (episode.overview != null) overviewList.next() else null,
                    localized = true
                )
            }
        }
    }

    // Builds the tag of a language and a region
    private fun tagOf(language: String, region: String): String = if (region.isEmpty()) language else "$language-$region"

    // The episode of TMDB for each one of ani.zip, from its seasons and, when many are left, from the first season too
    private suspend fun paired(found: EpisodeSet, tag: String): Map<Int, TmdbEpisode> {
        val tmdbId = found.tmdbId ?: return emptyMap()
        val wanted = found.episodes.mapNotNull { it.season?.takeIf { season -> season > 0 } }.distinct().sorted().take(MaxSeasons).ifEmpty { listOf(FirstSeason) }
        val loaded = load(tmdbId, wanted, tag).toMutableMap()
        var pairs = pair(found.episodes, loaded.values.flatten())
        if (pairs.size < found.episodes.count { it.aired != null } && FirstSeason !in loaded) {
            loaded += load(tmdbId, listOf(FirstSeason), tag)
            pairs = pair(found.episodes, loaded.values.flatten())
        }
        return pairs
    }

    // Pairs the episodes with the texts of TMDB by the day they aired
    private fun pair(episodes: List<TitleEpisode>, texts: List<TmdbEpisode>): Map<Int, TmdbEpisode> =
        DayMatcher.pair(episodes, texts, day = { it.airDate }, order = { it.number })

    // Loads the seasons of a series of TMDB at once
    private suspend fun load(tmdbId: Int, seasons: List<Int>, tag: String): Map<Int, List<TmdbEpisode>> = coroutineScope {
        seasons.map { season -> async { season to season(tmdbId, season, tag) } }.awaitAll().toMap()
    }

    // One season, kept on the phone for a week, and kept as empty when TMDB does not have it
    private suspend fun season(tmdbId: Int, season: Int, tag: String): List<TmdbEpisode> = withContext(Dispatchers.IO) {
        val key = "$tmdbId $season $tag"
        AppStore.get(StoreSpace, key, StoreMillis)?.let { kept -> read(kept)?.let { return@withContext it } }
        val found = TmdbClient.season(tmdbId, season, tag) ?: return@withContext emptyList()
        AppStore.put(StoreSpace, key, write(found))
        found
    }

    // Writes the TMDB episodes as text to keep them
    private fun write(list: List<TmdbEpisode>): String = JSONArray().apply {
        list.forEach { put(JSONArray().put(it.number).put(it.airDate ?: "").put(it.name).put(it.overview).put(it.vote.toDouble()).put(it.votes)) }
    }.toString()

    // Reads the TMDB episodes that were kept
    private fun read(text: String): List<TmdbEpisode>? =
        try {
            val list = JSONArray(text)
            (0 until list.length()).map { index ->
                val item = list.getJSONArray(index)
                TmdbEpisode(item.getInt(0), item.getString(1).takeIf { it.isNotEmpty() }, item.getString(2), item.getString(3), item.getDouble(4).toFloat(), item.getInt(5))
            }
        } catch (_: Exception) {
            null
        }
}
