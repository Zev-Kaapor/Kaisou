package com.kaisou.app.core.artwork

// JSON parsing
import org.json.JSONArray
import org.json.JSONObject

// Makes the gallery of a title from the pictures both sites have, and writes it as text to be kept and reads it back
object GalleryBuilder {
    // The most pictures of each kind kept, and the width TMDB gives a small picture of each kind at
    private const val MaxPerKind = 40
    private const val PosterSize = "w342"
    private const val BackdropSize = "w780"
    private const val LogoSize = "w300"

    // The address that all the pictures of TMDB start with when whole, which a size name takes the place of to get a small one
    private const val TmdbWhole = "https://image.tmdb.org/t/p/original"
    private const val TmdbSizes = "https://image.tmdb.org/t/p/"

    // The least width of a picture of each kind to be in the gallery, so only the ones of high quality are: full HD for a backdrop
    private const val MinPosterWidth = 1000
    private const val MinBackdropWidth = 1920
    private const val MinLogoWidth = 1000

    // The gallery of the pictures of both sites, only the big ones, the better rated first, no picture twice
    fun of(posters: List<Artwork>, backdrops: List<Artwork>, logos: List<Artwork>): TitleGallery =
        TitleGallery(images(posters, PosterSize, MinPosterWidth), images(backdrops, BackdropSize, MinBackdropWidth), images(logos, LogoSize, MinLogoWidth))

    // The pictures of a kind as the gallery has them, the ones wide enough, the best rated first and up to the most kept
    private fun images(list: List<Artwork>, size: String, minWidth: Int): List<GalleryImage> =
        list.filter { it.width >= minWidth }.distinctBy { it.url }.sortedByDescending { it.score }.take(MaxPerKind).map { artwork ->
            GalleryImage(artwork.url, thumbnailOf(artwork, size), artwork.width, artwork.height, artwork.language)
        }

    // The small picture: the one TheTVDB gives, or the size of TMDB asked for in the address, or the whole one when neither is known
    private fun thumbnailOf(artwork: Artwork, size: String): String =
        artwork.thumbnail ?: if (artwork.url.startsWith(TmdbWhole)) TmdbSizes + size + artwork.url.removePrefix(TmdbWhole) else artwork.url

    // Writes the gallery as text to keep it
    fun write(gallery: TitleGallery): String =
        JSONObject().put("p", write(gallery.posters)).put("b", write(gallery.backdrops)).put("l", write(gallery.logos)).toString()

    // Writes the pictures of a kind
    private fun write(list: List<GalleryImage>): JSONArray = JSONArray().apply {
        list.forEach { put(JSONArray().put(it.url).put(it.thumbnail).put(it.width).put(it.height).put(it.language ?: "")) }
    }

    // Reads the gallery that was kept, null when the text is broken
    fun read(text: String): TitleGallery? =
        try {
            val json = JSONObject(text)
            TitleGallery(read(json.getJSONArray("p")), read(json.getJSONArray("b")), read(json.getJSONArray("l")))
        } catch (_: Exception) {
            null
        }

    // Reads the pictures of a kind
    private fun read(list: JSONArray): List<GalleryImage> =
        (0 until list.length()).map { index ->
            val item = list.getJSONArray(index)
            GalleryImage(item.getString(0), item.getString(1), item.getInt(2), item.getInt(3), item.getString(4).takeIf { it.isNotEmpty() })
        }
}
