package com.kaisou.app.features.title.data

// Java time
import java.time.LocalDate

// Pairs the episodes of ani.zip with those of another source by the day they aired, one to one, the unmatched ones being left out
object DayMatcher {
    // The item of each episode that has a match, by episode number, the day written as 2026-10-03 and the order settling a tie
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

    // Moves a day by some days, keeping it when it cannot be read
    private fun shift(day: String, days: Long): String = runCatching { LocalDate.parse(day).plusDays(days).toString() }.getOrDefault(day)
}
