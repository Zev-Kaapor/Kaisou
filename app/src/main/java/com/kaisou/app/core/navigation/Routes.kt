package com.kaisou.app.core.navigation

// The screens of the settings, which open over the tabs: the list of sections, and one route for each section by its name
const val SettingsRoute = "settings"
const val SettingsSectionArg = "section"
const val SettingsSectionRoute = "settings/{$SettingsSectionArg}"

fun settingsSectionRoute(section: String): String = "settings/$section"
