package com.kaisou.app.core.rating

// Chooses the rating to show: the country of the user, MyAnimeList, another country, then the adult mark of AniList
object RatingPicker {
    // Code of the country of Brazil
    private const val Brazil = "BR"

    // The countries looked at when the user own does not have one and MyAnimeList does not either, the best known scales first
    private val Elsewhere = listOf("US", "GB", "DE", "BR", "AU", "KR")

    // What the country of the user gives, null when it gives none or it is not one that is known
    fun fromRegion(region: String, certifications: Map<String, String>): ContentRating? {
        val label = certifications[region] ?: return null
        val age = Certifications.ageOf(label) ?: return null
        return ContentRating(AgeGroup.of(age), official = true, symbol = region == Brazil)
    }

    // The rating to show, null when nothing is known. The ones that did not come from the country of the user are approximations
    fun pick(region: String, certifications: Map<String, String>, mal: MalRating?, adult: Boolean): ContentRating? {
        fromRegion(region, certifications)?.let { return it }
        val brazilian = region == Brazil
        val group = mal?.group
            ?: Elsewhere.firstNotNullOfOrNull { country -> certifications[country]?.let(Certifications::ageOf) }?.let(AgeGroup::of)
            ?: AgeGroup.Eighteen.takeIf { adult }
            ?: return null
        return ContentRating(group, official = false, symbol = brazilian)
    }
}
