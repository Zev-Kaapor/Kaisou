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
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle
import java.util.Locale
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
    private const val StoreSpace = "episodes1"
    private const val StoreMillis = 6L * 60L * 60L * 1000L
    private const val EnglishCode = "en"
    private const val RomajiCode = "x-jat"

    // The episodes in order, empty when ani.zip does not know the title, null when it could not be reached, which is not kept
    suspend fun episodes(anilistId: Int, language: String, region: String): List<TitleEpisode>? = withContext(Dispatchers.IO) {
        val key = "$anilistId $language $region"
        AppStore.get(StoreSpace, key, StoreMillis)?.let { kept -> read(kept)?.let { return@withContext it } }
        try {
            val episodes = Gates.AniZip.run { fetch(anilistId, language, region) }
            AppStore.put(StoreSpace, key, write(episodes))
            episodes
        } catch (_: Exception) {
            null
        }
    }

    private fun fetch(anilistId: Int, language: String, region: String): List<TitleEpisode> {
        val connection = URL("$MappingsUrl?anilist_id=$anilistId").openConnection() as HttpURLConnection
        try {
            connection.connectTimeout = TimeoutMillis
            connection.readTimeout = TimeoutMillis
            connection.setRequestProperty("Accept", "application/json")
            when (connection.responseCode) {
                HttpURLConnection.HTTP_OK -> Unit
                HttpURLConnection.HTTP_NOT_FOUND -> return emptyList()
                TooManyRequests.Code -> throw TooManyRequests.of(connection)
                else -> throw IOException("ani.zip answered ${connection.responseCode}")
            }
            JsonReader(InputStreamReader(connection.inputStream, Charsets.UTF_8)).use { reader ->
                // A title that is not known comes back as a plain string
                if (reader.peek() != JsonToken.BEGIN_OBJECT) return emptyList()
                val found = mutableListOf<TitleEpisode>()
                reader.beginObject()
                while (reader.hasNext()) {
                    if (reader.nextName() == "episodes" && reader.peek() == JsonToken.BEGIN_OBJECT) readEpisodes(reader, language, region, found) else reader.skipValue()
                }
                return found.sortedBy { it.number }
            }
        } finally {
            connection.disconnect()
        }
    }

    // The episodes are an object by number, the ones with a letter, the specials, are left out
    private fun readEpisodes(reader: JsonReader, language: String, region: String, into: MutableList<TitleEpisode>) {
        reader.beginObject()
        while (reader.hasNext()) {
            val key = reader.nextName()
            if (key.all { it.isDigit() } && reader.peek() == JsonToken.BEGIN_OBJECT) readEpisode(reader, language, region)?.let(into::add) else reader.skipValue()
        }
        reader.endObject()
    }

    private fun readEpisode(reader: JsonReader, language: String, region: String): TitleEpisode? {
        var number: Int? = null
        var titles = emptyMap<String, String>()
        var image: String? = null
        var minutes: Int? = null
        var aired: String? = null
        reader.beginObject()
        while (reader.hasNext()) {
            when (reader.nextName()) {
                "episodeNumber" -> number = nextInt(reader)
                "title" -> titles = readTitles(reader)
                "image" -> image = nextString(reader)
                "runtime" -> minutes = nextInt(reader)?.takeIf { it > 0 }
                "airDate" -> aired = nextString(reader)
                else -> reader.skipValue()
            }
        }
        reader.endObject()
        val episode = number ?: return null
        // The name in the language of the app with its country, then without it, then English, then the romaji
        val title = listOf("$language-$region", language, EnglishCode, RomajiCode).firstNotNullOfOrNull { titles[it]?.takeIf { name -> name.isNotBlank() } }
        return TitleEpisode(episode, title?.replace('`', '\''), image, minutes, aired?.let(::dateText))
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

    // The day as the phone writes it
    private fun dateText(day: String): String? =
        runCatching { LocalDate.parse(day).format(DateTimeFormatter.ofLocalizedDate(FormatStyle.MEDIUM).withLocale(Locale.getDefault())) }.getOrNull()

    private fun write(episodes: List<TitleEpisode>): String =
        JSONArray().apply {
            episodes.forEach { episode ->
                put(
                    JSONObject().put("n", episode.number).put("t", episode.title ?: JSONObject.NULL).put("i", episode.image ?: JSONObject.NULL)
                        .put("m", episode.minutes ?: JSONObject.NULL).put("a", episode.aired ?: JSONObject.NULL)
                )
            }
        }.toString()

    private fun read(text: String): List<TitleEpisode>? =
        try {
            val list = JSONArray(text)
            (0 until list.length()).map { index ->
                val json = list.getJSONObject(index)
                TitleEpisode(
                    number = json.getInt("n"),
                    title = if (json.isNull("t")) null else json.getString("t"),
                    image = if (json.isNull("i")) null else json.getString("i"),
                    minutes = if (json.isNull("m")) null else json.getInt("m"),
                    aired = if (json.isNull("a")) null else json.getString("a")
                )
            }
        } catch (_: Exception) {
            null
        }
}
