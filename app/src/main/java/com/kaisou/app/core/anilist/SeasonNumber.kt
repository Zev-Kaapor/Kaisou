package com.kaisou.app.core.anilist

// Java time and math
import java.time.LocalDate
import java.time.temporal.ChronoUnit
import kotlin.math.abs

// Works out which season of its series a title is, since AniList has one title for each season. From the surest to the least:
// the season of TheTVDB that first aired when the title started, then the number the name says, then the first one when nothing
// comes before the title
object SeasonNumber {
    // How far apart, in days, the start of the title and the first episode of the season can be: the first episode can come to
    // the streaming after the broadcast, or the other way around
    private const val ToleranceDays = 60L

    private val Ordinal = Regex("\\b(\\d{1,2})(?:st|nd|rd|th)\\s+season\\b", RegexOption.IGNORE_CASE)
    private val Numbered = Regex("\\bseason\\s*(\\d{1,2})\\b", RegexOption.IGNORE_CASE)
    private val Worded = Regex("\\b(second|third|fourth|fifth|sixth)\\s+season\\b", RegexOption.IGNORE_CASE)
    private val Japanese = Regex("(\\d{1,2})\\s*期")
    private val Words = listOf("second", "third", "fourth", "fifth", "sixth")
    // The first season is the second word of the list, the one after the first
    private const val FirstWordSeason = 2

    // The season a name says, null when none of the names does. Parts and arcs are not seasons
    fun marker(names: List<String>): Int? =
        names.firstNotNullOfOrNull { name ->
            Ordinal.find(name)?.groupValues?.get(1)?.toIntOrNull()
                ?: Numbered.find(name)?.groupValues?.get(1)?.toIntOrNull()
                ?: Worded.find(name)?.groupValues?.get(1)?.let { Words.indexOf(it.lowercase()) + FirstWordSeason }
                ?: Japanese.find(name)?.groupValues?.get(1)?.toIntOrNull()
        }

    // The season of the series that first aired nearest to the day the title started, as long as it is within the tolerance. Null
    // when the day is not known or no season is close enough
    fun byDate(start: LocalDate?, firstAired: Map<Int, LocalDate>): Int? {
        if (start == null) return null
        return firstAired.entries
            .map { it.key to abs(ChronoUnit.DAYS.between(it.value, start)) }
            .filter { it.second <= ToleranceDays }
            .minByOrNull { it.second }
            ?.first
    }

    // The season to show, null when it is not known
    fun resolve(byDate: Int?, marker: Int?, hasPrequel: Boolean): Int? =
        byDate ?: marker ?: if (!hasPrequel) 1 else null
}
