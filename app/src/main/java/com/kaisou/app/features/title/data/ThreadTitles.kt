package com.kaisou.app.features.title.data

// Project resources
import com.kaisou.app.core.language.AppLanguage

// Puts the titles of the threads in the language of the app all in one go, keeping the ones that do not come back
object ThreadTitles {
    // Translates the titles of a list of threads
    suspend fun translated(threads: List<TitleThread>): List<TitleThread> {
        val titles = MarkdownTranslator.translateLines(threads.map { it.title }, AppLanguage.translationCode)
        return threads.mapIndexed { index, thread -> thread.copy(title = titles[index]) }
    }
}
