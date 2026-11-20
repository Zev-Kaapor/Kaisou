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
    // Lying down the hero is shorter and its details tighter: the share of the height it takes, how tall the title is, how many lines
    // the synopsis has and the gap above the buttons. The main button is not stretched over the column
    const val HeroLandscapeHeightFraction = 0.78f
    val HeroLandscapeTitleHeight = 56.dp
    const val HeroLandscapeSynopsisLines = 2
    val HeroLandscapeButtonsTop = 14.dp
    val HeroLandscapeButtonMax = 320.dp
    // Lying down the art sits on the right and its left edge melts into the page, so the details are over the page and not over the
    // faces. The share of the width the wide picture takes, the same for the poster that takes its place, how far into the art the
    // melting goes and how much of the page color is left along the way, and how much of the height at the bottom melts into the page
    const val HeroLandscapeArtFraction = 0.7f
    const val HeroLandscapePosterFraction = 0.42f
    const val HeroArtEdgeFadeReach = 0.5f
    val HeroArtEdgeFadeStops = listOf(0f to 1f, 0.2f to 0.82f, 0.45f to 0.5f, 0.7f to 0.18f, 1f to 0f)
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
    // What is under the hero is asked for again by itself: when the user comes to the app, if the Home is older than the first time
    // given, so a quick switch between apps does not ask every time, and then every few minutes while it is on the screen
    const val ResumeRefreshMinMillis = 60_000L
    const val AutoRefreshMillis = 5L * 60_000L
    // Placeholder of what is under the hero: how many friends and how many cards of each shelf it shows, the name of a shelf and the
    // name of a friend as bars, and the two lines under a card, how much of the width each takes
    const val SkeletonFriends = 5
    const val SkeletonCards = 6
    val SkeletonShelfTitleWidth = 140.dp
    val SkeletonShelfTitleHeight = 18.dp
    val SkeletonFriendName = 56.dp
    val SkeletonLineHeight = 12.dp
    const val SkeletonTitleFraction = 0.8f
    const val SkeletonDetailsFraction = 0.5f
    // Space between the sections of the page
    val SectionSpacing = 24.dp
    // The cards of the shelves: how wide a poster and a wide card are, and the shape of each as width over height. They keep their size
    // lying down, where more of them fit in a row
    val PosterWidth = 108.dp
    val WideWidth = 200.dp
    const val PosterRatio = 2f / 3f
    const val WideRatio = 16f / 9f
    // The tab in the corner of a card, the score at the bottom and the new episode at the top: the room around its words, how round its
    // free corner is and the star of the score
    val TabHorizontal = 8.dp
    val TabVertical = 3.dp
    val TabCorner = 12.dp
    val TabStar = 10.dp
    // The screen to write a status: the round button that posts, its icon and its spinner, how faint the markers of the text and what is
    // not available are
    val StatusHeadingSize = 22.sp
    // A picture in a text is at most this tall, and the icon of a video that opens outside the app
    val StatusImageMaxHeight = 320.dp
    val StatusVideoIcon = 24.dp
    val StatusSendSize = 40.dp
    val StatusSendIcon = 20.dp
    val StatusSpinner = 20.dp
    val StatusSpinnerStroke = 2.dp
    const val MarkerAlpha = 0.6f
    const val StatusDisabledAlpha = 0.4f
    // A wide card shows its picture and the wide picture of its title in turn, all the cards together: how long each stays and how long
    // the change takes
    const val CardAlternateMillis = 5000L
    const val CardCrossfadeMillis = 700
    // The advisory rating at the start of the line under a card: how big its square is and how long it takes to come up
    val CardRatingSize = 16.dp
    // The line next to it: a size between the small and the medium, the room above it and the one between it and the square of the rating
    val CardDetailsSize = 11.sp
    val CardDetailsTop = 2.dp
    val CardDetailsGap = 6.dp
    const val CardRatingFadeMillis = 300
    // The line between the episode and the score in the tab
    // Row of the friends, as on Dantotsu: the picture, the ring around it split in one piece for each activity, how thick the ring
    // is, the most room between two pieces and the most all the gaps take together, in degrees, and the gap above the name
    val FriendAvatar = 64.dp
    val FriendRing = 78.dp
    val FriendRingStroke = 2.5.dp
    const val FriendRingGapMax = 12f
    const val FriendRingGapsTotal = 108f
    val FriendNameTop = 6.dp
    // The round plus on the picture of the user, where a status will be posted, and how far it sits from the corner of the picture
    val FriendPlus = 22.dp
    val FriendPlusIcon = 14.dp
    // The viewer of the stories: the bars on top and the space between them, how long a title and a text stay, how much of the width
    // on the left goes back instead of forward, how much the background is blurred and darkened, and how wide the cover is
    val StoryBarHeight = 3.dp
    val StoryBarGap = 4.dp
    const val StoryBarTrackAlpha = 0.3f
    const val StoryItemMillis = 5000
    const val StoryTextMillis = 9000
    const val StoryTapFraction = 0.33f
    val StoryBackdropBlur = 28.dp
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
