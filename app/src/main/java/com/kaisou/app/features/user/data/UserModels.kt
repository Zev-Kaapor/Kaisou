package com.kaisou.app.features.user.data

// Android resources
import androidx.annotation.StringRes
// Java time
import java.time.LocalDate
// Project resources
import com.kaisou.app.R
import com.kaisou.app.features.home.data.CardUi
import com.kaisou.app.features.profile.data.ProfileRef
import com.kaisou.app.features.title.data.TitlePerson

// What a user has done with anime or manga: how many titles, how much of them (days watched, or chapters read) and the mean of the scores
// they gave, from 0 to 100
data class UserTotals(val count: Int, val amount: Double, val meanScore: Double)

// How much a user did on a day, and the level AniList gives it from 1 up, which says how strong the day is painted
data class ActivityDay(val date: LocalDate, val amount: Int, val level: Int)

// A genre and how many titles of it the user has
data class UserGenre(val name: String, val count: Int)

// A studio the user has as a favourite, which opens its page
data class UserStudio(val name: String, val ref: ProfileRef)

// Something the person did lately: a title moved on their list, with the line that says what, its card and when, in seconds, or a text they
// wrote, with no card, how many liked it and commented on it, and whether the user liked it and follows it
data class UserActivity(
    val id: Int,
    val text: String,
    val card: CardUi?,
    val createdAt: Long,
    val likeCount: Int = 0,
    val replyCount: Int = 0,
    val isLiked: Boolean = false,
    val isSubscribed: Boolean = false
)

// What comes after the page of a person: the last things they did and how many follow them and how many they follow
class UserTail(val recent: List<UserActivity>, val followers: Int, val following: Int)

// A page of what the person did, and whether there are more
class UserActivityPage(val items: List<UserActivity>, val hasMore: Boolean)

// The kinds of favourites a person has, with the name of the kind in the address and in the question to AniList, and the title of its page
enum class FavouriteKind(val path: String, @StringRes val title: Int) {
    Anime("anime", R.string.user_fav_anime),
    Manga("manga", R.string.user_fav_manga),
    Characters("characters", R.string.user_fav_characters),
    Staff("staff", R.string.user_fav_staff),
    Studios("studios", R.string.user_fav_studios);

    companion object {
        fun of(path: String?): FavouriteKind = entries.firstOrNull { it.path == path } ?: Anime
    }
}

// A page of the favourites of one kind: the titles, the people or the studios, whichever the kind is, and whether there are more
class FavouritesPage(val cards: List<CardUi>, val people: List<TitlePerson>, val studios: List<UserStudio>, val hasMore: Boolean)

// What the user has as favourites, each kind apart, and the kinds that have more than came, which have the arrow to the whole list
data class UserFavourites(
    val anime: List<CardUi>,
    val manga: List<CardUi>,
    val characters: List<TitlePerson>,
    val staff: List<TitlePerson>,
    val studios: List<UserStudio>,
    val more: Set<FavouriteKind> = emptySet()
)

// The page of a person of AniList: who they are, what they say of themselves, whether the user follows them, the totals, the days they
// were active, the genres they like and their favourites. The badge is what a supporter of AniList wrote for it, null for the others, and
// the year they joined AniList is null when it is not known
data class UserUi(
    val id: Int,
    val name: String,
    val joinedYear: Int?,
    // The names they had before, the newest first
    val previousNames: List<String>,
    val avatar: String?,
    val banner: String?,
    val about: String,
    val badge: String?,
    val isSelf: Boolean,
    val isFollowing: Boolean,
    val isFollower: Boolean,
    val siteUrl: String?,
    val anime: UserTotals,
    val manga: UserTotals,
    val activity: List<ActivityDay>,
    val animeGenres: List<UserGenre>,
    val mangaGenres: List<UserGenre>,
    val favourites: UserFavourites
)
