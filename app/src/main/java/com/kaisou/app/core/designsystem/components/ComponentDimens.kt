package com.kaisou.app.core.designsystem.components

// UI styling and utilities
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

// Sizes of the shared components
object ComponentDimens {
    // Round buttons over images
    val IconButton = 40.dp
    val IconButtonLarge = 48.dp
    // Icon inside a button
    val ButtonIcon = 20.dp
    // Official age rating symbol, regular and small under a poster, and the outline of the tag of a rating in text
    val RatingBadge = 16.dp
    val PosterRatingBadge = 16.dp
    val RatingTagBorder = 1.dp
    // Calendar icon inside the weekly release tag
    val BadgeIcon = 11.dp
    // Outline of the round button next to the main one
    val ButtonBorder = 2.dp
    // Portrait cover cards, a 2 by 3 poster
    val PosterWidth = 140.dp
    const val PosterAspect = 2f / 3f
    // Landscape cards of episodes, a 16 by 9 thumbnail
    val LandscapeWidth = 232.dp
    const val LandscapeAspect = 16f / 9f
    // Story cards, avatar and ring around it
    val StoryWidth = 92.dp
    val StoryHeight = 136.dp
    val StoryAvatar = 28.dp
    val StoryRing = 2.dp
    // Name of a title in text: lines it may take, and the size by the length of the name, one step every few characters
    const val TitleMaxLines = 3
    const val TitleCharsPerStep = 10
    val TitleSizes = listOf(32.sp, 29.sp, 25.sp, 27.sp, 23.sp, 22.sp, 21.sp, 18.sp, 17.sp)
    const val TitleLineHeightRatio = 1.2f
    // The size also follows the width: reference width, how much of the difference counts and the limits
    const val TitleReferenceWidth = 360f
    const val TitleScaleWeight = 0.5f
    const val TitleScaleMin = 0.9f
    const val TitleScaleMax = 1.5f
    // Logo of a title: widest share of the space it may take and the lowest it may get
    const val LogoMaxWidthFraction = 0.75f
    val LogoMinHeight = 36.dp
    // Placeholder of a title while its artwork is on the way: share of the width and height
    const val TitleSkeletonWidthFraction = 0.55f
    val TitleSkeletonHeight = 56.dp
    // Light that sweeps over the placeholders: how long it takes to cross and the pause before it comes again
    const val SkeletonSweepMillis = 900
    const val SkeletonPauseMillis = 300
    // Lines of text as placeholders: height, space between them, and how much of the width the last one takes
    val SkeletonLineHeight = 12.dp
    val SkeletonLineGap = 7.5.dp
    const val SkeletonLastLineFraction = 0.6f
}
