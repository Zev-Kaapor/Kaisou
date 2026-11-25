package com.kaisou.app.core.rating

// The rating of MyAnimeList in the American scale, with the age group it falls in, the closest to the Brazilian scale
enum class MalRating(val code: String, val group: AgeGroup) {
    General("g", AgeGroup.All),
    Children("pg", AgeGroup.Ten),
    Teens("pg_13", AgeGroup.Fourteen),
    Violence("r", AgeGroup.Sixteen),
    MildNudity("r+", AgeGroup.Sixteen);

    // Gives a rating from the code the API sends
    companion object {
        // The rating from the code the API sends, like pg_13
        fun fromCode(code: String): MalRating? = entries.firstOrNull { it.code == code }
    }
}
