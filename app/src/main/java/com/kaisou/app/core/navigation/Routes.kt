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

// All the news of a title: its id on AniList, the one on MyAnimeList (none is -1) and whether it is a manga, which say where they are asked
const val NewsMalArg = "mal"
const val NewsMangaArg = "manga"
const val NewsRoute = "title/{$TitleIdArg}/news?$NewsMalArg={$NewsMalArg}&$NewsMangaArg={$NewsMangaArg}"

fun newsRoute(id: Int, malId: Int?, manga: Boolean): String = "title/$id/news?$NewsMalArg=${malId ?: -1}&$NewsMangaArg=$manga"
