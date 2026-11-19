package com.kaisou.app.core.navigation

// The screens of the settings, which open over the tabs: the list of sections, and one route for each section by its name
const val SettingsRoute = "settings"
const val SettingsSectionArg = "section"
const val SettingsSectionRoute = "settings/{$SettingsSectionArg}"

fun settingsSectionRoute(section: String): String = "settings/$section"

// The page of a title, anime or manga, by its id on AniList
const val TitleIdArg = "id"
const val TitleRoute = "title/{$TitleIdArg}"

fun titleRoute(id: Int): String = "title/$id"

// The page of a character, a person of the staff or a studio, by its kind and its id on AniList
const val ProfileKindArg = "kind"
const val ProfileRoute = "profile/{$ProfileKindArg}/{$TitleIdArg}"

fun profileRoute(kind: String, id: Int): String = "profile/$kind/$id"

// All the titles of a character, a person of the staff or a studio
const val ProfileTabArg = "tab"
const val ProfileMediaRoute = "profile/{$ProfileKindArg}/{$TitleIdArg}/media?$ProfileTabArg={$ProfileTabArg}"

fun profileMediaRoute(kind: String, id: Int, tab: String): String = "profile/$kind/$id/media?$ProfileTabArg=$tab"

// All the characters or all the staff of a title
const val CastKindArg = "kind"
const val CastCharacters = "characters"
const val CastStaff = "staff"
const val CastRoute = "title/{$TitleIdArg}/cast/{$CastKindArg}"

fun castRoute(id: Int, staff: Boolean): String = "title/$id/cast/${if (staff) CastStaff else CastCharacters}"

// All the news of a title: its id on AniList, the one on MyAnimeList (none is -1) and whether it is a manga, which say where they are asked
const val NewsMalArg = "mal"
const val NewsMangaArg = "manga"
const val NewsRoute = "title/{$TitleIdArg}/news?$NewsMalArg={$NewsMalArg}&$NewsMangaArg={$NewsMangaArg}"

fun newsRoute(id: Int, malId: Int?, manga: Boolean): String = "title/$id/news?$NewsMalArg=${malId ?: -1}&$NewsMangaArg=$manga"
