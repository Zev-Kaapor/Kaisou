package com.kaisou.app.features.title.ui.components

// Android framework
import android.content.Context
// Kotlin coroutines
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
// Project resources
import com.kaisou.app.R
import com.kaisou.app.core.designsystem.components.OverlaySheet
import com.kaisou.app.core.designsystem.components.Toasts
import com.kaisou.app.features.title.data.ListChanges
import com.kaisou.app.features.title.data.ListDraft
import com.kaisou.app.features.title.data.TitleRepository

// Opens the editor of the list for a title from any card of the app and saves what is done there, so the pages ask for the list again
object ListEditing {
    // The work goes on after the sheet is closed, so what was changed is not lost
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main)

    // Asks for the title and what the account needs, then shows the editor over the screen that is open
    fun open(context: Context, id: Int) {
        val application = context.applicationContext
        scope.launch {
            val titles = TitleRepository(application)
            val title = titles.header(id)
            if (title == null) {
                Toasts.show(application.getString(R.string.title_list_failed), isError = true)
                return@launch
            }
            val options = titles.listOptions(title.manga)
            OverlaySheet.show {
                ListEditorSheet(
                    title = title,
                    options = options,
                    onSave = { draft -> save(application, id, draft) },
                    onRemove = { remove(application, title.entry?.id) },
                    onDismiss = OverlaySheet::dismiss
                )
            }
        }
    }

    // Puts the title on the list or changes its entry, the island says what happened
    private fun save(context: Context, id: Int, draft: ListDraft) {
        scope.launch {
            if (TitleRepository(context).saveEntry(id, draft) != null) {
                ListChanges.changed()
                Toasts.show(context.getString(R.string.title_list_saved))
            } else {
                Toasts.show(context.getString(R.string.title_list_failed), isError = true)
            }
        }
    }

    // Takes the title off the list
    private fun remove(context: Context, entryId: Int?) {
        if (entryId == null) return
        scope.launch {
            if (TitleRepository(context).removeEntry(entryId)) {
                ListChanges.changed()
                Toasts.show(context.getString(R.string.title_list_removed))
            } else {
                Toasts.show(context.getString(R.string.title_list_failed), isError = true)
            }
        }
    }
}
