package com.kaisou.app.features.user.data

// What narrows a list: the facts, the genres and tags to have or to leave out, and whether the title is on the list of the one looking
data class ListFilter(
    val source: String? = null,
    val format: String? = null,
    val airing: String? = null,
    val season: String? = null,
    val year: Int? = null,
    val country: String? = null,
    val onMine: Boolean? = null,
    val included: Set<String> = emptySet(),
    val excluded: Set<String> = emptySet(),
    val includedTags: Set<String> = emptySet(),
    val excludedTags: Set<String> = emptySet()
) {
    // Whether anything narrows the list
    val active: Boolean
        get() = this != ListFilter()

    // Whether a title stays: it has the facts, the genres and tags to have, none to leave out, and the place on the list of the viewer
    fun matches(entry: UserListEntry, mine: Set<Int>?): Boolean =
        (source == null || entry.source == source) &&
            (format == null || entry.formatKey == format) &&
            (airing == null || entry.airing == airing) &&
            (season == null || entry.season == season) &&
            (year == null || entry.year == year) &&
            (country == null || entry.country == country) &&
            (onMine == null || mine == null || (entry.mediaId in mine) == onMine) &&
            entry.genres.containsAll(included) && entry.genres.none { it in excluded } &&
            entry.tags.containsAll(includedTags) && entry.tags.none { it in excludedTags }
}
