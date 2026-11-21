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
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.offset
import androidx.compose.runtime.remember
import androidx.compose.ui.res.stringArrayResource
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.withStyle
import com.kaisou.app.core.auth.AniListSession
import com.kaisou.app.features.title.data.TitleFriend
import com.kaisou.app.features.title.data.TitleUi
import com.kaisou.app.core.language.DubLanguage
import com.kaisou.app.core.designsystem.components.shine
import androidx.compose.foundation.layout.Spacer
import androidx.compose.ui.graphics.vector.ImageVector

// A row of titles that slides, as the cards of the Home, each one opens the page of its title. It can start at a card that is not the
// first, like the place of this title in its series
@Composable
fun CardsBlock(
    title: Int,
    cards: List<CardUi>,
    onOpenTitle: (Int) -> Unit,
    startIndex: Int = 0,
    onSeeAll: (() -> Unit)? = null,
    simple: Boolean = false,
    shineCurrent: Boolean = false
) {
    val dimens = KaisouTheme.dimens
    // With a page for all of them only a few are shown, around the one of the page when it is in the series, and the last card says there are more
    val from = if (onSeeAll != null) (startIndex - 2).coerceIn(0, (cards.size - TitleDimens.CardsShown).coerceAtLeast(0)) else 0
    val shown = if (onSeeAll != null) cards.drop(from).take(TitleDimens.CardsShown) else cards

    Column(modifier = Modifier.fillMaxWidth().padding(top = TitleDimens.BlockTop)) {
        Row(modifier = Modifier.fillMaxWidth().padding(horizontal = dimens.screenPadding), verticalAlignment = Alignment.CenterVertically) {
            BlockTitle(text = stringResource(title), modifier = Modifier.weight(1f))
            if (onSeeAll != null) SeeAllArrow(onSeeAll)
        }
        LazyRow(
            state = rememberLazyListState(initialFirstVisibleItemIndex = (startIndex - from).coerceAtLeast(0)),
            contentPadding = PaddingValues(horizontal = dimens.screenPadding),
            horizontalArrangement = Arrangement.spacedBy(dimens.spaceSmall),
            modifier = Modifier.padding(top = dimens.spaceSmall)
        ) {
            items(shown, key = { it.anilistId }) { card ->
                if (simple) {
                    SimpleCard(card, onClick = { onOpenTitle(card.anilistId) }, shine = shineCurrent && card.anilistId == cards.getOrNull(startIndex)?.anilistId)
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
internal fun SimpleCard(card: CardUi, onClick: () -> Unit, badge: String? = null, shine: Boolean = false) {
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
            // The place of the title in the series, when the cards are the order to take it
            badge?.let {
                CornerTab(shape = RoundedCornerShape(bottomEnd = HomeDimens.TabCorner), modifier = Modifier.align(Alignment.TopStart)) {
                    Text(text = it, style = MaterialTheme.typography.labelSmall, color = KaisouTheme.colors.onAccentFill)
                }
            }
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
                    modifier = Modifier.weight(1f).shine(shine)
                )
            }
        }
    }
}

// How many lines of what they do and of who voices them fit under the name of a person
private const val PersonLines = 2

// The button that opens a sheet to pick one of some: a capsule with its fill, the icon of what it picks when it has one, the one in use and the arrow
@Composable
fun PickerPill(text: String, onClick: () -> Unit, modifier: Modifier = Modifier, icon: ImageVector? = null) {
    val dimens = KaisouTheme.dimens

    Row(
        modifier = modifier
            .clip(CircleShape)
            .background(KaisouTheme.colors.elevated)
            .clickable(role = Role.DropdownList, onClick = onClick)
            .padding(start = dimens.spaceMedium, end = dimens.spaceSmall, top = dimens.spaceExtraSmall, bottom = dimens.spaceExtraSmall),
        horizontalArrangement = Arrangement.spacedBy(dimens.spaceExtraSmall),
        verticalAlignment = Alignment.CenterVertically
    ) {
        icon?.let { Icon(imageVector = it, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(TitleDimens.SortIcon)) }
        Text(text = text, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurface)
        Icon(imageVector = KaisouIcons.ChevronDown, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(TitleDimens.SortIcon))
    }
}

// The arrow at the end of the title of a block, which opens the page with all of what the block has
@Composable
private fun SeeAllArrow(onClick: () -> Unit) {
    Box(
        modifier = Modifier
            .size(TitleDimens.SeeAllTouch)
            .clip(CircleShape)
            .clickable(role = Role.Button, onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = KaisouIcons.ChevronRight,
            contentDescription = stringResource(R.string.cast_see_all),
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.size(TitleDimens.SeeAllIcon)
        )
    }
}

// A row of people that slides, the characters with their voices or the staff with what they did, each one opens its page. When there is a way
// to see all of them it is the last card of the row
@Composable
fun PeopleBlock(title: Int, people: List<TitlePerson>, onOpen: (TitlePerson) -> Unit, onSeeAll: (() -> Unit)? = null, trailing: (@Composable () -> Unit)? = null) {
    val dimens = KaisouTheme.dimens

    Column(modifier = Modifier.fillMaxWidth().padding(top = TitleDimens.BlockTop)) {
        Row(modifier = Modifier.fillMaxWidth().padding(horizontal = dimens.screenPadding), verticalAlignment = Alignment.CenterVertically) {
            BlockTitle(text = stringResource(title), modifier = Modifier.weight(1f))
            trailing?.let {
                it()
                if (onSeeAll != null) Spacer(modifier = Modifier.width(dimens.spaceSmall))
            }
            if (onSeeAll != null) SeeAllArrow(onSeeAll)
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
                                maxLines = if (person.voice != null || person.language != null) 1 else PersonLines,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                        // The voice in the original language of the title, and for a person that voices a character the language of the voice
                        (person.voice ?: person.language?.let { DubLanguage.nameOf(it) })?.let { line ->
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

// The friends that have the title in a row that slides, the user first. Each has the picture with the score they gave on it, the name, where
// the title is on their list and how far they are. A tap opens their page on AniList
@Composable
internal fun FriendsBlock(title: TitleUi) {
    val dimens = KaisouTheme.dimens
    val viewer = remember { AniListSession.viewer }
    val keys = stringArrayResource(R.array.title_list_keys)
    val names = stringArrayResource(if (title.manga) R.array.title_list_names_manga else R.array.title_list_names_anime)
    val you = stringResource(R.string.title_friend_you)
    val total = (if (title.manga) title.chapters else title.episodes)?.takeIf { it > 0 }
    val entry = title.entry
    // The user is the first of them when the title is on their list
    val me = if (entry != null && viewer != null) {
        TitleFriend(
            name = you,
            avatar = viewer.avatar,
            status = names.getOrNull(keys.indexOf(entry.status.apiName)).orEmpty(),
            progress = entry.progress,
            score = entry.scoreOutOfTen.takeIf { it > 0f },
            url = "https://anilist.co/user/${viewer.name}",
            isMe = true
        )
    } else {
        null
    }
    val everyone = listOfNotNull(me) + title.friends

    Column(modifier = Modifier.fillMaxWidth().padding(top = TitleDimens.BlockTop)) {
        BlockTitle(text = stringResource(R.string.title_section_friends), modifier = Modifier.padding(horizontal = dimens.screenPadding))
        LazyRow(
            contentPadding = PaddingValues(horizontal = dimens.screenPadding),
            horizontalArrangement = Arrangement.spacedBy(dimens.spaceSmall),
            modifier = Modifier.padding(top = dimens.spaceSmall)
        ) {
            items(everyone, key = { it.url }) { friend -> FriendItem(friend = friend, total = total, manga = title.manga) }
        }
    }
}

// One friend: the picture with the score on its bottom, a ring in the accent when it is the user, the name, the state on the list and how far
// they are, which says what it counts like the cards do
@Composable
private fun FriendItem(friend: TitleFriend, total: Int?, manga: Boolean) {
    val dimens = KaisouTheme.dimens
    val context = LocalContext.current
    val format = rememberScoreFormat()
    val accent = MaterialTheme.colorScheme.primary
    val separator = stringResource(R.string.home_card_progress_separator)
    val unknown = stringResource(R.string.home_card_progress_unknown)
    val unit = stringResource(if (manga) R.string.home_card_unit_chapters else R.string.home_card_unit_episodes)
    val progress = buildAnnotatedString {
        friend.progress?.let { done ->
            withStyle(SpanStyle(color = accent)) { append(done.toString()) }
            append(separator)
            append(total?.toString() ?: unknown)
            append(" ")
            append(unit)
        }
    }

    Column(
        modifier = Modifier
            .width(TitleDimens.FriendWidth)
            .clickable(role = Role.Button) { InAppBrowser.open(context, friend.url) },
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(contentAlignment = Alignment.BottomCenter) {
            RoundImage(
                url = friend.avatar,
                size = TitleDimens.FriendAvatar,
                modifier = if (friend.isMe) Modifier.border(TitleDimens.FriendRing, accent, CircleShape) else Modifier
            )
            friend.score?.let { score ->
                CornerTab(shape = CircleShape, modifier = Modifier.offset(y = TitleDimens.FriendBadgeOffset)) {
                    Text(text = format.format(score), style = MaterialTheme.typography.labelSmall, color = KaisouTheme.colors.onAccentFill)
                    Icon(
                        imageVector = KaisouIcons.Star,
                        contentDescription = null,
                        tint = KaisouTheme.colors.onAccentFill,
                        modifier = Modifier.size(HomeDimens.TabStar)
                    )
                }
            }
        }
        Text(
            text = friend.name,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onBackground,
            textAlign = TextAlign.Center,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.padding(top = TitleDimens.FriendBadgeOffset + dimens.spaceExtraSmall)
        )
        Text(
            text = friend.status,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
        Text(
            text = progress,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}
