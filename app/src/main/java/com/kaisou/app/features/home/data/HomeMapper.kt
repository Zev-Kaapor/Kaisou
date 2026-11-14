package com.kaisou.app.features.home.data

// Android framework
import android.content.res.Resources
import android.text.Html
// UI styling and utilities
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.lerp
import androidx.core.graphics.toColorInt
// Java time
import java.time.Instant
import java.time.ZoneId
// Project resources
import com.kaisou.app.R
import com.kaisou.app.core.anilist.AniMedia
import com.kaisou.app.core.anilist.SeasonNumber
import com.kaisou.app.core.artwork.ArtQuery
import com.kaisou.app.core.designsystem.theme.Background
import com.kaisou.app.core.designsystem.theme.ContainerHigh

// Turns what AniList says into what the Home shows, in the language of the app
class HomeMapper(private val resources: Resources) {
    // Names AniList gives to the formats and the genres, with how the app shows them
    private val formatNames by lazy { names(R.array.format_keys, R.array.format_names) }
    private val genreNames by lazy { names(R.array.genre_keys, R.array.genre_names) }

    fun hero(media: AniMedia): HeroUi {
        val continuing = media.listStatus == WatchingStatus
        return HeroUi(
            anilistId = media.id,
            malId = media.malId,
            adult = media.adult,
            seasonMarker = SeasonNumber.marker(media.names),
            hasPrequel = media.hasPrequel,
            startDate = media.startDate,
            title = media.title,
            query = ArtQuery(media.names, media.year, media.format == MovieFormat),
            format = formatNames[media.format].orEmpty(),
            // The first genres, in the language of the app, the ones it does not know are left out
            genres = media.genres.mapNotNull { genreNames[it] }.take(MaxGenres).joinToString(", "),
            year = media.year,
            synopsis = synopsis(media.description),
            releaseDay = media.nextAiringAt?.let { Instant.ofEpochSecond(it).atZone(ZoneId.systemDefault()).dayOfWeek },
            // A title the user is in the middle of continues from the episode after the last one they saw, the others
            // start from the first
            nextEpisode = if (continuing) (media.listProgress ?: 0) + 1 else FirstEpisode,
            continuing = continuing,
            coverUrl = media.cover,
            artColors = colors(media.color)
        )
    }

    // The gradient that shows until the image arrives, from the color of the cover to the page
    private fun colors(hex: String?): List<Color> {
        val base = hex?.let { runCatching { Color(it.toColorInt()) }.getOrNull() } ?: ContainerHigh
        return listOf(base, lerp(base, Background, FadeToPage))
    }

    private fun names(keys: Int, names: Int): Map<String, String> =
        resources.getStringArray(keys).zip(resources.getStringArray(names)).toMap()

    // AniList sends the synopsis with markup and line breaks
    private fun synopsis(raw: String?): String =
        raw?.let { Html.fromHtml(it, Html.FROM_HTML_MODE_COMPACT).toString().replace(Spaces, " ").trim() }.orEmpty()

    private companion object {
        const val FirstEpisode = 1
        const val WatchingStatus = "CURRENT"
        const val MovieFormat = "MOVIE"
        const val MaxGenres = 3
        const val FadeToPage = 0.7f
        val Spaces = Regex("\\s+")
    }
}
