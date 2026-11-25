package com.kaisou.app.features.home.ui

// UI styling and utilities
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

// Sizes that only the Home uses
object HomeDimens {
    // Header: the logo, the round buttons on the right and the picture of the account inside its button
    val Logo = 32.dp
    val HeaderAction = 48.dp
    val HeaderIcon = 24.dp
    val Avatar = 44.dp
    val AvatarIcon = 20.dp

    // Ring around the picture of the account and the dark gap between the ring and the picture, both inside the size of the avatar
    val AvatarRing = 1.5.dp
    val AvatarRingGap = 2.dp

    // Placeholders of the header while the account is not there: the name and the line of numbers
    val SkeletonNameWidth = 96.dp
    val SkeletonNameHeight = 14.dp
    val SkeletonStatsWidth = 140.dp
    val SkeletonStatsHeight = 10.dp

    // Icon of an image in the middle of the placeholder of the art
    val SkeletonArtIcon = 48.dp

    // Round counter of the notifications on the bell, and how far it sits from the corner of the button
    val CounterSize = 16.dp
    val CounterOffset = 6.dp

    // Most the counter shows, a bigger number reads as this one and a plus
    const val CounterMax = 99

    // The art of the hero standing up is a poster, as tall as the screen is wide times this ratio
    const val HeroAspect = 1.5f

    // Where the art fades into the page, as a share of its height standing up and of its width lying down
    const val HeroFadeStart = 0.38f
    const val HeroFadeEnd = 0.72f

    // Lying down the hero is shorter with tighter details: its height, the title, the synopsis lines and the gap above the buttons
    const val HeroLandscapeHeightFraction = 0.78f
    val HeroLandscapeTitleHeight = 56.dp

    // How many lines of synopsis the hero has lying down
    const val HeroLandscapeSynopsisLines = 2
    val HeroLandscapeButtonsTop = 14.dp
    val HeroLandscapeButtonMax = 320.dp

    // Lying down the art is on the right and melts into the page: its width share, how far it melts and how much page color stays
    const val HeroLandscapeArtFraction = 0.7f
    const val HeroLandscapePosterFraction = 0.42f
    const val HeroArtEdgeFadeReach = 0.5f
    val HeroArtEdgeFadeStops = listOf(0f to 1f, 0.2f to 0.82f, 0.45f to 0.5f, 0.7f to 0.18f, 1f to 0f)

    // How much of the height at the bottom melts into the page
    const val HeroLandscapeBottomFade = 0.3f

    // Share of the width the details of the hero take lying down, the art shows on the rest
    const val HeroLandscapeInfoWidth = 0.5f

    // Veil at the top of the hero, only as tall as the header so the art keeps its light: how strong it is at the very top
    const val HeroVeilStrength = 0.55f

    // How far the veil goes below the header, to pass the descenders of the letters
    val HeroVeilExtra = 13.dp

    // How faint the divider between the year and format and the genres is, so it does not look like part of the text
    const val MetaDividerAlpha = 0.5f

    // Details of the hero: room of the title, gap to the buttons, gap to the bottom of the art, lines of the synopsis and the tag
    val HeroTitleHeight = 80.dp
    val HeroButtonsTop = 22.dp
    val HeroInfoBottom = 6.dp

    // How many lines of synopsis the hero has standing up
    const val HeroSynopsisLines = 3
    val HeroTagHeight = 16.dp

    // Time each title stays on the carousel, the fade to the next one and how far a swipe goes to change it
    const val HeroAdvanceMillis = 6000L
    const val HeroFadeMillis = 600
    val HeroSwipeThreshold = 48.dp

    // How much of the height of the hero has to be scrolled for the header to turn solid, before its icons reach the text
    const val HeaderSolidFraction = 0.4f

    // Placeholder of the hero: the title takes this share of the width and how tall it is, the tag and the line of details
    const val SkeletonTitleWidthFraction = 0.55f
    val SkeletonTitleHeight = 56.dp
    val SkeletonTagWidth = 40.dp
    val SkeletonMetaWidth = 170.dp

    // What is under the hero is asked again by itself when the user comes back after a while, and every few minutes
    const val ResumeRefreshMinMillis = 60_000L
    const val AutoRefreshMillis = 5L * 60_000L

    // Placeholder of what is under the hero: how many friends and cards, and how much width the bars take
    const val SkeletonFriends = 5
    const val SkeletonCards = 6
    val SkeletonShelfTitleWidth = 140.dp
    val SkeletonShelfTitleHeight = 18.dp
    val SkeletonFriendName = 56.dp
    val SkeletonLineHeight = 12.dp

    // How much of the width the title and the details of a placeholder card take
    const val SkeletonTitleFraction = 0.8f
    const val SkeletonDetailsFraction = 0.5f

    // Space between the sections of the page
    val SectionSpacing = 24.dp

    // The cards of the shelves: how wide a poster and a wide card are and their shape, kept lying down
    val PosterWidth = 108.dp
    val WideWidth = 200.dp

    // The shapes of a poster and of a wide card, as width over height
    const val PosterRatio = 2f / 3f
    const val WideRatio = 16f / 9f

    // The tab in the corner of a card, the score and the new episode: the room around its words, its corner and the star
    val TabHorizontal = 8.dp
    val TabVertical = 3.dp
    val TabCorner = 12.dp
    val TabStar = 10.dp

    // The screen to write a status: the round post button, its icon and spinner, and how faint the markers and what is unavailable are
    val StatusHeadingSize = 22.sp

    // A picture in a text is at most this tall, and the icon of a video that opens outside the app
    val StatusImageMaxHeight = 320.dp
    val StatusVideoIcon = 24.dp
    val StatusSendSize = 40.dp
    val StatusSendIcon = 20.dp
    val StatusSpinner = 20.dp
    val StatusSpinnerStroke = 2.dp

    // How faint the markers and the unavailable parts of the status screen are
    const val MarkerAlpha = 0.6f
    const val StatusDisabledAlpha = 0.4f

    // A wide card alternates its picture and the wide one of its title, all together: how long each stays and the change takes
    const val CardAlternateMillis = 5000L
    const val CardCrossfadeMillis = 700

    // The advisory rating at the start of the line under a card: how big its square is and how long it takes to come up
    val CardRatingSize = 16.dp

    // The line next to the title of a card: its size, the room above it and the one before the rating
    val CardDetailsSize = 11.sp
    val CardDetailsTop = 2.dp
    val CardDetailsGap = 6.dp

    // How long the rating fades in
    const val CardRatingFadeMillis = 300

    // How long a card is on the screen before its rating is asked for
    const val CardRatingDelayMillis = 400L

    // The friends row, as on Dantotsu: the picture, the ring split by activity, its thickness, the gaps and the room above the name
    val FriendAvatar = 64.dp
    val FriendRing = 78.dp
    val FriendRingStroke = 2.5.dp

    // The most room between two pieces of the ring and the most all the gaps take, in degrees
    const val FriendRingGapMax = 12f
    const val FriendRingGapsTotal = 108f
    val FriendNameTop = 6.dp

    // The round plus on the picture of the user, where a status will be posted, and how far it sits from the corner of the picture
    val FriendPlus = 22.dp
    val FriendPlusIcon = 14.dp

    // The viewer of the stories: the bars, how long an item stays, the share that goes back, the blur and darkness and the cover width
    val StoryBarHeight = 3.dp
    val StoryBarGap = 4.dp

    // How faint the track of the bars is, how long an item and a text stay, and the share of the width that goes back
    const val StoryBarTrackAlpha = 0.3f
    const val StoryItemMillis = 5000
    const val StoryTextMillis = 9000
    const val StoryTapFraction = 0.33f

    // The turn of the cube between people: its time, the camera distance, how dark the far side gets and the fade between activities
    const val StoryTurnMillis = 380
    const val StoryCubeCamera = 12f
    const val StoryCubeShade = 0.55f
    const val StoryFadeMillis = 220
    val StoryRise = 16.dp
    val StoryBackdropBlur = 28.dp

    // How dark the background of a story is and how wide its cover is
    const val StoryBackdropAlpha = 0.65f
    const val StoryCoverFraction = 0.6f
    val StoryHeaderAvatar = 40.dp

    // The dots and the X are this far from each other, which is less than their own room, so they sit close
    val StoryHeaderActionsGap = (-8).dp

    // The comments of an activity: how much of the height the panel takes, how dark the rest gets, and the picture of a person in it
    const val RepliesPanelFraction = 0.65f
    const val RepliesPanelFractionLandscape = 0.9f

    // The heart pops from this share of its size when the like is given, and how long the count takes to change
    const val LikePopFrom = 0.6f
    const val CountMillis = 200
    val ReplyAvatar = 32.dp
    val ReplyActionIcon = 20.dp
}
