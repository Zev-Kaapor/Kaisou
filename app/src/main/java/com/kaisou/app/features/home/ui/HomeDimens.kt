package com.kaisou.app.features.home.ui

// UI styling and utilities
import androidx.compose.ui.unit.dp

// Sizes that only the Home uses
object HomeDimens {
    // The art of the hero is a poster, as tall as the screen is wide times this ratio
    const val HeroAspect = 1.5f
    // Where the art starts and finishes fading into the page, as a share of its height
    const val HeroFadeStart = 0.38f
    const val HeroFadeEnd = 0.72f
    // Veil under the header
    val HeroScrimHeight = 120.dp
    // Details of the hero: room for the logo, gap to the buttons and gap to the bottom of the art
    val HeroLogoHeight = 80.dp
    val HeroButtonsTop = 22.dp
    val HeroInfoBottom = 6.dp
    const val HeroSynopsisLines = 3
    // How far a swipe has to go to change the title
    val HeroSwipeThreshold = 48.dp
    // Time each title stays on the carousel and the duration of the fade to the next one
    const val HeroAdvanceMillis = 6000L
    const val HeroFadeMillis = 600
    // How much of the height of the hero has to be scrolled for the header to turn solid, before its text reaches the header
    const val HeaderSolidFraction = 0.4f
    // Header, the logo and the actions on the right
    val Logo = 32.dp
    val HeaderAction = 48.dp
    val HeaderIcon = 24.dp
    val Avatar = 32.dp
    val AvatarIcon = 18.dp
    // Space between the sections of the page
    val SectionSpacing = 32.dp
    // Placeholders of the Home: the tag and the line of the details, the title of a shelf and the cards under it
    val SkeletonTagWidth = 40.dp
    val SkeletonMetaWidth = 170.dp
    val SkeletonShelfTitleWidth = 150.dp
    val SkeletonShelfTitleHeight = 18.dp
    const val SkeletonCards = 4
}
