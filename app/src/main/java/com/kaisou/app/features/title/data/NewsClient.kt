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
// Java time and text
import java.io.Reader
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle
import java.util.Locale
// Project resources
import com.kaisou.app.core.cache.AppStore
import com.kaisou.app.core.language.AppLanguage
import com.kaisou.app.core.translation.GoogleTranslator
import com.kaisou.app.core.net.Gates
import com.kaisou.app.core.net.HttpText

// The news about a title: for an anime they come from Kuroiru by the id of MyAnimeList, which gathers the headlines of Anime News Network,
// and for a manga from MangaBaka by the id of AniList. Each one is the headline, the day and where it opens
object NewsClient {
    private const val KuroiruUrl = "https://kuroiru.co/api/anime"
    private const val BakaUrl = "https://api.mangabaka.org/v1"
    private const val AniNewsUrl = "https://www.animenewsnetwork.com/news/."
    private const val StoreSpace = "news4"
    private const val StoreMillis = 3L * 60L * 60L * 1000L
    private const val MaxNews = 30

    // The news from the newest, empty when there are none or the site does not know the title, null when it could not be reached, which is
    // not kept
    suspend fun news(anilistId: Int, malId: Int?, manga: Boolean): List<TitleNews>? {
        val found = withContext(Dispatchers.IO) {
            if (!manga && malId == null) return@withContext emptyList()
            val key = if (manga) "manga $anilistId" else "anime $malId"
            AppStore.get(StoreSpace, key, StoreMillis)?.let { kept -> read(kept)?.let { return@withContext it } }
            try {
                Gates.News.run { if (manga) fromBaka(anilistId) else fromKuroiru(checkNotNull(malId)) }.also { AppStore.put(StoreSpace, key, write(it)) }
            } catch (_: Exception) {
                null
            }
        } ?: return null
        return inTheLanguage(found)
    }

    // The headlines in the language of the app, what is kept is how the sites say them and the translation is kept by the translator
    private suspend fun inTheLanguage(news: List<TitleNews>): List<TitleNews> {
        if (!AppLanguage.needsTranslation || news.isEmpty()) return news
        val titles = GoogleTranslator.translateLines(news.map { it.title }, AppLanguage.translationCode)
        return news.zip(titles) { item, title -> item.copy(title = title) }
    }

    private fun fromKuroiru(malId: Int): List<TitleNews> = HttpText.get("$KuroiruUrl/$malId")?.let(::parseKuroiru).orEmpty()

    // MangaBaka knows a series by a number of its own, which is asked first, and its news come as a big list, newest first, so only the
    // beginning of it is read
    private fun fromBaka(anilistId: Int): List<TitleNews> {
        val source = HttpText.get("$BakaUrl/source/anilist/$anilistId") ?: return emptyList()
        val id = JSONObject(source).optJSONObject("data")?.optJSONArray("series")?.optJSONObject(0)?.optInt("id")?.takeIf { it > 0 } ?: return emptyList()
        return HttpText.read("$BakaUrl/series/$id/news", block = ::parseBaka).orEmpty()
    }

    // The news of Kuroiru are in no order, so the newest come first. The headlines come with the marks of a page in them
    internal fun parseKuroiru(body: String): List<TitleNews> {
        val list = JSONObject(body).optJSONArray("news") ?: return emptyList()
        return (0 until list.length())
            .mapNotNull { index ->
                val item = list.getJSONObject(index)
                val title = unescape(item.optString("title")).takeIf { it.isNotBlank() } ?: return@mapNotNull null
                val url = link(item.optString("link")) ?: return@mapNotNull null
                val seconds = item.optLong("time").takeIf { it > 0 }
                (seconds ?: 0L) to TitleNews(title, seconds?.let { dateText(Instant.ofEpochSecond(it)) }, null, null, null, url)
            }
            .sortedByDescending { it.first }
            .map { it.second }
            .take(MaxNews)
    }

    // The news of MangaBaka are read as they come and the reading stops when there are enough
    internal fun parseBaka(source: Reader): List<TitleNews> {
        val found = mutableListOf<TitleNews>()
        JsonReader(source).use { reader ->
            reader.beginObject()
            while (reader.hasNext()) {
                if (reader.nextName() == "data" && reader.peek() == JsonToken.BEGIN_ARRAY) {
                    reader.beginArray()
                    while (reader.hasNext() && found.size < MaxNews) {
                        readBaka(reader)?.let(found::add)
                    }
                    return found
                } else {
                    reader.skipValue()
                }
            }
        }
        return found
    }

    private fun readBaka(reader: JsonReader): TitleNews? {
        var title: String? = null
        var url: String? = null
        var published: String? = null
        var author: String? = null
        reader.beginObject()
        while (reader.hasNext()) {
            when (reader.nextName()) {
                "title" -> title = text(reader)
                "url" -> url = text(reader)
                "published_at" -> published = text(reader)
                "author" -> author = text(reader)
                else -> reader.skipValue()
            }
        }
        reader.endObject()
        val name = title?.let(::unescape)?.takeIf { it.isNotBlank() } ?: return null
        val address = url?.takeIf { it.startsWith("http") } ?: return null
        return TitleNews(name, published?.let { runCatching { dateText(Instant.parse(it)) }.getOrNull() }, author?.takeIf { it.isNotBlank() }, null, null, address)
    }

    private fun text(reader: JsonReader): String? =
        if (reader.peek() == JsonToken.STRING) {
            reader.nextString()
        } else {
            reader.skipValue()
            null
        }

    // A link is an address, or the number of an article of Anime News Network
    private fun link(raw: String): String? {
        val text = raw.trim()
        return when {
            text.startsWith("http") -> text
            text.removePrefix(".").let { it.isNotEmpty() && it.all(Char::isDigit) } -> AniNewsUrl + text.removePrefix(".")
            else -> null
        }
    }

    // The words of a headline come as the page writes them, with &quot; and &#39; for the marks
    internal fun unescape(text: String): String =
        Regex("&(#x?[0-9a-fA-F]+|[a-zA-Z]+);").replace(text) { match ->
            val name = match.groupValues[1]
            when {
                name.startsWith("#x") -> name.drop(2).toIntOrNull(16)?.let { String(Character.toChars(it)) }
                name.startsWith("#") -> name.drop(1).toIntOrNull()?.let { String(Character.toChars(it)) }
                else -> Entities[name]
            } ?: match.value
        }

    private val Entities = mapOf("quot" to "\"", "amp" to "&", "apos" to "'", "lt" to "<", "gt" to ">", "nbsp" to " ")

    // The day as the phone writes it
    private fun dateText(moment: Instant): String =
        DateTimeFormatter.ofLocalizedDate(FormatStyle.MEDIUM).withLocale(Locale.getDefault()).withZone(ZoneId.systemDefault()).format(moment)

    private fun write(news: List<TitleNews>): String =
        JSONArray().apply {
            news.forEach { item ->
                put(JSONObject().put("t", item.title).put("d", item.date ?: JSONObject.NULL).put("a", item.author ?: JSONObject.NULL).put("u", item.url))
            }
        }.toString()

    private fun read(text: String): List<TitleNews>? =
        try {
            val list = JSONArray(text)
            (0 until list.length()).map { index ->
                val json = list.getJSONObject(index)
                TitleNews(
                    title = json.getString("t"),
                    date = if (json.isNull("d")) null else json.getString("d"),
                    author = if (json.isNull("a")) null else json.getString("a"),
                    excerpt = null,
                    image = null,
                    url = json.getString("u")
                )
            }
        } catch (_: Exception) {
            null
        }
}
