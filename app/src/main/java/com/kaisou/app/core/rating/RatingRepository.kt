package com.kaisou.app.core.rating

// Finds the rating of a title for the country of the user. MyAnimeList is only asked when the country does not give one
object RatingRepository {
    suspend fun find(region: String, malId: Int?, adult: Boolean, certifications: Map<String, String>): ContentRating? {
        RatingPicker.fromRegion(region, certifications)?.let { return it }
        val mal = malId?.let { MalClient.rating(it) }
        return RatingPicker.pick(region, certifications, mal, adult)
    }
}
