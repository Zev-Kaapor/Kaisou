package com.kaisou.app.core.designsystem.theme

// Durations of the animations of the app, in milliseconds
object KaisouMotion {
    // Navigation icon growing when selected, and how small it is when it is not
    const val IconScaleMillis = 150
    const val UnselectedIconScale = 0.875f

    // Menu of the account rising from the bottom and going back
    const val SheetMillis = 300

    // Fade when switching between the main tabs
    const val TabSwitchMillis = 200

    // How far a screen slides when it comes in or goes out, as a fraction of its width
    const val PageSlideDivisor = 8

    // A block of text opening to show all of it and closing back
    const val ExpandMillis = 320

    // Loading placeholders, one light sweep and the wait before the next one
    const val SkeletonSweepMillis = 900
    const val SkeletonPauseMillis = 300

    // The light that passes over what the page wants seen, how long it takes and how long it waits for the next one
    const val ShineSweepMillis = 1100
    const val ShinePauseMillis = 4500
}
