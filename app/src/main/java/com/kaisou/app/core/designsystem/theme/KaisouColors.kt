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
    // Dark fade under text over images
    val mediaScrim: Color,
    // Filled accent controls, darker than the accent so the white text stays readable
    val accentFill: Color,
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
    // Brand gradient from blue to violet to pink, the colors of the logo
    val gradientStart: Color,
    val gradientMiddle: Color,
    val gradientEnd: Color,
    // Main buttons, violet fading to a darker violet
    val buttonStart: Color,
    val buttonEnd: Color
)

// Extra colors of the app
val KaisouPalette = KaisouColors(
    onMedia = OnFilled,
    mediaScrim = Color(0xCC000000),
    accentFill = Color(0xFF7C4DEB),
    onAccentFill = OnFilled,
    elevated = ContainerHigh,
    navBar = Color(0xFF09090D),
    navUnselected = Color(0xFF676D75),
    navSelected = Color(0xFFAB7DFF),
    skeleton = Color(0xFF2E2E2E),
    skeletonHighlight = Color(0xFF4A4A4A),
    glowStart = Color(0x388A5CF2),
    glowEnd = Color(0x384B8DFF),
    gradientStart = Color(0xFF4B8DFF),
    gradientMiddle = Accent,
    gradientEnd = Color(0xFFFA597F),
    buttonStart = Color(0xFF7C4DEB),
    buttonEnd = Color(0xFF6A3FD8)
)
