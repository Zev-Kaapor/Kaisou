package com.kaisou.app.features.title.ui.components

// Compose layouts and foundations
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
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
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.style.TextOverflow
// Image loading
import coil3.compose.AsyncImage
// Project resources
import com.kaisou.app.R
import com.kaisou.app.core.designsystem.icons.KaisouIcons
import com.kaisou.app.core.browser.InAppBrowser
import com.kaisou.app.core.designsystem.theme.KaisouTheme
import com.kaisou.app.features.home.data.CardUi
import com.kaisou.app.features.home.ui.components.CardRating
import com.kaisou.app.features.home.ui.components.rememberCardRating
import com.kaisou.app.features.home.ui.HomeDimens
import com.kaisou.app.features.home.ui.components.TitleCard
import com.kaisou.app.features.title.data.MangaCover
import com.kaisou.app.features.title.data.TitlePerson
import com.kaisou.app.features.title.ui.TitleDimens
import androidx.compose.foundation.shape.RoundedCornerShape
import com.kaisou.app.features.home.ui.components.CornerTab
import com.kaisou.app.features.home.ui.components.rememberScoreFormat

// A row of titles that slides, as the cards of the Home, each one opens the page of its title. It can start at a card that is not the
// first, like the place of this title in its series
@Composable
fun CardsBlock(title: Int, cards: List<CardUi>, onOpenTitle: (Int) -> Unit, startIndex: Int = 0, onSeeAll: (() -> Unit)? = null, simple: Boolean = false) {
    val dimens = KaisouTheme.dimens

    Column(modifier = Modifier.fillMaxWidth().padding(top = TitleDimens.BlockTop)) {
        BlockTitle(text = stringResource(title), modifier = Modifier.padding(horizontal = dimens.screenPadding))
        LazyRow(
            state = rememberLazyListState(initialFirstVisibleItemIndex = startIndex),
            contentPadding = PaddingValues(horizontal = dimens.screenPadding),
            horizontalArrangement = Arrangement.spacedBy(dimens.spaceSmall),
            modifier = Modifier.padding(top = dimens.spaceSmall)
        ) {
            items(cards, key = { it.anilistId }) { card ->
                if (simple) {
                    SimpleCard(card, onClick = { onOpenTitle(card.anilistId) })
                } else {
                    TitleCard(card, HomeDimens.PosterWidth, HomeDimens.PosterRatio, showWide = false, onClick = { onOpenTitle(card.anilistId) }, detailsLines = 2)
                }
            }
            if (onSeeAll != null) {
                item(key = "see-all") {
                    Column(
                        modifier = Modifier
                            .width(HomeDimens.PosterWidth)
                            .aspectRatio(HomeDimens.PosterRatio)
                            .clip(MaterialTheme.shapes.medium)
                            .background(KaisouTheme.colors.elevated)
                            .clickable(role = Role.Button, onClick = onSeeAll),
                        verticalArrangement = Arrangement.Center,
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Icon(imageVector = KaisouIcons.ChevronRight, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
                        Text(text = stringResource(R.string.cast_see_all), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            }
        }
    }
}

// A title in the style of the people: its picture, its name under it and, under that, what it is in the story, in two lines so it is not cut.
// The age rating is at the start of the line under the name, and there is no score over the picture, since what matters here is how it is
// tied to the title that is open
@Composable
private fun SimpleCard(card: CardUi, onClick: () -> Unit) {
    val dimens = KaisouTheme.dimens

    Column(modifier = Modifier.width(HomeDimens.PosterWidth).clickable(role = Role.Button, onClick = onClick)) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(HomeDimens.PosterRatio)
                .clip(MaterialTheme.shapes.medium)
                .background(KaisouTheme.colors.skeleton)
        ) {
            card.imageUrl?.let { AsyncImage(model = it, contentDescription = null, contentScale = ContentScale.Crop, modifier = Modifier.matchParentSize()) }
            // Only what the user gave it is shown, with the person that says it is theirs
            card.userScore?.let { userScore ->
                val format = rememberScoreFormat()
                CornerTab(shape = RoundedCornerShape(topStart = HomeDimens.TabCorner), modifier = Modifier.align(Alignment.BottomEnd)) {
                    Text(text = format.format(userScore), style = MaterialTheme.typography.labelSmall, color = KaisouTheme.colors.onAccentFill)
                    Icon(
                        imageVector = KaisouIcons.PersonFilled,
                        contentDescription = stringResource(R.string.cd_your_score),
                        tint = KaisouTheme.colors.onAccentFill,
                        modifier = Modifier.size(HomeDimens.TabStar)
                    )
                }
            }
        }
        Text(
            text = card.title,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onBackground,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.padding(top = dimens.spaceExtraSmall)
        )
        // The rating of the title at the start of the line, looked for only while the card is on the screen, with its room kept meanwhile
        val rating = rememberCardRating(card.rating)
        val showRating = !rating.loaded || rating.rating != null
        // The room of two lines is kept under the line, so the shelf does not change its height when a card with two lines comes by,
        // while what is in it stays at the top as it is with one line
        val detailsStyle = MaterialTheme.typography.labelSmall.copy(fontSize = HomeDimens.CardDetailsSize)
        val detailsRoom = with(LocalDensity.current) { (detailsStyle.lineHeight * 2).toDp() + HomeDimens.CardDetailsTop }
        Box(modifier = Modifier.heightIn(min = detailsRoom)) {
            Row(
                modifier = Modifier.padding(top = HomeDimens.CardDetailsTop),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(HomeDimens.CardDetailsGap)
            ) {
                if (showRating) CardRating(rating.rating)
                Text(
                    text = card.details,
                    style = detailsStyle,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f)
                )
            }
        }
    }
}

// How many lines of what they do and of who voices them fit under the name of a person
private const val PersonLines = 2

// A row of people that slides, the characters with their voices or the staff with what they did, each one opens its page. When there is a way
// to see all of them it is the last card of the row
@Composable
fun PeopleBlock(title: Int, people: List<TitlePerson>, onOpen: (TitlePerson) -> Unit, onSeeAll: (() -> Unit)? = null, trailing: (@Composable () -> Unit)? = null) {
    val dimens = KaisouTheme.dimens

    Column(modifier = Modifier.fillMaxWidth().padding(top = TitleDimens.BlockTop)) {
        Row(modifier = Modifier.fillMaxWidth().padding(horizontal = dimens.screenPadding), verticalAlignment = Alignment.CenterVertically) {
            BlockTitle(text = stringResource(title), modifier = Modifier.weight(1f))
            trailing?.invoke()
        }
        LazyRow(
            contentPadding = PaddingValues(horizontal = dimens.screenPadding),
            horizontalArrangement = Arrangement.spacedBy(dimens.spaceSmall),
            modifier = Modifier.padding(top = dimens.spaceSmall)
        ) {
            items(people, key = { it.url }) { person ->
                Column(
                    modifier = Modifier
                        .width(TitleDimens.PersonWidth)
                        .clickable(role = Role.Button) { onOpen(person) }
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .aspectRatio(TitleDimens.PersonRatio)
                            .clip(MaterialTheme.shapes.medium)
                            .background(KaisouTheme.colors.skeleton)
                    ) {
                        person.image?.let {
                            AsyncImage(model = it, contentDescription = null, contentScale = ContentScale.Crop, modifier = Modifier.matchParentSize())
                        }
                    }
                    Text(
                        text = person.name,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onBackground,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.padding(top = dimens.spaceExtraSmall)
                    )
                    // What they do in the title, and who voices a character, in two lines together whose room is always kept, so the row
                    // does not change its height when a person with more lines comes by
                    val lineHeight = MaterialTheme.typography.labelSmall.lineHeight
                    val room = with(LocalDensity.current) { (lineHeight * PersonLines).toDp() }
                    Column(modifier = Modifier.heightIn(min = room)) {
                        person.role?.let { role ->
                            Text(
                                text = role,
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                maxLines = if (person.voice != null) 1 else PersonLines,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                        person.voice?.let { voice ->
                            Text(
                                text = voice,
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                }
            }
            if (onSeeAll != null) {
                item(key = "see-all") {
                    Column(
                        modifier = Modifier
                            .width(TitleDimens.PersonWidth)
                            .aspectRatio(TitleDimens.PersonRatio)
                            .clip(MaterialTheme.shapes.medium)
                            .background(KaisouTheme.colors.elevated)
                            .clickable(role = Role.Button, onClick = onSeeAll),
                        verticalArrangement = Arrangement.Center,
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Icon(imageVector = KaisouIcons.ChevronRight, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
                        Text(
                            text = stringResource(R.string.cast_see_all),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }
    }
}

// A round picture of a person, the gray of the placeholders under it until it arrives
@Composable
fun RoundImage(url: String?, size: androidx.compose.ui.unit.Dp, modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .width(size)
            .aspectRatio(1f)
            .clip(CircleShape)
            .background(KaisouTheme.colors.skeleton),
        contentAlignment = Alignment.Center
    ) {
        url?.let { AsyncImage(model = it, contentDescription = null, contentScale = ContentScale.Crop, modifier = Modifier.matchParentSize()) }
    }
}

// The covers of the volumes of a manga in a row that slides, each with its volume and its language under it, and a tap opens the picture
// whole in the browser that stays over the app
@Composable
fun CoversBlock(covers: List<MangaCover>) {
    val dimens = KaisouTheme.dimens
    val context = LocalContext.current
    val separator = stringResource(R.string.home_card_separator)

    Column(modifier = Modifier.fillMaxWidth().padding(top = TitleDimens.BlockTop)) {
        BlockTitle(text = stringResource(R.string.title_section_covers), modifier = Modifier.padding(horizontal = dimens.screenPadding))
        LazyRow(
            contentPadding = PaddingValues(horizontal = dimens.screenPadding),
            horizontalArrangement = Arrangement.spacedBy(dimens.spaceSmall),
            modifier = Modifier.padding(top = dimens.spaceSmall)
        ) {
            items(covers, key = { it.original }) { cover ->
                Column(
                    modifier = Modifier.width(TitleDimens.CoverWidth).clickable(role = Role.Button) { InAppBrowser.open(context, cover.original) },
                    verticalArrangement = Arrangement.spacedBy(dimens.spaceExtraSmall)
                ) {
                    AsyncImage(
                        model = cover.thumbnail,
                        contentDescription = null,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier
                            .fillMaxWidth()
                            .aspectRatio(TitleDimens.CoverRatio)
                            .clip(MaterialTheme.shapes.medium)
                            .background(KaisouTheme.colors.skeleton)
                    )
                    Text(
                        text = listOfNotNull(cover.volume?.let { stringResource(R.string.title_cover_volume, it) }, cover.language.uppercase().takeIf { it.isNotBlank() })
                            .joinToString(separator),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
        }
    }
}
