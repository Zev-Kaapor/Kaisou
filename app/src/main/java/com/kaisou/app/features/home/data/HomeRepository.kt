package com.kaisou.app.features.home.data

// Android framework
import android.content.Context
// Project resources
import com.kaisou.app.core.anilist.AniListClient
import com.kaisou.app.core.auth.AniListSession

// Asks AniList for the Home and turns the answer into what the screens show
class HomeRepository(context: Context) {
    private val client = AniListClient()
    private val mapper = HomeMapper(context.resources)

    // The featured titles are drawn from the pool each time the app opens, so there is always something new. Null when
    // AniList cannot be reached or has nothing to feature
    suspend fun heroes(): List<HeroUi>? =
        client.heroPool(AniListSession.token)
            ?.takeIf { it.isNotEmpty() }
            ?.shuffled()
            ?.take(HeroCount)
            ?.map(mapper::hero)
}
