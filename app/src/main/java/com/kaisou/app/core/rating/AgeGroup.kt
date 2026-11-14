package com.kaisou.app.core.rating

// Android resources
import androidx.annotation.DrawableRes
// Project resources
import com.kaisou.app.R

// The one scale every rating is turned into, the youngest age it is for. The symbols are the official Brazilian ones, of the
// Ministry of Justice
enum class AgeGroup(val minAge: Int, @DrawableRes val symbol: Int) {
    All(0, R.drawable.ic_rating_l),
    Ten(10, R.drawable.ic_rating_10),
    Twelve(12, R.drawable.ic_rating_12),
    Fourteen(14, R.drawable.ic_rating_14),
    Sixteen(16, R.drawable.ic_rating_16),
    Eighteen(18, R.drawable.ic_rating_18);

    companion object {
        // The group an age falls in: the last one that starts at or under it
        fun of(age: Int): AgeGroup = entries.last { age >= it.minAge }
    }
}
