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
// Compose gestures
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
// Compose state and runtime
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
// UI styling and utilities
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
// Android and Java utilities
import android.icu.text.CompactDecimalFormat
import android.text.format.DateUtils
import java.text.NumberFormat
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.chrono.IsoChronology
import java.time.format.DateTimeFormatterBuilder
import java.time.format.FormatStyle
import java.util.Locale
// Project resources
import com.kaisou.app.R
import com.kaisou.app.core.browser.InAppBrowser
import com.kaisou.app.core.designsystem.icons.KaisouIcons
import com.kaisou.app.core.designsystem.theme.KaisouTheme
import com.kaisou.app.features.title.data.Load
import com.kaisou.app.features.title.data.TitleActivity
import com.kaisou.app.features.title.data.TitleCommunity
import com.kaisou.app.features.title.data.TitleReview
import com.kaisou.app.features.title.data.TitleScoreBar
import com.kaisou.app.features.title.data.TitleStatusShare
import com.kaisou.app.features.title.data.TitleThread
import com.kaisou.app.features.title.data.TitleTrend
import com.kaisou.app.features.title.ui.TitleDimens
import com.kaisou.app.features.home.ui.components.SpoilerText
import com.kaisou.app.features.home.ui.components.rememberScoreFormat

// The tab of the community: the reviews, how the scores and the lists are spread, and the threads of the
// forum. A block that has nothing is left out, and when all of them have nothing the tab says so
fun LazyListScope.titleCommunityItems(community: Load<TitleCommunity>, onOpenReviews: () -> Unit, onOpenReview: (Int) -> Unit, onOpenThreads: () -> Unit, onOpenThread: (Int) -> Unit, onRetry: () -> Unit) {
    when (community) {
        is Load.Done -> {
            val value = community.value
            val empty = value.reviews.isEmpty() && value.scores.isEmpty() && value.statuses.isEmpty() && value.threads.isEmpty() && value.trends.isEmpty() && value.popularity == 0 && value.favourites == 0 && value.activity.isEmpty()
            if (empty) item(key = "community-empty") { CommunityMessage() }
            if (value.popularity > 0 || value.favourites > 0) item(key = "community-counters") { CountersBlock(value.popularity, value.favourites) }
            if (value.reviews.isNotEmpty()) item(key = "community-reviews") { ReviewsBlock(value.reviews, onOpenReviews, onOpenReview) }
            if (value.scores.isNotEmpty() || value.statuses.isNotEmpty() || value.trends.isNotEmpty()) item(key = "community-stats") { StatsBlock(value.trends, value.scores, value.statuses) }
            if (value.activity.isNotEmpty()) item(key = "community-activity") { ActivityBlock(value.activity) }
            if (value.threads.isNotEmpty()) item(key = "community-threads") { ThreadsBlock(value.threads, onOpenThreads, onOpenThread) }
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

// How many people have the title on a list and how many made it a favourite, in two cards side by side
@Composable
private fun CountersBlock(popularity: Int, favourites: Int) {
    val dimens = KaisouTheme.dimens
    val format = remember { NumberFormat.getIntegerInstance(Locale.getDefault()) }

    Row(
        modifier = Modifier.fillMaxWidth().padding(horizontal = dimens.screenPadding).padding(top = TitleDimens.BlockTop),
        horizontalArrangement = Arrangement.spacedBy(dimens.spaceSmall)
    ) {
        listOf(R.string.title_stats_popularity to popularity, R.string.title_stats_favourites to favourites).forEach { (label, amount) ->
            Column(
                modifier = Modifier
                    .weight(1f)
                    .clip(MaterialTheme.shapes.medium)
                    .background(KaisouTheme.colors.elevated)
                    .padding(dimens.spaceMedium),
                verticalArrangement = Arrangement.spacedBy(dimens.spaceExtraSmall)
            ) {
                Text(text = format.format(amount), style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.onBackground)
                Text(text = stringResource(label), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }
}

// The score on the scale of ten with its star, in the accent
@Composable
internal fun ScoreMark(score: Float) {
    val format = rememberScoreFormat()

    Row(horizontalArrangement = Arrangement.spacedBy(KaisouTheme.dimens.spaceExtraSmall), verticalAlignment = Alignment.CenterVertically) {
        Icon(imageVector = KaisouIcons.Star, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(TitleDimens.ScoreStar))
        Text(text = format.format(score), style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onBackground)
    }
}

// The first reviews, each in a card, and the arrow of the title opens all of them
@Composable
private fun ReviewsBlock(reviews: List<TitleReview>, onOpenReviews: () -> Unit, onOpenReview: (Int) -> Unit) {
    val dimens = KaisouTheme.dimens

    Column(modifier = Modifier.fillMaxWidth().padding(horizontal = dimens.screenPadding).padding(top = TitleDimens.BlockTop)) {
        Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            BlockTitle(text = stringResource(R.string.title_section_reviews), modifier = Modifier.weight(1f))
            SeeAllArrow(onOpenReviews)
        }
        reviews.forEach { review ->
            ReviewCard(review = review, onClick = { onOpenReview(review.id) }, modifier = Modifier.padding(top = dimens.spaceSmall))
        }
    }
}

// A review in a card with who wrote it, the score, how it starts and how many found it useful, and it opens to be read
@Composable
internal fun ReviewCard(review: TitleReview, onClick: () -> Unit, modifier: Modifier = Modifier) {
    val dimens = KaisouTheme.dimens

    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(MaterialTheme.shapes.medium)
            .background(KaisouTheme.colors.elevated)
            .clickable(role = Role.Button, onClick = onClick)
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
            text = if (review.votes > 0) stringResource(R.string.title_review_votes, review.helpful, review.votes) else stringResource(R.string.title_review_useful, review.helpful),
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

// AniList gives the score of a review from 0 to 100
private const val ScoreScale = 10f
private const val ScoreTens = 10

// How much happened around the title each day, in bars that rise from the base with the first and the last day under them, how many people gave
// each score, in bars of the same kind, and how many have the title in each state of their list, in lines with a bar that fills as far as the largest
@Composable
private fun StatsBlock(trends: List<TitleTrend>, scores: List<TitleScoreBar>, statuses: List<TitleStatusShare>) {
    val dimens = KaisouTheme.dimens
    val format = remember { NumberFormat.getIntegerInstance(Locale.getDefault()) }
    val compact = remember { CompactDecimalFormat.getInstance(Locale.getDefault(), CompactDecimalFormat.CompactStyle.SHORT) }

    Column(modifier = Modifier.fillMaxWidth().padding(horizontal = dimens.screenPadding).padding(top = TitleDimens.BlockTop)) {
        BlockTitle(stringResource(R.string.title_section_stats))
        if (trends.isNotEmpty()) {
            val top = trends.maxOf { it.activity }.coerceAtLeast(1)
            // The short date of the language with the whole year
            val day = remember { DateTimeFormatter.ofPattern(DateTimeFormatterBuilder.getLocalizedDateTimePattern(FormatStyle.SHORT, null, IsoChronology.INSTANCE, Locale.getDefault()).replace(Regex("y+"), "yyyy"), Locale.getDefault()).withZone(ZoneId.systemDefault()) }
            // The day that is picked, the last one until a touch picks another
            var picked by remember(trends) { mutableIntStateOf(trends.lastIndex) }
            Row(modifier = Modifier.fillMaxWidth().padding(top = dimens.spaceSmall), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Text(text = stringResource(R.string.title_stats_activity), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Text(
                    text = stringResource(R.string.title_stats_day, day.format(Instant.ofEpochSecond(trends[picked].day)), format.format(trends[picked].activity)),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.primary
                )
            }
            // A touch or a drag over the bars picks the day under the finger
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = dimens.spaceSmall)
                    .height(TitleDimens.ScoreBarHeight)
                    .pointerInput(trends) { detectTapGestures { picked = (it.x / size.width * trends.size).toInt().coerceIn(trends.indices) } }
                    .pointerInput(trends) { detectHorizontalDragGestures { change, _ -> picked = (change.position.x / size.width * trends.size).toInt().coerceIn(trends.indices) } },
                horizontalArrangement = Arrangement.spacedBy(TitleDimens.TrendBarGap),
                verticalAlignment = Alignment.Bottom
            ) {
                trends.forEachIndexed { index, trend ->
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .height(TitleDimens.ScoreBarHeight * (trend.activity.toFloat() / top).coerceAtLeast(TitleDimens.ScoreBarMinimum))
                            .clip(MaterialTheme.shapes.extraSmall)
                            .background(MaterialTheme.colorScheme.primary.copy(alpha = if (index == picked) 1f else TitleDimens.TrendDimAlpha))
                    )
                }
            }
            Row(modifier = Modifier.fillMaxWidth().padding(top = dimens.spaceExtraSmall), horizontalArrangement = Arrangement.SpaceBetween) {
                Text(text = day.format(Instant.ofEpochSecond(trends.first().day)), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Text(text = day.format(Instant.ofEpochSecond(trends.last().day)), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
        if (scores.isNotEmpty()) {
            val top = scores.maxOf { it.amount }.coerceAtLeast(1)
            Text(
                text = stringResource(R.string.title_stats_scores),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = if (trends.isEmpty()) dimens.spaceSmall else dimens.spaceLarge)
            )
            Row(
                modifier = Modifier.fillMaxWidth().padding(top = dimens.spaceSmall),
                horizontalArrangement = Arrangement.spacedBy(TitleDimens.ScoreBarGap),
                verticalAlignment = Alignment.Bottom
            ) {
                scores.forEach { bar ->
                    Column(modifier = Modifier.weight(1f), horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(text = compact.format(bar.amount), style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onBackground, maxLines = 1)
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

// How long ago something was, as the phone says it, like 3 days ago, from the day it happened in seconds
internal fun relativeTime(seconds: Long): String =
    DateUtils.getRelativeTimeSpanString(seconds * MillisPerSecond, System.currentTimeMillis(), DateUtils.MINUTE_IN_MILLIS).toString()

private const val MillisPerSecond = 1000L

// What people did to the title lately, each one with who did it, what and when, and it opens the page of the person
@Composable
private fun ActivityBlock(activity: List<TitleActivity>) {
    val dimens = KaisouTheme.dimens
    val context = LocalContext.current

    Column(modifier = Modifier.fillMaxWidth().padding(horizontal = dimens.screenPadding).padding(top = TitleDimens.BlockTop)) {
        BlockTitle(stringResource(R.string.title_section_activity))
        activity.forEach { item ->
            val day = relativeTime(item.createdAt)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = dimens.spaceSmall)
                    .clip(MaterialTheme.shapes.medium)
                    .clickable(role = Role.Button) { InAppBrowser.open(context, "https://anilist.co/user/${item.author}") },
                horizontalArrangement = Arrangement.spacedBy(dimens.spaceSmall),
                verticalAlignment = Alignment.CenterVertically
            ) {
                RoundImage(url = item.avatar, size = TitleDimens.CommentAvatar)
                Column(modifier = Modifier.weight(1f)) {
                    Text(text = item.author, style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onBackground, maxLines = 1, overflow = TextOverflow.Ellipsis)
                    Text(text = item.text, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 1, overflow = TextOverflow.Ellipsis)
                }
                Text(text = day, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 1)
            }
        }
    }
}

// The first threads of the forum about the title, each opens to be read, and the arrow of the title opens all of them
@Composable
private fun ThreadsBlock(threads: List<TitleThread>, onOpenThreads: () -> Unit, onOpenThread: (Int) -> Unit) {
    val dimens = KaisouTheme.dimens

    Column(modifier = Modifier.fillMaxWidth().padding(horizontal = dimens.screenPadding).padding(top = TitleDimens.BlockTop)) {
        Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            BlockTitle(text = stringResource(R.string.title_section_threads), modifier = Modifier.weight(1f))
            SeeAllArrow(onOpenThreads)
        }
        threads.forEach { thread -> ThreadRow(thread = thread, onClick = { onOpenThread(thread.id) }, modifier = Modifier.padding(top = dimens.spaceSmall)) }
    }
}

// A thread in a card: the picture of who wrote it at the start, then its title, who wrote it and when, and how many answered and liked it
@Composable
internal fun ThreadRow(thread: TitleThread, onClick: () -> Unit, modifier: Modifier = Modifier) {
    val dimens = KaisouTheme.dimens
    val separator = stringResource(R.string.home_card_separator)
    val day = relativeTime(thread.createdAt)

    Row(
        modifier = modifier
            .fillMaxWidth()
            .clip(MaterialTheme.shapes.medium)
            .background(KaisouTheme.colors.elevated)
            .clickable(role = Role.Button, onClick = onClick)
            .padding(start = dimens.spaceMedium, top = dimens.spaceMedium, end = dimens.spaceMedium, bottom = dimens.spaceSmall),
        horizontalArrangement = Arrangement.spacedBy(dimens.spaceMedium),
        verticalAlignment = Alignment.Top
    ) {
        RoundImage(url = thread.avatar, size = TitleDimens.ReviewReaderAvatar)
        Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(dimens.spaceExtraSmall)) {
            SpoilerText(
                text = thread.title,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurface,
                linkColor = MaterialTheme.colorScheme.primary,
                headingSize = MaterialTheme.typography.bodyMedium.fontSize,
                maxLines = 2
            )
            Text(
                text = listOf(thread.author, day).filter { it.isNotBlank() }.joinToString(separator),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            // The same room above the numbers as there is under them, to the edge of the card: the line of the numbers has room of its own under
            // them, so the card has less padding at the bottom than at the top
            Row(
                modifier = Modifier.fillMaxWidth().padding(top = dimens.spaceMedium - dimens.spaceExtraSmall / 2),
                horizontalArrangement = Arrangement.spacedBy(dimens.spaceMedium, Alignment.End),
                verticalAlignment = Alignment.CenterVertically
            ) {
                ThreadCount(KaisouIcons.Comment, thread.replies)
                ThreadCount(KaisouIcons.Heart, thread.likes)
            }
        }
    }
}

// An icon with a number, small and faint
@Composable
private fun ThreadCount(icon: ImageVector, count: Int) {
    Row(horizontalArrangement = Arrangement.spacedBy(KaisouTheme.dimens.spaceExtraSmall), verticalAlignment = Alignment.CenterVertically) {
        Icon(imageVector = icon, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(TitleDimens.ThreadCountIcon))
        Text(text = count.toString(), style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

