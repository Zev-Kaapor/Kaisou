package com.kaisou.app.core.anilist

// Java time and math
import java.time.LocalDate
import java.time.temporal.ChronoUnit
import kotlin.math.abs

// Works out which season of its series a title is, from TheTVDB, then from the name, then the first
object SeasonNumber {
    // How many days apart the start of a title and the first episode of a season can be
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

    // Names that say a second part of a season, such as Part 2 or 2nd Cour
    private val Continuation = Regex(
        "\\b(?:part|cour)\\s*(?:[2-9]|ii|iii)\\b|\\b[2-9](?:nd|rd|th)\\s+(?:part|cour)\\b|\\b(?:second|third|fourth)\\s+(?:part|cour)\\b|" +
            "\\b[2-9](?:nd|rd|th)\\s*(?:&\\s*[2-9](?:nd|rd|th)\\s*)?stage\\b",
        RegexOption.IGNORE_CASE
    )

    // Tells whether a name says the title goes on in the season before
    fun isContinuation(names: List<String>): Boolean = names.any { Continuation.containsMatchIn(it) }

    private val NumberedPart = Regex("\\b(?:part|cour)\\s*([2-9]|iii|ii)\\b|\\b([2-9])(?:nd|rd|th)\\s+(?:part|cour)\\b|\\b(second|third|fourth)\\s+(?:part|cour)\\b", RegexOption.IGNORE_CASE)
    private val PartWords = listOf("second", "third", "fourth")
    private val PartRomans = mapOf("ii" to 2, "iii" to 3)

    // The part of a season a name says, such as 2 for Part 2, null when none does
    fun part(names: List<String>): Int? =
        names.firstNotNullOfOrNull { name ->
            NumberedPart.find(name)?.groupValues?.let { groups ->
                groups[1].takeIf { it.isNotEmpty() }?.let { it.toIntOrNull() ?: PartRomans[it.lowercase()] }
                    ?: groups[2].toIntOrNull()
                    ?: groups[3].takeIf { it.isNotEmpty() }?.let { PartWords.indexOf(it.lowercase()) + FirstWordPart }
            }
        }

    // The last season of the series that had started when the title started, where a second part goes on
    fun byContinuation(start: LocalDate?, firstAired: Map<Int, LocalDate>): Int? {
        if (start == null) return null
        return firstAired.entries.filter { !it.value.isAfter(start) }.maxByOrNull { it.value }?.key
    }

    // The season that first aired nearest to the day the title started, within the tolerance
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

    // The season of a title from its day, the seasons of its series, the number in its name and whether it is a second part
    fun of(start: LocalDate?, firstAired: Map<Int, LocalDate>, marker: Int?, hasPrequel: Boolean, continuation: Boolean): Int? {
        val between = start != null && hasPrequel && firstAired.values.any { it.isAfter(start) }
        return resolve(byDate(start, firstAired), marker, hasPrequel, if (continuation || between) byContinuation(start, firstAired) else null)
    }
}
