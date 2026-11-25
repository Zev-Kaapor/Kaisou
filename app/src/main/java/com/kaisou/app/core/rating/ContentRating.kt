package com.kaisou.app.core.rating

// Android resources
import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
// Project resources
import com.kaisou.app.R

// A title with no age to show: not rated yet, which is RP as the ESRB says it, or not rated at all, which is NR
enum class Unrated(@DrawableRes val symbol: Int, @StringRes val description: Int) {
    Pending(R.drawable.ic_rating_pending, R.string.age_rating_pending),
    NotRated(R.drawable.ic_rating_not_rated, R.string.age_rating_not_rated)
}

// The advisory rating of a title as shown, official or an approximation, with the symbol of the country or the global square
class ContentRating(
    val group: AgeGroup,
    val official: Boolean,
    val symbol: Boolean,
    val unrated: Unrated? = null
) {
    // The ratings for a title still pending and for one that has none
    companion object {
        val Pending = ContentRating(AgeGroup.All, official = true, symbol = false, unrated = Unrated.Pending)
        val NotRated = ContentRating(AgeGroup.All, official = true, symbol = false, unrated = Unrated.NotRated)
    }
}
