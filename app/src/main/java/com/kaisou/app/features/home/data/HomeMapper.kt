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
import com.kaisou.app.core.anilist.AniCard
import com.kaisou.app.core.anilist.AniMedia
import com.kaisou.app.core.anilist.AniReply
import com.kaisou.app.core.anilist.AniRecommended
import com.kaisou.app.core.anilist.AniShelves
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

    // Everything under the hero in the order it shows. The user goes apart from the friends, the new episodes of what the user follows
    // come first, then the others, and a shelf with nothing on it is left out
    fun shelves(source: AniShelves, userId: Int, recommended: AniRecommended?, sequels: List<AniCard>?, hideAdult: Boolean = false): HomeShelvesUi {
        val mine = (source.watching + source.plannedAnime).map { it.card.id }.toSet()
        val airing = source.airing.sortedByDescending { it.card.id in mine }
        val now = System.currentTimeMillis()
        return HomeShelvesUi(
            ownStory = stories(source.mine, now, hideAdult).firstOrNull()?.items.orEmpty(),
            friends = stories(source.following.filterNot { it.userId == userId }, now, hideAdult),
            shelves = listOfNotNull(
                shelf(ShelfId.Watching, CardShape.Poster, source.watching.map { card(it.card, done = it.progress, total = it.card.episodes) }),
                shelf(ShelfId.Reading, CardShape.Poster, source.reading.map { card(it.card, done = it.progress, total = it.card.chapters) }),
                shelf(
                    ShelfId.NewEpisodes,
                    CardShape.Wide,
                    airing.map {
                        val aired = DateUtils.getRelativeTimeSpanString(it.airedAt * MillisPerSecond, now, DateUtils.MINUTE_IN_MILLIS).toString()
                        card(it.card, details = aired, episode = it.episode, wide = true)
                    }
                ),
                shelf(ShelfId.PlannedAnime, CardShape.Poster, source.plannedAnime.map { card(it.card, details = size(it.card)) }),
                shelf(ShelfId.PlannedManga, CardShape.Poster, source.plannedManga.map { card(it.card, details = size(it.card)) }),
                shelf(ShelfId.MissingSequels, CardShape.Poster, sequels.orEmpty().map { card(it, details = size(it)) }),
                shelf(ShelfId.RecommendedAnime, CardShape.Poster, recommended?.anime.orEmpty().map { card(it, details = size(it)) }),
                shelf(ShelfId.RecommendedManga, CardShape.Poster, recommended?.manga.orEmpty().map { card(it, details = size(it)) }),
                shelf(ShelfId.Trending, CardShape.Poster, source.trending.map { card(it, details = size(it)) }),
                shelf(ShelfId.TrendingManga, CardShape.Poster, source.trendingManga.map { card(it, details = size(it)) }),
                shelf(ShelfId.Popular, CardShape.Poster, source.popular.map { card(it, details = size(it)) }),
                shelf(ShelfId.Upcoming, CardShape.Poster, source.upcoming.map { card(it, details = size(it)) }),
                shelf(ShelfId.NewAnime, CardShape.Poster, source.newAnime.map { card(it, details = size(it)) }),
                shelf(ShelfId.NewManga, CardShape.Poster, source.newManga.map { card(it, details = size(it)) })
            )
        )
    }

    // The activities of the last days grouped by person, the one who did something last first, each with what they did from the oldest
    // to the newest, the way a story plays. The ones about adult titles are only left out when asked to
    private fun stories(activities: List<AniActivity>, now: Long, hideAdult: Boolean): List<StoryUi> {
        val oldest = now / MillisPerSecond - StoryDays * SecondsPerDay
        return activities
            .filter { it.createdAt > oldest && !(hideAdult && it.adult) }
            .distinctBy { it.id }
            .groupBy { it.userId }
            .map { (userId, items) ->
                val latest = items.first()
                StoryUi(userId, latest.userName, latest.avatar, items.sortedBy { it.createdAt }.map(::storyItem))
            }
            .sortedByDescending { story -> story.items.maxOf { it.createdAt } }
    }

    private fun storyItem(activity: AniActivity): StoryItemUi {
        val line = line(activity)
        return StoryItemUi(
            id = activity.id,
            createdAt = activity.createdAt,
            line = line,
            highlight = line?.let { highlight(it, activity) },
            text = activity.text?.trim(),
            coverUrl = activity.card?.cover,
            backdropUrl = activity.card?.banner,
            likeCount = activity.likeCount,
            replyCount = activity.replyCount,
            isLiked = activity.isLiked
        )
    }

    fun reply(source: AniReply): ReplyUi = ReplyUi(source.id, source.userId, source.userName, source.avatar, source.text.trim(), source.createdAt, source.likeCount, source.isLiked)

    // Where the episodes or chapters are in the line, or the title, which ends it, when there are none
    private fun highlight(line: String, activity: AniActivity): IntRange? {
        val progress = activity.progress
        val title = activity.card?.title
        val start = when {
            progress != null -> line.indexOf(progress)
            title != null -> line.lastIndexOf(title)
            else -> -1
        }
        val length = progress?.length ?: title?.length ?: 0
        return if (start >= 0 && length > 0) start until start + length else null
    }

    // What a person did to a title, in the language of the app: the status AniList writes and the episodes or chapters
    private fun line(activity: AniActivity): String? {
        val card = activity.card ?: return null
        val status = activity.status.orEmpty().lowercase()
        val progress = activity.progress.orEmpty()
        val range = RangeMark in progress

        fun of(single: Int, several: Int) = resources.getString(if (range) several else single, progress, card.title)
        return when {
            status.startsWith("rewatched") -> of(R.string.story_rewatched, R.string.story_rewatched_range)
            status.startsWith("watched") -> of(R.string.story_watched, R.string.story_watched_range)
            status.startsWith("reread") -> of(R.string.story_reread, R.string.story_reread_range)
            status.startsWith("read") -> of(R.string.story_read, R.string.story_read_range)
            status.startsWith("plans to watch") -> resources.getString(R.string.story_plans_watch, card.title)
            status.startsWith("plans to read") -> resources.getString(R.string.story_plans_read, card.title)
            status.startsWith("completed") -> resources.getString(R.string.story_completed, card.title)
            status.startsWith("paused") -> resources.getString(R.string.story_paused, card.title)
            status.startsWith("dropped") -> resources.getString(R.string.story_dropped, card.title)
            else -> resources.getString(R.string.story_other, status, card.title)
        }
    }

    private fun shelf(id: ShelfId, shape: CardShape, cards: List<CardUi>): ShelfUi? =
        if (cards.isEmpty()) null else ShelfUi(id, shape, cards)

    // The format and how long a title is, like TV and 12 episodes, or only what is known of them. A title that is not out yet says
    // it is coming soon where its length would be, and one that is still going and has no total says how many aired so far, or that it
    // is going on, since AniList only gives a total when there is an end
    private fun size(card: AniCard): String {
        val format = card.format?.let { formatNames[it] }
        val length = when {
            card.status == NotYetReleased -> resources.getString(R.string.home_card_soon)
            card.episodes != null -> resources.getQuantityString(R.plurals.home_card_episodes, card.episodes, card.episodes)
            card.chapters != null -> resources.getQuantityString(R.plurals.home_card_chapters, card.chapters, card.chapters)
            card.aired != null -> resources.getQuantityString(R.plurals.home_card_episodes, card.aired, card.aired)
            card.status == Hiatus -> resources.getString(R.string.home_card_hiatus)
            card.status == Cancelled -> resources.getString(R.string.home_card_cancelled)
            card.status == Releasing -> resources.getString(R.string.home_card_ongoing)
            else -> null
        }
        return listOfNotNull(format, length).joinToString(resources.getString(R.string.home_card_separator))
    }

    // Every card has the score of its title on the scale of ten. The wide ones use the banner of the title when it has one, and the
    // cover when it does not
    private fun card(
        source: AniCard,
        details: String = "",
        episode: Int? = null,
        done: Int? = null,
        total: Int? = null,
        wide: Boolean = false
    ): CardUi =
        CardUi(
            anilistId = source.id,
            title = source.title,
            imageUrl = if (wide) source.banner ?: source.cover else source.cover,
            score = source.score?.let { it / ScoreScale },
            episode = episode,
            done = done,
            total = total,
            details = details,
            query = if (wide) ArtQuery(source.names, source.year, source.format == MovieFormat) else null
        )

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
        // AniList scores go from 0 to 100, the shelves show them from 0 to 10
        const val ScoreScale = 10f
        const val MillisPerSecond = 1000L
        // How far back the stories go, in days, and how many seconds a day has
        const val StoryDays = 3L
        const val SecondsPerDay = 86_400L
        const val RangeMark = " - "

        const val FirstEpisode = 1
        const val WatchingStatus = "CURRENT"
        const val NotYetReleased = "NOT_YET_RELEASED"
        const val Releasing = "RELEASING"
        const val Hiatus = "HIATUS"
        const val Cancelled = "CANCELLED"
        const val MovieFormat = "MOVIE"
        const val MaxGenres = 3
        const val FadeToPage = 0.7f
        val Spaces = Regex("\\s+")
    }
}
