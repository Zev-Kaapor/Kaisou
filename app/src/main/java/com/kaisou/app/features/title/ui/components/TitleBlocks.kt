package com.kaisou.app.features.title.ui.components

// Compose layouts and foundations
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
// Material 3 components
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
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.style.TextOverflow
// Image loading
import coil3.compose.AsyncImage
// Project resources
import com.kaisou.app.R
import com.kaisou.app.core.browser.InAppBrowser
import com.kaisou.app.core.designsystem.theme.KaisouTheme
import com.kaisou.app.features.home.data.CardUi
import com.kaisou.app.features.home.ui.HomeDimens
import com.kaisou.app.features.home.ui.components.TitleCard
import com.kaisou.app.features.title.data.MangaCover
import com.kaisou.app.features.title.data.TitlePerson
import com.kaisou.app.features.title.ui.TitleDimens

// A row of titles that slides, as the cards of the Home, each one opens the page of its title. It can start at a card that is not the
// first, like the place of this title in its series
@Composable
fun CardsBlock(title: Int, cards: List<CardUi>, onOpenTitle: (Int) -> Unit, startIndex: Int = 0) {
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
                TitleCard(card, HomeDimens.PosterWidth, HomeDimens.PosterRatio, showWide = false, onClick = { onOpenTitle(card.anilistId) })
            }
        }
    }
}

// A row of people that slides, the characters with their voices or the staff with what they did, each one opens its page on AniList in the
// browser that stays over the app
@Composable
fun PeopleBlock(title: Int, people: List<TitlePerson>) {
    val dimens = KaisouTheme.dimens
    val context = LocalContext.current

    Column(modifier = Modifier.fillMaxWidth().padding(top = TitleDimens.BlockTop)) {
        BlockTitle(text = stringResource(title), modifier = Modifier.padding(horizontal = dimens.screenPadding))
        LazyRow(
            contentPadding = PaddingValues(horizontal = dimens.screenPadding),
            horizontalArrangement = Arrangement.spacedBy(dimens.spaceSmall),
            modifier = Modifier.padding(top = dimens.spaceSmall)
        ) {
            items(people, key = { it.url }) { person ->
                Column(
                    modifier = Modifier
                        .width(TitleDimens.PersonWidth)
                        .clickable(role = Role.Button) { InAppBrowser.open(context, person.url) }
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
                    // What they do in the title, and who voices a character
                    listOfNotNull(person.role, person.voice).forEach { line ->
                        Text(
                            text = line,
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
