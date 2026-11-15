package com.kaisou.app.core.artwork

// Coroutines
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.selects.select
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull
// Project resources
import com.kaisou.app.core.anilist.AniListClient
import com.kaisou.app.core.cache.AppStore
import com.kaisou.app.core.cache.TimedCache

// Finds the poster, the backdrop, the logos, the ratings of the countries and the seasons of the series of a title. Everything that can go on together does: the two sites are asked at the same
// time, and the pictures are picked among what both have, as soon as both answered or the slower one waited too long
class ArtworkRepository {
    private class Found(val art: TitleArt, val failed: Boolean)

    private val ids = IdResolver(AniListClient())

    // What was found per title, one with nothing means the title has none, so it is not asked again
    private val cache = TimedCache<TitleArt>(CacheMillis, MaxEntries)

    // The ratings alone, by title: they do not depend on the language
    private val certificationsCache = TimedCache<Map<String, String>>(CacheMillis, CertificationsMaxEntries)

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
            withContext(Dispatchers.IO) { ids.resolve(anilistId, query) }
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
            withContext(Dispatchers.IO) { ids.resolve(anilistId, query) }
        } catch (_: Exception) {
            return@coroutineScope Found(TitleArt.None, failed = true)
        } ?: return@coroutineScope Found(TitleArt.None, failed = false)

        val tmdb = async(Dispatchers.IO) {
            resolved.tmdbId?.let { TmdbClient.images(resolved.kind, it) } ?: SiteImages.None
        }
        val certified = async(Dispatchers.IO) {
            resolved.tmdbId?.let { TmdbClient.certifications(resolved.kind, it) } ?: Certified.None
        }
        val seasons = async(Dispatchers.IO) {
            if (resolved.kind == "tv" && resolved.tvdbId != null) TvdbClient.seasonStarts(resolved.tvdbId) else SeasonStarts.None
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
        val starts = withTimeoutOrNull(OtherSiteWaitMillis) { seasons.await() }
        if (starts == null) seasons.cancel()
        Found(
            art = TitleArt(
                posterUrl = PosterPicker.pick(posters, language)?.url,
                backdropUrl = BackdropPicker.pick(backdrops, language)?.url,
                logos = LogoPicker.pick(logos, language),
                certifications = ratings?.byCountry.orEmpty(),
                seasonStarts = starts?.byNumber.orEmpty()
            ),
            failed = (first.failed && (second?.failed ?: true)) || ratings == null || ratings.failed || starts == null || starts.failed
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
        private const val StoreSpace = "art2"
        private const val StoreMillis = 24L * 60L * 60L * 1000L

        // The ratings alone for the cards: how many titles stay in memory, and a week on the disk, they almost never change
        private const val CertificationsMaxEntries = 600
        private const val CertificationsSpace = "cert1"
        private const val CertificationsStoreMillis = 7L * 24L * 60L * 60L * 1000L
    }
}
