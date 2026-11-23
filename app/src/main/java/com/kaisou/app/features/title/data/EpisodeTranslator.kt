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

// Puts the name and what each episode tells in the language of the app, from TMDB. ani.zip only has them in English, and its seasons are not the
// ones of TMDB, so an episode is matched to the one of TMDB that aired the same day, or the day before or after it for the time zones. An
// episode with no match keeps what it has. What the people of TMDB scored each episode is had from the same answer
object EpisodeTranslator {
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

    // The episodes with their texts in the language, the same ones when the language is English, when TMDB is not set or when it cannot be reached
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

    // What the people of TMDB scored each episode, from 0 to 10, by the number of the episode. The episodes that too few people scored are left out
    suspend fun votes(found: EpisodeSet, language: String, region: String): Map<Int, Float> =
        if (found.tmdbId == null) {
            emptyMap()
        } else {
            paired(found, tagOf(language, region)).filterValues { it.votes >= MinVotes && it.vote > 0f }.mapValues { it.value.vote }
        }

    // The ones of the episodes that TMDB did not give in the language, which are the ones of a series it does not have or has only in part,
    // put in it by Google Translate. It is only for the episodes that are asked for, the ones of the season that is open, so a long series
    // does not send hundreds of texts
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

    private fun tagOf(language: String, region: String): String = if (region.isEmpty()) language else "$language-$region"

    // The episode of TMDB for each episode of ani.zip, by number, from the seasons of the series that ani.zip says and, when too many are left
    // without one, from the first season too, since the seasons of TMDB are not always the ones of TheTVDB and a series may have all of them
    // in the first
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

    private fun pair(episodes: List<TitleEpisode>, texts: List<TmdbEpisode>): Map<Int, TmdbEpisode> =
        DayMatcher.pair(episodes, texts, day = { it.airDate }, order = { it.number })

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

    private fun write(list: List<TmdbEpisode>): String = JSONArray().apply {
        list.forEach { put(JSONArray().put(it.number).put(it.airDate ?: "").put(it.name).put(it.overview).put(it.vote.toDouble()).put(it.votes)) }
    }.toString()

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
