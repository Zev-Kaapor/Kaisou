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
    val score: Double,
    // The small version of the picture when the site gives one, null when only the whole one is known
    val thumbnail: String? = null
)

// A picture of the gallery of a title: the whole one, the small one for a grid, its size and its language, null when it has none
class GalleryImage(val url: String, val thumbnail: String, val width: Int, val height: Int, val language: String?)

// The posters, backdrops and logos of a title from both sites, to look at and not only the one picked for the page
class TitleGallery(val posters: List<GalleryImage>, val backdrops: List<GalleryImage>, val logos: List<GalleryImage>) {
    val isEmpty: Boolean get() = posters.isEmpty() && backdrops.isEmpty() && logos.isEmpty()

    // The same pictures with the language of the user first, then the ones with no text, then English, keeping the order
    fun inLanguage(language: String): TitleGallery {
        // The place of a picture in the order, by its language
        fun rank(image: GalleryImage) = when (image.language) {
            language -> 0
            null -> 1
            "en" -> 2
            else -> 3
        }
        return TitleGallery(posters.sortedBy(::rank), backdrops.sortedBy(::rank), logos.sortedBy(::rank))
    }

    // The gallery with nothing in it
    companion object {
        val None = TitleGallery(emptyList(), emptyList(), emptyList())
    }
}

// What a site answered: its pictures of one kind, and whether the answer failed, a title the site does not know is not a failure
class Artworks(val list: List<Artwork>, val failed: Boolean) {
    // The empty list of pictures and the one that failed
    companion object {
        val None = Artworks(emptyList(), failed = false)
        val Failed = Artworks(emptyList(), failed = true)
    }
}

// What a site has of a title: its posters, its logos and its backdrops, the wide pictures
class SiteImages(val posters: Artworks, val logos: Artworks, val backdrops: Artworks) {
    val failed: Boolean get() = posters.failed || logos.failed || backdrops.failed

    // The pictures of a site with nothing in them and the ones that failed
    companion object {
        val None = SiteImages(Artworks.None, Artworks.None, Artworks.None)
        val Failed = SiteImages(Artworks.Failed, Artworks.Failed, Artworks.Failed)
    }
}

// The ratings the countries give a title, by country as in BR, each as the country writes it, and whether the answer failed
class Certified(val byCountry: Map<String, String>, val failed: Boolean) {
    // The ratings with nothing in them and the ones that failed
    companion object {
        val None = Certified(emptyMap(), failed = false)
        val Failed = Certified(emptyMap(), failed = true)
    }
}

// The day each season of a series first aired, by season number, and whether the answer failed
class SeasonStarts(val byNumber: Map<Int, LocalDate>, val failed: Boolean) {
    // The season starts with nothing in them and the ones that failed
    companion object {
        val None = SeasonStarts(emptyMap(), failed = false)
        val Failed = SeasonStarts(emptyMap(), failed = true)
    }
}

// What a site has of one season of a series: the day it first aired, its posters, its backdrops and whether it failed
class SeasonArtwork(val airDate: LocalDate?, val posters: Artworks, val backdrops: Artworks, val failed: Boolean) {
    // The season with nothing in it and the one that failed
    companion object {
        val None = SeasonArtwork(null, Artworks.None, Artworks.None, failed = false)
        val Failed = SeasonArtwork(null, Artworks.None, Artworks.None, failed = true)
    }
}

// The picture of a season of a series that is its own, the poster and the backdrop, each null when there is none
class SeasonPictures(val posterUrl: String?, val backdropUrl: String?) {
    // The pictures of a season with nothing in them
    companion object {
        val None = SeasonPictures(null, null)
    }
}

// What is known of a title: its best poster and backdrop, its logos, the ratings of the countries and its place on TMDB
class TitleArt(
    val posterUrl: String?,
    val backdropUrl: String?,
    val logos: List<String>,
    val certifications: Map<String, String>,
    val tmdbKind: String? = null,
    val tmdbId: Int? = null
) {
    // The title with nothing known of it
    companion object {
        val None = TitleArt(null, null, emptyList(), emptyMap())
    }
}

// What is known of a title to look it up by name: the names it goes by, the year it started and whether it is a movie
class ArtQuery(val names: List<String>, val year: Int?, val movie: Boolean)

// The ids of a title on the other sites, the kind is what TMDB calls it, tv or movie
class TitleIds(val kind: String, val tmdbId: Int?, val tvdbId: Int?, val imdbId: String?)
