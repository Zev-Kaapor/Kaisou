package com.kaisou.app.features.home.ui

// UI styling and utilities
import androidx.compose.ui.unit.dp

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
    // Space between the sections of the page
    val SectionSpacing = 32.dp
}
