package com.kaisou.app.core.artwork

// Coroutines
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.selects.select
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull
// Java time
import java.time.LocalDate
// Project resources
import com.kaisou.app.core.anilist.AniListClient
import com.kaisou.app.core.cache.AppStore
import com.kaisou.app.core.cache.TimedCache
import kotlinx.coroutines.delay

// Finds the poster, the backdrop, the logos, the ratings of the countries and the seasons of the series of a title. Everything that can go on together does: the two sites are asked at the same
// time, and the pictures are picked among what both have, as soon as both answered or the slower one waited too long
class ArtworkRepository {
    private class Found(val art: TitleArt, val failed: Boolean)

    private val ids = IdResolver(AniListClient())

    // What was found per title, one with nothing means the title has none, so it is not asked again
    private val cache = TimedCache<TitleArt>(CacheMillis, MaxEntries)

    // The ratings alone, by title: they do not depend on the language
    // The ids of the titles, which are asked once
    private val idsCache = TimedCache<TitleIds>(CacheMillis, MaxEntries)

    private val certificationsCache = TimedCache<Map<String, String>>(CacheMillis, CertificationsMaxEntries)

    // The ids a title has on TMDB and TheTVDB, kept in memory and on the phone for a month since they do not change, so the pictures, the ratings and
    // the seasons of a title do not each ask for them again. Null when nothing is known of the title, which is not kept. Throws when it could not
    // be asked
    private suspend fun idsOf(anilistId: Int, query: ArtQuery): TitleIds? {
        idsCache.get(anilistId)?.let { return it.value }
        withContext(Dispatchers.IO) { AppStore.get(IdsSpace, anilistId.toString(), IdsStoreMillis) }?.let(::readIds)?.let {
            idsCache.put(anilistId, it)
            return it
        }
        val found = withContext(Dispatchers.IO) { ids.resolve(anilistId, query) } ?: return null
        idsCache.put(anilistId, found)
        withContext(Dispatchers.IO) { AppStore.put(IdsSpace, anilistId.toString(), listOf(found.kind, found.tmdbId, found.tvdbId, found.imdbId).joinToString(IdsSeparator) { it?.toString().orEmpty() }) }
        return found
    }

    private fun readIds(text: String): TitleIds? {
        val parts = text.split(IdsSeparator)
        if (parts.size != IdsParts || parts[0].isEmpty()) return null
        return TitleIds(parts[0], parts[1].toIntOrNull(), parts[2].toIntOrNull(), parts[3].ifEmpty { null })
    }

    // The day each season of the series of a title first aired, asked apart from the pictures and the ratings so a slow one of them does not take it
    // away. The answer is kept on the phone by series for a day, so the titles of the same series and the next times do not ask again, and a
    // failed question is asked again a few times before it is given up. Empty when the title is not a series or it could not be found
    suspend fun seasonStarts(anilistId: Int, query: ArtQuery): Map<Int, LocalDate> {
        val resolved = try {
            idsOf(anilistId, query)
        } catch (_: Exception) {
            null
        } ?: return emptyMap()
        val tvdbId = resolved.tvdbId?.takeIf { resolved.kind == "tv" } ?: return emptyMap()
        val key = tvdbId.toString()
        withContext(Dispatchers.IO) { AppStore.get(SeasonsSpace, key, SeasonsStoreMillis) }?.let(::readSeasons)?.let { return it }
        repeat(SeasonAttempts) { attempt ->
            val found = withContext(Dispatchers.IO) { TvdbClient.seasonStarts(tvdbId) }
            if (!found.failed) {
                withContext(Dispatchers.IO) { AppStore.put(SeasonsSpace, key, found.byNumber.entries.joinToString(",") { "${it.key}=${it.value}" }) }
                return found.byNumber
            }
            if (attempt < SeasonAttempts - 1) delay(SeasonRetryMillis)
        }
        return emptyMap()
    }

    // The seasons as they were kept, as 1=2012-10-06,2=2014-04-05, and nothing for a series that has none. Null when the text is not one of these
    private fun readSeasons(text: String): Map<Int, LocalDate>? =
        try {
            if (text.isBlank()) emptyMap() else text.split(",").associate { part -> part.substringBefore("=").toInt() to LocalDate.parse(part.substringAfter("=")) }
        } catch (_: Exception) {
            null
        }

    // Logs in to TheTVDB ahead of time, so the first title does not wait for it
    suspend fun prewarm() {
        withContext(Dispatchers.IO) { TvdbClient.warmUp() }
    }

    // The best poster and the logos worth trying of a title in the language given
    suspend fun art(anilistId: Int, query: ArtQuery, language: String): TitleArt {
        // The pictures depend on the language, so it is part of what is kept
        val key = "$anilistId $language"
        cache.get(key.hashCode())?.let { return it.value }
        withContext(Dispatchers.IO) { AppStore.get(StoreSpace, key, StoreMillis) }?.let(TitleArtJson::read)?.let {
            cache.put(key.hashCode(), it)
            return it
        }
        val found = fetch(anilistId, query, language)
        // A failed request is not kept, so it is tried again next time
        if (!found.failed) {
            cache.put(key.hashCode(), found.art)
            withContext(Dispatchers.IO) { AppStore.put(StoreSpace, key, TitleArtJson.write(found.art)) }
        }
        return found.art
    }

    // The id on YouTube of the trailer TMDB has for a title, for the ones AniList has none for: one question to TMDB, once for a title, with the
    // answer kept, even when it is that there is none. The place of the title on TMDB is already known from its pictures. Null when there is
    // none or it could not be asked, which is not kept
    suspend fun trailer(anilistId: Int, query: ArtQuery, language: String): String? {
        val key = anilistId.toString()
        withContext(Dispatchers.IO) { AppStore.get(TrailerSpace, key, TrailerStoreMillis) }?.let { return it.takeIf(String::isNotEmpty) }
        val found = art(anilistId, query, language)
        val tmdbId = found.tmdbId ?: return null
        val id = withContext(Dispatchers.IO) { TmdbClient.trailer(found.tmdbKind ?: "tv", tmdbId) } ?: return null
        withContext(Dispatchers.IO) { AppStore.put(TrailerSpace, key, id) }
        return id.takeIf(String::isNotEmpty)
    }

    // The ratings of the countries of a title and nothing else, for the cards that only need them: the ids and one question to TMDB,
    // with no pictures. Null when they could not be found, which is not kept, so it is asked again
    suspend fun certifications(anilistId: Int, query: ArtQuery): Map<String, String>? {
        certificationsCache.get(anilistId)?.let { return it.value }
        withContext(Dispatchers.IO) { AppStore.get(CertificationsSpace, anilistId.toString(), CertificationsStoreMillis) }
            ?.let(TitleArtJson::readCertifications)
            ?.let {
                certificationsCache.put(anilistId, it)
                return it
            }
        val resolved = try {
            idsOf(anilistId, query)
        } catch (_: Exception) {
            return null
        }
        val certified = resolved?.tmdbId?.let { tmdbId -> withContext(Dispatchers.IO) { TmdbClient.certifications(resolved.kind, tmdbId) } } ?: Certified.None
        if (certified.failed) return null
        certificationsCache.put(anilistId, certified.byCountry)
        withContext(Dispatchers.IO) { AppStore.put(CertificationsSpace, anilistId.toString(), TitleArtJson.writeCertifications(certified.byCountry)) }
        return certified.byCountry
    }

    private suspend fun fetch(anilistId: Int, query: ArtQuery, language: String): Found = coroutineScope {
        val resolved = try {
            idsOf(anilistId, query)
        } catch (_: Exception) {
            return@coroutineScope Found(TitleArt.None, failed = true)
        } ?: return@coroutineScope Found(TitleArt.None, failed = false)

        val tmdb = async(Dispatchers.IO) {
            resolved.tmdbId?.let { TmdbClient.images(resolved.kind, it) } ?: SiteImages.None
        }
        val certified = async(Dispatchers.IO) {
            resolved.tmdbId?.let { TmdbClient.certifications(resolved.kind, it) } ?: Certified.None
        }
        val tvdb = async(Dispatchers.IO) {
            coroutineScope {
                val posters = async { TvdbClient.posters(resolved.tvdbId, resolved.imdbId) }
                val logos = async { TvdbClient.logos(resolved.tvdbId, resolved.imdbId) }
                val backdrops = async { TvdbClient.backdrops(resolved.tvdbId, resolved.imdbId) }
                SiteImages(posters.await(), logos.await(), backdrops.await())
            }
        }

        // The one that answers first is waited for the other, but not for long, a slow site does not hold the title
        val tmdbFirst = select<Boolean> {
            tmdb.onAwait { true }
            tvdb.onAwait { false }
        }
        val first = (if (tmdbFirst) tmdb else tvdb).await()
        val secondRequest = if (tmdbFirst) tvdb else tmdb
        val second = withTimeoutOrNull(OtherSiteWaitMillis) { secondRequest.await() }
        if (second == null) secondRequest.cancel()

        val posters = first.posters.list + second?.posters?.list.orEmpty()
        val logos = first.logos.list + second?.logos?.list.orEmpty()
        val backdrops = first.backdrops.list + second?.backdrops?.list.orEmpty()
        // The ratings and the seasons were asked from the start, so they are almost always there by now
        val ratings = withTimeoutOrNull(OtherSiteWaitMillis) { certified.await() }
        if (ratings == null) certified.cancel()
        Found(
            art = TitleArt(
                posterUrl = PosterPicker.pick(posters, language)?.url,
                backdropUrl = BackdropPicker.pick(backdrops, language)?.url,
                logos = LogoPicker.pick(logos, language),
                certifications = ratings?.byCountry.orEmpty(),
                tmdbKind = resolved.kind,
                tmdbId = resolved.tmdbId
            ),
            failed = (first.failed && (second?.failed ?: true)) || ratings == null || ratings.failed
        )
    }

    companion object {
        // One instance for the whole app, so what was found stays in memory
        val Shared = ArtworkRepository()

        // How long the site that answered second is waited for, and how long what was found is kept, and how many titles at most
        private const val OtherSiteWaitMillis = 2_000L
        private const val CacheMillis = 60L * 60L * 1000L
        private const val MaxEntries = 200

        // What is kept on the disk and for how long: a day, since a title does not change its pictures or its ratings that fast
        private const val StoreSpace = "art5"
        // Where the ids of the titles are kept, and for how long: a month
        private const val IdsSpace = "ids1"
        private const val IdsStoreMillis = 30L * 24L * 60L * 60L * 1000L
        private const val IdsSeparator = "|"
        private const val IdsParts = 4
        // Where the seasons of a series are kept, for how long and how many times they are asked when they fail, with how long between them
        private const val SeasonsSpace = "seasons1"
        private const val SeasonsStoreMillis = 24L * 60L * 60L * 1000L
        private const val SeasonAttempts = 3
        private const val SeasonRetryMillis = 1_500L
        // Where the trailers are kept, and for how long: a week, a title seldom gets one
        private const val TrailerSpace = "trailer1"
        private const val TrailerStoreMillis = 7L * 24L * 60L * 60L * 1000L
        private const val StoreMillis = 24L * 60L * 60L * 1000L

        // The ratings alone for the cards: how many titles stay in memory, and a week on the disk, they almost never change
        private const val CertificationsMaxEntries = 600
        private const val CertificationsSpace = "cert1"
        private const val CertificationsStoreMillis = 7L * 24L * 60L * 60L * 1000L
    }
}
