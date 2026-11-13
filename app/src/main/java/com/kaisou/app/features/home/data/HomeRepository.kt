package com.kaisou.app.features.home.data

// Android framework
import android.content.Context
// Coroutines
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
// Project resources
import com.kaisou.app.R
import com.kaisou.app.core.anilist.AniEntry
import com.kaisou.app.core.anilist.AniListClient
import com.kaisou.app.core.auth.AniListSession
import com.kaisou.app.core.model.ContentRating
import com.kaisou.app.core.rating.RatingRepository

// What the Home shows first, besides the artwork of the featured titles
class HomeContent(
    val heroes: List<HeroUi>,
    val stories: List<StoryUi>,
    val newEpisodes: List<EpisodeUi>,
    val shelves: Map<HomeShelf, List<PosterUi>>
)

// Asks AniList for the Home and turns the answer into what the screens show. What comes first is asked together,
// the shelves lower on the page are asked after, each one on its own, so one that fails does not take the others
class HomeRepository(context: Context) : PosterRatings {
    private val client = AniListClient()
    private val mapper = HomeMapper(context.resources)
    // Whether the ratings show in the Brazilian symbols, which follows the language of the app
    private val brazilian = context.resources.getBoolean(R.bool.brazilian_rating)

    // What is for everybody and what is for the user are asked together. Null when what is for everybody cannot be had,
    // when what is for the user fails the rest still shows
    suspend fun load(): HomeContent? = coroutineScope {
        val token = AniListSession.token
        val userId = AniListSession.viewer?.id
        val everybody = async { client.home() }
        val personal = async { if (token != null && userId != null) client.personal(token, userId) else null }
        val home = everybody.await() ?: return@coroutineScope null
        val mine = personal.await()
        val now = System.currentTimeMillis() / MillisPerSecond
        HomeContent(
            // The featured titles are drawn from the pool each time the app opens, so there is always something new
            heroes = home.pool.shuffled().take(HeroCount).map(mapper::hero),
            // The feed of the people the user follows also holds what the user did, which is not for this row
            stories = mine?.activities?.filter { it.userId != userId }?.map(mapper::story).orEmpty(),
            newEpisodes = home.airing.map { mapper.episode(it, now) },
            shelves = shelves(
                HomeShelf.Watching to mine?.watching?.map(mapper::continueItem),
                HomeShelf.Reading to mine?.reading?.map(mapper::continueItem),
                HomeShelf.PlannedAnime to mine?.plannedAnime?.let(::posters),
                HomeShelf.PlannedManga to mine?.plannedManga?.let(::posters),
                HomeShelf.Trending to home.trending.map(mapper::poster)
            )
        )
    }

    // The next season and what was added to the catalog last
    suspend fun discover(): Map<HomeShelf, List<PosterUi>> {
        val found = client.discover() ?: return emptyMap()
        return shelves(
            HomeShelf.NextSeason to found.nextSeason.map(mapper::poster),
            HomeShelf.NewAnime to found.newAnime.map(mapper::poster),
            HomeShelf.NewManga to found.newManga.map(mapper::poster)
        )
    }

    // What people recommend after the titles on the list of the user
    suspend fun recommended(): Map<HomeShelf, List<PosterUi>> {
        val token = AniListSession.token ?: return emptyMap()
        val userId = AniListSession.viewer?.id ?: return emptyMap()
        val found = client.recommended(token, userId) ?: return emptyMap()
        val (manga, anime) = found.partition { it.type == MangaType }
        return shelves(
            HomeShelf.RecommendedAnime to anime.map(mapper::poster),
            HomeShelf.RecommendedManga to manga.map(mapper::poster)
        )
    }

    // The sequels of what the user finished that they have not started
    suspend fun missingSequels(): Map<HomeShelf, List<PosterUi>> {
        val token = AniListSession.token ?: return emptyMap()
        val userId = AniListSession.viewer?.id ?: return emptyMap()
        val found = client.missingSequels(token, userId) ?: return emptyMap()
        return shelves(HomeShelf.MissingSequels to found.map(mapper::poster))
    }

    override fun known(item: PosterUi): ContentRating? = RatingRepository.Shared.known(item.anilistId, item.malId, brazilian)

    override suspend fun load(item: PosterUi): ContentRating? =
        RatingRepository.Shared.load(item.anilistId, item.malId, brazilian)

    // The ratings of the shelves are asked as soon as they arrive, from the top down, so most are there before their cards show
    fun prefetchRatings(shelves: Map<HomeShelf, List<PosterUi>>) {
        RatingRepository.Shared.prefetch(shelves.values.flatten().mapNotNull { it.malId })
    }

    private fun posters(entries: List<AniEntry>): List<PosterUi> = entries.map { mapper.poster(it.media) }

    // The shelves that have something to show, the ones that are empty or that failed are left out
    private fun shelves(vararg pairs: Pair<HomeShelf, List<PosterUi>?>): Map<HomeShelf, List<PosterUi>> =
        pairs.mapNotNull { (shelf, items) -> items?.takeIf { it.isNotEmpty() }?.let { shelf to it } }.toMap()

    private companion object {
        const val MillisPerSecond = 1000L
        // How many featured titles are drawn from the pool
        const val HeroCount = 6
        const val MangaType = "MANGA"
    }
}
