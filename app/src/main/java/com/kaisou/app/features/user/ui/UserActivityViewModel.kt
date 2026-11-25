package com.kaisou.app.features.user.ui

// Android framework and lifecycle
import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.SavedStateHandle
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.lifecycle.viewModelScope
// Reactive flows
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
// Project resources
import com.kaisou.app.R
import com.kaisou.app.core.auth.AniListSession
import com.kaisou.app.core.designsystem.components.ScrollPosition
import com.kaisou.app.core.designsystem.components.Toasts
import com.kaisou.app.core.designsystem.icons.KaisouIcons
import com.kaisou.app.core.navigation.UserIdArg
import com.kaisou.app.features.home.data.HomeRepository
import com.kaisou.app.features.home.data.RepliesUi
import com.kaisou.app.features.home.data.ReplyUi
import com.kaisou.app.features.user.data.UserActivity
import com.kaisou.app.features.user.data.UserRepository

// What the page of all the activity shows: what came, whether there is more, whether a page is coming, and the open comments
data class UserActivityUi(
    val items: List<UserActivity> = emptyList(),
    val hasMore: Boolean = true,
    val loading: Boolean = false,
    val failed: Boolean = false,
    val replies: RepliesUi? = null
)

// Asks for what a person did a page at a time and does what the buttons of an activity ask: heart, bell, comments and delete
class UserActivityViewModel(application: Application, savedStateHandle: SavedStateHandle) : AndroidViewModel(application) {
    private val id: Int = checkNotNull(savedStateHandle[UserIdArg])
    private val repository = UserRepository(application)
    private val home = HomeRepository(application)
    private val mutableState = MutableStateFlow(UserActivityUi())
    val state: StateFlow<UserActivityUi> = mutableState.asStateFlow()
    private var page = 0

    // The hearts that are going to AniList, a comment counted as the negative of its id
    private val liking = mutableSetOf<Int>()

    // Where the page was left, which stays when the phone is turned
    val scroll = ScrollPosition()

    // Whether the activities are the user's own, which they can delete
    val isOwn: Boolean = AniListSession.viewer?.id == id

    init {
        loadMore()
    }

    // Asks for the next page when there is one and none is being asked
    fun loadMore() {
        val now = mutableState.value
        if (now.loading || !now.hasMore) return
        mutableState.value = now.copy(loading = true, failed = false)
        viewModelScope.launch {
            val next = repository.activityPage(id, page + 1)
            val before = mutableState.value
            mutableState.value = if (next == null) {
                before.copy(loading = false, failed = true)
            } else {
                page++
                before.copy(items = (before.items + next.items).distinctBy { it.id }, hasMore = next.hasMore, loading = false)
            }
        }
    }

    // The heart of an activity changes at once and goes back if AniList does not take it, ending with the count AniList says
    fun toggleLike(activityId: Int) {
        val before = mutableState.value.items.firstOrNull { it.id == activityId } ?: return
        if (!liking.add(activityId)) return
        edit(activityId) { it.copy(isLiked = !it.isLiked, likeCount = (it.likeCount + if (it.isLiked) -1 else 1).coerceAtLeast(0)) }
        viewModelScope.launch {
            val result = home.toggleLike(activityId)
            if (result == null) {
                edit(activityId) { before }
                say(R.string.like_failed, isError = true)
            } else {
                edit(activityId) { it.copy(isLiked = result.isLiked, likeCount = result.likeCount) }
                say(if (result.isLiked) R.string.activity_liked else R.string.like_removed, icon = if (result.isLiked) KaisouIcons.HeartFilled else KaisouIcons.Heart)
            }
            liking.remove(activityId)
        }
    }

    // The bell follows an activity or stops following it. The island says what happened
    fun toggleFollow(activityId: Int) {
        val before = mutableState.value.items.firstOrNull { it.id == activityId } ?: return
        edit(activityId) { it.copy(isSubscribed = !before.isSubscribed) }
        viewModelScope.launch {
            val application = getApplication<Application>()
            val now = home.toggleSubscription(activityId, !before.isSubscribed)
            if (now == null) {
                edit(activityId) { before }
                Toasts.show(application.getString(R.string.activity_follow_failed), isError = true)
            } else {
                edit(activityId) { it.copy(isSubscribed = now) }
                Toasts.show(
                    message = application.getString(if (now) R.string.activity_followed else R.string.activity_unfollowed),
                    icon = if (now) KaisouIcons.BellRinging else KaisouIcons.Bell
                )
            }
        }
    }

    // Deletes an activity of the user. It goes from the list at once, and if AniList does not delete it the list comes back
    fun deleteActivity(activityId: Int) {
        val before = mutableState.value.items
        mutableState.update { it.copy(items = it.items.filterNot { item -> item.id == activityId }) }
        viewModelScope.launch {
            if (home.deleteActivity(activityId)) {
                say(R.string.activity_deleted, icon = KaisouIcons.Trash)
            } else {
                mutableState.update { it.copy(items = before) }
                say(R.string.activity_delete_failed, isError = true)
            }
        }
    }

    // Opens the comments of an activity and asks for them
    fun openReplies(activityId: Int) {
        mutableState.update { it.copy(replies = RepliesUi(activityId)) }
        viewModelScope.launch {
            val items = home.replies(activityId)
            mutableState.update { state ->
                val open = state.replies
                if (open?.activityId != activityId) state else state.copy(replies = open.copy(items = items ?: open.items, loading = false, failed = items == null))
            }
        }
    }

    // Closes the replies of an activity
    fun closeReplies() {
        mutableState.update { it.copy(replies = null) }
    }

    // Writes a comment on the open activity or saves the new text of the one being edited, showing it when AniList takes it
    fun sendReply(text: String) {
        val open = mutableState.value.replies ?: return
        val trimmed = text.trim()
        if (trimmed.isEmpty() || open.sending) return
        mutableState.update { it.copy(replies = open.copy(sending = true, failed = false)) }
        viewModelScope.launch {
            val editing = open.editingId
            val reply = if (editing != null) home.editReply(open.activityId, editing, trimmed) else home.reply(open.activityId, trimmed)
            if (reply != null && editing == null) edit(open.activityId) { it.copy(replyCount = it.replyCount + 1) }
            if (reply == null) say(R.string.reply_failed, isError = true) else say(if (editing != null) R.string.reply_edited else R.string.reply_sent, icon = KaisouIcons.Comment)
            mutableState.update { state ->
                val current = state.replies
                when {
                    current?.activityId != open.activityId -> state
                    reply == null -> state.copy(replies = current.copy(sending = false, failed = true))
                    editing != null -> state.copy(replies = current.copy(items = current.items.map { if (it.id == editing) reply else it }, sending = false, failed = false, editingId = null))
                    else -> state.copy(replies = current.copy(items = current.items + reply, sending = false, failed = false))
                }
            }
        }
    }

    // The heart of a comment, as the one of an activity
    fun toggleReplyLike(replyId: Int) {
        val before = mutableState.value.replies?.items?.firstOrNull { it.id == replyId } ?: return
        if (!liking.add(-replyId)) return
        editReply(replyId) { it.copy(isLiked = !it.isLiked, likeCount = (it.likeCount + if (it.isLiked) -1 else 1).coerceAtLeast(0)) }
        viewModelScope.launch {
            val result = home.toggleReplyLike(replyId)
            if (result == null) {
                editReply(replyId) { before }
                say(R.string.like_failed, isError = true)
            } else {
                editReply(replyId) { it.copy(isLiked = result.isLiked, likeCount = result.likeCount) }
                say(if (result.isLiked) R.string.comment_liked else R.string.like_removed, icon = if (result.isLiked) KaisouIcons.HeartFilled else KaisouIcons.Heart)
            }
            liking.remove(-replyId)
        }
    }

    // Deletes a comment of the user. It goes from the list at once, and if AniList does not delete it the comments are asked for again
    fun deleteReply(replyId: Int) {
        val open = mutableState.value.replies ?: return
        mutableState.update { state -> state.copy(replies = open.copy(items = open.items.filterNot { it.id == replyId }, editingId = null)) }
        edit(open.activityId) { it.copy(replyCount = (it.replyCount - 1).coerceAtLeast(0)) }
        viewModelScope.launch {
            if (home.deleteReply(replyId)) say(R.string.reply_deleted, icon = KaisouIcons.Trash) else openReplies(open.activityId)
        }
    }

    // A comment of the user is put in the field to be changed, and the change can be given up
    fun startEditingReply(replyId: Int) {
        mutableState.update { state -> state.copy(replies = state.replies?.copy(editingId = replyId, failed = false)) }
    }

    // Stops editing a reply
    fun cancelEditingReply() {
        mutableState.update { state -> state.copy(replies = state.replies?.copy(editingId = null, failed = false)) }
    }

    // The island says what happened, in the words of the app
    private fun say(message: Int, icon: ImageVector? = null, isError: Boolean = false) {
        val text = getApplication<Application>().getString(message)
        Toasts.show(message = text, isError = isError, icon = icon)
    }

    // Changes one activity of the list
    private fun edit(activityId: Int, change: (UserActivity) -> UserActivity) {
        mutableState.update { state -> state.copy(items = state.items.map { if (it.id == activityId) change(it) else it }) }
    }

    // Changes one reply of the open activity
    private fun editReply(replyId: Int, change: (ReplyUi) -> ReplyUi) {
        mutableState.update { state ->
            val open = state.replies ?: return@update state
            state.copy(replies = open.copy(items = open.items.map { if (it.id == replyId) change(it) else it }))
        }
    }
}
