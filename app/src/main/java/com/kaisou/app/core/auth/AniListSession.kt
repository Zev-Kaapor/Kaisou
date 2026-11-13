package com.kaisou.app.core.auth

// Android framework
import android.content.Context
import android.content.SharedPreferences
import androidx.core.content.edit
// Project resources
import com.kaisou.app.core.anilist.AniViewer

// The login of the user: the token of AniList and who it belongs to, kept on the device only, the backup leaves this file out
object AniListSession {
    private const val FileName = "kaisou_session"
    private const val TokenKey = "token"
    private const val ExpiresKey = "expires_at"
    private const val IdKey = "user_id"
    private const val NameKey = "user_name"
    private const val AvatarKey = "user_avatar"
    private const val NoUser = -1

    private lateinit var preferences: SharedPreferences

    fun init(context: Context) {
        if (!::preferences.isInitialized) {
            preferences = context.applicationContext.getSharedPreferences(FileName, Context.MODE_PRIVATE)
        }
    }

    // The token while it is still valid, null when there is none or it has expired
    val token: String?
        get() {
            val saved = preferences.getString(TokenKey, null) ?: return null
            return saved.takeIf { System.currentTimeMillis() < preferences.getLong(ExpiresKey, 0L) }
        }

    // The user of the last time the token was checked, so the app can open before the answer comes
    val viewer: AniViewer?
        get() {
            val id = preferences.getInt(IdKey, NoUser)
            val name = preferences.getString(NameKey, null)
            if (id == NoUser || name == null) return null
            return AniViewer(id = id, name = name, avatar = preferences.getString(AvatarKey, null))
        }

    fun saveToken(token: String, expiresAtMillis: Long) {
        preferences.edit {
            putString(TokenKey, token)
            putLong(ExpiresKey, expiresAtMillis)
        }
    }

    fun saveViewer(viewer: AniViewer) {
        preferences.edit {
            putInt(IdKey, viewer.id)
            putString(NameKey, viewer.name)
            putString(AvatarKey, viewer.avatar)
        }
    }

    // Signs out: the token and the user are forgotten
    fun clear() {
        preferences.edit { clear() }
    }
}
