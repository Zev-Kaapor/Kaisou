package com.kaisou.app.core.rating

// Chooses the rating to show, from the nearest to the farthest: the one the country of the user gives, the one of MyAnimeList,
// the one another country gives and, last, the adult mark of AniList
object RatingPicker {
    private const val Brazil = "BR"

    // The countries looked at when the user own does not have one and MyAnimeList does not either, the best known scales first
    private val Elsewhere = listOf("US", "GB", "DE", "BR", "AU", "KR")

    // What the country of the user gives, null when it gives none or it is not one that is known
    fun fromRegion(region: String, certifications: Map<String, String>): ContentRating? {
        val label = certifications[region] ?: return null
        val age = Certifications.ageOf(label) ?: return null
        val brazilian = region == Brazil
        return ContentRating(AgeGroup.of(age), label.takeUnless { brazilian }, official = true, symbol = brazilian)
    }

    // The rating to show, null when nothing is known. The ones that did not come from the country of the user are approximations
    fun pick(region: String, certifications: Map<String, String>, mal: MalRating?, adult: Boolean): ContentRating? {
        fromRegion(region, certifications)?.let { return it }
        val brazilian = region == Brazil
        val group = mal?.group
            ?: Elsewhere.firstNotNullOfOrNull { country -> certifications[country]?.let(Certifications::ageOf) }?.let(AgeGroup::of)
            ?: AgeGroup.Eighteen.takeIf { adult }
            ?: return null
        return ContentRating(group, label = null, official = false, symbol = brazilian)
    }
}
