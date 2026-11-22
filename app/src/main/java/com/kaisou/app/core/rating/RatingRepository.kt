package com.kaisou.app.core.rating

// Coroutines
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
// Project resources
import com.kaisou.app.core.artwork.ArtworkRepository
import com.kaisou.app.core.cache.TimedCache

// Finds the rating of a title for the country of the user. MyAnimeList is only asked when the country does not give one
object RatingRepository {
    private const val CacheMillis = 60L * 60L * 1000L
    private const val MaxEntries = 600

    // What was found for the cards, by title and country. A title with no rating is kept as none, so it is not asked again
    private val cache = TimedCache<ContentRating?>(CacheMillis, MaxEntries)

    // Where the age of a manga comes from, which a test can change so it asks nothing
    internal var mangaRating: suspend (id: Int, region: String) -> ContentRating? = MangaAgeRepository::rating

    suspend fun find(region: String, malId: Int?, adult: Boolean, certifications: Map<String, String>): ContentRating? {
        RatingPicker.fromRegion(region, certifications)?.let { return it }
        val mal = malId?.let { MalClient.rating(it) }
        return RatingPicker.pick(region, certifications, mal, adult)
    }

    // What is already known of a card, so it shows with no wait when it comes back on the screen. The first of the pair says
    // whether it is known, since a title with no rating is known too
    fun known(region: String, title: RatingQuery): Pair<Boolean, ContentRating?> {
        val entry = cache.get(key(region, title))
        if (entry == null) {
            // A manga whose age was already worked out, say by the shelf it is on, needs no wait
            val worked = if (title.manga) MangaAgeRepository.known(title.anilistId, region) else null
            return if (worked != null) true to worked else false to null
        }
        return true to entry.value
    }

    // The rating of a title for a card, null when it has none. It needs little: the countries and MyAnimeList are asked together
    // and the one of the country wins as soon as it is there. A manga has no rating on those sources, so its age is worked out from its tags, and
    // it is not rated only when it has none to say it by. A result that came with a source down is not kept, so it is asked again
    suspend fun forCard(region: String, title: RatingQuery): ContentRating? {
        known(region, title).let { (isKnown, rating) -> if (isKnown) return rating }
        if (title.manga) {
            // AniList could not be reached: the adult mark is what is known, and nothing is kept
            val worked = mangaRating(title.anilistId, region) ?: return RatingPicker.pick(region, emptyMap(), null, title.adult)
            return worked.also { keep(region, title, it) }
        }
        return coroutineScope {
            val mal = async { title.malId?.let { MalClient.rating(it) } }
            val countries = ArtworkRepository.Shared.certifications(title.anilistId, title.art)
            val own = countries?.let { RatingPicker.fromRegion(region, it) }
            if (own != null) {
                mal.cancel()
                keep(region, title, own)
                return@coroutineScope own
            }
            val picked = RatingPicker.pick(region, countries.orEmpty(), mal.await(), title.adult)
            // An anime nobody rated is pending, but only once the countries really answered, a failure shows nothing and is asked again
            if (countries == null) return@coroutineScope picked
            (picked ?: ContentRating.Pending).also { keep(region, title, it) }
        }
    }

    private fun keep(region: String, title: RatingQuery, rating: ContentRating?) {
        cache.put(key(region, title), rating)
    }

    private fun key(region: String, title: RatingQuery): Int = "${title.anilistId} $region".hashCode()
}
