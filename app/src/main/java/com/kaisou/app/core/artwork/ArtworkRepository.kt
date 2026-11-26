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
import java.time.temporal.ChronoUnit
// Project resources
import com.kaisou.app.core.cache.AppStore
import com.kaisou.app.core.cache.TimedCache
import kotlinx.coroutines.delay

// Finds the pictures, the ratings and the seasons of a title, asking both sites at once and picking from what both have
class ArtworkRepository {
    // What was found of a title and whether the answer failed
    private class Found(val art: TitleArt, val failed: Boolean)

    private val ids = IdResolver()

    // What was found per title, one with nothing means the title has none, so it is not asked again
    private val cache = TimedCache<TitleArt>(CacheMillis, MaxEntries)

    // The ratings alone by title, which do not depend on the language, and the ids of the titles, asked once
    private val idsCache = TimedCache<TitleIds>(CacheMillis, MaxEntries)

    private val certificationsCache = TimedCache<Map<String, String>>(CacheMillis, CertificationsMaxEntries)

    // The ids of a title on TMDB and TheTVDB, kept for a month, null when nothing is known, throws when it could not be asked
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

    // Reads kept ids, null when the text is broken
    private fun readIds(text: String): TitleIds? {
        val parts = text.split(IdsSeparator)
        if (parts.size != IdsParts || parts[0].isEmpty()) return null
        return TitleIds(parts[0], parts[1].toIntOrNull(), parts[2].toIntOrNull(), parts[3].ifEmpty { null })
    }

    // The day each season of a series first aired, kept for a day and asked again a few times when it fails
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

    // Reads kept seasons, as 1=2012-10-06,2=2014-04-05, null when the text is not one of these
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

    // The poster and the backdrop of a season, taken from TMDB when its season began with the title, else from TheTVDB
    suspend fun seasonArt(anilistId: Int, query: ArtQuery, season: Int, start: LocalDate?, language: String): SeasonPictures {
        val key = "$anilistId $season $language"
        withContext(Dispatchers.IO) { AppStore.get(SeasonArtSpace, key, SeasonArtStoreMillis) }?.let(::readSeasonPictures)?.let { return it }
        val resolved = try {
            idsOf(anilistId, query)
        } catch (_: Exception) {
            null
        }
        if (resolved == null || resolved.kind != "tv") return SeasonPictures.None
        val (tmdb, tvdb) = coroutineScope {
            val first = async(Dispatchers.IO) { resolved.tmdbId?.let { TmdbClient.seasonArt(it, season) } ?: SeasonArtwork.None }
            val second = async(Dispatchers.IO) { resolved.tvdbId?.let { TvdbClient.seasonArt(it, season) } ?: SeasonArtwork.None }
            first.await() to second.await()
        }
        val fromTmdb = tmdb.takeIf { began(it.airDate, start) }?.let { PosterPicker.pick(it.posters.list, language, PosterPicker.SeasonMinWidth) }
        val fromTvdb = PosterPicker.pick(tvdb.posters.list, language, PosterPicker.SeasonMinWidth)
        val pictures = SeasonPictures((fromTmdb ?: fromTvdb)?.url, BackdropPicker.pick(tvdb.backdrops.list, language)?.url)
        if (!(tmdb.failed && tvdb.failed)) {
            withContext(Dispatchers.IO) { AppStore.put(SeasonArtSpace, key, "${pictures.posterUrl.orEmpty()}\n${pictures.backdropUrl.orEmpty()}") }
        }
        return pictures
    }

    // Whether a season began about when the title did, a little after it or up to a year and a bit before
    private fun began(airDate: LocalDate?, start: LocalDate?): Boolean {
        if (airDate == null || start == null) return false
        return ChronoUnit.DAYS.between(airDate, start) in -SeasonLateDays..SeasonEarlyDays
    }

    // Reads the kept pictures of a season, null when the text is broken
    private fun readSeasonPictures(text: String): SeasonPictures? {
        val parts = text.split("\n")
        if (parts.size != 2) return null
        return SeasonPictures(parts[0].ifEmpty { null }, parts[1].ifEmpty { null })
    }

    // The id on YouTube of the trailer TMDB has for a title, kept even when there is none, null when it could not be asked
    suspend fun trailer(anilistId: Int, query: ArtQuery, language: String): String? {
        val key = anilistId.toString()
        withContext(Dispatchers.IO) { AppStore.get(TrailerSpace, key, TrailerStoreMillis) }?.let { return it.takeIf(String::isNotEmpty) }
        val found = art(anilistId, query, language)
        val tmdbId = found.tmdbId ?: return null
        val id = withContext(Dispatchers.IO) { TmdbClient.trailer(found.tmdbKind ?: "tv", tmdbId) } ?: return null
        withContext(Dispatchers.IO) { AppStore.put(TrailerSpace, key, id) }
        return id.takeIf(String::isNotEmpty)
    }

    // The ratings of the countries of a title and nothing else, null when they could not be found, which is not kept
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

    // Asks both sites for what a title has and picks the best of each
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

    // The one instance of the whole app, so what was found stays in memory
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

        // Where the seasons are kept, how long, and how many times they are asked again when they fail
        private const val SeasonsSpace = "seasons1"
        private const val SeasonsStoreMillis = 24L * 60L * 60L * 1000L
        private const val SeasonAttempts = 3
        private const val SeasonRetryMillis = 1_500L

        // Where the trailers are kept, and for how long: a week, a title seldom gets one
        private const val SeasonArtSpace = "seasonart1"
        private const val SeasonArtStoreMillis = 7L * 24L * 60L * 60L * 1000L
        private const val SeasonLateDays = 60L
        private const val SeasonEarlyDays = 450L

        // Where the trailers are kept, how long the trailers and the other answers are kept
        private const val TrailerSpace = "trailer1"
        private const val TrailerStoreMillis = 7L * 24L * 60L * 60L * 1000L
        private const val StoreMillis = 24L * 60L * 60L * 1000L

        // The ratings alone for the cards: how many titles stay in memory, and a week on the disk, they almost never change
        private const val CertificationsMaxEntries = 600
        private const val CertificationsSpace = "cert1"
        private const val CertificationsStoreMillis = 7L * 24L * 60L * 60L * 1000L
    }
}
