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
    private const val FirstWordPart = 2

    // The season a name says, null when none of the names does. Parts and arcs are not seasons
    fun marker(names: List<String>): Int? =
        names.firstNotNullOfOrNull { name ->
            Ordinal.find(name)?.groupValues?.get(1)?.toIntOrNull()
                ?: Numbered.find(name)?.groupValues?.get(1)?.toIntOrNull()
                ?: Worded.find(name)?.groupValues?.get(1)?.let { Words.indexOf(it.lowercase()) + FirstWordSeason }
                ?: Japanese.find(name)?.groupValues?.get(1)?.toIntOrNull()
        }

    // A name that says it is the second part of a season or a later cour or stage, as Part 2, 2nd Cour, Second Cour or 2nd & 3rd Stage: it goes on in the
    // season that came before it
    private val Continuation = Regex(
        "\\b(?:part|cour)\\s*(?:[2-9]|ii|iii)\\b|\\b[2-9](?:nd|rd|th)\\s+(?:part|cour)\\b|\\b(?:second|third|fourth)\\s+(?:part|cour)\\b|" +
            "\\b[2-9](?:nd|rd|th)\\s*(?:&\\s*[2-9](?:nd|rd|th)\\s*)?stage\\b",
        RegexOption.IGNORE_CASE
    )

    fun isContinuation(names: List<String>): Boolean = names.any { Continuation.containsMatchIn(it) }

    private val NumberedPart = Regex("\\b(?:part|cour)\\s*([2-9]|iii|ii)\\b|\\b([2-9])(?:nd|rd|th)\\s+(?:part|cour)\\b|\\b(second|third|fourth)\\s+(?:part|cour)\\b", RegexOption.IGNORE_CASE)
    private val PartWords = listOf("second", "third", "fourth")
    private val PartRomans = mapOf("ii" to 2, "iii" to 3)

    // The part of a season or of a series a name says, as 2 for Part 2, 2nd Cour or Second Part, null when none of the names says one. The first
    // part is never said, so it is never found
    fun part(names: List<String>): Int? =
        names.firstNotNullOfOrNull { name ->
            NumberedPart.find(name)?.groupValues?.let { groups ->
                groups[1].takeIf { it.isNotEmpty() }?.let { it.toIntOrNull() ?: PartRomans[it.lowercase()] }
                    ?: groups[2].toIntOrNull()
                    ?: groups[3].takeIf { it.isNotEmpty() }?.let { PartWords.indexOf(it.lowercase()) + FirstWordPart }
            }
        }

    // The last season of the series that had already started when the title started, which is where a second part goes on. Null when the day is
    // not known or none had started
    fun byContinuation(start: LocalDate?, firstAired: Map<Int, LocalDate>): Int? {
        if (start == null) return null
        return firstAired.entries.filter { !it.value.isAfter(start) }.maxByOrNull { it.value }?.key
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
    fun resolve(byDate: Int?, marker: Int?, hasPrequel: Boolean, continued: Int? = null): Int? =
        byDate ?: marker ?: continued ?: if (!hasPrequel) 1 else null

    // The season of a title from what is known of it: its day, the seasons of its series, the number its name says and whether its name says it
    // is a second part
    // A title that has a prequel and starts between two seasons of its series is in the first of them, whatever its name says
    fun of(start: LocalDate?, firstAired: Map<Int, LocalDate>, marker: Int?, hasPrequel: Boolean, continuation: Boolean): Int? {
        val between = start != null && hasPrequel && firstAired.values.any { it.isAfter(start) }
        return resolve(byDate(start, firstAired), marker, hasPrequel, if (continuation || between) byContinuation(start, firstAired) else null)
    }
}
