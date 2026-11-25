package com.kaisou.app.core.navigation

// Compose state and runtime
import androidx.compose.runtime.staticCompositionLocalOf
// Address text
import java.net.URLDecoder

// How a link of an AniList text opens inside the app: true for a title, character, staff, studio or person, false goes to the browser
val LocalAniLinks = staticCompositionLocalOf<(String) -> Boolean> { { false } }

// How the page of a person of AniList opens from only their name, as in a story, a review or a thread, given by the shell
val LocalOpenUser = staticCompositionLocalOf<(String) -> Unit> { {} }

private val AniLink = Regex("^https?://(?:www\\.)?anilist\\.co/(anime|manga|character|staff|studio)/(\\d+)", RegexOption.IGNORE_CASE)
private val UserLink = Regex("^https?://(?:www\\.)?anilist\\.co/user/([^/?#\\s]+)", RegexOption.IGNORE_CASE)

// The route of the page that an address of AniList opens, null when the address is not of one of them
fun aniLinkRoute(address: String): String? {
    UserLink.find(address.trim())?.let { return userRoute(URLDecoder.decode(it.groupValues[1], Charsets.UTF_8.name())) }
    val match = AniLink.find(address.trim()) ?: return null
    val id = match.groupValues[2].toIntOrNull() ?: return null
    return when (val kind = match.groupValues[1].lowercase()) {
        "anime", "manga" -> titleRoute(id)
        else -> profileRoute(kind, id)
    }
}
