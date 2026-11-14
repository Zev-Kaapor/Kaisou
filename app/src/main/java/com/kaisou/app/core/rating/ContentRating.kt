package com.kaisou.app.core.rating

// The advisory rating of a title as it is shown. It is official when the country of the user gives it, and an approximation when
// it was worked out from another source. The symbol is the Brazilian one, the others are a tag with the label, or with the age
// when there is no label
class ContentRating(
    val group: AgeGroup,
    val label: String?,
    val official: Boolean,
    val symbol: Boolean
)
