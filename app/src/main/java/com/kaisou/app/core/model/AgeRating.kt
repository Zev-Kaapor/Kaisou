package com.kaisou.app.core.model

// Android resources
import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
// Project resources
import com.kaisou.app.R

// Brazilian advisory rating, official symbols of the Ministry of Justice
enum class AgeRating(
    @DrawableRes val icon: Int,
    @StringRes val description: Int
) {
    Free(R.drawable.ic_rating_l, R.string.age_rating_free),
    Ten(R.drawable.ic_rating_10, R.string.age_rating_10),
    Twelve(R.drawable.ic_rating_12, R.string.age_rating_12),
    Fourteen(R.drawable.ic_rating_14, R.string.age_rating_14),
    Sixteen(R.drawable.ic_rating_16, R.string.age_rating_16),
    Eighteen(R.drawable.ic_rating_18, R.string.age_rating_18);

    companion object {
        // The rating from its code on TMDB, like "L" or "14", null for a code that is not one of the Brazilian ones
        fun fromCode(code: String): AgeRating? =
            when (code.trim().uppercase()) {
                "L" -> Free
                "10" -> Ten
                "12" -> Twelve
                "14" -> Fourteen
                "16" -> Sixteen
                "18" -> Eighteen
                else -> null
            }
    }
}
