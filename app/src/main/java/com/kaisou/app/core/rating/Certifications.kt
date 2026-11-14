package com.kaisou.app.core.rating

// Reads the rating a country gives a title, as TMDB has it, into the youngest age it is for
object Certifications {
    // The labels with no number that stand for an age: for all ages, parental guidance and restricted ones
    private val WordAges = mapOf(
        "L" to 0, "LIVRE" to 0, "AL" to 0, "ALL" to 0, "U" to 0, "G" to 0, "TV-G" to 0, "TV-Y" to 0,
        "PG" to 8, "TV-PG" to 8,
        "M" to 15,
        "R" to 17, "TV-MA" to 17,
        "NC-17" to 18
    )
    private val Number = Regex("\\d{1,2}")

    // The age of a label like TV-14, 12, L or MA15+, null when it is not one that is known
    fun ageOf(label: String): Int? {
        val clean = label.trim().uppercase()
        WordAges[clean]?.let { return it }
        return Number.find(clean)?.value?.toIntOrNull()
    }
}
