package com.kaisou.app.core.artwork

// Java time
import java.time.LocalDate

// A picture found on one of the sites, a poster or a logo. The language is the two letters one and null when the picture has none
class Artwork(
    val url: String,
    val width: Int,
    val height: Int,
    val language: String?,
    // How the people of the site rated it, only used to settle a tie in size
    val score: Double
)

// What a site answered: its pictures of one kind, and whether the answer failed, a title the site does not know is not a failure
class Artworks(val list: List<Artwork>, val failed: Boolean) {
    companion object {
        val None = Artworks(emptyList(), failed = false)
        val Failed = Artworks(emptyList(), failed = true)
    }
}

// What a site has of a title: its posters, its logos and its backdrops, the wide pictures
class SiteImages(val posters: Artworks, val logos: Artworks, val backdrops: Artworks) {
    val failed: Boolean get() = posters.failed || logos.failed || backdrops.failed

    companion object {
        val None = SiteImages(Artworks.None, Artworks.None, Artworks.None)
        val Failed = SiteImages(Artworks.Failed, Artworks.Failed, Artworks.Failed)
    }
}

// The ratings the countries give a title, by country as in BR, each as the country writes it, and whether the answer failed
class Certified(val byCountry: Map<String, String>, val failed: Boolean) {
    companion object {
        val None = Certified(emptyMap(), failed = false)
        val Failed = Certified(emptyMap(), failed = true)
    }
}

// The day each season of a series first aired, by season number, and whether the answer failed
class SeasonStarts(val byNumber: Map<Int, LocalDate>, val failed: Boolean) {
    companion object {
        val None = SeasonStarts(emptyMap(), failed = false)
        val Failed = SeasonStarts(emptyMap(), failed = true)
    }
}

// What a site has of one season of a series: the day the season first aired when the site says it, its posters and its backdrops, and whether
// the answer failed
class SeasonArtwork(val airDate: LocalDate?, val posters: Artworks, val backdrops: Artworks, val failed: Boolean) {
    companion object {
        val None = SeasonArtwork(null, Artworks.None, Artworks.None, failed = false)
        val Failed = SeasonArtwork(null, Artworks.None, Artworks.None, failed = true)
    }
}

// The picture of a season of a series that is its own, the poster and the backdrop, each null when there is none
class SeasonPictures(val posterUrl: String?, val backdropUrl: String?) {
    companion object {
        val None = SeasonPictures(null, null)
    }
}

// What is known of a title: the address of its best poster and of its best backdrop, the addresses of its logos in the order they
// are worth trying, the ratings of the countries and where it is on TMDB, which is what its trailer is asked by
class TitleArt(
    val posterUrl: String?,
    val backdropUrl: String?,
    val logos: List<String>,
    val certifications: Map<String, String>,
    val tmdbKind: String? = null,
    val tmdbId: Int? = null
) {
    companion object {
        val None = TitleArt(null, null, emptyList(), emptyMap())
    }
}

// What is known of a title to look it up by name when the other sites do not have its id: the names it goes by, the year
// it started and whether it is a movie
class ArtQuery(val names: List<String>, val year: Int?, val movie: Boolean)

// The ids of a title on the other sites, the kind is what TMDB calls it, tv or movie
class TitleIds(val kind: String, val tmdbId: Int?, val tvdbId: Int?, val imdbId: String?)
