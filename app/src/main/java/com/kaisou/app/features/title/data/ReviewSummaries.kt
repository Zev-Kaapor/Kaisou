package com.kaisou.app.features.title.data

// Project resources
import com.kaisou.app.core.language.AppLanguage

// Puts the summaries of the reviews, the line under the name that tells what the review says, in the language of the app, all in one go. A summary
// that does not come back stays as it was written
object ReviewSummaries {
    suspend fun translated(reviews: List<TitleReview>): List<TitleReview> {
        val summaries = MarkdownTranslator.translateLines(reviews.map { it.summary }, AppLanguage.translationCode)
        return reviews.mapIndexed { index, review -> review.copy(summary = summaries[index]) }
    }
}
