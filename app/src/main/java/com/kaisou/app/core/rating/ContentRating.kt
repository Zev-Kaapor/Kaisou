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

// The advisory rating of a title as it is shown. It is official when the country of the user gives it, and an approximation when
// it was worked out from another source. The symbol is the Brazilian one in Brazil, and the global square with the age in every
// other country. A title with none of them says why in unrated, and has no age
class ContentRating(
    val group: AgeGroup,
    val official: Boolean,
    val symbol: Boolean,
    val unrated: Unrated? = null
) {
    companion object {
        val Pending = ContentRating(AgeGroup.All, official = true, symbol = false, unrated = Unrated.Pending)
        val NotRated = ContentRating(AgeGroup.All, official = true, symbol = false, unrated = Unrated.NotRated)
    }
}
