package com.kaisou.app.features.auth.ui

// Android framework
import android.app.Application
import android.content.Context
import android.net.Uri
import androidx.annotation.StringRes
import androidx.core.net.toUri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
// Coroutines and reactive flows
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
// Project resources
import com.kaisou.app.BuildConfig
import com.kaisou.app.R
import com.kaisou.app.core.anilist.AniListClient
import com.kaisou.app.core.browser.InAppBrowser
import com.kaisou.app.core.anilist.AniViewer
import com.kaisou.app.core.anilist.ViewerResult
import com.kaisou.app.core.auth.AniListSession
import com.kaisou.app.core.auth.PinToken
import com.kaisou.app.core.cache.AppStore

// Where the user is: the token is being checked, the user has to sign in, or the user is in
sealed interface AuthState {
    data object Checking : AuthState

    // Busy while the token that came is being checked, the error is what to tell when the last try did not work
    data class LoggedOut(val busy: Boolean = false, @StringRes val error: Int? = null) : AuthState

    data class LoggedIn(val viewer: AniViewer) : AuthState
}

// The login with AniList: the user authorizes in the browser, AniList sends the token back to the app through its own
// address, and it is kept to open the app already signed in the next time
class AuthViewModel(application: Application) : AndroidViewModel(application) {
    private val client = AniListClient()
    private val session = AniListSession.also { it.init(application) }
    private val mutableState = MutableStateFlow(initialState())
    val state: StateFlow<AuthState> = mutableState.asStateFlow()

    init {
        // A token from another time is checked again, the app opens at once when the user is known
        if (session.token != null) validate()
    }

    // Signs out: the token and the user are forgotten, and so is the pool of the featured titles, which came with the lists of the user
    fun signOut() {
        session.clear()
        AppStore.clear("pool")
        AppStore.clear("top1")
        AppStore.clear("below1")
        AppStore.clear("sequelids1")
        AppStore.clear("recommended1")
        AppStore.clear("sequels1")
        mutableState.value = AuthState.LoggedOut()
    }

    private fun initialState(): AuthState {
        if (session.token == null) return AuthState.LoggedOut()
        return session.viewer?.let { AuthState.LoggedIn(it) } ?: AuthState.Checking
    }

    // Opens the page of AniList where the user authorizes the app, in a tab of the browser. The page asks for the
    // sign in first when the user is not signed in there
    fun startLogin(context: Context) {
        if (BuildConfig.ANILIST_CLIENT_ID.isEmpty()) {
            mutableState.value = AuthState.LoggedOut(error = R.string.login_missing_client)
            return
        }
        openInBrowser(context, "$AuthorizeUrl?client_id=${BuildConfig.ANILIST_CLIENT_ID}&response_type=token".toUri())
    }

    // True when the sign in with a token can open the page of AniList that shows it, which needs the client made for that
    val tokenByBrowser: Boolean = BuildConfig.ANILIST_PIN_CLIENT_ID.isNotEmpty()

    // Opens the page of AniList that shows the token to copy, the user authorizes, copies it and comes back to paste it
    fun startTokenLogin(context: Context) {
        if (!tokenByBrowser) return
        openInBrowser(context, "$AuthorizeUrl?client_id=${BuildConfig.ANILIST_PIN_CLIENT_ID}&response_type=token".toUri())
    }

    // Tells if a text is a token made for the client of the page that shows it
    fun isPinToken(text: String): Boolean = PinToken.isFor(text, BuildConfig.ANILIST_PIN_CLIENT_ID)

    // Opens the page where an account is created, the user comes back and signs in
    fun startSignUp(context: Context) {
        openInBrowser(context, SignUpUrl.toUri())
    }

    private fun openInBrowser(context: Context, address: Uri) {
        InAppBrowser.open(context, address.toString())
    }

    // The user pasted a token of AniList: it is kept and checked as the one that comes from the browser
    fun signInWithToken(token: String) {
        val clean = token.trim()
        if (clean.isEmpty()) {
            mutableState.value = AuthState.LoggedOut(error = R.string.login_token_invalid)
            return
        }
        session.saveToken(clean, System.currentTimeMillis() + DefaultTokenSeconds * MillisPerSecond)
        mutableState.value = AuthState.LoggedOut(busy = true)
        validate(pasted = true)
    }

    // The browser brought the user back: the token comes after the # of the address
    fun handleRedirect(uri: Uri?) {
        if (uri == null || uri.scheme != RedirectScheme || uri.host != RedirectHost) return
        val values = (uri.fragment ?: uri.query).orEmpty()
            .split("&")
            .mapNotNull { part -> part.split("=", limit = 2).takeIf { it.size == 2 }?.let { it[0] to it[1] } }
            .toMap()
        val token = values["access_token"]
        if (token.isNullOrEmpty()) {
            mutableState.value = AuthState.LoggedOut(error = R.string.login_failed)
            return
        }
        val seconds = values["expires_in"]?.toLongOrNull() ?: DefaultTokenSeconds
        session.saveToken(token, System.currentTimeMillis() + seconds * MillisPerSecond)
        mutableState.value = AuthState.LoggedOut(busy = true)
        validate()
    }

    // Asks AniList who the token belongs to, a token that was pasted and is refused is a different thing to tell than a
    // session that ran out
    private fun validate(pasted: Boolean = false) {
        val token = session.token ?: return
        viewModelScope.launch {
            when (val result = client.viewer(token)) {
                is ViewerResult.Found -> {
                    session.saveViewer(result.viewer)
                    mutableState.value = AuthState.LoggedIn(result.viewer)
                }
                ViewerResult.Rejected -> {
                    session.clear()
                    mutableState.value = AuthState.LoggedOut(
                        error = if (pasted) R.string.login_token_invalid else R.string.login_expired
                    )
                }
                // Without an answer the user that is known keeps the app, otherwise there is nobody to show
                ViewerResult.Unreachable -> if (mutableState.value !is AuthState.LoggedIn) {
                    mutableState.value = AuthState.LoggedOut(error = R.string.login_network)
                }
            }
        }
    }

    private companion object {
        const val AuthorizeUrl = "https://anilist.co/api/v2/oauth/authorize"
        const val SignUpUrl = "https://anilist.co/signup"
        // The redirect URL of the API client registered on AniList
        const val RedirectScheme = "kaisou"
        const val RedirectHost = "anilist"
        const val DefaultTokenSeconds = 31_536_000L
        const val MillisPerSecond = 1000L
    }
}
