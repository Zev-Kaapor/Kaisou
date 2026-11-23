package com.kaisou.app.features.title.data

// Java time
import java.time.LocalDate

// Pairs the episodes of ani.zip with the ones another source has, by the day they aired, since the seasons and the numbers of the sources are
// not the same: the source of the episode is whatever has the same day, or the day before or after it for the time zones. Two on the same day
// are given in the order they come, as long as both sides have as many, and the ones that do not match one to one are left out
object DayMatcher {
    // The item for each episode that has a match, by the number of the episode. The day is written as 2026-10-03, and the order settles the
    // items that share a day
    fun <T> pair(episodes: List<TitleEpisode>, items: List<T>, day: (T) -> String?, order: (T) -> Int): Map<Int, T> {
        val byDay = items.filter { day(it) != null }.sortedWith(compareBy({ day(it) }, { order(it) })).groupBy { day(it) }
        val episodesByDay = episodes.filter { it.aired != null }.groupBy { it.aired }.mapValues { entry -> entry.value.sortedBy { it.number } }
        val paired = LinkedHashMap<Int, T>()
        for (episode in episodes) {
            val aired = episode.aired ?: continue
            val same = episodesByDay.getValue(aired)
            val candidates = listOf(aired, shift(aired, -1), shift(aired, 1)).firstNotNullOfOrNull { byDay[it] } ?: continue
            val item = when {
                same.size == candidates.size -> candidates[same.indexOf(episode)]
                same.size == 1 -> candidates.first()
                else -> continue
            }
            paired[episode.number] = item
        }
        return paired
    }

    private fun shift(day: String, days: Long): String = runCatching { LocalDate.parse(day).plusDays(days).toString() }.getOrDefault(day)
}
