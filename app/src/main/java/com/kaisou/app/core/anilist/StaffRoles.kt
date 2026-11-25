package com.kaisou.app.core.anilist

// The staff parts as AniList writes them, put in the language of the app, keeping what is in parentheses
class StaffRoles(keys: Array<String>, names: Array<String>) {
    private val byKey = keys.map { it.lowercase() }.zip(names).toMap()

    // Puts a part of the staff in the language of the app, unchanged when it is not known
    fun translate(raw: String): String {
        val base = raw.substringBefore(" (").trim()
        val translated = byKey[base.lowercase()] ?: return raw
        return translated + raw.removePrefix(base)
    }
}
