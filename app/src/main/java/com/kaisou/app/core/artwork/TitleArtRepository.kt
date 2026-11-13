package com.kaisou.app.core.artwork

// Android framework
import android.util.JsonReader
import android.util.JsonToken
// Coroutines
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
// Project resources
import com.kaisou.app.BuildConfig
import com.kaisou.app.core.anilist.AniListClient
import com.kaisou.app.core.cache.TimedCache
import com.kaisou.app.core.model.AgeRating
// Java networking and utilities
import java.io.InputStreamReader
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder
import java.util.Locale

// Artwork and texts of a title, every list holds full image addresses and is empty when there is none.
// The title and the overview are in the language of the device, null when the sources have none in it,
// the English overview is the one to translate in that case
data class TitleArt(
    val logos: List<String>,
    val posters: List<String>,
    val backdrops: List<String>,
    val title: String?,
    val overview: String?,
    val englishOverview: String?,
    // Brazilian advisory rating, null when no source has it
    val rating: AgeRating?
)

// What is known of a title to look it up by name when the other sites do not have its id: the names it goes by,
// the year it started and whether it is a movie
class TitleQuery(val names: List<String>, val year: Int?, val movie: Boolean)

// Finds the artwork of a title from the AniList id: the ids on the other sites come from ani.zip, from the season before
// or from a search by name, then TMDB is asked and TheTVDB fills what TMDB did not have.
// What neither has is left empty and AniList is the last one
class TitleArtRepository {
    // Artwork found per title, an empty one means the title has none, so it is not asked again
    private val arts = TimedCache<TitleArt>(CacheMillis, MaxEntries)

    private val anilist = AniListClient()

    suspend fun art(anilistId: Int, query: TitleQuery): TitleArt {
        arts.get(anilistId)?.let { return it.value }
        val fetched = withContext(Dispatchers.IO) { fetchArt(anilistId, query, Locale.getDefault()) }
        // A failed request is not cached, so it is tried again next time
        if (fetched.failed.not()) arts.put(anilistId, fetched.value)
        return fetched.value
    }

    // The Brazilian advisory rating that came with the artwork of a title, without asking anything
    fun knownRating(anilistId: Int): AgeRating? = arts.get(anilistId)?.value?.rating

    private class Fetched<T>(val value: T, val failed: Boolean)

    // The ids of a title on the other sites, the kind is what TMDB calls it
    private class Ids(val kind: String, val tmdbId: Int?, val tvdbId: Int?, val imdbId: String?)

    private class Image(val language: String?, val region: String?, val vote: Double, val width: Int, val path: String)

    private class Images(val logos: List<Image>, val posters: List<Image>, val backdrops: List<Image>)

    private class Translation(val language: String?, val region: String?, val title: String?, val overview: String?)

    private class Details(val images: Images, val translations: List<Translation>, val rating: AgeRating?)

    // A result of a search by name on TMDB
    private class Hit(val id: Int, val names: List<String>, val year: Int?, val votes: Int)

    private fun fetchArt(anilistId: Int, query: TitleQuery, locale: Locale): Fetched<TitleArt> {
        val ids = resolveIds(anilistId, query)
        if (ids.failed) return Fetched(NoArt, failed = true)
        val known = ids.value ?: return Fetched(NoArt, failed = false)

        var failed = false
        var art = NoArt
        if (known.tmdbId != null && BuildConfig.TMDB_TOKEN.isNotEmpty()) {
            // One request brings the images in the languages that matter, every translation of the title and its ratings
            val languages = listOf(locale.language, EnglishLanguage, NoLanguage).distinct().joinToString(",")
            val details = request(
                "$TmdbUrl/${known.kind}/${known.tmdbId}?include_image_language=$languages" +
                    "&append_to_response=images,translations,${ratingsPath(known.kind)}",
                token = BuildConfig.TMDB_TOKEN,
                read = ::readDetails
            )
            failed = details.failed
            art = details.value?.let { toArt(it, locale) } ?: NoArt
        }

        // TheTVDB fills the logo, the poster and the name that TMDB did not have
        if (art.logos.isEmpty() || art.posters.isEmpty() || art.title == null) {
            val tvdb = TvdbClient.art(known.tvdbId, known.imdbId, locale, needTitle = art.title == null)
            failed = failed || tvdb.failed
            tvdb.art?.let { found ->
                art = art.copy(
                    logos = art.logos.ifEmpty { found.logos },
                    posters = art.posters.ifEmpty { found.posters },
                    title = art.title ?: found.title
                )
            }
        }
        return Fetched(art, failed)
    }

    // Where TMDB keeps the ratings: a series has one per country and a movie one per release
    private fun ratingsPath(kind: String): String = if (kind == "movie") "release_dates" else "content_ratings"

    // The ids of the title: the ones ani.zip has for it. When it has none for TMDB, which can happen with ids of the
    // other sites in hand, the one of the season before it is used, since the seasons share the series (the newest entries
    // are not in ani.zip yet), and then the first result of a search by name that really looks like it
    private fun resolveIds(anilistId: Int, query: TitleQuery): Fetched<Ids?> {
        val direct = request("$MappingsUrl?anilist_id=$anilistId", token = null, read = ::readIds)
        if (direct.failed) return Fetched(null, failed = true)
        val known = direct.value
        if (known?.tmdbId != null) return Fetched(known, failed = false)

        var current = anilistId
        for (hop in 1..MaxPrequelHops) {
            val prequel = anilist.prequelOf(current) ?: break
            val found = request("$MappingsUrl?anilist_id=$prequel", token = null, read = ::readIds)
            if (found.failed) return Fetched(known, failed = true)
            if (found.value?.tmdbId != null) return Fetched(withTmdb(known, found.value), failed = false)
            current = prequel
        }
        val search = searchByName(query)
        return Fetched(withTmdb(known, search.value), failed = search.failed)
    }

    // The ids that were known with the one of TMDB that was found, the ones of the other sites stay as they were
    private fun withTmdb(base: Ids?, found: Ids?): Ids? {
        if (found?.tmdbId == null) return base
        return Ids(found.kind, found.tmdbId, base?.tvdbId ?: found.tvdbId, base?.imdbId ?: found.imdbId)
    }

    private fun searchByName(query: TitleQuery): Fetched<Ids?> {
        if (BuildConfig.TMDB_TOKEN.isEmpty()) return Fetched(null, failed = false)
        val kind = if (query.movie) "movie" else "tv"
        for (name in query.names.map(::withoutSeason).filter { it.isNotBlank() }.distinct()) {
            val search = request(
                "$TmdbUrl/search/$kind?include_adult=false&query=${URLEncoder.encode(name, "UTF-8")}",
                token = BuildConfig.TMDB_TOKEN,
                read = ::readHits
            )
            if (search.failed) return Fetched(null, failed = true)
            val hits = search.value.orEmpty()
            val match = hits.withIndex().firstOrNull { (index, hit) -> isSameTitle(hit, name, query.year, index == 0) }
            if (match != null) return Fetched(Ids(kind, match.value.id, tvdbId = null, imdbId = null), failed = false)
        }
        return Fetched(null, failed = false)
    }

    // The name without the part that says which season it is, the other sites have the series and not the season
    private fun withoutSeason(name: String): String {
        var result = name
        for (marker in SeasonMarkers) result = marker.replace(result, " ")
        return result.replace(Spaces, " ").trim().trimEnd(':', '-', '–', ' ')
    }

    // A result is the title when its name holds the searched one, or the other way around, and the series started before
    // the season and not long before. The first result of a search is also taken when it is a known title of that time,
    // since the site finds the name in other languages that the result does not show
    private fun isSameTitle(hit: Hit, searched: String, year: Int?, first: Boolean): Boolean {
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

    // A null answer means the server does not know the title, a failure is something else
    private fun <T> request(url: String, token: String?, read: (HttpURLConnection) -> T?): Fetched<T?> {
        val connection = URL(url).openConnection() as HttpURLConnection
        return try {
            connection.connectTimeout = TimeoutMillis
            connection.readTimeout = TimeoutMillis
            connection.setRequestProperty("Accept", "application/json")
            if (token != null) connection.setRequestProperty("Authorization", "Bearer $token")
            when (connection.responseCode) {
                HttpURLConnection.HTTP_OK -> Fetched(read(connection), failed = false)
                HttpURLConnection.HTTP_NOT_FOUND -> Fetched(null, failed = false)
                else -> Fetched(null, failed = true)
            }
        } catch (_: Exception) {
            Fetched(null, failed = true)
        } finally {
            connection.disconnect()
        }
    }

    // The answer of ani.zip can be huge because of the episodes, so it is read as a stream and only the mapping is kept
    private fun readIds(connection: HttpURLConnection): Ids? {
        JsonReader(InputStreamReader(connection.inputStream, Charsets.UTF_8)).use { reader ->
            // A title that is not known comes back as a plain string
            if (reader.peek() != JsonToken.BEGIN_OBJECT) return null
            var ids: Ids? = null
            reader.beginObject()
            while (reader.hasNext()) {
                if (reader.nextName() == "mappings") ids = readMappings(reader) else reader.skipValue()
            }
            return ids
        }
    }

    private fun readMappings(reader: JsonReader): Ids? {
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
        return Ids(if (type == MovieType) "movie" else "tv", tmdbId, tvdbId, imdbId)
    }

    // The results of a search: a series has a name and a movie has a title, each with the original one
    private fun readHits(connection: HttpURLConnection): List<Hit> {
        JsonReader(InputStreamReader(connection.inputStream, Charsets.UTF_8)).use { reader ->
            val hits = mutableListOf<Hit>()
            reader.beginObject()
            while (reader.hasNext()) {
                if (reader.nextName() != "results") {
                    reader.skipValue()
                    continue
                }
                reader.beginArray()
                while (reader.hasNext()) {
                    var id = 0
                    val names = mutableListOf<String>()
                    var year: Int? = null
                    var votes = 0
                    reader.beginObject()
                    while (reader.hasNext()) {
                        when (reader.nextName()) {
                            "id" -> id = reader.nextInt()
                            "name", "title", "original_name", "original_title" -> nextStringOrNull(reader)?.let { names += it }
                            "first_air_date", "release_date" -> year = nextStringOrNull(reader)?.take(YearLength)?.toIntOrNull()
                            "vote_count" -> votes = reader.nextInt()
                            else -> reader.skipValue()
                        }
                    }
                    reader.endObject()
                    if (id != 0) hits += Hit(id, names, year, votes)
                }
                reader.endArray()
            }
            return hits
        }
    }

    private fun readDetails(connection: HttpURLConnection): Details {
        JsonReader(InputStreamReader(connection.inputStream, Charsets.UTF_8)).use { reader ->
            var images = Images(emptyList(), emptyList(), emptyList())
            var translations = emptyList<Translation>()
            var rating: AgeRating? = null
            reader.beginObject()
            while (reader.hasNext()) {
                when (reader.nextName()) {
                    "images" -> images = readImages(reader)
                    "translations" -> translations = readTranslations(reader)
                    "content_ratings", "release_dates" -> rating = readRating(reader)
                    else -> reader.skipValue()
                }
            }
            return Details(images, translations, rating)
        }
    }

    private fun readImages(reader: JsonReader): Images {
        var logos = emptyList<Image>()
        var posters = emptyList<Image>()
        var backdrops = emptyList<Image>()
        reader.beginObject()
        while (reader.hasNext()) {
            when (reader.nextName()) {
                "logos" -> logos = readImageList(reader)
                "posters" -> posters = readImageList(reader)
                "backdrops" -> backdrops = readImageList(reader)
                else -> reader.skipValue()
            }
        }
        reader.endObject()
        return Images(logos, posters, backdrops)
    }

    private fun readImageList(reader: JsonReader): List<Image> {
        val list = mutableListOf<Image>()
        reader.beginArray()
        while (reader.hasNext()) {
            var language: String? = null
            var region: String? = null
            var vote = 0.0
            var width = 0
            var path: String? = null
            reader.beginObject()
            while (reader.hasNext()) {
                when (reader.nextName()) {
                    "iso_639_1" -> language = nextStringOrNull(reader)
                    "iso_3166_1" -> region = nextStringOrNull(reader)
                    "vote_average" -> vote = reader.nextDouble()
                    "width" -> width = reader.nextInt()
                    "file_path" -> path = nextStringOrNull(reader)
                    else -> reader.skipValue()
                }
            }
            reader.endObject()
            if (path != null) list += Image(language, region, vote, width, path)
        }
        reader.endArray()
        return list
    }

    // The translations come in an object with the list inside it, a series has a name and a movie has a title
    private fun readTranslations(reader: JsonReader): List<Translation> {
        val list = mutableListOf<Translation>()
        reader.beginObject()
        while (reader.hasNext()) {
            if (reader.nextName() != "translations") {
                reader.skipValue()
                continue
            }
            reader.beginArray()
            while (reader.hasNext()) {
                var language: String? = null
                var region: String? = null
                var title: String? = null
                var overview: String? = null
                reader.beginObject()
                while (reader.hasNext()) {
                    when (reader.nextName()) {
                        "iso_639_1" -> language = nextStringOrNull(reader)
                        "iso_3166_1" -> region = nextStringOrNull(reader)
                        "data" -> {
                            reader.beginObject()
                            while (reader.hasNext()) {
                                when (reader.nextName()) {
                                    "name", "title" -> title = nextStringOrNull(reader)?.takeIf { it.isNotBlank() } ?: title
                                    "overview" -> overview = nextStringOrNull(reader)?.takeIf { it.isNotBlank() }
                                    else -> reader.skipValue()
                                }
                            }
                            reader.endObject()
                        }
                        else -> reader.skipValue()
                    }
                }
                reader.endObject()
                list += Translation(language, region, title, overview)
            }
            reader.endArray()
        }
        reader.endObject()
        return list
    }

    // The Brazilian rating among the ones of every country, the same shape alone or inside the details
    private fun readRating(reader: JsonReader): AgeRating? {
        var rating: AgeRating? = null
        reader.beginObject()
        while (reader.hasNext()) {
            if (reader.nextName() != "results") {
                reader.skipValue()
                continue
            }
            reader.beginArray()
            while (reader.hasNext()) {
                var country: String? = null
                val codes = mutableListOf<String>()
                reader.beginObject()
                while (reader.hasNext()) {
                    when (reader.nextName()) {
                        "iso_3166_1" -> country = nextStringOrNull(reader)
                        "rating" -> nextStringOrNull(reader)?.let { codes += it }
                        "release_dates" -> {
                            reader.beginArray()
                            while (reader.hasNext()) {
                                reader.beginObject()
                                while (reader.hasNext()) {
                                    if (reader.nextName() == "certification") nextStringOrNull(reader)?.let { codes += it } else reader.skipValue()
                                }
                                reader.endObject()
                            }
                            reader.endArray()
                        }
                        else -> reader.skipValue()
                    }
                }
                reader.endObject()
                if (country == RatingCountry) rating = codes.firstNotNullOfOrNull(AgeRating::fromCode) ?: rating
            }
            reader.endArray()
        }
        reader.endObject()
        return rating
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

    // Logos follow the language of the device and then English. Posters and backdrops prefer the ones
    // without a language, then the language of the device and then English
    private fun toArt(details: Details, locale: Locale): TitleArt {
        val images = details.images
        val language = locale.language
        val region = locale.country
        fun sameLanguage(image: Image) = image.language == language
        fun sameRegion(image: Image) = sameLanguage(image) && image.region == region
        fun english(image: Image) = image.language == EnglishLanguage
        fun plain(image: Image) = image.language == null

        return TitleArt(
            logos = pick(images.logos, MinLogoWidth, LogoVoteShare, ::sameRegion, ::sameLanguage, ::english),
            posters = pick(images.posters, MinPosterWidth, 0f, ::plain, ::sameRegion, ::sameLanguage, ::english),
            backdrops = pick(images.backdrops, MinBackdropWidth, 0f, ::plain, ::sameRegion, ::sameLanguage, ::english),
            title = text(details.translations, language, region) { it.title },
            overview = text(details.translations, language, region) { it.overview },
            englishOverview = text(details.translations, EnglishLanguage, "") { it.overview },
            rating = details.rating
        )
    }

    // The text of the translation in the language of the device, the one of its country first
    private fun text(list: List<Translation>, language: String, region: String, field: (Translation) -> String?): String? =
        list.firstOrNull { it.language == language && it.region == region && !field(it).isNullOrBlank() }?.let(field)
            ?: list.firstOrNull { it.language == language && !field(it).isNullOrBlank() }?.let(field)

    // From the first group that has any: the ones sharp enough to fill the screen without looking blurred, or the biggest
    // one when none is, and of those the ones rated close to the best when a share is asked, as there are logos
    // of other versions of the title with almost no votes. Then the best rated ones
    private fun pick(images: List<Image>, minWidth: Int, voteShare: Float, vararg groups: (Image) -> Boolean): List<String> {
        for (group in groups) {
            val found = images.filter(group)
            if (found.isEmpty()) continue
            val sharp = found.filter { it.width >= minWidth }.ifEmpty { listOf(found.maxBy { it.width }) }
            val best = sharp.maxOf { it.vote }
            val chosen = if (voteShare > 0f && best > 0.0) sharp.filter { it.vote >= best * voteShare } else sharp
            return chosen.sortedByDescending { it.vote }.take(MaxPerTitle).map { "$ImageUrl$OriginalSize${it.path}" }
        }
        return emptyList()
    }

    companion object {
        // One instance for the whole app, so the artwork found stays in memory
        val Shared = TitleArtRepository()

        private val NoArt =
            TitleArt(emptyList(), emptyList(), emptyList(), title = null, overview = null, englishOverview = null, rating = null)
        private const val MappingsUrl = "https://api.ani.zip/mappings"
        private const val TmdbUrl = "https://api.themoviedb.org/3"
        private const val ImageUrl = "https://image.tmdb.org/t/p/"
        // Every image is asked in its original size, the device scales it down to what the screen shows
        private const val OriginalSize = "original"
        private const val MovieType = "MOVIE"
        private const val EnglishLanguage = "en"
        // How TMDB names the images that have no language
        private const val NoLanguage = "null"
        // How many images of a group are kept to choose from
        private const val MaxPerTitle = 10
        // Narrowest image that is still sharp when it fills the screen, by kind, and how close to the best rating a logo has to be
        private const val MinPosterWidth = 1000
        private const val MinBackdropWidth = 1280
        private const val MinLogoWidth = 600
        private const val LogoVoteShare = 0.5f
        // How many seasons back are followed to find the series, and how far a search by name may go from the year of a title
        private const val MaxPrequelHops = 3
        private const val SearchYearSpan = 15
        private const val MinSearchVotes = 5
        private const val YearLength = 4
        private const val TimeoutMillis = 15_000
        // The country of the ratings that are shown
        private const val RatingCountry = "BR"
        // How long the artwork of a title is kept and how many titles at most
        private const val CacheMillis = 60L * 60L * 1000L
        private const val MaxEntries = 200

        private val Spaces = Regex("\\s+")
        private val NotLetterOrDigit = Regex("[^\\p{L}\\p{N}]")

        // The ways a name says which season it is, from the most specific one
        private val SeasonMarkers = listOf(
            Regex("\\b\\d+(st|nd|rd|th)\\s*(-|&|and)\\s*\\d+(st|nd|rd|th)\\s+(season|stage|part|cour)\\b", RegexOption.IGNORE_CASE),
            Regex("\\b(\\d+(st|nd|rd|th)|first|second|third|fourth|final)\\s+(season|stage|part|cour)\\b", RegexOption.IGNORE_CASE),
            Regex("\\b(season|part|cour|stage)\\s*\\d+\\b", RegexOption.IGNORE_CASE)
        )
    }
}
