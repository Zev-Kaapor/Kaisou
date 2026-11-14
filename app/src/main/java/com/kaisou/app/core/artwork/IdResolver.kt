package com.kaisou.app.core.artwork

// Android framework
import android.util.JsonReader
import android.util.JsonToken
// Project resources
import com.kaisou.app.core.anilist.AniListClient
import com.kaisou.app.core.net.Gates
import com.kaisou.app.core.net.TooManyRequests
// Java networking and utilities
import java.io.IOException
import java.io.InputStreamReader
import java.net.HttpURLConnection
import java.net.URL

// Finds the ids a title has on TMDB and TheTVDB from its id on AniList: ani.zip has them for most titles. For the ones it
// does not know, the newest seasons, a search by name on TMDB is tried and then the season before the title, since the
// seasons share the series. Blocks on the network, so it is called from a background thread
class IdResolver(private val anilist: AniListClient) {

    // Null when nothing is known of the title. Throws when ani.zip cannot be reached, so the caller tries again later. A series that
    // ani.zip gives no TheTVDB id for gets the one TMDB knows, since it is what the seasons and the other pictures are asked by
    fun resolve(anilistId: Int, query: ArtQuery): TitleIds? = resolveIds(anilistId, query)?.let(::withTvdbId)

    private fun withTvdbId(ids: TitleIds): TitleIds {
        if (ids.tvdbId != null || ids.kind != "tv" || ids.tmdbId == null) return ids
        val tvdbId = TmdbClient.tvdbIdOf(ids.tmdbId) ?: return ids
        return TitleIds(ids.kind, ids.tmdbId, tvdbId, ids.imdbId)
    }

    private fun resolveIds(anilistId: Int, query: ArtQuery): TitleIds? {
        val direct = mappingsOf(anilistId)
        if (direct?.tmdbId != null) return direct

        searchByName(query)?.let { return merge(direct, it) }

        var current = anilistId
        repeat(MaxPrequelHops) {
            val prequel = anilist.prequelOf(current) ?: return direct
            val ids = runCatching { mappingsOf(prequel) }.getOrNull()
            if (ids?.tmdbId != null) return merge(direct, ids)
            current = prequel
        }
        return direct
    }

    // The ids that were known with the one of TMDB that was found, the ones of the other sites stay as they were
    private fun merge(base: TitleIds?, found: TitleIds): TitleIds =
        TitleIds(found.kind, found.tmdbId, base?.tvdbId ?: found.tvdbId, base?.imdbId ?: found.imdbId)

    // The first result of a search by name that really looks like the title
    private fun searchByName(query: ArtQuery): TitleIds? {
        if (!TmdbClient.isSet) return null
        val kind = if (query.movie) "movie" else "tv"
        for (name in query.names.map(::withoutSeason).filter { it.isNotBlank() }.distinct()) {
            val hits = TmdbClient.search(kind, name)
            val match = hits.withIndex().firstOrNull { (index, hit) -> isSameTitle(hit, name, query.year, index == 0) }
            if (match != null) return TitleIds(kind, match.value.id, tvdbId = null, imdbId = null)
        }
        return null
    }

    // The name without the part that says which season it is, the other sites have the series and not the season
    private fun withoutSeason(name: String): String {
        var result = name
        for (marker in SeasonMarkers) result = marker.replace(result, " ")
        return result.replace(Spaces, " ").trim().trimEnd(':', '-', '–', ' ')
    }

    // A result is the title when its name holds the searched one, or the other way around, and the series started before
    // the season and not long before. The first result is also taken when it is a known title of that time, since the site
    // finds the name in other languages that the result does not show
    private fun isSameTitle(hit: TmdbHit, searched: String, year: Int?, first: Boolean): Boolean {
        val wanted = normalize(searched)
        val similar = hit.names.any { name ->
            val found = normalize(name)
            found.isNotEmpty() && (found.contains(wanted) || wanted.contains(found))
        }
        val hitYear = hit.year
        val timeOk = year == null || hitYear == null || (hitYear <= year && year - hitYear <= SearchYearSpan)
        return timeOk && (similar || (first && hit.votes >= MinSearchVotes))
    }

    private fun normalize(text: String): String = text.lowercase().replace(NotLetterOrDigit, "")

    // The ids of ani.zip. Its answer can be huge because of the episodes, so it is read as a stream and only the mapping is kept
    private fun mappingsOf(anilistId: Int): TitleIds? = Gates.AniZip.run { mappingsOnce(anilistId) }

    private fun mappingsOnce(anilistId: Int): TitleIds? {
        val connection = URL("$MappingsUrl?anilist_id=$anilistId").openConnection() as HttpURLConnection
        try {
            connection.connectTimeout = TimeoutMillis
            connection.readTimeout = TimeoutMillis
            connection.setRequestProperty("Accept", "application/json")
            when (connection.responseCode) {
                HttpURLConnection.HTTP_OK -> Unit
                HttpURLConnection.HTTP_NOT_FOUND -> return null
                TooManyRequests.Code -> throw TooManyRequests.of(connection)
                else -> throw IOException("ani.zip answered ${connection.responseCode}")
            }
            JsonReader(InputStreamReader(connection.inputStream, Charsets.UTF_8)).use { reader ->
                // A title that is not known comes back as a plain string
                if (reader.peek() != JsonToken.BEGIN_OBJECT) return null
                var ids: TitleIds? = null
                reader.beginObject()
                while (reader.hasNext()) {
                    if (reader.nextName() == "mappings") ids = readMappings(reader) else reader.skipValue()
                }
                return ids
            }
        } finally {
            connection.disconnect()
        }
    }

    private fun readMappings(reader: JsonReader): TitleIds? {
        var type: String? = null
        var tmdbId: Int? = null
        var tvdbId: Int? = null
        var imdbId: String? = null
        reader.beginObject()
        while (reader.hasNext()) {
            when (reader.nextName()) {
                "type" -> type = nextStringOrNull(reader)
                "themoviedb_id" -> tmdbId = nextIntOrNull(reader)
                "thetvdb_id" -> tvdbId = nextIntOrNull(reader)
                "imdb_id" -> imdbId = nextStringOrNull(reader)
                else -> reader.skipValue()
            }
        }
        reader.endObject()
        if (tmdbId == null && tvdbId == null && imdbId == null) return null
        return TitleIds(if (type == MovieType) "movie" else "tv", tmdbId, tvdbId, imdbId)
    }

    private fun nextStringOrNull(reader: JsonReader): String? =
        if (reader.peek() == JsonToken.NULL) {
            reader.nextNull()
            null
        } else {
            reader.nextString()
        }

    // The ids come as numbers or as text, and some titles have none
    private fun nextIntOrNull(reader: JsonReader): Int? =
        when (reader.peek()) {
            JsonToken.STRING -> reader.nextString().toIntOrNull()
            JsonToken.NUMBER -> reader.nextInt()
            else -> {
                reader.skipValue()
                null
            }
        }

    private companion object {
        const val MappingsUrl = "https://api.ani.zip/mappings"
        const val TimeoutMillis = 8_000
        const val MovieType = "MOVIE"
        // How many seasons back are followed to find the series, and how far a search by name may go from the year of a title
        const val MaxPrequelHops = 2
        const val SearchYearSpan = 15
        const val MinSearchVotes = 5

        val Spaces = Regex("\\s+")
        val NotLetterOrDigit = Regex("[^\\p{L}\\p{N}]")

        // The ways a name says which season it is, from the most specific one
        val SeasonMarkers = listOf(
            Regex("\\b\\d+(st|nd|rd|th)\\s*(-|&|and)\\s*\\d+(st|nd|rd|th)\\s+(season|stage|part|cour)\\b", RegexOption.IGNORE_CASE),
            Regex("\\b(\\d+(st|nd|rd|th)|first|second|third|fourth|final)\\s+(season|stage|part|cour)\\b", RegexOption.IGNORE_CASE),
            Regex("\\b(season|part|cour|stage)\\s*\\d+\\b", RegexOption.IGNORE_CASE)
        )
    }
}
