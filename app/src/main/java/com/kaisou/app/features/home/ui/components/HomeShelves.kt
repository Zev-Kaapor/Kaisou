package com.kaisou.app.features.home.ui.components

// Compose layouts and foundations
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
// Material 3 components
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
// Compose state and runtime
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.setValue
import androidx.compose.runtime.remember
// UI styling and utilities
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.Dp
// Image loading
import coil3.compose.AsyncImage
// Coroutines
import kotlinx.coroutines.delay
// Java text and utilities
import java.text.NumberFormat
import java.util.Locale
// Project resources
import com.kaisou.app.R
import com.kaisou.app.core.artwork.ArtworkRepository
import com.kaisou.app.core.designsystem.components.AgeRatingBadge
import com.kaisou.app.core.designsystem.components.SkeletonHost
import com.kaisou.app.core.designsystem.components.skeleton
import com.kaisou.app.core.designsystem.icons.KaisouIcons
import com.kaisou.app.core.language.AppLanguage
import com.kaisou.app.core.designsystem.theme.KaisouTheme
import com.kaisou.app.core.rating.ContentRating
import com.kaisou.app.core.rating.RatingQuery
import com.kaisou.app.core.rating.RatingRepository
import com.kaisou.app.features.home.data.CardShape
import com.kaisou.app.features.home.data.CardUi
import com.kaisou.app.features.home.data.StoryUi
import com.kaisou.app.features.home.data.ShelfUi
import com.kaisou.app.features.home.ui.HomeDimens

// The user and the people they follow, as on Dantotsu: a round picture inside a ring with one piece for each thing the person did
// lately, gray for what was already seen, and the name under it. The user comes first, with a plus where a status will be posted
@Composable
fun HomeFriends(own: StoryUi, friends: List<StoryUi>, seen: Set<Int>, onOpen: (StoryUi) -> Unit, onAdd: () -> Unit, modifier: Modifier = Modifier) {
    val dimens = KaisouTheme.dimens

    LazyRow(
        modifier = modifier,
        contentPadding = PaddingValues(horizontal = dimens.screenPadding),
        horizontalArrangement = Arrangement.spacedBy(dimens.spaceMedium)
    ) {
        item(key = "own") {
            FriendStory(story = own, name = stringResource(R.string.home_your_status), seen = seen, isOwn = true, onClick = { onOpen(own) }, onAdd = onAdd)
        }
        items(friends, key = { it.userId }) { friend ->
            FriendStory(story = friend, name = friend.name, seen = seen, isOwn = false, onClick = { onOpen(friend) }, onAdd = {})
        }
    }
}

@Composable
private fun FriendStory(story: StoryUi, name: String, seen: Set<Int>, isOwn: Boolean, onClick: () -> Unit, onAdd: () -> Unit, modifier: Modifier = Modifier) {
    val colors = KaisouTheme.colors

    Column(
        modifier = modifier.width(HomeDimens.FriendRing).clickable(enabled = story.items.isNotEmpty(), onClick = onClick),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(modifier = Modifier.size(HomeDimens.FriendRing), contentAlignment = Alignment.Center) {
            StoryRing(seen = story.items.map { it.id in seen }, unseenColor = MaterialTheme.colorScheme.primary, seenColor = colors.navUnselected)
            AsyncImage(
                model = story.avatarUrl,
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .size(HomeDimens.FriendAvatar)
                    .clip(CircleShape)
                    .background(colors.skeleton)
            )
            if (isOwn) {
                Box(
                    modifier = Modifier
                        .align(Alignment.BottomEnd)
                        .size(HomeDimens.FriendPlus)
                        .clip(CircleShape)
                        .background(Brush.linearGradient(listOf(colors.buttonStart, colors.buttonEnd)))
                        .clickable(onClick = onAdd),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(imageVector = KaisouIcons.Plus, contentDescription = stringResource(R.string.status_title), tint = colors.onAccentFill, modifier = Modifier.size(HomeDimens.FriendPlusIcon))
                }
            }
        }
        Text(
            text = name,
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onBackground,
            textAlign = TextAlign.Center,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.padding(top = HomeDimens.FriendNameTop)
        )
    }
}

// The ring around a picture, with a piece for each activity from the oldest, gray when it was seen. One activity or none is a whole
// circle, more are split with a gap between the pieces that shrinks as they grow in number, so the gaps never take more than a share
@Composable
private fun StoryRing(seen: List<Boolean>, unseenColor: Color, seenColor: Color) {
    Canvas(modifier = Modifier.size(HomeDimens.FriendRing)) {
        val parts = seen.size
        if (parts == 0) return@Canvas
        val stroke = HomeDimens.FriendRingStroke.toPx()
        val arcSize = Size(size.width - stroke, size.height - stroke)
        val topLeft = Offset(stroke / 2, stroke / 2)
        if (parts == 1) {
            drawArc(if (seen.first()) seenColor else unseenColor, 0f, FullCircle, false, topLeft, arcSize, style = Stroke(stroke))
            return@Canvas
        }
        val gap = minOf(HomeDimens.FriendRingGapMax, HomeDimens.FriendRingGapsTotal / parts)
        val sweep = (FullCircle - gap * parts) / parts
        seen.forEachIndexed { index, wasSeen ->
            drawArc(if (wasSeen) seenColor else unseenColor, index * (sweep + gap) + RingStart + gap / 2, sweep, false, topLeft, arcSize, style = Stroke(stroke))
        }
    }
}

// A shelf of the Home: its name and a row of cards that slides sideways, the first one at the margin of the screen and the last
// one cut by the edge so it shows there is more. Lying down more of them fit in the row
@Composable
fun HomeShelf(shelf: ShelfUi, modifier: Modifier = Modifier) {
    val dimens = KaisouTheme.dimens
    // The wide cards change their picture together, on the clock of the shelf
    var showWide by remember { mutableStateOf(false) }
    if (shelf.shape == CardShape.Wide) {
        LaunchedEffect(Unit) {
            while (true) {
                delay(HomeDimens.CardAlternateMillis)
                showWide = !showWide
            }
        }
    }

    Column(modifier = modifier) {
        Text(
            text = stringResource(shelf.id.titleRes),
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onBackground,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.padding(horizontal = dimens.screenPadding)
        )
        LazyRow(
            contentPadding = PaddingValues(horizontal = dimens.screenPadding),
            horizontalArrangement = Arrangement.spacedBy(dimens.spaceSmall),
            modifier = Modifier.padding(top = dimens.spaceSmall)
        ) {
            items(shelf.cards, key = { it.anilistId }) { card ->
                if (shelf.shape == CardShape.Poster) {
                    TitleCard(card, HomeDimens.PosterWidth, HomeDimens.PosterRatio, showWide)
                } else {
                    TitleCard(card, HomeDimens.WideWidth, HomeDimens.WideRatio, showWide)
                }
            }
        }
    }
}

// A title on a shelf: its picture with the tab of the score, and of the episode that aired when it is a new one, at the bottom corner,
// a thin line around it so dark covers do not melt into the page, then its name and a line with how far the user is in it, the part
// done in the violet of the app, or the details of the title
@Composable
private fun TitleCard(card: CardUi, width: Dp, ratio: Float, showWide: Boolean, modifier: Modifier = Modifier) {
    val dimens = KaisouTheme.dimens
    val colors = KaisouTheme.colors
    val accent = MaterialTheme.colorScheme.primary

    Column(modifier = modifier.width(width)) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(ratio)
                .clip(MaterialTheme.shapes.medium)
                .background(colors.skeleton)
        ) {
            CardPicture(card, showWide)
            CardTab(episode = card.episode, score = card.score, modifier = Modifier.align(Alignment.BottomEnd))
            Box(
                modifier = Modifier
                    .matchParentSize()
                    .border(dimens.hairline, MaterialTheme.colorScheme.outline, MaterialTheme.shapes.medium)
            )
        }
        Text(
            text = card.title,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onBackground,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.padding(top = dimens.spaceSmall)
        )
        val separator = stringResource(R.string.home_card_progress_separator)
        val unknown = stringResource(R.string.home_card_progress_unknown)
        val line = if (card.done != null) {
            buildAnnotatedString {
                withStyle(SpanStyle(color = accent)) { append(card.done.toString()) }
                append(separator)
                append(card.total?.toString() ?: unknown)
            }
        } else {
            buildAnnotatedString { append(card.details) }
        }
        val rating = rememberCardRating(card.rating)
        // The room of the rating is kept while it is looked for, so the line does not move when it comes up
        val showRating = !rating.loaded || rating.rating != null
        if (line.isNotEmpty() || showRating) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(dimens.spaceSmall)) {
                if (showRating) CardRating(rating.rating)
                Text(
                    text = line,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f)
                )
            }
        }
    }
}

// The signature of the cards: a tab in the violet of the buttons that sits in a corner of the picture, round only on its free
// corner. The score has it at the bottom and a new episode at the top
@Composable
private fun CornerTab(shape: Shape, modifier: Modifier = Modifier, content: @Composable RowScope.() -> Unit) {
    val dimens = KaisouTheme.dimens
    val colors = KaisouTheme.colors

    Row(
        modifier = modifier
            .clip(shape)
            .background(Brush.linearGradient(listOf(colors.buttonStart, colors.buttonEnd)))
            .padding(horizontal = HomeDimens.TabHorizontal, vertical = HomeDimens.TabVertical),
        horizontalArrangement = Arrangement.spacedBy(dimens.spaceExtraSmall),
        verticalAlignment = Alignment.CenterVertically,
        content = content
    )
}

// The tab of a card: the episode that aired, if it is a new one, a line, and the score with its star, like E7 | 6,2 *. A title with
// no score yet has dashes in its place, since zero would say people did not like it
@Composable
private fun CardTab(episode: Int?, score: Float?, modifier: Modifier = Modifier) {
    val colors = KaisouTheme.colors
    val format = remember { NumberFormat.getNumberInstance(Locale.getDefault()).apply { minimumFractionDigits = 1; maximumFractionDigits = 1 } }

    CornerTab(shape = RoundedCornerShape(topStart = HomeDimens.TabCorner), modifier = modifier) {
        if (episode != null) {
            Text(text = stringResource(R.string.home_card_episode, episode), style = MaterialTheme.typography.labelSmall, color = colors.onAccentFill)
        }
        if (episode != null) {
            Box(
                modifier = Modifier
                    .size(KaisouTheme.dimens.hairline, HomeDimens.TabStar)
                    .background(colors.onAccentFill.copy(alpha = HomeDimens.MetaDividerAlpha))
            )
        }
        Text(
            text = score?.let(format::format) ?: stringResource(R.string.home_card_no_score),
            style = MaterialTheme.typography.labelSmall,
            color = colors.onAccentFill
        )
        Icon(
            imageVector = KaisouIcons.Star,
            contentDescription = null,
            tint = colors.onAccentFill,
            modifier = Modifier.size(HomeDimens.TabStar)
        )
    }
}

// What is known of the rating of a card: whether it was looked for already, and the rating, null when the title has none
private class CardRatingState(val loaded: Boolean, val rating: ContentRating?)

// Looks for the rating of a title only while its card is on the screen, from what is kept first
@Composable
private fun rememberCardRating(query: RatingQuery): CardRatingState {
    val region = AppLanguage.region
    val kept = RatingRepository.known(region, query)
    val state by produceState(CardRatingState(kept.first, kept.second), query.anilistId, region) {
        value = CardRatingState(loaded = true, rating = RatingRepository.forCard(region, query))
    }
    return state
}

// The square of the rating at the start of the line under a card, a gray square of the placeholder while it is looked for, that gives
// way to it softly when found
@Composable
private fun CardRating(rating: ContentRating?) {
    val alpha by animateFloatAsState(
        targetValue = if (rating != null) 1f else 0f,
        animationSpec = tween(HomeDimens.CardRatingFadeMillis),
        label = "cardRating"
    )

    Box(
        modifier = Modifier
            .size(HomeDimens.CardRatingSize)
            .background(KaisouTheme.colors.skeleton.copy(alpha = 1f - alpha), MaterialTheme.shapes.extraSmall)
    ) {
        rating?.let { AgeRatingBadge(it, size = HomeDimens.CardRatingSize, modifier = Modifier.alpha(alpha)) }
    }
}

// The picture of a card. A new episode shows the picture it has and the wide picture of its title, found the way the hero finds its
// own, in turn, all the cards of the shelf at the same moment. The second one is only shown once it is downloaded
@Composable
private fun CardPicture(card: CardUi, showWide: Boolean) {
    val query = card.query
    val wide by produceState<String?>(null, card.anilistId) {
        value = query?.let { ArtworkRepository.Shared.art(card.anilistId, it, AppLanguage.code).backdropUrl }
    }
    var ready by remember(wide) { mutableStateOf(false) }
    val alpha by animateFloatAsState(
        targetValue = if (ready && showWide) 1f else 0f,
        animationSpec = tween(HomeDimens.CardCrossfadeMillis),
        label = "cardPicture"
    )

    Box(modifier = Modifier.fillMaxSize()) {
        AsyncImage(model = card.imageUrl, contentDescription = null, contentScale = ContentScale.Crop, modifier = Modifier.matchParentSize())
        // It loads behind the first one, with no opacity, and shows when it is ready
        wide?.let { url ->
            AsyncImage(
                model = url,
                contentDescription = null,
                contentScale = ContentScale.Crop,
                onSuccess = { ready = true },
                modifier = Modifier.matchParentSize().alpha(alpha)
            )
        }
    }
}

// The ring is drawn from the top, all the way round
private const val RingStart = -90f
private const val FullCircle = 360f

// The shelves of the placeholder, in the shapes of the cards of the shelves that come: posters, and wide ones where the new episodes go
private val SkeletonShelves = listOf(CardShape.Poster, CardShape.Poster, CardShape.Wide, CardShape.Poster, CardShape.Poster)

// Placeholder of what is under the hero while nothing has arrived, each block where the friends and the cards of the shelves go, in the
// sizes of the real ones, so nothing moves when they arrive. The light is the same that sweeps the hero
fun LazyListScope.homeShelvesSkeleton() {
    item(key = "skeleton-friends") { FriendsSkeleton() }
    itemsIndexed(SkeletonShelves, key = { index, _ -> "skeleton-shelf-$index" }) { _, shape -> ShelfSkeleton(shape) }
}

// The round pictures of the friends with a bar for the name under each
@Composable
private fun FriendsSkeleton() {
    val dimens = KaisouTheme.dimens

    SkeletonHost(modifier = Modifier.fillMaxWidth()) {
        LazyRow(
            contentPadding = PaddingValues(horizontal = dimens.screenPadding),
            horizontalArrangement = Arrangement.spacedBy(dimens.spaceMedium),
            userScrollEnabled = false
        ) {
            items(HomeDimens.SkeletonFriends) {
                Column(modifier = Modifier.width(HomeDimens.FriendRing), horizontalAlignment = Alignment.CenterHorizontally) {
                    Box(modifier = Modifier.size(HomeDimens.FriendRing).skeleton(CircleShape))
                    Box(
                        modifier = Modifier
                            .padding(top = HomeDimens.FriendNameTop)
                            .size(HomeDimens.SkeletonFriendName, HomeDimens.SkeletonLineHeight)
                            .skeleton(MaterialTheme.shapes.extraSmall)
                    )
                }
            }
        }
    }
}

// The name of a shelf as a bar and a row of cards with two lines under each, like the placeholder of the shelves of Wavvy
@Composable
private fun ShelfSkeleton(shape: CardShape) {
    val dimens = KaisouTheme.dimens
    val width = if (shape == CardShape.Poster) HomeDimens.PosterWidth else HomeDimens.WideWidth
    val ratio = if (shape == CardShape.Poster) HomeDimens.PosterRatio else HomeDimens.WideRatio

    SkeletonHost(modifier = Modifier.fillMaxWidth()) {
        Box(
            modifier = Modifier
                .padding(horizontal = dimens.screenPadding)
                .size(HomeDimens.SkeletonShelfTitleWidth, HomeDimens.SkeletonShelfTitleHeight)
                .skeleton(MaterialTheme.shapes.extraSmall)
        )
        LazyRow(
            contentPadding = PaddingValues(horizontal = dimens.screenPadding),
            horizontalArrangement = Arrangement.spacedBy(dimens.spaceSmall),
            userScrollEnabled = false,
            modifier = Modifier.padding(top = dimens.spaceSmall)
        ) {
            items(HomeDimens.SkeletonCards) {
                Column(modifier = Modifier.width(width)) {
                    Box(modifier = Modifier.fillMaxWidth().aspectRatio(ratio).skeleton(MaterialTheme.shapes.medium))
                    Box(
                        modifier = Modifier
                            .padding(top = dimens.spaceSmall)
                            .fillMaxWidth(HomeDimens.SkeletonTitleFraction)
                            .height(HomeDimens.SkeletonLineHeight)
                            .skeleton(MaterialTheme.shapes.extraSmall)
                    )
                    Box(
                        modifier = Modifier
                            .padding(top = dimens.spaceExtraSmall)
                            .fillMaxWidth(HomeDimens.SkeletonDetailsFraction)
                            .height(HomeDimens.SkeletonLineHeight)
                            .skeleton(MaterialTheme.shapes.extraSmall)
                    )
                }
            }
        }
    }
}
