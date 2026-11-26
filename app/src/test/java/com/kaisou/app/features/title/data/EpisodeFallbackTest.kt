package com.kaisou.app.features.title.data

import com.kaisou.app.core.artwork.TmdbEpisode
import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.LocalDate

// Tests of the episodes made from IMDb and TMDB for a title ani.zip has none for
class EpisodeFallbackTest {
    private val today = LocalDate.of(2026, 11, 25)

    // An episode of TMDB with the day, the name and the text it may have
    private fun tmdb(number: Int, day: String?, name: String = "Name ${'A' + number}", still: String? = "still$number") =
        TmdbEpisode(number, day, name, "Overview $number", 0f, 0, still, 24)

    // An episode of IMDb with what it may have
    private fun imdb(number: Int, day: String?, name: String? = "Imdb $number", plot: String? = "Plot $number", image: String? = "img$number") =
        ImdbClient.Info(2, number, day, name, plot, image, 23)

    // The title airs on Fridays and the next episode is the eighth
    private val airing = EpisodeFallback.Airing(LocalDate.of(2026, 10, 9), nextEpisode = 8, finished = false, total = null)

    // IMDb comes first for the text and the picture, TMDB gives the name in English, the day and what IMDb lacks
    @Test
    fun imdbComesFirstAndTmdbFillsWhatItLacks() {
        val tmdbList = (1..8).map { tmdb(it, LocalDate.of(2026, 10, 9).plusWeeks(it - 1L).toString()) }
        val imdbList = listOf(imdb(1, "2026-10-08"), imdb(2, "2026-10-15", plot = null, image = null))
        val episodes = EpisodeFallback.merge(airing, 2, tmdbList, imdbList, today)
        assertEquals(7, episodes.size)
        assertEquals("Name B", episodes[0].title)
        assertEquals("Plot 1", episodes[0].overview)
        assertEquals("img1", episodes[0].image)
        assertEquals("Overview 2", episodes[1].overview)
        assertEquals("still2", episodes[1].image)
        assertEquals("Name D", episodes[2].title)
        assertEquals(2, episodes[2].season)
    }

    // A title with no next episode counts only the episodes whose day came
    @Test
    fun aTitleOnTheWayCountsOnlyTheEpisodesWhoseDayCame() {
        val list = listOf(tmdb(1, "2026-11-13"), tmdb(2, "2026-11-20"), tmdb(3, "2026-11-27"))
        val episodes = EpisodeFallback.merge(EpisodeFallback.Airing(null, null, finished = false, total = null), 1, list, emptyList(), today)
        assertEquals(listOf(1, 2), episodes.map { it.number })
    }

    // A season that holds two titles starts the second one at the episode that aired on its first day
    @Test
    fun aSecondTitleInTheSeasonStartsWhereItsFirstDayIs() {
        val list = (1..12).map { tmdb(it, LocalDate.of(2026, 7, 3).plusWeeks(it - 1L).toString()) }
        val second = EpisodeFallback.Airing(LocalDate.of(2026, 9, 18), nextEpisode = null, finished = true, total = 3)
        val episodes = EpisodeFallback.merge(second, 1, list, emptyList(), today)
        assertEquals(1, episodes.size)
        assertEquals(1, episodes.single().number)
        assertEquals("Name M", episodes.single().title)
    }

    // A name TMDB gives that is only a number says nothing, so the one of IMDb is used
    @Test
    fun aNameThatIsOnlyANumberIsLeftOut() {
        val episodes = EpisodeFallback.merge(airing, 2, listOf(tmdb(1, "2026-10-09", name = "Episode 1")), listOf(imdb(1, "2026-10-08")), today)
        assertEquals("Imdb 1", episodes.single().title)
    }
}
