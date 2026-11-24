package com.kaisou.app.features.user.ui

import androidx.compose.ui.unit.dp

// Sizes of the page of a person
object UserDimens {
    // The banner behind the name, which goes up to the top of the screen, with the room of the status bar added to it, the share of it at
    // the top that stays clear before it starts to fade into the page, and the picture of the person over it
    val BannerHeight = 168.dp
    const val BannerClear = 0.35f
    val Avatar = 88.dp

    // The days AniList keeps of a person, about half a year, as a grid of weeks as wide as the page: the space between the squares as a
    // share of one square, how round they are and the line around the day that is picked
    const val ActivityWeeks = 27
    const val ActivityDaysInWeek = 7
    const val ActivityGap = 0.2f
    const val ActivityCorner = 0.25f
    val ActivityPickWidth = 1.5.dp
    // The strongest level AniList gives a day, and the weakest that is painted, as a share of the strongest color
    const val ActivityTopLevel = 10f
    const val ActivityFloor = 0.6f

    // The strip with the scale under the totals: how dark it is, how tall its bar is, and how much of its width the bar has at the top of the scale
    const val ScaleShade = 0.5f
    val TotalsBarHeight = 10.dp
    val ScaleTickWidth = 1.dp
    val ScaleTickHeight = 6.dp
    const val TotalsBarReach = 5f / 6f

    // The button to follow, in the line of the name: its height, its icon, and the width of its placeholder
    val FollowSize = 26.dp
    val FollowIcon = 14.dp
    val FollowSkeletonWidth = 78.dp

    // How many genres are named over the bar, and how many have a piece of it
    const val GenreNamed = 4
    const val GenreBarPieces = 6
    val GenreBarHeight = 8.dp

    // The cover at the start of a thing the person did to a title
    val RecentCover = 60.dp

    // The placeholders: the shares of the width of the bars, how many lines and cards, and the height of the card of genres
    const val SkeletonName = 0.5f
    const val SkeletonSince = 0.35f
    const val SkeletonCounts = 0.55f
    const val SkeletonTitle = 0.4f
    const val SkeletonLastLine = 0.6f
    const val SkeletonAboutLines = 4
    const val SkeletonRecent = 3
    const val SkeletonButtons = 4
    val SkeletonGenres = 112.dp
    val SkeletonTotals = 96.dp
    val SkeletonDays = 152.dp
}
