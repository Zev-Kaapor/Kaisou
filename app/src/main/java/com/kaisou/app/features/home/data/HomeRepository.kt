package com.kaisou.app.features.home.data

// Android framework
import android.content.Context
// Project resources
import com.kaisou.app.core.anilist.AniCard
import com.kaisou.app.core.anilist.AniLike
import com.kaisou.app.core.anilist.AniListClient
import com.kaisou.app.core.anilist.AniRecommended
import com.kaisou.app.core.anilist.AniShelves
import com.kaisou.app.core.auth.AniListSession

// What is known of what goes under the hero, in the four parts it comes in, each one null while it is not known
data class HomeParts(
    val top: AniShelves? = null,
    val below: AniShelves? = null,
    val recommended: AniRecommended? = null,
    val sequels: List<AniCard>? = null
)

// Asks AniList for the Home and turns the answer into what the screens show
class HomeRepository(context: Context) {
    private val client = AniListClient()
    private val mapper = HomeMapper(context.resources)

    // The four parts under the hero: the top is what the page waits for, and the lower ones can be asked from what was kept
    suspend fun top(userId: Int): AniShelves? = client.topShelves(userId, AniListSession.token)

    // Asks for the lower part of the Home, a copy kept within the age given being used as it is
    suspend fun below(userId: Int, maxAgeMillis: Long = 0L): AniShelves? = client.belowShelves(userId, AniListSession.token, maxAgeMillis)

    // Asks for what is recommended, a copy kept within the age given being used as it is
    suspend fun recommended(userId: Int, maxAgeMillis: Long = 0L): AniRecommended? = client.recommended(userId, AniListSession.token, maxAgeMillis)

    // Asks for the missing sequels, a copy kept within the age given being used as it is
    suspend fun sequels(userId: Int, maxAgeMillis: Long = 0L): List<AniCard>? = client.missingSequels(userId, AniListSession.token, maxAgeMillis)

    // Gives what was kept of the lower parts
    suspend fun kept(userId: Int): HomeParts =
        HomeParts(below = client.keptBelow(userId), recommended = client.keptRecommended(userId), sequels = client.keptSequels(userId))

    // The stories and the shelves from the parts that are known, null while the top is not
    fun build(userId: Int, parts: HomeParts): HomeShelvesUi? {
        val top = parts.top ?: return null
        return mapper.shelves(top.merge(parts.below ?: AniShelves.Empty), userId, parts.recommended, parts.sequels)
    }

    // The like of an activity, given or taken back, and how it stands now, null when it did not go
    suspend fun toggleLike(activityId: Int): AniLike? = client.toggleLike(activityId, reply = false, token = AniListSession.token)

    // Follows an activity or stops following it, and whether it is followed now, null when it did not go
    suspend fun toggleSubscription(activityId: Int, subscribe: Boolean): Boolean? = client.toggleSubscription(activityId, subscribe, AniListSession.token)

    // The same for a comment
    suspend fun toggleReplyLike(replyId: Int): AniLike? = client.toggleLike(replyId, reply = true, token = AniListSession.token)

    // Deleting a comment of the user and changing its text, which give true and the comment as AniList kept it
    suspend fun deleteReply(replyId: Int): Boolean = client.deleteReply(replyId, AniListSession.token)

    // Edits a reply to an activity
    suspend fun editReply(activityId: Int, replyId: Int, text: String): ReplyUi? =
        client.editReply(activityId, replyId, text, AniListSession.token)?.let(mapper::reply)

    // Deletes an activity of the user, true when AniList did it
    suspend fun deleteActivity(activityId: Int): Boolean = client.deleteActivity(activityId, AniListSession.token)

    // Posts a status of the user, true when AniList took it
    suspend fun postStatus(text: String): Boolean = client.postStatus(text, AniListSession.token)

    // The comments of an activity from the oldest, and writing one, null when AniList did not answer
    suspend fun replies(activityId: Int): List<ReplyUi>? =
        client.replies(activityId, AniListSession.token)?.sortedBy { it.createdAt }?.map(mapper::reply)

    // Posts a reply to an activity
    suspend fun reply(activityId: Int, text: String): ReplyUi? = client.reply(activityId, text, AniListSession.token)?.let(mapper::reply)

    // The featured titles are drawn from the pool each time the app opens, null when AniList fails or has none
    suspend fun heroes(fresh: Boolean = false): List<HeroUi>? =
        client.heroPool(AniListSession.token, fresh)
            ?.takeIf { it.isNotEmpty() }
            ?.shuffled()
            ?.take(HeroCount)
            ?.map(mapper::hero)
}
