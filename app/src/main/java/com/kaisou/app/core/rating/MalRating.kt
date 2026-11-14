package com.kaisou.app.core.rating

// The rating of MyAnimeList, in the American scale it uses, with the group it falls in. The Brazilian scale is the closest to
// it: the one most of the titles that have both got, when about 140 of them were compared, the others were one step away
enum class MalRating(val code: String, val group: AgeGroup) {
    General("g", AgeGroup.All),
    Children("pg", AgeGroup.Ten),
    Teens("pg_13", AgeGroup.Fourteen),
    Violence("r", AgeGroup.Sixteen),
    MildNudity("r+", AgeGroup.Sixteen);

    companion object {
        // The rating from the code the API sends, like pg_13
        fun fromCode(code: String): MalRating? = entries.firstOrNull { it.code == code }
    }
}
