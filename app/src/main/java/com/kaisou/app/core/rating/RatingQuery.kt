package com.kaisou.app.core.rating

// Project resources
import com.kaisou.app.core.artwork.ArtQuery

// What a card gives to find the rating of its title: the ids the sources know it by, whether AniList marks it for adults, whether it is
// a manga, which has no rating of the countries or of MyAnimeList, and what the sites are asked by to find the title
class RatingQuery(
    val anilistId: Int,
    val malId: Int?,
    val adult: Boolean,
    val manga: Boolean,
    val art: ArtQuery
)
