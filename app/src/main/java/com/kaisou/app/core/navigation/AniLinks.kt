package com.kaisou.app.core.navigation

// Compose state and runtime
import androidx.compose.runtime.staticCompositionLocalOf

// How a link of the text of AniList is opened inside the app: it returns true when the address is of a title, a character, a person of the
// staff or a studio, which open their own pages, and false for any other, which goes to the browser. The shell of the app gives it, since it
// is the one that knows how to go to a page
val LocalAniLinks = staticCompositionLocalOf<(String) -> Boolean> { { false } }

private val AniLink = Regex("^https?://(?:www\\.)?anilist\\.co/(anime|manga|character|staff|studio)/(\\d+)", RegexOption.IGNORE_CASE)

// The route of the page that an address of AniList opens, null when the address is not of one of them
fun aniLinkRoute(address: String): String? {
    val match = AniLink.find(address.trim()) ?: return null
    val id = match.groupValues[2].toIntOrNull() ?: return null
    return when (val kind = match.groupValues[1].lowercase()) {
        "anime", "manga" -> titleRoute(id)
        else -> profileRoute(kind, id)
    }
}
