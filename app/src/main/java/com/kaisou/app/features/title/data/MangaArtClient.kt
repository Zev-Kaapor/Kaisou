package com.kaisou.app.features.title.data

// Coroutines
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
// JSON parsing
import org.json.JSONArray
import org.json.JSONObject
// Java networking
import java.net.URLEncoder
// Project resources
import com.kaisou.app.core.cache.AppStore
import com.kaisou.app.core.net.Gates
import com.kaisou.app.core.net.HttpText

// A cover of a volume of a manga: which volume it is of, the language of the edition and the addresses of the picture, whole and small
class MangaCover(val volume: String?, val language: String, val original: String, val thumbnail: String)

// The covers of the volumes of a manga from MangaDex, found by name and the AniList id among its links
object MangaArtClient {
    // Addresses of the API and of the covers, the thumbnail suffix, where and how long covers are kept, and how many names are tried
    private const val ApiUrl = "https://api.mangadex.org"
    private const val ImageUrl = "https://uploads.mangadex.org/covers"
    private const val ThumbnailSuffix = ".512.jpg"
    private const val StoreSpace = "mangadex1"
    private const val StoreMillis = 24L * 60L * 60L * 1000L
    private const val MaxNames = 3
    private const val MaxCovers = 100
    private const val FirstVolume = "1"

    // The covers by volume, the user's language first, then English and Japanese, empty when unknown, null when it failed
    suspend fun covers(anilistId: Int, names: List<String>, language: String, region: String): List<MangaCover>? = withContext(Dispatchers.IO) {
        val key = anilistId.toString()
        val kept = AppStore.get(StoreSpace, key, StoreMillis)
        val body = kept ?: try {
            Gates.MangaDex.run { fetch(anilistId, names) }.also { AppStore.put(StoreSpace, key, it) }
        } catch (_: Exception) {
            return@withContext null
        }
        runCatching { parse(body, preferred(language, region)) }.getOrNull()
    }

    // The covers kept a moment ago for a manga, null when nothing is kept, so an empty answer is not taken for one
    suspend fun kept(anilistId: Int, language: String, region: String): List<MangaCover>? = withContext(Dispatchers.IO) {
        val body = AppStore.get(StoreSpace, anilistId.toString(), StoreMillis) ?: return@withContext null
        runCatching { parse(body, preferred(language, region)) }.getOrNull()
    }

    // The cover that stands for the manga: the first volume in the user's language, then the others, then the first listed
    fun poster(covers: List<MangaCover>): MangaCover? = covers.firstOrNull { it.volume == FirstVolume } ?: covers.firstOrNull()

    // What MangaDex answers, ready to be kept: the id of the manga and its covers, or nothing when it is not there
    private fun fetch(anilistId: Int, names: List<String>): String {
        val id = names.distinct().take(MaxNames).firstNotNullOfOrNull { name -> search(anilistId, name) } ?: return EmptyAnswer
        val address = "$ApiUrl/cover?${enc("manga[]")}=$id&limit=$MaxCovers&${enc("order[volume]")}=asc"
        val covers = HttpText.get(address) ?: return EmptyAnswer
        return JSONObject().put("manga", id).put("covers", JSONObject(covers).optJSONArray("data") ?: JSONArray()).toString()
    }

    // Searches MangaDex for the manga by a name and returns its id when the AniList id is among its links
    private fun search(anilistId: Int, name: String): String? {
        val ratings = listOf("safe", "suggestive", "erotica", "pornographic").joinToString("&") { "${enc("contentRating[]")}=$it" }
        val body = HttpText.get("$ApiUrl/manga?title=${enc(name)}&limit=10&$ratings") ?: return null
        return findManga(body, anilistId)
    }

    // The manga of the list that has the id of AniList in its links
    internal fun findManga(body: String, anilistId: Int): String? {
        val list = JSONObject(body).optJSONArray("data") ?: return null
        return (0 until list.length()).map { list.getJSONObject(it) }
            .firstOrNull { it.optJSONObject("attributes")?.optJSONObject("links")?.optString("al") == anilistId.toString() }
            ?.optString("id")?.takeIf { it.isNotBlank() }
    }

    // The covers of what was kept, in the order for the languages
    internal fun parse(body: String, preferred: List<String>): List<MangaCover> {
        val json = JSONObject(body)
        val manga = json.optString("manga").takeIf { it.isNotBlank() } ?: return emptyList()
        val list = json.optJSONArray("covers") ?: return emptyList()
        return (0 until list.length()).mapNotNull { index ->
            val attributes = list.getJSONObject(index).optJSONObject("attributes") ?: return@mapNotNull null
            val file = attributes.optString("fileName").takeIf { it.isNotBlank() } ?: return@mapNotNull null
            val volume = attributes.optString("volume").takeIf { it.isNotBlank() && it != "null" }
            val address = "$ImageUrl/$manga/$file"
            MangaCover(volume, attributes.optString("locale"), address, address + ThumbnailSuffix)
        }.sortedWith(
            compareBy<MangaCover> { it.volume?.toDoubleOrNull() ?: Double.MAX_VALUE }
                .thenBy { cover -> preferred.indexOf(cover.language.lowercase()).let { if (it < 0) preferred.size else it } }
        )
    }

    // The languages in the order of what the user prefers: their language with the region, then without it, English and Japanese
    private fun preferred(language: String, region: String): List<String> =
        listOf("$language-${region.lowercase()}", language, "en", "ja").distinct()

    // Makes a text safe for an address
    private fun enc(text: String): String = URLEncoder.encode(text, Charsets.UTF_8.name())

    // What is kept when MangaDex does not know the manga, so it is not asked again
    private const val EmptyAnswer = "{\"covers\":[]}"
}
