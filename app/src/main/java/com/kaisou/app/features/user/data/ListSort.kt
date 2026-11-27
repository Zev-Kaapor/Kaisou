package com.kaisou.app.features.user.data

// Android framework
import androidx.annotation.StringRes
// Project resources
import com.kaisou.app.R

// How the titles of a list are ordered, as AniList offers it: by what, and whether the first of them is the lowest or the highest
enum class ListSort(@StringRes val label: Int, private val ascending: Boolean = false, private val key: (UserListEntry) -> Long? = { null }) {
    Title(R.string.user_sort_title, ascending = true),
    Score(R.string.user_sort_score, key = { it.score.takeIf { value -> value > 0 }?.toLong() }),
    Progress(R.string.user_sort_progress, key = { it.progress.toLong() }),
    Updated(R.string.user_sort_updated, key = { it.updatedAt.takeIf { value -> value > 0 } }),
    Added(R.string.user_sort_added, key = { it.addedAt.takeIf { value -> value > 0 } }),
    Started(R.string.user_sort_started, key = { it.startedOn.takeIf { value -> value > 0 }?.toLong() }),
    Completed(R.string.user_sort_completed, key = { it.completedOn.takeIf { value -> value > 0 }?.toLong() }),
    Release(R.string.user_sort_release, key = { it.releaseOn.takeIf { value -> value > 0 }?.toLong() }),
    Weighted(R.string.user_sort_weighted, key = { it.average.takeIf { value -> value > 0 }?.toLong() }),
    Popularity(R.string.user_sort_popularity, key = { it.popularity.takeIf { value -> value > 0 }?.toLong() });

    // Whether it goes up from the lowest: the title does and the rest go down from the highest, until the person turns it round
    fun goesUp(reversed: Boolean): Boolean = ascending != reversed

    // Compares two titles: with nothing to compare goes last either way, and the ones tied come in the order of their names
    fun comparator(reversed: Boolean = false): Comparator<UserListEntry> {
        val up = goesUp(reversed)
        val byName = compareBy<UserListEntry, String>(String.CASE_INSENSITIVE_ORDER) { it.title }
        if (this == Title) return if (up) byName else byName.reversed()

        val byValue = Comparator<UserListEntry> { first, second ->
            val a = key(first)
            val b = key(second)
            when {
                a == null && b == null -> 0
                a == null -> 1
                b == null -> -1
                up -> a.compareTo(b)
                else -> b.compareTo(a)
            }
        }
        return byValue.thenComparing(byName)
    }

    // Finds a sort by its name
    companion object {
        // Gives the sort of a name, last changed when it is not known
        fun of(name: String?): ListSort = entries.firstOrNull { it.name == name } ?: Updated
    }
}
