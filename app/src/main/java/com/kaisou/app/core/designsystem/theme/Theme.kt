package com.kaisou.app.core.designsystem.theme

// Material 3 components
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
// Compose state and runtime
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.staticCompositionLocalOf

private val DarkScheme = darkColorScheme(
    primary = Accent,
    onPrimary = OnAccent,
    background = DarkBackground,
    onBackground = DarkOnBackground,
    surface = DarkSurface,
    onSurface = DarkOnBackground,
    surfaceVariant = DarkSurfaceHigh,
    onSurfaceVariant = DarkOnSurfaceVariant,
    outline = DarkOutline
)

private val LocalKaisouColors = staticCompositionLocalOf { DarkKaisouColors }

// The app is dark only for now
@Composable
fun KaisouTheme(content: @Composable () -> Unit) {
    CompositionLocalProvider(
        LocalKaisouColors provides DarkKaisouColors,
        LocalDimens provides Dimens()
    ) {
        MaterialTheme(
            colorScheme = DarkScheme,
            typography = KaisouTypography,
            shapes = KaisouShapes,
            content = content
        )
    }
}

// Access to the tokens that are not part of MaterialTheme
object KaisouTheme {
    val colors: KaisouColors
        @Composable
        @ReadOnlyComposable
        get() = LocalKaisouColors.current

    val dimens: Dimens
        @Composable
        @ReadOnlyComposable
        get() = LocalDimens.current
}
