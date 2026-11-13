package com.kaisou.app.core.designsystem.icons

// UI styling and utilities
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.PathParser
import androidx.compose.ui.unit.dp

// Grid and stroke the icons are drawn on, the color is set by the tint
private const val IconGrid = 24f
private const val IconStroke = 2f

private fun strokeIcon(name: String, vararg paths: String): ImageVector =
    ImageVector.Builder(
        name = name,
        defaultWidth = IconGrid.dp,
        defaultHeight = IconGrid.dp,
        viewportWidth = IconGrid,
        viewportHeight = IconGrid
    ).apply {
        for (path in paths) {
            addPath(
                pathData = PathParser().parsePathString(path).toNodes(),
                stroke = SolidColor(Color.Black),
                strokeLineWidth = IconStroke,
                strokeLineCap = StrokeCap.Round,
                strokeLineJoin = StrokeJoin.Round
            )
        }
    }.build()

// Icons of the app, simple lines on a 24 grid
object KaisouIcons {
    val Home = strokeIcon(
        "Home",
        "m3 9 9-7 9 7v11a2 2 0 0 1-2 2H5a2 2 0 0 1-2-2z",
        "M9 22V12h6v10"
    )
    val Compass = strokeIcon(
        "Compass",
        "M2 12a10 10 0 1 0 20 0a10 10 0 1 0-20 0",
        "M16.24 7.76L14.12 14.12L7.76 16.24L9.88 9.88Z"
    )
    val Calendar = strokeIcon(
        "Calendar",
        "M5 4h14a2 2 0 0 1 2 2v14a2 2 0 0 1-2 2H5a2 2 0 0 1-2-2V6a2 2 0 0 1 2-2z",
        "M16 2v4",
        "M8 2v4",
        "M3 10h18"
    )
    val Close = strokeIcon("Close", "M18 6 6 18", "m6 6 12 12")
    val Library = strokeIcon(
        "Library",
        "m16 6 4 14",
        "M12 6v14",
        "M8 8v12",
        "M4 4v16"
    )
}
