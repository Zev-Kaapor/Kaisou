package com.kaisou.app.core.designsystem.components

// Compose layouts and foundations
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
// Material 3 components
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
// Compose state and runtime
import androidx.compose.runtime.Composable
// UI styling and utilities
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringArrayResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.Dp
// Java time
import java.time.DayOfWeek
// Project resources
import com.kaisou.app.R
import com.kaisou.app.core.designsystem.icons.KaisouIcons
import com.kaisou.app.core.designsystem.theme.KaisouTheme
import com.kaisou.app.core.rating.ContentRating

// Day of the week a title gets a new episode, a small blue tag with a calendar
@Composable
fun WeeklyReleaseBadge(
    day: DayOfWeek,
    modifier: Modifier = Modifier
) {
    val dimens = KaisouTheme.dimens
    val colors = KaisouTheme.colors

    // The days run from Monday to Sunday, the same order as the resources
    val label = stringArrayResource(R.array.weekday_short)[day.ordinal]
    val description = stringResource(
        R.string.weekly_release_description,
        stringArrayResource(R.array.weekday_full)[day.ordinal]
    )

    Row(
        modifier = modifier
            .height(ComponentDimens.BadgeHeight)
            .clip(MaterialTheme.shapes.extraSmall)
            .background(colors.weeklyBadge)
            .semantics { contentDescription = description }
            .padding(horizontal = dimens.spaceExtraSmall),
        horizontalArrangement = Arrangement.spacedBy(dimens.spaceExtraSmall),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = KaisouIcons.Calendar,
            contentDescription = null,
            tint = colors.onWeeklyBadge,
            modifier = Modifier.size(ComponentDimens.BadgeIcon)
        )
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = colors.onWeeklyBadge
        )
    }
}

// Advisory rating of a title as a square: the symbol of Brazil, the age with a plus elsewhere, RP or NR with no age
@Composable
fun AgeRatingBadge(
    rating: ContentRating,
    modifier: Modifier = Modifier,
    size: Dp = ComponentDimens.RatingSymbol
) {
    val unrated = rating.unrated
    val symbol = unrated?.symbol ?: if (rating.symbol) rating.group.symbol else rating.group.global
    val name = if (unrated != null) stringResource(unrated.description) else stringArrayResource(R.array.age_rating_names)[rating.group.ordinal]
    val description = if (rating.official) name else stringResource(R.string.age_rating_approximate, name)

    Image(
        painter = painterResource(symbol),
        contentDescription = description,
        modifier = modifier.size(size)
    )
}
