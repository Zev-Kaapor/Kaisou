package com.kaisou.app.core.browser

// Android framework
import android.app.Activity
import android.content.ActivityNotFoundException
import android.content.Context
import android.content.ContextWrapper
import android.content.Intent
import androidx.core.net.toUri
// Browser inside the app
import androidx.browser.customtabs.CustomTabColorSchemeParams
import androidx.browser.customtabs.CustomTabsIntent
// UI styling and utilities
import androidx.compose.ui.graphics.toArgb
// Project resources
import com.kaisou.app.core.designsystem.theme.Background

// Opens an address in a custom tab over the app, or in the browser, or nothing happens when nothing can open it
object InAppBrowser {
    private val Scheme = Regex("^[a-zA-Z][a-zA-Z0-9+.-]*:")

    // Opens an address of the web
    fun open(context: Context, address: String) {
        val trimmed = address.trim()
        if (trimmed.isEmpty()) return
        val uri = (if (Scheme.containsMatchIn(trimmed)) trimmed else "https://$trimmed").toUri()
        val colors = CustomTabColorSchemeParams.Builder().setToolbarColor(Background.toArgb()).build()
        val tab = CustomTabsIntent.Builder()
            .setColorScheme(CustomTabsIntent.COLOR_SCHEME_DARK)
            .setDefaultColorSchemeParams(colors)
            .build()

        // Out of an activity, which a window over the screen can be, a tab has to start its own task
        if (activityOf(context) == null) tab.intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        try {
            tab.launchUrl(context, uri)
        } catch (_: ActivityNotFoundException) {
            try {
                context.startActivity(Intent(Intent.ACTION_VIEW, uri).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
            } catch (_: ActivityNotFoundException) {
                // Nothing on the phone opens it
            }
        }
    }

    // Finds the activity a context lives in, going through the wrappers
    private tailrec fun activityOf(context: Context): Activity? = when (context) {
        is Activity -> context
        is ContextWrapper -> activityOf(context.baseContext)
        else -> null
    }
}
