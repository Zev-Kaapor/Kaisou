package com.kaisou.app.core.rating

// Coroutines
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
// JSON parsing
import org.json.JSONArray
import org.json.JSONObject
// Project resources
import com.kaisou.app.core.anilist.AniListClient
import com.kaisou.app.core.auth.AniListSession
import com.kaisou.app.core.cache.AppStore
import com.kaisou.app.core.cache.TimedCache

// The age of the manga on the cards, asked of AniList in batches of up to 50 and kept in memory and on the phone
object MangaAgeRepository {
    // The region of Brazil, how long a batch waits and how big it is, the mark of no age, and where and how long ages are kept
    private const val BrazilRegion = "BR"
    private const val BatchWaitMillis = 250L
    private const val BatchSize = 50
    private const val NoAge = -1
    private const val StoreSpace = "manga-age1"
    private const val StoreMillis = 7L * 24L * 60L * 60L * 1000L
    private const val CacheMillis = 60L * 60L * 1000L
    private const val MaxEntries = 1000
    private const val NoneKept = "none"
    private const val Query = "query (\$ids: [Int]) { Page(perPage: $BatchSize) { media(id_in: \$ids, type: MANGA) { id isAdult genres " +
        "tags { name rank isMediaSpoiler isAdult } } } }"

    private val client = AniListClient()
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    // The age of a title found, as a number, or NoAge for one with nothing known to say it by
    private val memory = TimedCache<Int>(CacheMillis, MaxEntries)

    // The titles that wait for the next question, with who waits for each. The question goes when the wait of a moment is over
    private val waiting = LinkedHashMap<Int, CompletableDeferred<Int?>>()

    // The titles of the question that went and has no answer yet, so whoever asks for one of them waits for that answer
    private val asking = HashMap<Int, CompletableDeferred<Int?>>()
    private var running = false

    // The rating of a manga only when it is already in memory, with no wait, null when it is not. A card with it shows it at once
    fun known(id: Int, region: String): ContentRating? = memory.get(id)?.let { made(it.value, region) }

    // Says the manga of a shelf, all at once, so they go in the least questions and the cards find the age ready. Nothing waits for it
    fun prefetch(ids: List<Int>) {
        scope.launch { ids.distinct().forEach { id -> launch { ageOf(id) } } }
    }

    // The rating of a manga for the country from its tags, not rated when it has none, null when AniList failed, which is not kept
    suspend fun rating(id: Int, region: String): ContentRating? {
        return made(ageOf(id) ?: return null, region)
    }

    // Builds the rating of a manga from an age
    private fun made(age: Int, region: String): ContentRating =
        if (age == NoAge) ContentRating.NotRated else ContentRating(AgeGroup.of(age), official = false, symbol = region == BrazilRegion)

    // Gives the age of a manga from memory, the disk or AniList, null when it could not be asked
    private suspend fun ageOf(id: Int): Int? {
        memory.get(id)?.let { return it.value }
        withContext(Dispatchers.IO) { AppStore.get(StoreSpace, id.toString(), StoreMillis) }?.let { kept ->
            val age = if (kept == NoneKept) NoAge else kept.toIntOrNull()
            if (age != null) {
                memory.put(id, age)
                return age
            }
        }
        val answer = synchronized(waiting) {
            val one = asking[id] ?: waiting.getOrPut(id) { CompletableDeferred() }
            if (!running) {
                running = true
                scope.launch { runBatches() }
            }
            one
        }
        return answer.await()
    }

    // Asks for the titles that wait, up to 50 at a time, until none is left
    private suspend fun runBatches() {
        delay(BatchWaitMillis)
        while (true) {
            val batch = synchronized(waiting) {
                if (waiting.isEmpty()) {
                    running = false
                    return
                }
                waiting.keys.take(BatchSize).associateWith { waiting.remove(it)!! }.also { asking.putAll(it) }
            }
            val found = ask(batch.keys.toList())
            synchronized(waiting) { asking.keys.removeAll(batch.keys) }
            batch.forEach { (id, one) -> one.complete(found?.get(id) ?: if (found == null) null else NoAge) }
        }
    }

    // The age of each title of the group, by its id. Null when AniList could not be reached
    private suspend fun ask(ids: List<Int>): Map<Int, Int>? {
        val root = client.query(Query, JSONObject().put("ids", JSONArray(ids)), AniListSession.token) ?: return null
        val list = root.optJSONObject("Page")?.optJSONArray("media") ?: return null
        val ages = HashMap<Int, Int>()
        for (index in 0 until list.length()) {
            val media = list.getJSONObject(index)
            val tags = media.optJSONArray("tags")?.let { array ->
                (0 until array.length()).mapNotNull { i ->
                    array.getJSONObject(i).let { tag -> tag.optString("name").takeIf { it.isNotBlank() }?.let { AgeTag(it, tag.optInt("rank"), tag.optBoolean("isMediaSpoiler"), tag.optBoolean("isAdult")) } }
                }
            }.orEmpty()
            val genres = media.optJSONArray("genres")?.let { array -> (0 until array.length()).map { array.getString(it) } }.orEmpty()
            val group = MangaAgeEstimate.of(tags, genres, media.optBoolean("isAdult"))
            ages[media.getInt("id")] = group?.minAge ?: NoAge
        }

        // The ones AniList did not answer for are kept as nothing to say them by, since asking again would be the same
        ids.forEach { ages.putIfAbsent(it, NoAge) }
        withContext(Dispatchers.IO) {
            ages.forEach { (id, age) ->
                memory.put(id, age)
                AppStore.put(StoreSpace, id.toString(), if (age == NoAge) NoneKept else age.toString())
            }
        }
        return ages
    }
}
