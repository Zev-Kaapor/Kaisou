package com.kaisou.app.features.title.ui

// UI styling and utilities
import androidx.compose.ui.unit.dp

// Sizes that only the page of a title uses
object TitleDimens {
    // How long the page waits to ask AniList again while it is open, for the friends and for what the user changes somewhere else
    const val AutoRefreshMillis = 3L * 60_000L
    // The star of the score under the name of the title, and the lines of the synopsis before it is opened
    val ScoreStar = 20.dp
    const val SynopsisLines = 4
    // The gap above the synopsis, and how faint the bell is while the notices of its releases are off
    val SynopsisTop = 16.dp
    const val SeriesScrimAlpha = 0.85f
    val ThemeLabelWidth = 48.dp
    val TagTipGap = 40.dp
    const val NewsPreview = 3
    val TrailerYoutube = 64.dp
    val SeriesLandscapeMax = 520.dp
    val TrailerLandscapeMax = 400.dp
    val CoverWidth = 112.dp
    val StepButton = 36.dp
    val ListCover = 56.dp
    val ListLine = 1.5.dp
    val ListIcon = 18.dp
    val ListBar = 8.dp
    val ListPill = 44.dp
    val ListPercentWidth = 40.dp
    val ListScoreBox = 72.dp
    val ListScoreIcon = 32.dp
    val HintIcon = 12.dp
    val SkeletonSortWidth = 96.dp
    // How many posters the placeholder of a page of titles has, and how many rows the one of a page of people
    const val CardsSkeletonCount = 18
    // How many people a row shows before its last card sends to the page with all of them
    const val PeopleShown = 12
    const val CastSkeletonRows = 8
    val SkeletonSortHeight = 28.dp
    val SkeletonPillHeight = 32.dp
    val SkeletonPills = listOf(72.dp, 104.dp, 88.dp, 120.dp)
    val SkeletonFacts = listOf(64.dp, 96.dp, 80.dp, 72.dp)
    val SkeletonTabWidth = 72.dp
    val SortIcon = 16.dp
    val HintGap = 4.dp
    val ProfileImageWidth = 112.dp
    val ProfileImageHeight = 160.dp
    val ProfileMark = 40.dp
    // The picture of a card of the page of all the people, which runs the whole height of the card
    val CastCardImageWidth = 88.dp
    val CastCardHeight = 124.dp
    val ListNumberBig = 72.dp
    const val CoverRatio = 2f / 3f
    const val NewsLines = 3
    // The gap above each block under the synopsis, and the room inside a chip of the strip, of the tags and of the links
    val BlockTop = 24.dp
    val ChipHorizontal = 12.dp
    val ChipVertical = 6.dp
    // The rows of titles show this many at most, and when there are more a button opens all of them on a page of their own
    const val CardsShown = 10
    val SeeAllTouch = 36.dp
    val SeeAllIcon = 24.dp
    // The tabs: how tall each is and how thick the line under the one that is on
    val TabHeight = 44.dp
    val TabIndicator = 2.dp
    // How far to the side, as a share of the width, the items of a section start when it is opened
    const val SectionShift = 0.25f
    // The lines of data: how much of the width the name takes, and the room above each line
    const val DataLabelFraction = 0.38f
    val DataLineTop = 6.dp
    // The play on the picture of the trailer, how big it is and how dark the round behind it
    val TrailerPlay = 56.dp
    val TrailerPlayIcon = 24.dp
    const val TrailerPlayAlpha = 0.55f
    // The icon of a link, and how faint the tags that give away the story are
    val LinkIcon = 14.dp
    // How dark the foot of the wide picture of the prequel and of the sequel is, under their names
    // How tall the placeholder of the details is
    const val DetailsSkeletonLines = 6
    // A person of the cast or of the staff: how wide the picture is and its shape, the face being taller than wide
    val PersonWidth = 96.dp
    const val PersonRatio = 0.72f
    // An episode: how wide its picture is and its shape, the room above it, how faint it is when it was done and the mark on it
    val EpisodeThumb = 148.dp
    const val EpisodeRatio = 16f / 9f
    val EpisodeTop = 12.dp
    const val WatchedAlpha = 0.45f
    val WatchedMark = 28.dp
    val WatchedMarkIcon = 16.dp
    // The community: a friend on the row, how wide it is, the picture, the ring of the user and how far the score sticks out under the
    // picture, then the picture of who wrote a review, the lines of a review, the bars of the scores and of the lists
    val FriendWidth = 84.dp
    val FriendAvatar = 72.dp
    val FriendRing = 2.dp
    val FriendBadgeOffset = 8.dp
    val ReviewAvatar = 28.dp
    const val ReviewLines = 3
    val ScoreBarHeight = 64.dp
    val ScoreBarGap = 4.dp
    const val ScoreBarMinimum = 0.04f
    val StatusBarHeight = 6.dp
}
