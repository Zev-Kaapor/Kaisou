package com.kaisou.app.core.model

// Android resources
import androidx.annotation.StringRes
// Project resources
import com.kaisou.app.R

// The advisory rating of a title: the Brazilian symbol when the app is in Portuguese of Brazil, or the rating of
// MyAnimeList as it is in the other languages
sealed interface ContentRating {
    data class Brazilian(val rating: AgeRating) : ContentRating

    data class Mal(val rating: MalRating) : ContentRating
}

// Rating of MyAnimeList, in the American scale it uses. The Brazilian one is the closest to it: the one most of the titles
// that have both got, when about 140 of them were compared, the others were one step away
enum class MalRating(
    val code: String,
    @StringRes val label: Int,
    @StringRes val description: Int,
    val brazilian: AgeRating
) {
    General("g", R.string.mal_rating_g, R.string.mal_rating_g_description, AgeRating.Free),
    Children("pg", R.string.mal_rating_pg, R.string.mal_rating_pg_description, AgeRating.Ten),
    Teens("pg_13", R.string.mal_rating_pg13, R.string.mal_rating_pg13_description, AgeRating.Fourteen),
    Violence("r", R.string.mal_rating_r, R.string.mal_rating_r_description, AgeRating.Sixteen),
    MildNudity("r+", R.string.mal_rating_r_plus, R.string.mal_rating_r_plus_description, AgeRating.Sixteen);

    companion object {
        // The rating from the code the API sends, like "pg_13"
        fun fromCode(code: String): MalRating? = entries.firstOrNull { it.code == code }
    }
}
