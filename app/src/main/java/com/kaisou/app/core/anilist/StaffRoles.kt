package com.kaisou.app.core.anilist

// The parts of the staff as AniList writes them, like Theme Song Performance (ED1) or Episode Director (ep 5): the part is put in the
// language of the app when it is one that is known and what comes in parentheses stays as it came
class StaffRoles(keys: Array<String>, names: Array<String>) {
    private val byKey = keys.map { it.lowercase() }.zip(names).toMap()

    fun translate(raw: String): String {
        val base = raw.substringBefore(" (").trim()
        val translated = byKey[base.lowercase()] ?: return raw
        return translated + raw.removePrefix(base)
    }
}
