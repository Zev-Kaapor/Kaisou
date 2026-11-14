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
    val Person = strokeIcon(
        "Person",
        "M19 21v-2a4 4 0 0 0-4-4H9a4 4 0 0 0-4 4v2",
        "M12 3a4 4 0 1 0 0 8 4 4 0 0 0 0-8z"
    )
    val PlayOutline = strokeIcon("PlayOutline", "M6 3l14 9-14 9V3z")
    val Bookmark = strokeIcon("Bookmark", "m19 21-7-4-7 4V5a2 2 0 0 1 2-2h10a2 2 0 0 1 2 2v16z")
    val Image = strokeIcon(
        "Image",
        "M5 3h14a2 2 0 0 1 2 2v14a2 2 0 0 1-2 2H5a2 2 0 0 1-2-2V5a2 2 0 0 1 2-2z",
        "M11 9a2 2 0 1 1-4 0a2 2 0 1 1 4 0",
        "m21 15-3.086-3.086a2 2 0 0 0-2.828 0L6 21"
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
    val Close = strokeIcon("Close", "M18 6 6 18", "m6 6 12 12")
    val Back = strokeIcon("Back", "m12 19-7-7 7-7", "M19 12H5")
    val ChevronRight = strokeIcon("ChevronRight", "m9 18 6-6-6-6")
    val Check = strokeIcon("Check", "M20 6 9 17l-5-5")
    val Info = strokeIcon(
        "Info",
        "M2 12a10 10 0 1 0 20 0a10 10 0 1 0-20 0",
        "M12 16v-4",
        "M12 8h.01"
    )
    val Settings = strokeIcon(
        "Settings",
        "M12.22 2h-.44a2 2 0 0 0-2 2v.18a2 2 0 0 1-1 1.73l-.43.25a2 2 0 0 1-2 0l-.15-.08a2 2 0 0 0-2.73.73l-.22.38a2 2 0 0 0 .73 2.73l.15.1a2 2 0 0 1 1 1.72v.51a2 2 0 0 1-1 1.74l-.15.09a2 2 0 0 0-.73 2.73l.22.38a2 2 0 0 0 2.73.73l.15-.08a2 2 0 0 1 2 0l.43.25a2 2 0 0 1 1 1.73V20a2 2 0 0 0 2 2h.44a2 2 0 0 0 2-2v-.18a2 2 0 0 1 1-1.73l.43-.25a2 2 0 0 1 2 0l.15.08a2 2 0 0 0 2.73-.73l.22-.39a2 2 0 0 0-.73-2.73l-.15-.08a2 2 0 0 1-1-1.74v-.5a2 2 0 0 1 1-1.74l.15-.09a2 2 0 0 0 .73-2.73l-.22-.38a2 2 0 0 0-2.73-.73l-.15.08a2 2 0 0 1-2 0l-.43-.25a2 2 0 0 1-1-1.73V4a2 2 0 0 0-2-2z",
        "M9 12a3 3 0 1 0 6 0a3 3 0 1 0-6 0"
    )
    val SignOut = strokeIcon(
        "SignOut",
        "M9 21H5a2 2 0 0 1-2-2V5a2 2 0 0 1 2-2h4",
        "m16 17 5-5-5-5",
        "M21 12H9"
    )
    val Translate = strokeIcon(
        "Translate",
        "m5 8 6 6",
        "m4 14 6-6 2-3",
        "M2 5h12",
        "M7 2h1",
        "m22 22-5-10-5 10",
        "M14 18h6"
    )
    val Library = strokeIcon(
        "Library",
        "m16 6 4 14",
        "M12 6v14",
        "M8 8v12",
        "M4 4v16"
    )
}
