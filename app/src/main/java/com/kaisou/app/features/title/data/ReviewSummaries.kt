package com.kaisou.app.features.title.data

// Project resources
import com.kaisou.app.core.language.AppLanguage

// Puts the summaries of the reviews in the language of the app all in one go, keeping the ones that do not come back
object ReviewSummaries {
    // Translates the summaries of a list of reviews
    suspend fun translated(reviews: List<TitleReview>): List<TitleReview> {
        val summaries = MarkdownTranslator.translateLines(reviews.map { it.summary }, AppLanguage.translationCode)
        return reviews.mapIndexed { index, review -> review.copy(summary = summaries[index]) }
    }
}
