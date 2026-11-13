package com.kaisou.app.core.designsystem.theme

// Android components
import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
// Compose layouts and foundations
// Material 3 components
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
// Compose state and runtime
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.staticCompositionLocalOf
// UI styling and utilities
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.platform.LocalWindowInfo
import androidx.compose.ui.platform.LocalDensity
import androidx.core.view.WindowCompat

// Material colors of the app
private val AppColorScheme = darkColorScheme(
    primary = Accent,
    onPrimary = OnAccent,
    primaryContainer = AccentContainer,
    onPrimaryContainer = OnAccentContainer,
    secondary = OnSurfaceVariant,
    onSecondary = OnAccent,
    secondaryContainer = ContainerHighest,
    onSecondaryContainer = OnBackground,
    tertiary = Accent,
    onTertiary = OnAccent,
    tertiaryContainer = AccentContainer,
    onTertiaryContainer = OnAccentContainer,
    background = Background,
    onBackground = OnBackground,
    surface = Background,
    onSurface = OnBackground,
    surfaceVariant = Container,
    onSurfaceVariant = OnSurfaceVariant,
    surfaceTint = Accent,
    inverseSurface = OnBackground,
    inverseOnSurface = Background,
    error = Error,
    onError = OnFilled,
    errorContainer = ErrorContainer,
    onErrorContainer = OnErrorContainer,
    outline = Outline,
    outlineVariant = OutlineVariant,
    scrim = Scrim,
    surfaceBright = ContainerHighest,
    surfaceDim = ContainerLowest,
    surfaceContainer = Container,
    surfaceContainerHigh = ContainerHigh,
    surfaceContainerHighest = ContainerHighest,
    surfaceContainerLow = ContainerLow,
    surfaceContainerLowest = ContainerLowest
)

// Extra colors of the app
private val LocalKaisouColors = staticCompositionLocalOf { KaisouPalette }

// App theme, the app has only the dark one
@Composable
fun KaisouTheme(content: @Composable () -> Unit) {
    val density = LocalDensity.current
    val widthDp = with(density) { LocalWindowInfo.current.containerSize.width.toDp().value.toInt() }

    SystemBarsAppearance()

    CompositionLocalProvider(
        LocalDimens provides dimensFor(windowWidthFor(widthDp)),
        LocalKaisouColors provides KaisouPalette
    ) {
        MaterialTheme(
            colorScheme = AppColorScheme,
            typography = Typography,
            shapes = KaisouShapes,
            content = content
        )
    }
}

// Shortcuts to read the app tokens
object KaisouTheme {
    val colors: KaisouColors
        @Composable get() = LocalKaisouColors.current

    val dimens: Dimens
        @Composable get() = LocalDimens.current
}

// Icons of the system bars are light over the dark app, they show up when the bars come back with a swipe
@Composable
private fun SystemBarsAppearance() {
    val view = LocalView.current

    if (!view.isInEditMode) {
        SideEffect {
            val window = view.context.findActivity()?.window ?: return@SideEffect
            val controller = WindowCompat.getInsetsController(window, view)
            controller.isAppearanceLightStatusBars = false
            controller.isAppearanceLightNavigationBars = false
        }
    }
}

// Activity behind a context that may be wrapped
private tailrec fun Context.findActivity(): Activity? = when (this) {
    is Activity -> this
    is ContextWrapper -> baseContext.findActivity()
    else -> null
}
