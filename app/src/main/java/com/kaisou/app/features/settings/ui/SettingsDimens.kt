package com.kaisou.app.features.settings.ui

// UI styling and utilities
import androidx.compose.ui.unit.dp

// Sizes of the settings, the groups in cards with rows of an icon, two lines of words and an arrow
object SettingsDimens {
    // The room at the sides and between the groups
    val Side = 16.dp
    val GroupGap = 20.dp
    val ListTop = 8.dp

    // The name of a group over its card
    val GroupTitleStart = 8.dp
    val GroupTitleBottom = 8.dp

    // The card of a group and its corners
    val CardCorner = 20.dp

    // A row of a card, its height, the room at its sides, its icon, the room between the icon and the words and the arrow at its end
    val RowHeight = 68.dp
    val RowVertical = 12.dp
    val RowPadding = 16.dp
    val RowIcon = 24.dp
    val RowIconGap = 16.dp
    val RowArrow = 20.dp

    // The room between the lines of what changed and between the buttons of the updates, and the thin ring of the spinner
    val NotesGap = 8.dp
    val NotesBetween = 16.dp
    val ButtonGap = 12.dp
    val ProgressStroke = 2.dp
    val NotesDetailTop = 4.dp

    // The picture of each project on the screen of the thanks, and how many lines of words a project may take
    val ThanksIcon = 40.dp
    const val ThanksLines = 4

    // The photo of the author on the page about the app
    val AuthorPhoto = 56.dp

    // The line between two rows of a card, which starts where the words start
    val DividerStart = RowPadding + RowIcon + RowIconGap

    // The sheet of choices: the room at its sides and at its bottom, around its title, the corners of its cards, the room inside a row,
    // the gap before the note of the default one and the check at the end, and the share of the width the title takes lying down
    val ChoiceSide = 16.dp
    val ChoiceBottom = 24.dp
    val ChoiceTitleHorizontal = 8.dp
    val ChoiceTitleVertical = 12.dp
    val ChoiceCorner = 20.dp
    val ChoiceRowHorizontal = 16.dp
    val ChoiceRowVertical = 16.dp
    val ChoiceTagGap = 8.dp
    val ChoiceCheck = 24.dp
    const val ChoiceLandscapeTitleWeight = 0.35f
}
