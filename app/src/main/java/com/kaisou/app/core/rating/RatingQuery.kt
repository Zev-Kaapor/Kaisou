package com.kaisou.app.core.rating

// Project resources
import com.kaisou.app.core.artwork.ArtQuery

// What a card gives to find its rating: the ids the sources know it by, the adult mark, whether it is a manga, and the names
class RatingQuery(
    val anilistId: Int,
    val malId: Int?,
    val adult: Boolean,
    val manga: Boolean,
    val art: ArtQuery
)
