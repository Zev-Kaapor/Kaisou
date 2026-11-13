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

private fun fillIcon(name: String, path: String): ImageVector =
    ImageVector.Builder(
        name = name,
        defaultWidth = IconGrid.dp,
        defaultHeight = IconGrid.dp,
        viewportWidth = IconGrid,
        viewportHeight = IconGrid
    ).apply {
        addPath(
            pathData = PathParser().parsePathString(path).toNodes(),
            fill = SolidColor(Color.Black)
        )
    }.build()

// Icons of the app, simple lines on a 24 grid
object KaisouIcons {
    val Search = strokeIcon(
        "Search",
        "M3 11a8 8 0 1 0 16 0a8 8 0 1 0-16 0",
        "m21 21-4.3-4.3"
    )
    val Bell = strokeIcon(
        "Bell",
        "M6 8a6 6 0 0 1 12 0c0 7 3 9 3 9H3s3-2 3-9",
        "M10.3 21a1.94 1.94 0 0 0 3.4 0"
    )
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
    val Library = strokeIcon(
        "Library",
        "m16 6 4 14",
        "M12 6v14",
        "M8 8v12",
        "M4 4v16"
    )
    val Plus = strokeIcon("Plus", "M5 12h14", "M12 5v14")
    val LogIn = strokeIcon(
        "LogIn",
        "M15 3h4a2 2 0 0 1 2 2v14a2 2 0 0 1-2 2h-4",
        "m10 17 5-5-5-5",
        "M15 12H3"
    )
    val Person = strokeIcon(
        "Person",
        "M19 21v-2a4 4 0 0 0-4-4H9a4 4 0 0 0-4 4v2",
        "M12 3a4 4 0 1 0 0 8 4 4 0 0 0 0-8z"
    )
    val Bookmark = strokeIcon("Bookmark", "m19 21-7-4-7 4V5a2 2 0 0 1 2-2h10a2 2 0 0 1 2 2v16z")
    val PlayOutline = strokeIcon("PlayOutline", "M6 3l14 9-14 9V3z")
    val MoreVertical = fillIcon(
        "MoreVertical",
        "M12 7a1.5 1.5 0 1 0 0-3 1.5 1.5 0 0 0 0 3z M12 13.5a1.5 1.5 0 1 0 0-3 1.5 1.5 0 0 0 0 3z M12 20a1.5 1.5 0 1 0 0-3 1.5 1.5 0 0 0 0 3z"
    )
}
