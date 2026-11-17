package com.kaisou.app.features.title.ui.components

// Compose layouts and foundations
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyListScope
// Material 3 components
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
// Compose state and runtime
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
// UI styling and utilities
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
// Java utilities
import java.text.NumberFormat
import java.util.Locale
// Project resources
import com.kaisou.app.R
import com.kaisou.app.core.browser.InAppBrowser
import com.kaisou.app.core.designsystem.icons.KaisouIcons
import com.kaisou.app.core.designsystem.theme.KaisouTheme
import com.kaisou.app.features.title.data.Load
import com.kaisou.app.features.title.data.TitleCommunity
import com.kaisou.app.features.title.data.TitleFriend
import com.kaisou.app.features.title.data.TitleReview
import com.kaisou.app.features.title.data.TitleScoreBar
import com.kaisou.app.features.title.data.TitleStatusShare
import com.kaisou.app.features.title.data.TitleThread
import com.kaisou.app.features.title.ui.TitleDimens

// The tab of the community: the friends that have the title, the reviews, how the scores and the lists are spread, and the threads of the
// forum. A block that has nothing is left out, and when all of them have nothing the tab says so
fun LazyListScope.titleCommunityItems(community: Load<TitleCommunity>, onRetry: () -> Unit) {
    when (community) {
        is Load.Done -> {
            val value = community.value
            val empty = value.friends.isEmpty() && value.reviews.isEmpty() && value.scores.isEmpty() && value.statuses.isEmpty() && value.threads.isEmpty()
            if (empty) item(key = "community-empty") { CommunityMessage() }
            if (value.friends.isNotEmpty()) item(key = "community-friends") { FriendsBlock(value.friends) }
            if (value.reviews.isNotEmpty()) item(key = "community-reviews") { ReviewsBlock(value.reviews) }
            if (value.scores.isNotEmpty() || value.statuses.isNotEmpty()) item(key = "community-stats") { StatsBlock(value.scores, value.statuses) }
            if (value.threads.isNotEmpty()) item(key = "community-threads") { ThreadsBlock(value.threads) }
        }
        is Load.Failed -> item(key = "community-error") { DetailsError(onRetry = onRetry) }
        else -> item(key = "community-loading") { DetailsSkeleton() }
    }
}

@Composable
private fun CommunityMessage() {
    Text(
        text = stringResource(R.string.title_community_empty),
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        textAlign = TextAlign.Center,
        modifier = Modifier.fillMaxWidth().padding(KaisouTheme.dimens.screenPadding).padding(top = TitleDimens.BlockTop)
    )
}

// The score on the scale of ten with its star, in the accent
@Composable
private fun ScoreMark(score: Float) {
    val format = remember { NumberFormat.getNumberInstance(Locale.getDefault()).apply { minimumFractionDigits = 1; maximumFractionDigits = 1 } }

    Row(horizontalArrangement = Arrangement.spacedBy(KaisouTheme.dimens.spaceExtraSmall), verticalAlignment = Alignment.CenterVertically) {
        Icon(imageVector = KaisouIcons.Star, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(TitleDimens.ScoreStar))
        Text(text = format.format(score), style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onBackground)
    }
}

// Each friend that has the title with where it is on their list, how far they are and the score they gave
@Composable
private fun FriendsBlock(friends: List<TitleFriend>) {
    val dimens = KaisouTheme.dimens
    val context = LocalContext.current
    val separator = stringResource(R.string.home_card_separator)

    Column(modifier = Modifier.fillMaxWidth().padding(horizontal = dimens.screenPadding).padding(top = TitleDimens.BlockTop)) {
        BlockTitle(stringResource(R.string.title_section_friends))
        friends.forEach { friend ->
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = dimens.spaceSmall)
                    .clickable(role = Role.Button) { InAppBrowser.open(context, friend.url) },
                horizontalArrangement = Arrangement.spacedBy(dimens.spaceMedium),
                verticalAlignment = Alignment.CenterVertically
            ) {
                RoundImage(url = friend.avatar, size = TitleDimens.AvatarSize)
                Column(modifier = Modifier.weight(1f)) {
                    Text(text = friend.name, style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onBackground, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    Text(
                        text = listOfNotNull(friend.status.takeIf { it.isNotEmpty() }, friend.progress?.toString()).joinToString(separator),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
                friend.score?.let { ScoreMark(it) }
            }
        }
    }
}

// The reviews, each in a card with who wrote it, the score, how it starts and how many found it useful, and it opens on AniList
@Composable
private fun ReviewsBlock(reviews: List<TitleReview>) {
    val dimens = KaisouTheme.dimens
    val context = LocalContext.current

    Column(modifier = Modifier.fillMaxWidth().padding(horizontal = dimens.screenPadding).padding(top = TitleDimens.BlockTop)) {
        BlockTitle(stringResource(R.string.title_section_reviews))
        reviews.forEach { review ->
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = dimens.spaceSmall)
                    .clip(MaterialTheme.shapes.medium)
                    .background(KaisouTheme.colors.elevated)
                    .clickable(role = Role.Button) { InAppBrowser.open(context, review.url) }
                    .padding(dimens.spaceMedium),
                verticalArrangement = Arrangement.spacedBy(dimens.spaceSmall)
            ) {
                Row(horizontalArrangement = Arrangement.spacedBy(dimens.spaceSmall), verticalAlignment = Alignment.CenterVertically) {
                    RoundImage(url = review.avatar, size = TitleDimens.ReviewAvatar)
                    Text(
                        text = review.author,
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.onSurface,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f)
                    )
                    review.score?.let { ScoreMark(it / ScoreScale) }
                }
                if (review.summary.isNotBlank()) {
                    Text(
                        text = review.summary,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurface,
                        maxLines = TitleDimens.ReviewLines,
                        overflow = TextOverflow.Ellipsis
                    )
                }
                Text(
                    text = stringResource(R.string.title_review_useful, review.helpful),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

// AniList gives the score of a review from 0 to 100
private const val ScoreScale = 10f
private const val ScoreTens = 10

// How many people gave each score, in bars that rise from the base, and how many have the title in each state of their list, in lines with
// a bar that fills as far as the largest
@Composable
private fun StatsBlock(scores: List<TitleScoreBar>, statuses: List<TitleStatusShare>) {
    val dimens = KaisouTheme.dimens
    val format = remember { NumberFormat.getIntegerInstance(Locale.getDefault()) }

    Column(modifier = Modifier.fillMaxWidth().padding(horizontal = dimens.screenPadding).padding(top = TitleDimens.BlockTop)) {
        BlockTitle(stringResource(R.string.title_section_stats))
        if (scores.isNotEmpty()) {
            val top = scores.maxOf { it.amount }.coerceAtLeast(1)
            Text(
                text = stringResource(R.string.title_stats_scores),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = dimens.spaceSmall)
            )
            Row(
                modifier = Modifier.fillMaxWidth().padding(top = dimens.spaceSmall),
                horizontalArrangement = Arrangement.spacedBy(TitleDimens.ScoreBarGap),
                verticalAlignment = Alignment.Bottom
            ) {
                scores.forEach { bar ->
                    Column(modifier = Modifier.weight(1f), horizontalAlignment = Alignment.CenterHorizontally) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(TitleDimens.ScoreBarHeight * (bar.amount.toFloat() / top).coerceAtLeast(TitleDimens.ScoreBarMinimum))
                                .clip(MaterialTheme.shapes.extraSmall)
                                .background(MaterialTheme.colorScheme.primary)
                        )
                        // AniList spreads the scores in tens, the page says them from 1 to 10
                        Text(
                            text = (bar.score / ScoreTens).toString(),
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(top = dimens.spaceExtraSmall)
                        )
                    }
                }
            }
        }
        if (statuses.isNotEmpty()) {
            val top = statuses.maxOf { it.amount }.coerceAtLeast(1)
            Text(
                text = stringResource(R.string.title_stats_status),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = dimens.spaceLarge)
            )
            statuses.sortedByDescending { it.amount }.forEach { share ->
                Column(modifier = Modifier.fillMaxWidth().padding(top = dimens.spaceSmall)) {
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text(text = share.label, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onBackground)
                        Text(text = format.format(share.amount), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    Box(
                        modifier = Modifier
                            .padding(top = dimens.spaceExtraSmall)
                            .fillMaxWidth()
                            .height(TitleDimens.StatusBarHeight)
                            .clip(MaterialTheme.shapes.extraSmall)
                            .background(KaisouTheme.colors.elevated)
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxHeight()
                                .fillMaxWidth(share.amount.toFloat() / top)
                                .background(MaterialTheme.colorScheme.primary)
                        )
                    }
                }
            }
        }
    }
}

// The threads of the forum about the title, each opens on AniList
@Composable
private fun ThreadsBlock(threads: List<TitleThread>) {
    val dimens = KaisouTheme.dimens
    val context = LocalContext.current
    val separator = stringResource(R.string.home_card_separator)

    Column(modifier = Modifier.fillMaxWidth().padding(horizontal = dimens.screenPadding).padding(top = TitleDimens.BlockTop)) {
        BlockTitle(stringResource(R.string.title_section_threads))
        threads.forEach { thread ->
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = dimens.spaceSmall)
                    .clickable(role = Role.Button) { InAppBrowser.open(context, thread.url) }
            ) {
                Text(text = thread.title, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onBackground, maxLines = 2, overflow = TextOverflow.Ellipsis)
                Text(
                    text = listOf(pluralStringResource(R.plurals.title_replies, thread.replies, thread.replies), thread.author).filter { it.isNotBlank() }.joinToString(separator),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}
