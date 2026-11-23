package com.kaisou.app.features.title.data

// Android framework
import android.util.JsonReader
import android.util.JsonToken
// Coroutines
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
// JSON parsing
import org.json.JSONArray
import org.json.JSONObject
// Java networking and time
import java.io.IOException
import java.io.InputStreamReader
import java.net.HttpURLConnection
import java.net.URL
// Project resources
import com.kaisou.app.core.cache.AppStore
import com.kaisou.app.core.net.Gates
import com.kaisou.app.core.net.TooManyRequests

// The episodes of an anime as ani.zip knows them: the name of each in the language of the app, a picture, how long it is and the day it
// aired. AniList only has them for a few titles. Its answer can be huge for a long series, so it is read as a stream and only what is
// needed is kept, and what was kept goes on the disk for a while, since an episode does not change
object EpisodeClient {
    private const val MappingsUrl = "https://api.ani.zip/mappings"
    private const val TimeoutMillis = 15_000
    private const val StoreSpace = "episodes5"
    private const val StoreMillis = 6L * 60L * 60L * 1000L
    private const val EnglishCode = "en"
    private const val RomajiCode = "x-jat"

    // The episodes in order and the id of the series on TMDB, with no episodes when ani.zip does not know the title, null when it could not be
    // reached, which is not kept
    suspend fun episodes(anilistId: Int, language: String, region: String): EpisodeSet? = withContext(Dispatchers.IO) {
        val key = "$anilistId $language $region"
        AppStore.get(StoreSpace, key, StoreMillis)?.let { kept -> read(kept)?.let { return@withContext it } }
        try {
            val found = Gates.AniZip.run { fetch(anilistId, language, region) }
            AppStore.put(StoreSpace, key, write(found))
            found
        } catch (_: Exception) {
            null
        }
    }

    private fun fetch(anilistId: Int, language: String, region: String): EpisodeSet {
        val connection = URL("$MappingsUrl?anilist_id=$anilistId").openConnection() as HttpURLConnection
        try {
            connection.connectTimeout = TimeoutMillis
            connection.readTimeout = TimeoutMillis
            connection.setRequestProperty("Accept", "application/json")
            when (connection.responseCode) {
                HttpURLConnection.HTTP_OK -> Unit
                HttpURLConnection.HTTP_NOT_FOUND -> return EpisodeSet(emptyList(), null)
                TooManyRequests.Code -> throw TooManyRequests.of(connection)
                else -> throw IOException("ani.zip answered ${connection.responseCode}")
            }
            JsonReader(InputStreamReader(connection.inputStream, Charsets.UTF_8)).use { reader ->
                // A title that is not known comes back as a plain string
                if (reader.peek() != JsonToken.BEGIN_OBJECT) return EpisodeSet(emptyList(), null)
                val found = mutableListOf<TitleEpisode>()
                var ids = SiteIds()
                reader.beginObject()
                while (reader.hasNext()) {
                    val name = reader.nextName()
                    when {
                        name == "episodes" && reader.peek() == JsonToken.BEGIN_OBJECT -> readEpisodes(reader, language, region, found)
                        name == "mappings" && reader.peek() == JsonToken.BEGIN_OBJECT -> ids = readIds(reader)
                        else -> reader.skipValue()
                    }
                }
                return EpisodeSet(found.sortedBy { it.number }, ids.tmdb, ids.imdb, ids.mal, ids.tvdb)
            }
        } finally {
            connection.disconnect()
        }
    }

    // The ids the title has on the other sites
    private class SiteIds(val tmdb: Int? = null, val imdb: String? = null, val mal: Int? = null, val tvdb: Int? = null)

    // The ids of the series on TMDB, IMDb and MyAnimeList, among the ids of the other sites
    private fun readIds(reader: JsonReader): SiteIds {
        var tmdb: Int? = null
        var imdb: String? = null
        var mal: Int? = null
        var tvdb: Int? = null
        reader.beginObject()
        while (reader.hasNext()) {
            when (reader.nextName()) {
                "themoviedb_id" -> tmdb = nextInt(reader)
                "imdb_id" -> imdb = nextString(reader)?.takeIf { it.isNotBlank() }
                "mal_id" -> mal = nextInt(reader)
                "thetvdb_id" -> tvdb = nextInt(reader)
                else -> reader.skipValue()
            }
        }
        reader.endObject()
        return SiteIds(tmdb, imdb, mal, tvdb)
    }

    // The episodes are an object by number, the ones with a letter, the specials, are left out
    private fun readEpisodes(reader: JsonReader, language: String, region: String, into: MutableList<TitleEpisode>) {
        reader.beginObject()
        while (reader.hasNext()) {
            val key = reader.nextName()
            if (key.all { it.isDigit() } && reader.peek() == JsonToken.BEGIN_OBJECT) into.add(readEpisode(reader, key.toInt(), language, region)) else reader.skipValue()
        }
        reader.endObject()
    }

    // The number is the one of the key, which is the one in the order of the title, while the one inside is the one in the season of TheTVDB
    private fun readEpisode(reader: JsonReader, number: Int, language: String, region: String): TitleEpisode {
        var titles = emptyMap<String, String>()
        var image: String? = null
        var minutes: Int? = null
        var aired: String? = null
        var overview: String? = null
        var season: Int? = null
        reader.beginObject()
        while (reader.hasNext()) {
            when (reader.nextName()) {
                "title" -> titles = readTitles(reader)
                "image" -> image = nextString(reader)
                "runtime" -> minutes = nextInt(reader)?.takeIf { it > 0 }
                "airDate" -> aired = nextString(reader)
                "overview" -> overview = nextString(reader)?.trim()?.takeIf { it.isNotEmpty() }
                "seasonNumber" -> season = nextInt(reader)
                else -> reader.skipValue()
            }
        }
        reader.endObject()
        // The name in the language of the app with its country, then without it, then English, then the romaji
        val title = listOf("$language-$region", language, EnglishCode, RomajiCode).firstNotNullOfOrNull { titles[it]?.takeIf { name -> name.isNotBlank() } }
        return TitleEpisode(number, title?.replace('`', '\''), image, minutes, aired, overview, null, season)
    }

    private fun readTitles(reader: JsonReader): Map<String, String> {
        if (reader.peek() != JsonToken.BEGIN_OBJECT) {
            reader.skipValue()
            return emptyMap()
        }
        val titles = mutableMapOf<String, String>()
        reader.beginObject()
        while (reader.hasNext()) {
            val name = reader.nextName()
            nextString(reader)?.let { titles[name] = it }
        }
        reader.endObject()
        return titles
    }

    private fun nextString(reader: JsonReader): String? =
        if (reader.peek() == JsonToken.NULL) {
            reader.nextNull()
            null
        } else {
            reader.nextString()
        }

    private fun nextInt(reader: JsonReader): Int? =
        when (reader.peek()) {
            JsonToken.NUMBER, JsonToken.STRING -> reader.nextString().toDoubleOrNull()?.toInt()
            else -> {
                reader.skipValue()
                null
            }
        }

    private fun write(found: EpisodeSet): String =
        JSONObject().put("t", found.tmdbId ?: JSONObject.NULL).put("i", found.imdbId ?: JSONObject.NULL).put("m", found.malId ?: JSONObject.NULL)
            .put("v", found.tvdbId ?: JSONObject.NULL).put("e", JSONArray().apply {
            found.episodes.forEach { episode ->
                put(
                    JSONObject().put("n", episode.number).put("t", episode.title ?: JSONObject.NULL).put("i", episode.image ?: JSONObject.NULL)
                        .put("m", episode.minutes ?: JSONObject.NULL).put("a", episode.aired ?: JSONObject.NULL).put("o", episode.overview ?: JSONObject.NULL)
                        .put("s", episode.season ?: JSONObject.NULL)
                )
            }
        }).toString()

    private fun read(text: String): EpisodeSet? =
        try {
            val kept = JSONObject(text)
            val list = kept.getJSONArray("e")
            val episodes = (0 until list.length()).map { index ->
                val json = list.getJSONObject(index)
                TitleEpisode(
                    number = json.getInt("n"),
                    title = if (json.isNull("t")) null else json.getString("t"),
                    image = if (json.isNull("i")) null else json.getString("i"),
                    minutes = if (json.isNull("m")) null else json.getInt("m"),
                    aired = if (json.isNull("a")) null else json.getString("a"),
                    overview = if (json.isNull("o")) null else json.getString("o"),
                    season = if (json.isNull("s")) null else json.getInt("s")
                )
            }
            EpisodeSet(
                episodes,
                if (kept.isNull("t")) null else kept.getInt("t"),
                if (kept.isNull("i")) null else kept.getString("i"),
                if (kept.isNull("m")) null else kept.getInt("m"),
                if (kept.isNull("v")) null else kept.getInt("v")
            )
        } catch (_: Exception) {
            null
        }
}
