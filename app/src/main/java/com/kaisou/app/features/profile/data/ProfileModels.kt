package com.kaisou.app.features.profile.data

// Android resources
import androidx.annotation.StringRes
import com.kaisou.app.R
// Project resources
import com.kaisou.app.features.home.data.CardUi
import com.kaisou.app.features.title.data.TitleDatum
import com.kaisou.app.features.title.data.TitlePerson

// What a page of a person can be about: a character, a person of the staff or a studio. The path is how the address of the page names it
enum class ProfileKind(val path: String) {
    Character("character"), Staff("staff"), Studio("studio");

    companion object {
        fun of(path: String?): ProfileKind = entries.firstOrNull { it.path == path } ?: Character
    }
}

// How the titles of a person are sorted, the same ways AniList has: the most popular, the best rated, the most favourited, the newest, the
// oldest and by the name. The newest and the oldest are grouped by year
enum class MediaSort(val apiName: String, @StringRes val label: Int, val byYear: Boolean = false) {
    Popular("POPULARITY_DESC", R.string.profile_sort_popular),
    Score("SCORE_DESC", R.string.profile_sort_score),
    Favourites("FAVOURITES_DESC", R.string.profile_sort_favourites),
    Newest("START_DATE_DESC", R.string.profile_sort_newest, byYear = true),
    Oldest("START_DATE", R.string.profile_sort_oldest, byYear = true),
    Title("TITLE_ROMAJI", R.string.profile_sort_title)
}

// The kinds of titles of a person, as the tabs of their page: the characters a person voiced, and the anime and the manga they are in or
// worked on. The path is how the address of the page names it
enum class MediaTab(val path: String, val apiType: String?, @StringRes val label: Int) {
    Voices("voices", null, R.string.profile_tab_voices),
    Anime("anime", "ANIME", R.string.profile_type_anime),
    Manga("manga", "MANGA", R.string.profile_type_manga);

    companion object {
        fun of(path: String?): MediaTab = entries.firstOrNull { it.path == path } ?: Anime
    }
}

// An item of the page of all the titles of a person: a title, or a character a person voiced with the title it is in. The key is what
// tells it apart from the others and the year is the one the title started
class ProfileMediaItem(val key: String, val card: CardUi?, val person: TitlePerson?, val year: Int?)

// A page of the titles of a person and whether there are more
class ProfileMediaPage(val items: List<ProfileMediaItem>, val hasMore: Boolean)

// Which page of a person opens: its kind and its id on AniList
data class ProfileRef(val kind: ProfileKind, val id: Int)

// A row of the page that slides: the titles they are in or did, or the people that go with them. Its name and what it has
class ProfileSection(
    @StringRes val title: Int,
    val cards: List<CardUi> = emptyList(),
    val people: List<TitlePerson> = emptyList(),
    val tab: MediaTab? = null,
    // Whether AniList has more than came, which, with the page for all of them, is what the arrow is for
    val more: Boolean = false
)

// The page of a character, a person of the staff or a studio: who it is, its picture, what is known of it, what it says of itself, whether
// the user has it as a favourite, and the rows of titles and people
data class ProfileUi(
    val ref: ProfileRef,
    val name: String,
    val nativeName: String?,
    val image: String?,
    val favourites: Int,
    val facts: List<TitleDatum>,
    val description: String,
    val isFavourite: Boolean,
    val siteUrl: String?,
    val sections: List<ProfileSection>
)
