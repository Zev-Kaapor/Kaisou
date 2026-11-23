package com.kaisou.app.features.title.data

// Project resources
import com.kaisou.app.core.language.AppLanguage

// Puts the titles of the threads in the language of the app, all in one go, with the marks they have kept, as the spoilers. A title that does
// not come back stays as it was written
object ThreadTitles {
    suspend fun translated(threads: List<TitleThread>): List<TitleThread> {
        if (threads.isEmpty()) return threads
        val lines = MarkdownTranslator.translate(threads.joinToString("\n") { it.title.replace('\n', ' ') }, AppLanguage.translationCode).lines()
        if (lines.size != threads.size) return threads
        return threads.mapIndexed { index, thread -> thread.copy(title = lines[index].ifBlank { thread.title }) }
    }
}
