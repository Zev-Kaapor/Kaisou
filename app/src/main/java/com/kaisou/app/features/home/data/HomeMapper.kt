package com.kaisou.app.features.home.data

// Android framework
import android.content.res.Resources
import android.text.Html
import android.text.format.DateUtils
// UI styling and utilities
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.lerp
import androidx.core.graphics.toColorInt
// Java time
import java.time.Instant
import java.time.ZoneId
// Project resources
import com.kaisou.app.R
import com.kaisou.app.core.anilist.AniActivity
import com.kaisou.app.core.anilist.AniAiring
import com.kaisou.app.core.anilist.AniEntry
import com.kaisou.app.core.anilist.AniMedia
import com.kaisou.app.core.designsystem.theme.DarkBackground
import com.kaisou.app.core.designsystem.theme.DarkSurfaceHigh

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
            title = media.title,
            names = media.names,
            movie = media.format == MovieFormat,
            releaseDay = media.nextAiringAt?.let {
                Instant.ofEpochSecond(it).atZone(ZoneId.systemDefault()).dayOfWeek
            },
            year = media.year,
            format = format(media.format),
            genres = genres(media.genres),
            synopsis = synopsis(media.description),
            // A title the user is in the middle of continues from the episode after the last one they saw,
            // the others start from the first
            nextEpisode = if (continuing) (media.listProgress ?: 0) + 1 else FirstEpisode,
            continuing = continuing,
            coverUrl = media.cover,
            artColors = colors(media.color)
        )
    }

    fun poster(media: AniMedia): PosterUi =
        PosterUi(
            anilistId = media.id,
            title = media.title,
            details = details(media),
            malId = malId(media),
            artColors = colors(media.color),
            imageUrl = media.cover
        )

    fun episode(airing: AniAiring, nowSeconds: Long): EpisodeUi =
        EpisodeUi(
            anilistId = airing.media.id,
            title = airing.media.title,
            details = DateUtils.getRelativeTimeSpanString(
                airing.airingAt * MillisPerSecond,
                nowSeconds * MillisPerSecond,
                DateUtils.MINUTE_IN_MILLIS
            ).toString(),
            tag = resources.getString(R.string.episode_tag, airing.episode),
            artColors = colors(airing.media.color),
            // The wide banner fits the thumbnail, the cover is the fallback
            imageUrl = airing.media.banner ?: airing.media.cover
        )

    // A title in the middle, shown as a poster like the others: the next episode or chapter and how far the user is
    fun continueItem(entry: AniEntry): PosterUi {
        val media = entry.media
        val manga = media.type == MangaType
        val total = if (manga) media.chapters else media.episodes
        val next = entry.progress + 1
        val details = when {
            manga && total != null -> resources.getString(R.string.continue_manga, next, entry.progress, total)
            manga -> resources.getString(R.string.continue_manga_open, next)
            total != null -> resources.getString(R.string.continue_anime, next, entry.progress, total)
            else -> resources.getQuantityString(R.plurals.continue_anime_open, entry.progress, next, entry.progress)
        }
        return PosterUi(
            anilistId = media.id,
            title = media.title,
            details = details,
            malId = malId(media),
            artColors = colors(media.color),
            imageUrl = media.cover
        )
    }

    // What a person the user follows did, over the cover of the title
    fun story(activity: AniActivity): StoryUi =
        StoryUi(
            userId = activity.userId,
            name = activity.userName,
            activity = activityText(activity),
            initial = activity.userName.firstOrNull()?.uppercase().orEmpty(),
            coverUrl = activity.media.cover,
            avatarUrl = activity.avatar,
            coverColors = colors(activity.media.color),
            avatarColors = colors(null),
            seen = false
        )

    // The activity in words, like "Assistiu ao ep. 6 · Frieren". The ones that are not known show only the title
    private fun activityText(activity: AniActivity): String {
        val title = activity.media.title
        val status = activity.status.lowercase()
        // For a range like "1 - 6" the last number is the one that tells how far the person went
        val progress = activity.progress?.substringAfterLast(' ').orEmpty()
        return when {
            status.startsWith("watched") -> resources.getString(R.string.activity_watched, progress, title)
            status.startsWith("rewatched") -> resources.getString(R.string.activity_rewatched, progress, title)
            status.startsWith("read") -> resources.getString(R.string.activity_read, progress, title)
            status.startsWith("reread") -> resources.getString(R.string.activity_reread, progress, title)
            status.startsWith("plans to watch") -> resources.getString(R.string.activity_plans_watch, title)
            status.startsWith("plans to read") -> resources.getString(R.string.activity_plans_read, title)
            status.startsWith("completed") -> resources.getString(R.string.activity_completed, title)
            status.startsWith("dropped") -> resources.getString(R.string.activity_dropped, title)
            status.startsWith("paused") -> resources.getString(R.string.activity_paused, title)
            else -> title
        }
    }

    // MyAnimeList has no advisory rating for a manga
    private fun malId(media: AniMedia): Int? = if (media.type == MangaType) null else media.malId

    private fun details(media: AniMedia): String {
        val format = format(media.format)
        return media.year?.let { resources.getString(R.string.poster_details_year_format, it, format) } ?: format
    }

    // The gradient that shows until the image arrives, from the color of the cover to the page
    private fun colors(hex: String?): List<Color> {
        val base = hex?.let { runCatching { Color(it.toColorInt()) }.getOrNull() } ?: DarkSurfaceHigh
        return listOf(base, lerp(base, DarkBackground, FadeToPage))
    }

    private fun format(key: String?): String = formatNames[key].orEmpty()

    // The first genres, in the language of the app, the ones it does not know are left out
    private fun genres(list: List<String>): String =
        list.mapNotNull { genreNames[it] }
            .take(MaxGenres)
            .joinToString(", ")

    private fun names(keys: Int, names: Int): Map<String, String> =
        resources.getStringArray(keys).zip(resources.getStringArray(names)).toMap()

    // AniList sends the synopsis with markup and line breaks
    private fun synopsis(raw: String?): String =
        raw?.let { Html.fromHtml(it, Html.FROM_HTML_MODE_COMPACT).toString().replace(Spaces, " ").trim() }.orEmpty()

    private companion object {
        const val FirstEpisode = 1
        const val MovieFormat = "MOVIE"
        const val MangaType = "MANGA"
        const val WatchingStatus = "CURRENT"
        const val MaxGenres = 3
        const val MillisPerSecond = 1000L
        const val FadeToPage = 0.7f
        val Spaces = Regex("\\s+")
    }
}
