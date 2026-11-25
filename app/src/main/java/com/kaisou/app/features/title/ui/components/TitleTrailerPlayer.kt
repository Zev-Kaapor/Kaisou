package com.kaisou.app.features.title.ui.components

// Android framework
import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import android.content.pm.ActivityInfo
import android.view.View
import android.view.ViewGroup
// Compose layouts and foundations
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
// Compose state and runtime
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
// UI styling and utilities
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
// The player of YouTube
import com.pierfrancescosoffritti.androidyoutubeplayer.core.player.PlayerConstants
import com.pierfrancescosoffritti.androidyoutubeplayer.core.player.YouTubePlayer
import com.pierfrancescosoffritti.androidyoutubeplayer.core.player.listeners.AbstractYouTubePlayerListener
import com.pierfrancescosoffritti.androidyoutubeplayer.core.player.listeners.FullscreenListener
import com.pierfrancescosoffritti.androidyoutubeplayer.core.player.options.IFramePlayerOptions
import com.pierfrancescosoffritti.androidyoutubeplayer.core.player.views.YouTubePlayerView

// The YouTube player in the page: starts when ready, stops with the app, goes full screen, and tells the page when it cannot play
@Composable
internal fun TrailerPlayer(videoId: String, onFailed: () -> Unit, modifier: Modifier = Modifier) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val currentFailed by rememberUpdatedState(onFailed)

    // The view of the video while it is on the full screen, with the way out of it
    var fullscreen by remember { mutableStateOf<Pair<View, () -> Unit>?>(null) }
    val player = remember { YouTubePlayerView(context).apply { enableAutomaticInitialization = false } }

    DisposableEffect(player) {
        lifecycleOwner.lifecycle.addObserver(player)
        player.addFullscreenListener(object : FullscreenListener {
            // Shows the video full screen
            override fun onEnterFullscreen(fullscreenView: View, exitFullscreen: () -> Unit) {
                fullscreen = fullscreenView to exitFullscreen
            }

            // Leaves the full screen
            override fun onExitFullscreen() {
                fullscreen = null
            }
        })
        val options = IFramePlayerOptions.Builder(context).controls(1).fullscreen(1).build()
        player.initialize(
            object : AbstractYouTubePlayerListener() {
                // Starts the video when the player is ready
                override fun onReady(youTubePlayer: YouTubePlayer) {
                    youTubePlayer.loadVideo(videoId, 0f)
                }

                // Tells the page when the player fails
                override fun onError(youTubePlayer: YouTubePlayer, error: PlayerConstants.PlayerError) {
                    currentFailed()
                }
            },
            options
        )
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(player)
            player.release()
        }
    }

    AndroidView(factory = { player }, modifier = modifier.fillMaxSize())

    fullscreen?.let { (view, exit) ->
        // The full screen lies down while it is open, and the page goes back to how it was when it closes
        DisposableEffect(Unit) {
            val activity = context.findActivity()
            val before = activity?.requestedOrientation
            activity?.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_SENSOR_LANDSCAPE
            onDispose { before?.let { activity.requestedOrientation = it } }
        }
        Dialog(onDismissRequest = exit, properties = DialogProperties(usePlatformDefaultWidth = false)) {
            Box(modifier = Modifier.fillMaxSize().background(Color.Black)) {
                AndroidView(
                    factory = {
                        (view.parent as? ViewGroup)?.removeView(view)
                        view
                    },
                    modifier = Modifier.fillMaxSize()
                )
            }
        }
    }
}

// Finds the activity a context lives in
private tailrec fun Context.findActivity(): Activity? =
    when (this) {
        is Activity -> this
        is ContextWrapper -> baseContext.findActivity()
        else -> null
    }
