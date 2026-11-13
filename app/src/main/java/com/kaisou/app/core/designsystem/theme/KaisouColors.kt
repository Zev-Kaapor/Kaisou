package com.kaisou.app.core.designsystem.theme

// Compose state and runtime
import androidx.compose.runtime.Immutable
// UI styling and utilities
import androidx.compose.ui.graphics.Color

// Colors that are not part of the Material color scheme
@Immutable
data class KaisouColors(
    // Text and icons over images and gradients
    val onMedia: Color,
    // Veil over images
    val scrimStrong: Color,
    // Background of the bottom navigation bar and the icon of an unselected item
    val navBar: Color,
    val navUnselected: Color,
    // End of the gradient ring around a story that was not seen yet
    val storyRingEnd: Color,
    // Hints and other quiet text
    val textTertiary: Color,
    // Tag with the day of the week of a weekly release and what is drawn over it
    val weeklyBadge: Color,
    val onWeeklyBadge: Color,
    // Placeholder shapes while something loads and the light that sweeps over them
    val skeleton: Color,
    val skeletonHighlight: Color
)

val DarkKaisouColors = KaisouColors(
    onMedia = DarkOnBackground,
    scrimStrong = ScrimStrong,
    navBar = DarkSurfaceHigh,
    navUnselected = DarkOnSurfaceVariant,
    storyRingEnd = StoryRingEnd,
    textTertiary = DarkTextTertiary,
    weeklyBadge = WeeklyBadge,
    onWeeklyBadge = DarkOnBackground,
    skeleton = Skeleton,
    skeletonHighlight = SkeletonHighlight
)
