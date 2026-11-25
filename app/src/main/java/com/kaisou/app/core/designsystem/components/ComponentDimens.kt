package com.kaisou.app.core.designsystem.components

// UI styling and utilities
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

// Sizes of the shared components
object ComponentDimens {
    // Round button with only an icon, and the icon inside it
    val IconButton = 48.dp
    val IconButtonIcon = 24.dp

    // The cards of a menu: the icon, the room inside a card, between its icon and its name and between the cards
    val MenuCardIcon = 24.dp
    val MenuCardHorizontal = 16.dp
    val MenuCardVertical = 14.dp
    val MenuCardTextStart = 16.dp
    val MenuCardGap = 8.dp

    // Area at the start of a pill button where its icon, badge or spinner goes, the label stays centered
    val ButtonLeading = 32.dp

    // Icon that sits alone in that area, and the spinner that takes its place
    val ButtonIcon = 20.dp

    // Stroke of the spinner inside a button, and the outline of the round button next to the main one
    val ButtonSpinnerStroke = 2.dp
    val ButtonBorder = 2.dp

    // Small tags: their height and the icon inside them
    val BadgeHeight = 16.dp
    val BadgeIcon = 11.dp

    // The official symbol of a rating
    val RatingSymbol = 20.dp

    // Name of a title in text: lines it may take, and the size by the length of the name, one step every few characters
    const val TitleMaxLines = 3
    const val TitleCharsPerStep = 10
    val TitleSizes = listOf(32.sp, 29.sp, 25.sp, 27.sp, 23.sp, 22.sp, 21.sp, 18.sp, 17.sp)

    // How much taller than its text the line of a title is
    const val TitleLineHeightRatio = 1.2f

    // The size also follows the width: reference width, how much of the difference counts and the limits
    const val TitleReferenceWidth = 360f
    const val TitleScaleWeight = 0.5f
    const val TitleScaleMin = 0.9f
    const val TitleScaleMax = 1.5f

    // Logo of a title: how much of the width and how tall it can get, it fits inside keeping its shape
    const val TitleLogoWidthFraction = 0.7f
    val TitleLogoMaxHeight = 80.dp

    // Lines of text as placeholders: height, space between them, and how much of the width the last one takes
    val SkeletonLineHeight = 12.dp
    val SkeletonLineGap = 7.5.dp

    // How much of the width the last line of a placeholder takes
    const val SkeletonLastLineFraction = 0.6f
}
