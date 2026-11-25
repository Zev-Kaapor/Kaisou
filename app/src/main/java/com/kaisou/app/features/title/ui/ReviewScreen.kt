package com.kaisou.app.features.title.ui

// Android framework
import android.content.Intent
// Compose layouts and foundations
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
// Material 3 components
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
// Compose state and runtime
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
// UI styling and utilities
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import com.kaisou.app.core.navigation.LocalOpenUser
import androidx.lifecycle.viewmodel.compose.viewModel
// Java time
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle
import java.util.Locale
// Project resources
import com.kaisou.app.R
import com.kaisou.app.core.designsystem.components.rememberKeptListState
import com.kaisou.app.core.designsystem.components.scrollIndicator
import com.kaisou.app.core.designsystem.icons.KaisouIcons
import com.kaisou.app.core.designsystem.theme.KaisouTheme
import com.kaisou.app.features.home.ui.components.HeaderAction
import com.kaisou.app.features.home.ui.components.MarkdownContent
import com.kaisou.app.features.title.data.Load
import com.kaisou.app.features.title.data.ReviewVote
import com.kaisou.app.features.title.data.TitleReview
import com.kaisou.app.features.title.ui.components.DetailsError
import com.kaisou.app.features.title.ui.components.DetailsSkeleton
import com.kaisou.app.features.title.ui.components.RoundImage
import com.kaisou.app.features.title.ui.components.ScoreMark

// A review read whole: author, day, score, text, helpful votes and the thumbs, translated unless turned off, with spoilers covered
@Composable
fun ReviewScreen(onBack: () -> Unit, modifier: Modifier = Modifier, viewModel: ReviewViewModel = viewModel()) {
    val dimens = KaisouTheme.dimens
    val context = LocalContext.current
    val state by viewModel.state.collectAsState()
    val listState = rememberKeptListState(viewModel.scroll)
    val loaded = (state.review as? Load.Done)?.value

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .windowInsetsPadding(WindowInsets.safeDrawing.only(WindowInsetsSides.Top + WindowInsetsSides.Horizontal))
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = dimens.spaceSmall, vertical = dimens.spaceExtraSmall),
            verticalAlignment = Alignment.CenterVertically
        ) {
            HeaderAction(icon = KaisouIcons.Back, contentDescription = stringResource(R.string.cd_back), onClick = onBack)
            Text(
                text = stringResource(R.string.title_review),
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onBackground,
                modifier = Modifier.weight(1f).padding(horizontal = dimens.spaceSmall)
            )
            if (loaded != null) {
                HeaderAction(
                    icon = KaisouIcons.Translate,
                    contentDescription = stringResource(R.string.title_translate),
                    onClick = viewModel::toggleTranslate,
                    active = state.translate
                )
                HeaderAction(
                    icon = KaisouIcons.Share,
                    contentDescription = stringResource(R.string.cd_share),
                    onClick = {
                        val send = Intent(Intent.ACTION_SEND).setType("text/plain").putExtra(Intent.EXTRA_TEXT, loaded.review.url)
                        context.startActivity(Intent.createChooser(send, null))
                    }
                )
            }
        }
        LazyColumn(
            state = listState,
            modifier = Modifier.fillMaxSize().scrollIndicator(listState),
            contentPadding = PaddingValues(horizontal = dimens.screenPadding, vertical = dimens.spaceSmall),
            verticalArrangement = Arrangement.spacedBy(dimens.spaceMedium)
        ) {
            when {
                loaded != null -> item(key = "review") {
                    val shown = if (state.translate) state.translated else null
                    ReviewContent(
                        review = loaded.review,
                        summary = shown?.summary ?: loaded.review.summary,
                        text = shown?.text ?: loaded.text,
                        vote = loaded.vote,
                        onVote = viewModel::vote
                    )
                }
                state.review is Load.Failed -> item(key = "error") { DetailsError(onRetry = viewModel::load) }
                else -> item(key = "loading") { DetailsSkeleton() }
            }
        }
    }
}

// The author with the picture, the day and the score, then the text, then the votes with the thumbs
@Composable
private fun ReviewContent(review: TitleReview, summary: String, text: String, vote: ReviewVote, onVote: (ReviewVote) -> Unit) {
    val dimens = KaisouTheme.dimens
    val day = remember { DateTimeFormatter.ofLocalizedDate(FormatStyle.MEDIUM).withLocale(Locale.getDefault()).withZone(ZoneId.systemDefault()) }

    val openUser = LocalOpenUser.current

    Column(verticalArrangement = Arrangement.spacedBy(dimens.spaceMedium)) {
        Row(horizontalArrangement = Arrangement.spacedBy(dimens.spaceSmall), verticalAlignment = Alignment.CenterVertically) {
            // The picture and the name of the author open their page
            Row(
                modifier = Modifier.weight(1f).clip(MaterialTheme.shapes.medium).clickable(role = Role.Button) { openUser(review.author) },
                horizontalArrangement = Arrangement.spacedBy(dimens.spaceSmall),
                verticalAlignment = Alignment.CenterVertically
            ) {
                RoundImage(url = review.avatar, size = TitleDimens.ReviewReaderAvatar)
                Column {
                    Text(text = review.author, style = MaterialTheme.typography.titleSmall, color = MaterialTheme.colorScheme.onBackground)
                    if (review.createdAt > 0) {
                        Text(
                            text = day.format(Instant.ofEpochSecond(review.createdAt)),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
            review.score?.let { ScoreMark(it / ReviewScoreScale) }
        }
        if (summary.isNotBlank()) {
            Text(text = summary, style = MaterialTheme.typography.titleSmall, color = MaterialTheme.colorScheme.onBackground)
        }
        MarkdownContent(text = text, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurface, hideSpoilers = true)
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = stringResource(R.string.title_review_votes, review.helpful, review.votes),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.weight(1f)
            )
            VoteButton(icon = KaisouIcons.ThumbsUp, description = stringResource(R.string.cd_review_like), active = vote == ReviewVote.Up, onClick = { onVote(ReviewVote.Up) })
            VoteButton(icon = KaisouIcons.ThumbsDown, description = stringResource(R.string.cd_review_dislike), active = vote == ReviewVote.Down, onClick = { onVote(ReviewVote.Down) })
        }
    }
}

// A thumb that is in the accent when it is the vote of the user
@Composable
private fun VoteButton(icon: ImageVector, description: String, active: Boolean, onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .size(TitleDimens.ReviewVoteTouch)
            .clip(CircleShape)
            .clickable(role = Role.Button, onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = icon,
            contentDescription = description,
            tint = if (active) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.size(TitleDimens.ReviewVoteIcon)
        )
    }
}

// AniList gives the score of a review from 0 to 100
private const val ReviewScoreScale = 10f
