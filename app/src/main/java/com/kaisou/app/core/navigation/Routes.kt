package com.kaisou.app.core.navigation

import java.net.URLEncoder

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

// The page of a person of AniList, by their name
const val UserNameArg = "name"
const val UserRoute = "user/{$UserNameArg}"

fun userRoute(name: String): String = "user/${URLEncoder.encode(name, Charsets.UTF_8.name()).replace("+", "%20")}"

// The whole list of one kind of favourites of a person, by their name and the name of the kind
const val UserFavouritesKindArg = "favouriteKind"
const val UserFavouritesRoute = "user/{$UserNameArg}/favourites/{$UserFavouritesKindArg}"

fun userFavouritesRoute(name: String, kind: String): String = "${userRoute(name)}/favourites/$kind"

// Everything a person did lately, by their name and their id on AniList
const val UserIdArg = "userId"
const val UserActivityRoute = "user/{$UserNameArg}/activity/{$UserIdArg}"

fun userActivityRoute(name: String, id: Int): String = "${userRoute(name)}/activity/$id"

// All the titles of a character, a person of the staff or a studio
const val ProfileTabArg = "tab"
const val ProfileMediaRoute = "profile/{$ProfileKindArg}/{$TitleIdArg}/media?$ProfileTabArg={$ProfileTabArg}"

fun profileMediaRoute(kind: String, id: Int, tab: String): String = "profile/$kind/$id/media?$ProfileTabArg=$tab"

// All the voices of a character
const val ProfileVoicesRoute = "profile/{$ProfileKindArg}/{$TitleIdArg}/voices"

fun profileVoicesRoute(kind: String, id: Int): String = "profile/$kind/$id/voices"

// All the characters or all the staff of a title
const val CastKindArg = "kind"
const val CastCharacters = "characters"
const val CastStaff = "staff"
const val CastRoute = "title/{$TitleIdArg}/cast/{$CastKindArg}"

fun castRoute(id: Int, staff: Boolean): String = "title/$id/cast/${if (staff) CastStaff else CastCharacters}"

// All the reviews of a title, and one review read whole, by its id on AniList
const val ReviewsRoute = "title/{$TitleIdArg}/reviews"
const val ReviewIdArg = "reviewId"
const val ReviewRoute = "review/{$ReviewIdArg}"

fun reviewsRoute(id: Int): String = "title/$id/reviews"

fun reviewRoute(id: Int): String = "review/$id"

// All the threads of the forum about a title, and one thread read whole, by its id on AniList
const val ThreadsRoute = "title/{$TitleIdArg}/threads"
const val ThreadIdArg = "threadId"
const val ThreadRoute = "thread/{$ThreadIdArg}"

fun threadsRoute(id: Int): String = "title/$id/threads"

fun threadRoute(id: Int): String = "thread/$id"

// All the covers of the volumes of a manga
const val CoversRoute = "title/{$TitleIdArg}/covers"

fun coversRoute(id: Int): String = "title/$id/covers"

// All the titles tied to a title, or the whole series in the order to take it, and whether it is a manga, which says what the order is called
const val CardsKindArg = "kind"
const val CardsRelated = "related"
const val CardsOrder = "order"
const val CardsMangaArg = "manga"
const val CardsRoute = "title/{$TitleIdArg}/cards/{$CardsKindArg}?$CardsMangaArg={$CardsMangaArg}"

fun cardsRoute(id: Int, order: Boolean, manga: Boolean): String = "title/$id/cards/${if (order) CardsOrder else CardsRelated}?$CardsMangaArg=$manga"

// All the news of a title: its id on AniList, the one on MyAnimeList (none is -1) and whether it is a manga, which say where they are asked
const val NewsMalArg = "mal"
const val NewsMangaArg = "manga"
const val NewsRoute = "title/{$TitleIdArg}/news?$NewsMalArg={$NewsMalArg}&$NewsMangaArg={$NewsMangaArg}"

fun newsRoute(id: Int, malId: Int?, manga: Boolean): String = "title/$id/news?$NewsMalArg=${malId ?: -1}&$NewsMangaArg=$manga"
