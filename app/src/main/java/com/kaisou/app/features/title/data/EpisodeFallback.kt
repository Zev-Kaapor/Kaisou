package com.kaisou.app.features.title.data

// Coroutines
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.withContext
// JSON parsing
import org.json.JSONArray
// Java time and math
import java.time.LocalDate
import java.time.temporal.ChronoUnit
import kotlin.math.abs
// Project resources
import com.kaisou.app.core.artwork.ArtworkRepository
import com.kaisou.app.core.artwork.TmdbClient
import com.kaisou.app.core.artwork.TmdbEpisode
import com.kaisou.app.core.cache.AppStore

// The episodes of a title that ani.zip has none for: the names, texts, pictures and days from IMDb, then from TMDB
object EpisodeFallback {
    // Where the IMDb details are kept and for how long, and the language TMDB is asked in, since the app translates after
    private const val ImdbSpace = "imdb-details1"
    private const val ImdbStoreMillis = 6L * 60L * 60L * 1000L
    private const val BaseLanguage = "en-US"

    // How many days from the start of the title an episode can be to be taken as its first one
    private const val FirstEpisodeDays = 3L

    // The name TMDB gives an episode it has no name for, which says less than the number does
    private val GenericName = Regex("^\\D*\\d+$")

    // The episodes the title has aired, from IMDb and TMDB, null when neither knows them, never throws
    suspend fun build(title: TitleUi, season: Int?): EpisodeSet? {
        val ids = ArtworkRepository.Shared.titleIds(title.anilistId, title.query)?.takeIf { it.kind == "tv" && it.tmdbId != null } ?: return null
        val tmdbId = ids.tmdbId ?: return null
        val seasonNumber = season?.takeIf { it > 0 } ?: 1
        return coroutineScope {
            val imdbId = ids.imdbId ?: withContext(Dispatchers.IO) { TmdbClient.imdbIdOf(tmdbId) }
            val tmdb = async { runCatching { EpisodeTranslator.season(tmdbId, seasonNumber, BaseLanguage) }.getOrDefault(emptyList()) }
            val imdb = async { imdbId?.let { runCatching { imdbInfo(it) }.getOrDefault(emptyList()) }.orEmpty().filter { info -> info.season == seasonNumber } }
            val airing = Airing(title.startDate, title.nextAiring?.episode, title.releaseState == ReleaseState.Finished, title.episodes)
            val episodes = merge(airing, seasonNumber, tmdb.await(), imdb.await(), LocalDate.now())
            if (episodes.isEmpty()) null else EpisodeSet(episodes, tmdbId, imdbId, title.malId, ids.tvdbId)
        }
    }

    // What the title says of its own airing: its first day, the episode that comes next, whether it ended and how many it has
    internal class Airing(val start: LocalDate?, val nextEpisode: Int?, val finished: Boolean, val total: Int?)

    // The episodes aired: the name from TMDB, as IMDb gives anime in romaji, the text and picture from IMDb first, the day from TMDB
    internal fun merge(airing: Airing, season: Int, tmdb: List<TmdbEpisode>, imdb: List<ImdbClient.Info>, today: LocalDate): List<TitleEpisode> {
        val offset = firstEpisodeOffset(airing.start, tmdb.map { it.number to it.airDate }, imdb.map { it.number to it.released })
        val tmdbByNumber = tmdb.associateBy { it.number }
        val imdbByNumber = imdb.associateBy { it.number }
        val numbers = (tmdbByNumber.keys + imdbByNumber.keys).sorted()
        val aired = airedCount(airing, offset, numbers, today, tmdbByNumber, imdbByNumber)
        return (1..aired).mapNotNull { number ->
            val source = number + offset
            val fromTmdb = tmdbByNumber[source]
            val fromImdb = imdbByNumber[source]
            if (fromTmdb == null && fromImdb == null) return@mapNotNull null
            TitleEpisode(
                number = number,
                title = fromTmdb?.name?.takeIf { it.isNotBlank() && !GenericName.matches(it) } ?: fromImdb?.name,
                image = fromImdb?.image ?: fromTmdb?.still,
                minutes = fromImdb?.minutes ?: fromTmdb?.minutes,
                aired = fromTmdb?.airDate ?: fromImdb?.released,
                overview = fromImdb?.plot ?: fromTmdb?.overview?.takeIf { it.isNotBlank() },
                season = season
            )
        }
    }

    // How many episodes of the season came before the one that opens the title, when the season holds more than one title
    private fun firstEpisodeOffset(start: LocalDate?, vararg days: List<Pair<Int, String?>>): Int {
        if (start == null) return 0
        for (list in days) {
            val first = list.filter { (_, day) -> day != null }.minByOrNull { (_, day) -> abs(ChronoUnit.DAYS.between(start, LocalDate.parse(day))) } ?: continue
            if (abs(ChronoUnit.DAYS.between(start, LocalDate.parse(first.second))) <= FirstEpisodeDays) return first.first - 1
        }
        return 0
    }

    // How many episodes the title has aired: until the next one, all of a finished one, or the ones whose day came
    private fun airedCount(
        airing: Airing,
        offset: Int,
        numbers: List<Int>,
        today: LocalDate,
        tmdb: Map<Int, TmdbEpisode>,
        imdb: Map<Int, ImdbClient.Info>
    ): Int {
        airing.nextEpisode?.let { return it - 1 }
        if (airing.finished) return airing.total ?: (numbers.maxOrNull()?.minus(offset) ?: 0)
        val last = numbers.filter { number ->
            val day = tmdb[number]?.airDate ?: imdb[number]?.released
            day != null && !LocalDate.parse(day).isAfter(today)
        }.maxOrNull()
        return last?.minus(offset) ?: 0
    }

    // What IMDb says of the episodes of a series, from the phone when it is recent, kept only when something came
    private suspend fun imdbInfo(imdbId: String): List<ImdbClient.Info> {
        withContext(Dispatchers.IO) { AppStore.get(ImdbSpace, imdbId, ImdbStoreMillis) }?.let(::read)?.let { return it }
        val found = ImdbClient.details(imdbId).orEmpty()
        if (found.isNotEmpty()) withContext(Dispatchers.IO) { AppStore.put(ImdbSpace, imdbId, write(found)) }
        return found
    }

    // Writes the IMDb details as text to keep them
    private fun write(list: List<ImdbClient.Info>): String = JSONArray().apply {
        list.forEach {
            put(JSONArray().put(it.season).put(it.number).put(it.released ?: "").put(it.name ?: "").put(it.plot ?: "").put(it.image ?: "").put(it.minutes ?: 0))
        }
    }.toString()

    // Reads the IMDb details that were kept, null when the text is broken
    private fun read(text: String): List<ImdbClient.Info>? =
        try {
            val list = JSONArray(text)
            (0 until list.length()).map { index ->
                val item = list.getJSONArray(index)
                ImdbClient.Info(
                    item.getInt(0), item.getInt(1), item.getString(2).takeIf { it.isNotEmpty() }, item.getString(3).takeIf { it.isNotEmpty() },
                    item.getString(4).takeIf { it.isNotEmpty() }, item.getString(5).takeIf { it.isNotEmpty() }, item.getInt(6).takeIf { it > 0 }
                )
            }
        } catch (_: Exception) {
            null
        }
}
