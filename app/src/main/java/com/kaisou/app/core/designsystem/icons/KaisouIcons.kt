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

// A solid shape, for the few icons that are filled
private fun fillIcon(name: String, path: String, grid: Float = IconGrid): ImageVector =
    ImageVector.Builder(
        name = name,
        defaultWidth = IconGrid.dp,
        defaultHeight = IconGrid.dp,
        viewportWidth = grid,
        viewportHeight = grid
    ).apply {
        addPath(pathData = PathParser().parsePathString(path).toNodes(), fill = SolidColor(Color.Black))
    }.build()

// The bell that rings: a filled body with its clapper, and two waves of line on each side
private const val BellRingingBodyPath =
    "M12,4.4 C10.9,4.4 10,5.2 9.9,6.2 C7.6,7 6.1,9.2 6.1,12 V15.5 L4.7,18.4 C4.5,18.8 4.8,19.2 5.2,19.2 H18.8 C19.2,19.2 19.5,18.8 19.3,18.4 L17.9,15.5 V12 C17.9,9.2 16.4,7 14.1,6.2 C14,5.2 13.1,4.4 12,4.4 Z" +
        "M9,20.6 H15 C14.5,21.7 13.4,22.3 12,22.3 C10.6,22.3 9.5,21.7 9,20.6 Z"
private const val BellRingingWavesPath =
    "M4,2.1 Q0.7,5.6 0.7,10.5 M6.3,4.3 Q3.5,6.7 3.5,10.5 M20,2.1 Q23.3,5.6 23.3,10.5 M17.7,4.3 Q20.5,6.7 20.5,10.5"
private const val BellWaveStroke = 1.5f

private fun ringingBellIcon(): ImageVector =
    ImageVector.Builder(
        name = "BellRinging",
        defaultWidth = IconGrid.dp,
        defaultHeight = IconGrid.dp,
        viewportWidth = IconGrid,
        viewportHeight = IconGrid
    ).apply {
        addPath(pathData = PathParser().parsePathString(BellRingingBodyPath).toNodes(), fill = SolidColor(Color.Black))
        addPath(
            pathData = PathParser().parsePathString(BellRingingWavesPath).toNodes(),
            stroke = SolidColor(Color.Black),
            strokeLineWidth = BellWaveStroke,
            strokeLineCap = StrokeCap.Round
        )
    }.build()

// Icons of the app, simple lines on a 24 grid
object KaisouIcons {
    // The bell when the releases of a title are being followed
    val BellRinging = ringingBellIcon()

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
    val PersonFilled = fillIcon(
        "PersonFilled",
        "M12 2a5 5 0 1 0 0 10 5 5 0 0 0 0-10z M4 21v-1a6 6 0 0 1 6-6h4a6 6 0 0 1 6 6v1z"
    )
    val PlayOutline = strokeIcon("PlayOutline", "M6 3l14 9-14 9V3z")
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
    val Plus = strokeIcon("Plus", "M5 12h14", "M12 5v14")
    // The three dots as Wavvy draws them, on a grid of 960
    val More = fillIcon(
        "More",
        "M480,800Q447,800 423.5,776.5Q400,753 400,720Q400,687 423.5,663.5Q447,640 480,640Q513,640 536.5,663.5Q560,687 560,720" +
            "Q560,753 536.5,776.5Q513,800 480,800ZM480,560Q447,560 423.5,536.5Q400,513 400,480Q400,447 423.5,423.5Q447,400 480,400" +
            "Q513,400 536.5,423.5Q560,447 560,480Q560,513 536.5,536.5Q513,560 480,560ZM480,320Q447,320 423.5,296.5Q400,273 400,240" +
            "Q400,207 423.5,183.5Q447,160 480,160Q513,160 536.5,183.5Q560,207 560,240Q560,273 536.5,296.5Q513,320 480,320Z",
        grid = 960f
    )
    val Share = strokeIcon(
        "Share",
        "M8.59 13.51l6.83 3.98",
        "M15.41 6.51l-6.82 3.98",
        "M21 5a3 3 0 1 1-6 0 3 3 0 0 1 6 0",
        "M9 12a3 3 0 1 1-6 0 3 3 0 0 1 6 0",
        "M21 19a3 3 0 1 1-6 0 3 3 0 0 1 6 0"
    )
    val ExternalLink = strokeIcon("ExternalLink", "M15 3h6v6", "M10 14 21 3", "M18 13v6a2 2 0 0 1-2 2H5a2 2 0 0 1-2-2V8a2 2 0 0 1 2-2h6")
    val Copy = strokeIcon(
        "Copy",
        "M8 8h12a2 2 0 0 1 2 2v10a2 2 0 0 1-2 2H8a2 2 0 0 1-2-2V10a2 2 0 0 1 2-2z",
        "M4 16c-1.1 0-2-.9-2-2V4c0-1.1.9-2 2-2h10c1.1 0 2 .9 2 2"
    )
    val Edit = strokeIcon(
        "Edit",
        "M21.174 6.812a1 1 0 0 0-3.986-3.987L3.842 16.174a2 2 0 0 0-.5.83l-1.321 4.352a.5.5 0 0 0 .623.622l4.353-1.32a2 2 0 0 0 .83-.497z",
        "m15 5 4 4"
    )
    val Trash = strokeIcon("Trash", "M3 6h18", "M19 6v14c0 1-1 2-2 2H7c-1 0-2-1-2-2V6", "M8 6V4c0-1 1-2 2-2h4c1 0 2 1 2 2v2", "M10 11v6", "M14 11v6")
    val Bold = strokeIcon("Bold", "M6 12h9a4 4 0 0 1 0 8H7a1 1 0 0 1-1-1V5a1 1 0 0 1 1-1h7a4 4 0 0 1 0 8")
    val Italic = strokeIcon("Italic", "M19 4h-9", "M14 20H5", "M15 4 9 20")
    val Strikethrough = strokeIcon("Strikethrough", "M16 4H9a3 3 0 0 0-2.83 4", "M14 12a4 4 0 0 1 0 8H6", "M4 12h16")
    val Spoiler = strokeIcon(
        "Spoiler",
        "M9.88 9.88a3 3 0 1 0 4.24 4.24",
        "M10.73 5.08A10.43 10.43 0 0 1 12 5c7 0 10 7 10 7a13.16 13.16 0 0 1-1.67 2.68",
        "M6.61 6.61A13.526 13.526 0 0 0 2 12s3 7 10 7a9.74 9.74 0 0 0 5.39-1.61",
        "m2 2 20 20"
    )
    val Link = strokeIcon(
        "Link",
        "M10 13a5 5 0 0 0 7.54.54l3-3a5 5 0 0 0-7.07-7.07l-1.72 1.71",
        "M14 11a5 5 0 0 0-7.54-.54l-3 3a5 5 0 0 0 7.07 7.07l1.71-1.71"
    )
    val Youtube = strokeIcon(
        "Youtube",
        "M2.5 17a24.12 24.12 0 0 1 0-10 2 2 0 0 1 1.4-1.4 49.56 49.56 0 0 1 16.2 0A2 2 0 0 1 21.5 7a24.12 24.12 0 0 1 0 10 2 2 0 0 1-1.4 1.4 49.55 49.55 0 0 1-16.2 0A2 2 0 0 1 2.5 17",
        "m10 15 5-3-5-3z"
    )
    val ListOrdered = strokeIcon("ListOrdered", "M10 12h11", "M10 18h11", "M10 6h11", "M4 10h2", "M4 6h1v4", "M6 18H4c0-1 2-2 2-3s-1-1.5-2-1")
    val ListBulleted = strokeIcon("ListBulleted", "M8 6h13", "M8 12h13", "M8 18h13", "M3 6h.01", "M3 12h.01", "M3 18h.01")
    val Heading = strokeIcon("Heading", "M6 12h12", "M6 20V4", "M18 20V4")
    val AlignCenter = strokeIcon("AlignCenter", "M17 12H7", "M19 18H5", "M21 6H3")
    val Quote = strokeIcon(
        "Quote",
        "M16 3a2 2 0 0 0-2 2v6a2 2 0 0 0 2 2 1 1 0 0 1 1 1v1a2 2 0 0 1-2 2 1 1 0 0 0-1 1v2a1 1 0 0 0 1 1 6 6 0 0 0 6-6V5a2 2 0 0 0-2-2z",
        "M5 3a2 2 0 0 0-2 2v6a2 2 0 0 0 2 2 1 1 0 0 1 1 1v1a2 2 0 0 1-2 2 1 1 0 0 0-1 1v2a1 1 0 0 0 1 1 6 6 0 0 0 6-6V5a2 2 0 0 0-2-2z"
    )
    val Code = strokeIcon("Code", "m16 18 6-6-6-6", "m8 6-6 6 6 6")
    val Video = strokeIcon(
        "Video",
        "m16 13 5.223 3.482a.5.5 0 0 0 .777-.416V7.87a.5.5 0 0 0-.752-.432L16 10.5",
        "M4 6h10a2 2 0 0 1 2 2v8a2 2 0 0 1-2 2H4a2 2 0 0 1-2-2V8a2 2 0 0 1 2-2z"
    )
    // The state of a title on the list of the user: not on it, being watched, being read, planned, done, done again, paused and dropped
    val ListPlus = strokeIcon("ListPlus", "M11 12H3", "M16 6H3", "M16 18H3", "M18 9v6", "M21 12h-6")
    val ListVideo = strokeIcon(
        "ListVideo",
        "M21 5H3", "M10 12H3", "M10 19H3",
        "M15 12.003a1 1 0 0 1 1.517-.859l4.997 2.997a1 1 0 0 1 0 1.718l-4.997 2.997a1 1 0 0 1-1.517-.86z"
    )
    val ListChecks = strokeIcon("ListChecks", "m3 17 2 2 4-4", "m3 7 2 2 4-4", "M13 6h8", "M13 12h8", "M13 18h8")
    val BookOpen = strokeIcon(
        "BookOpen",
        "M12 7v14",
        "M3 18a1 1 0 0 1-1-1V4a1 1 0 0 1 1-1h5a4 4 0 0 1 4 4 4 4 0 0 1 4-4h5a1 1 0 0 1 1 1v13a1 1 0 0 1-1 1h-6a3 3 0 0 0-3 3 3 3 0 0 0-3-3z"
    )
    val Clock = strokeIcon("Clock", "M12 22a10 10 0 1 0 0-20 10 10 0 0 0 0 20z", "M12 6v6l4 2")
    val Repeat = strokeIcon("Repeat", "m17 2 4 4-4 4", "M3 11v-1a4 4 0 0 1 4-4h14", "m7 22-4-4 4-4", "M21 13v1a4 4 0 0 1-4 4H3")
    val Frown = strokeIcon("Frown", "M12 22a10 10 0 1 0 0-20 10 10 0 0 0 0 20z", "M16 16s-1.5-2-4-2-4 2-4 2", "M9 9h.01", "M15 9h.01")
    val Meh = strokeIcon("Meh", "M12 22a10 10 0 1 0 0-20 10 10 0 0 0 0 20z", "M8 15h8", "M9 9h.01", "M15 9h.01")
    val Smile = strokeIcon("Smile", "M12 22a10 10 0 1 0 0-20 10 10 0 0 0 0 20z", "M8 14s1.5 2 4 2 4-2 4-2", "M9 9h.01", "M15 9h.01")
    val CirclePause = strokeIcon("CirclePause", "M12 22a10 10 0 1 0 0-20 10 10 0 0 0 0 20z", "M10 15V9", "M14 15V9")
    val CircleX = strokeIcon("CircleX", "M12 22a10 10 0 1 0 0-20 10 10 0 0 0 0 20z", "m15 9-6 6", "m9 9 6 6")
    val Heart = strokeIcon(
        "Heart",
        "M19 14c1.49-1.46 3-3.21 3-5.5A5.5 5.5 0 0 0 16.5 3c-1.76 0-3 .5-4.5 2-1.5-1.5-2.74-2-4.5-2A5.5 5.5 0 0 0 2 8.5c0 2.3 1.5 4.05 3 5.5l7 7Z"
    )
    val HeartFilled = fillIcon(
        "HeartFilled",
        "M19 14c1.49-1.46 3-3.21 3-5.5A5.5 5.5 0 0 0 16.5 3c-1.76 0-3 .5-4.5 2-1.5-1.5-2.74-2-4.5-2A5.5 5.5 0 0 0 2 8.5c0 2.3 1.5 4.05 3 5.5l7 7Z"
    )
    val Send = fillIcon("Send", "M2.01 21 23 12 2.01 3 2 10l15 2-15 2z")
    val Comment = strokeIcon("Comment", "M7.9 20A9 9 0 1 0 4 16.1L2 22Z")
    val Star = fillIcon(
        "Star",
        "M11.525 2.295a.53.53 0 0 1 .95 0l2.31 4.679a2.123 2.123 0 0 0 1.595 1.16l5.166.756a.53.53 0 0 1 .294.904l-3.736 3.638a2.123 2.123 0 0 0-.611 1.878l.882 5.14a.53.53 0 0 1-.771.56l-4.618-2.428a2.122 2.122 0 0 0-1.973 0L6.396 21.01a.53.53 0 0 1-.77-.56l.881-5.139a2.122 2.122 0 0 0-.611-1.879L2.16 9.795a.53.53 0 0 1 .294-.906l5.165-.755a2.122 2.122 0 0 0 1.597-1.16z"
    )
    val Back = strokeIcon("Back", "m12 19-7-7 7-7", "M19 12H5")
    val QuestionCircle = fillIcon(
        "QuestionCircle",
        "M504 256c0 136.997-111.043 248-248 248S8 392.997 8 256C8 119.083 119.043 8 256 8s248 111.083 248 248zM262.655 90c-54.497 0-89.255 22.957-116.549 63.758-3.536 5.286-2.353 12.415 2.715 16.258l34.699 26.31c5.205 3.947 12.621 3.008 16.665-2.122 17.864-22.658 30.113-35.797 57.303-35.797 20.429 0 45.698 13.148 45.698 32.958 0 14.976-12.363 22.667-32.534 33.976C247.128 238.528 216 254.941 216 296v4c0 6.627 5.373 12 12 12h56c6.627 0 12-5.373 12-12v-1.333c0-28.462 83.186-29.647 83.186-106.667 0-58.002-60.165-102-116.531-102zM256 338c-25.365 0-46 20.635-46 46 0 25.364 20.635 46 46 46s46-20.636 46-46c0-25.365-20.635-46-46-46z",
        grid = 512f
    )
    // The two ways to order the tags: by the letters, with an A over a Z and an arrow down, and by the percentage
    val SortAlphabetical = strokeIcon(
        "SortAlphabetical",
        "M3 10 5.5 4 8 10", "M4 8h3", "M3 14h5l-5 6h5", "M17 4v16", "m13.5 16.5 3.5 3.5 3.5-3.5"
    )
    val Percent = strokeIcon(
        "Percent",
        "M19 5 5 19", "M6.5 4a2.5 2.5 0 1 0 0 5 2.5 2.5 0 0 0 0-5z", "M17.5 15a2.5 2.5 0 1 0 0 5 2.5 2.5 0 0 0 0-5z"
    )
    val ChevronDown = strokeIcon("ChevronDown", "m6 9 6 6 6-6")
    val ArrowUpDown = strokeIcon("ArrowUpDown", "m21 16-4 4-4-4", "M17 20V4", "m3 8 4-4 4 4", "M7 4v16")
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
