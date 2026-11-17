package com.kaisou.app.core.designsystem.theme

// Compose state and runtime
import androidx.compose.runtime.Immutable
// UI styling and utilities
import androidx.compose.ui.graphics.Color

// Colors that depend on the theme but are not part of the Material color scheme
@Immutable
data class KaisouColors(
    // Text and icons over images and gradients
    val onMedia: Color,
    // Black of the veil at the top of the hero
    // Black of the veil at the top of the hero
    val veil: Color,
    // Soft shadow of the texts over pictures, the one of the icons over them and the color of the numbers over any art
    val textShadow: Color,
    val iconShadow: Color,
    val numberHighlight: Color,
    // The stars of the places a title has among the best: gold, silver and bronze for the first three, then one for the first ten, one for
    // the first fifty and one for the first hundred
    val medalGold: Color,
    val medalSilver: Color,
    val medalBronze: Color,
    val rankTopTen: Color,
    val rankTopFifty: Color,
    val rankTopHundred: Color,
    // Tag with the day of the week of a weekly release and what is drawn over it
    val weeklyBadge: Color,
    val onWeeklyBadge: Color,
    // Text and icons over the filled accent controls
    val onAccentFill: Color,
    // Surface for bars and sheets that sit above the background
    val elevated: Color,
    // Background of the bottom navigation bar
    val navBar: Color,
    // Icon of an unselected navigation item, and icon and name of the selected one
    val navUnselected: Color,
    val navSelected: Color,
    // Loading placeholders and the light that sweeps over them
    val skeleton: Color,
    val skeletonHighlight: Color,
    // Soft glow at the top of full screens, blending from one side to the other
    val glowStart: Color,
    val glowEnd: Color,
    // Main buttons, violet fading to a darker violet
    val buttonStart: Color,
    val buttonEnd: Color,
    // Thin ring around the picture of the account, from a light violet to a dark one
    val avatarRingStart: Color,
    val avatarRingMiddle: Color,
    val avatarRingEnd: Color
)

// Extra colors of the app
val KaisouPalette = KaisouColors(
    onMedia = OnFilled,
    veil = Color(0xFF000000),
    textShadow = Color(0xFF000000),
    iconShadow = Color(0xFF000000),
    numberHighlight = Color(0xFFFFFFFF),
    medalGold = Color(0xFFFFC53D),
    medalSilver = Color(0xFFC9CED8),
    medalBronze = Color(0xFFD08A4E),
    rankTopTen = Color(0xFF34D399),
    rankTopFifty = Color(0xFF60A5FA),
    rankTopHundred = Color(0xFFF472B6),
    weeklyBadge = Color(0xFF1578C8),
    onWeeklyBadge = OnFilled,
    onAccentFill = OnFilled,
    elevated = ContainerHigh,
    navBar = Color(0xFF09090D),
    navUnselected = Color(0xFF676D75),
    navSelected = Color(0xFFAB7DFF),
    skeleton = Color(0xFF2E2E2E),
    skeletonHighlight = Color(0xFF4A4A4A),
    glowStart = Color(0x388A5CF2),
    glowEnd = Color(0x384B8DFF),
    buttonStart = Color(0xFF7C4DEB),
    buttonEnd = Color(0xFF6A3FD8),
    avatarRingStart = Color(0xFFAB7DFF),
    avatarRingMiddle = Color(0xFF8A5CF2),
    avatarRingEnd = Color(0xFF6A3FD8)
)
