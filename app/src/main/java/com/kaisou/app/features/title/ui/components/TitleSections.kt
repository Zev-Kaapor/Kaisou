package com.kaisou.app.features.title.ui.components

// Android framework
import android.text.format.DateUtils
// Compose layouts and foundations
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
// Material 3 components
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.PlainTooltip
import androidx.compose.material3.TooltipAnchorPosition
import androidx.compose.material3.TooltipBox
import androidx.compose.material3.TooltipDefaults
import androidx.compose.material3.rememberTooltipState
import androidx.compose.runtime.produceState
import com.kaisou.app.core.language.AppLanguage
import com.kaisou.app.core.translation.GoogleTranslator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
// Compose state and runtime
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
// UI styling and utilities
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalWindowInfo
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
// Image loading
import coil3.compose.AsyncImage
// Java networking
import java.net.URLEncoder
import java.util.Locale
import java.time.format.DateTimeFormatter
import java.time.ZoneId
import java.text.Collator
import java.time.Instant
// Project resources
import com.kaisou.app.R
import com.kaisou.app.core.browser.InAppBrowser
import com.kaisou.app.core.designsystem.components.ComponentDimens
import com.kaisou.app.core.designsystem.components.SkeletonHost
import com.kaisou.app.core.designsystem.components.SkeletonLines
import com.kaisou.app.core.designsystem.components.skeleton
import com.kaisou.app.core.designsystem.icons.KaisouIcons
import com.kaisou.app.core.designsystem.theme.KaisouTheme
import com.kaisou.app.features.home.data.CardUi
import com.kaisou.app.features.home.ui.HomeDimens
import com.kaisou.app.features.home.ui.components.CornerTab
import com.kaisou.app.features.title.data.Load
import com.kaisou.app.features.title.data.MangaCover
import com.kaisou.app.features.title.data.NextAiring
import com.kaisou.app.core.language.TagOrder
import com.kaisou.app.core.language.TagOrderPreference
import com.kaisou.app.features.title.data.DatumHelp
import com.kaisou.app.features.title.data.TitleDatum
import com.kaisou.app.features.title.data.TitleDetails
import com.kaisou.app.features.title.data.TitleLink
import com.kaisou.app.features.title.data.TitleNews
import com.kaisou.app.features.title.data.TitleRanking
import com.kaisou.app.features.title.data.TitleRelation
import com.kaisou.app.features.title.data.TitleWatchOrder
import com.kaisou.app.features.title.data.TitlePeople
import com.kaisou.app.features.home.ui.components.CardTextSkeleton
import com.kaisou.app.features.home.ui.components.SpoilerText
import com.kaisou.app.features.profile.data.ProfileRef
import com.kaisou.app.features.title.data.TitlePerson
import com.kaisou.app.features.title.data.TitleTag
import com.kaisou.app.features.title.data.TitleTheme
import com.kaisou.app.features.title.data.TitleTrailer
import com.kaisou.app.features.title.data.TitleUi
import com.kaisou.app.features.title.ui.TitleDimens

// The tabs of the page of a title
enum class TitleTab { Details, Episodes, Community }

// A small pill with a fact or a name, in the quiet surface of the app. The color of the words and of the pill can be asked for
@Composable
internal fun Chip(
    text: String,
    modifier: Modifier = Modifier,
    onClick: (() -> Unit)? = null,
    leading: (@Composable () -> Unit)? = null,
    textColor: Color = MaterialTheme.colorScheme.onSurface,
    container: Color = KaisouTheme.colors.elevated
) {
    Row(
        modifier = modifier
            .clip(CircleShape)
            .background(container)
            .then(if (onClick != null) Modifier.clickable(role = Role.Button, onClick = onClick) else Modifier)
            .padding(horizontal = TitleDimens.ChipHorizontal, vertical = TitleDimens.ChipVertical),
        horizontalArrangement = Arrangement.spacedBy(KaisouTheme.dimens.spaceExtraSmall),
        verticalAlignment = Alignment.CenterVertically
    ) {
        leading?.invoke()
        Text(
            text = text,
            style = MaterialTheme.typography.bodySmall,
            color = textColor,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}

// The facts of the title that matter to who is deciding, in a row that slides: where it is, how long it is, when it came out and who made it
@Composable
fun TitleFacts(title: TitleUi, modifier: Modifier = Modifier) {
    val dimens = KaisouTheme.dimens
    val separator = stringResource(R.string.home_card_separator)
    val length = buildList {
        if (title.manga) {
            title.chapters?.let { add(pluralStringResource(R.plurals.home_card_chapters, it, it)) }
            title.volumes?.let { add(pluralStringResource(R.plurals.title_volumes, it, it)) }
        } else {
            title.episodes?.let { add(pluralStringResource(R.plurals.home_card_episodes, it, it)) }
            title.duration?.let { add(stringResource(R.string.title_duration_minutes, it)) }
        }
    }.joinToString(separator)
    val facts = listOfNotNull(
        title.releaseState?.let { stringResource(it.label) },
        length.takeIf { it.isNotEmpty() },
        title.seasonText.takeUnless { title.manga },
        title.studio
    )

    if (facts.isEmpty()) return
    LazyRow(
        modifier = modifier.padding(top = TitleDimens.SynopsisTop),
        contentPadding = PaddingValues(horizontal = dimens.screenPadding),
        horizontalArrangement = Arrangement.spacedBy(dimens.spaceSmall)
    ) {
        items(facts) { Chip(text = it) }
    }
}

// The next episode of a title that is airing and when it comes, as the phone says it, like in 6 days
@Composable
fun NextEpisodeCard(next: NextAiring, modifier: Modifier = Modifier) {
    val dimens = KaisouTheme.dimens
    val relative = remember(next.airingAt) {
        DateUtils.getRelativeTimeSpanString(next.airingAt * MillisPerSecond, System.currentTimeMillis(), DateUtils.MINUTE_IN_MILLIS).toString()
    }

    Row(
        modifier = modifier
            .padding(horizontal = dimens.screenPadding)
            .padding(top = dimens.spaceMedium)
            .fillMaxWidth()
            .clip(MaterialTheme.shapes.medium)
            .background(KaisouTheme.colors.elevated)
            .padding(horizontal = dimens.spaceLarge, vertical = dimens.spaceMedium),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = stringResource(R.string.title_next_episode, next.episode),
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.onSurface
        )
        Text(
            text = relative,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.primary
        )
    }
}

private const val MillisPerSecond = 1000L

// The tabs under the facts, the one that is on has the name in white and a line in the accent under it, over a thin line that goes all the
// way across
@Composable
fun TitleTabs(labels: List<String>, selected: Int, onSelect: (Int) -> Unit, modifier: Modifier = Modifier) {
    val dimens = KaisouTheme.dimens

    Column(modifier = modifier.padding(top = TitleDimens.BlockTop).fillMaxWidth()) {
        // Each tab takes the same share of the width, so all of them are on the screen with their name in the middle
        Row(modifier = Modifier.padding(horizontal = dimens.screenPadding)) {
            labels.forEachIndexed { index, label ->
                val on = index == selected
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .height(TitleDimens.TabHeight)
                        .clickable(role = Role.Tab) { onSelect(index) },
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = label,
                        style = MaterialTheme.typography.labelLarge,
                        color = if (on) MaterialTheme.colorScheme.onBackground else MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    if (on) {
                        Box(
                            modifier = Modifier
                                .align(Alignment.BottomCenter)
                                .fillMaxWidth()
                                .height(TitleDimens.TabIndicator)
                                .background(MaterialTheme.colorScheme.primary)
                        )
                    }
                }
            }
        }
        HorizontalDivider(thickness = dimens.hairline, color = MaterialTheme.colorScheme.outline)
    }
}

// The blocks of the tab of the details, one under the other: the places the title has among the best, the prequel and the sequel, the data,
// the tags, the trailer, the titles that are tied to this one, the whole series in order, the characters, the staff, what people
// recommend and the links. Each part comes in apart, so while one is not there a placeholder takes its place and the others show
fun LazyListScope.titleDetailsItems(
    details: Load<TitleDetails>,
    watchOrder: Load<TitleWatchOrder>,
    manga: Boolean,
    people: Load<TitlePeople>,
    recommendations: Load<List<CardUi>>,
    themes: Load<List<TitleTheme>>,
    news: Load<List<TitleNews>>,
    covers: List<MangaCover>,
    onOpenTitle: (Int) -> Unit,
    onRetryExtras: () -> Unit,
    onRetryNews: () -> Unit,
    onOpenNews: () -> Unit,
    onOpenProfile: (ProfileRef) -> Unit,
    onOpenCast: (staff: Boolean) -> Unit,
    onRetryWatchOrder: () -> Unit
) {
    when (details) {
        is Load.Done -> {
            val value = details.value
            if (value.rankings.isNotEmpty()) item(key = "details-rankings") { RankingsBlock(value.rankings) }
            // The next episode is in the card on top, the ones after it are here
            if (value.schedule.size > 1) item(key = "details-schedule") { ScheduleBlock(value.schedule) }
            if (value.data.isNotEmpty()) item(key = "details-data") { DataBlock(value.data, onOpenProfile) }
            if (value.tags.isNotEmpty()) item(key = "details-tags") { TagsBlock(value.tags) }
            val prequel = value.relations.firstOrNull { it.type == PrequelType }
            val sequel = value.relations.firstOrNull { it.type == SequelType }
            if (prequel != null || sequel != null) item(key = "details-series") { SeriesBlock(prequel, sequel, onOpenTitle) }
            value.trailer?.let { trailer -> item(key = "details-trailer") { TrailerBlock(trailer) } }
            (themes as? Load.Done)?.value?.takeIf { it.isNotEmpty() }?.let { list -> item(key = "details-themes") { ThemesBlock(list) } }
            // The news come right under the openings and the endings, and are left out while they are on the way
            when {
                news is Load.Done && news.value.isNotEmpty() -> item(key = "details-news") { NewsBlock(news.value, onOpenAll = onOpenNews) }
                news is Load.Failed -> item(key = "details-news-error") { DetailsError(onRetry = onRetryNews) }
            }
            if (covers.isNotEmpty()) item(key = "details-covers") { CoversBlock(covers) }
            if (value.relations.isNotEmpty()) {
                item(key = "details-relations") { CardsBlock(R.string.title_section_relations, value.relations.map { it.card }, onOpenTitle, simple = true) }
            }
        }
        is Load.Failed -> item(key = "details-error") { DetailsError(onRetry = onRetryExtras) }
        else -> item(key = "details-loading") { DetailsInfoSkeleton() }
    }
    when (watchOrder) {
        is Load.Done -> if (watchOrder.value.cards.size > 1) {
            item(key = "details-watch-order") {
                CardsBlock(if (manga) R.string.title_section_read_order else R.string.title_section_watch_order, watchOrder.value.cards, onOpenTitle, startIndex = watchOrder.value.current, simple = true)
            }
        }
        is Load.Failed -> item(key = "watch-order-error") { DetailsError(onRetry = onRetryWatchOrder) }
        else -> item(key = "watch-order-loading") { CardsRowSkeleton() }
    }
    when (people) {
        is Load.Done -> {
            // The rows end with a link to all of them when what came is a full page, so there may be more
            val open: (TitlePerson) -> Unit = { person -> person.ref?.let(onOpenProfile) }
            if (people.value.characters.isNotEmpty()) {
                item(key = "details-characters") {
                    PeopleBlock(R.string.title_section_characters, people.value.characters, open, onSeeAll = { onOpenCast(false) }.takeIf { people.value.characters.size >= PeoplePage }, trailing = { DubLanguageMenu() })
                }
            }
            if (people.value.staff.isNotEmpty()) {
                item(key = "details-staff") {
                    PeopleBlock(R.string.title_section_staff, people.value.staff, open, onSeeAll = { onOpenCast(true) }.takeIf { people.value.staff.size >= PeoplePage })
                }
            }
        }
        is Load.Failed -> item(key = "people-error") { DetailsError(onRetry = onRetryExtras) }
        else -> item(key = "people-loading") { PeopleRowSkeleton() }
    }
    when (recommendations) {
        is Load.Done -> if (recommendations.value.isNotEmpty()) {
            item(key = "details-recommendations") { CardsBlock(R.string.title_section_recommendations, recommendations.value, onOpenTitle, simple = true) }
        }
        is Load.Failed -> item(key = "recommendations-error") { DetailsError(onRetry = onRetryExtras) }
        else -> item(key = "recommendations-loading") { CardsRowSkeleton() }
    }
    if (details is Load.Done) {
        val watch = details.value.links.filter { it.streaming }
        val others = details.value.links.filterNot { it.streaming }
        if (watch.isNotEmpty()) item(key = "details-watch") { LinksBlock(title = R.string.title_section_watch, links = watch) }
        if (others.isNotEmpty()) item(key = "details-links") { LinksBlock(title = R.string.title_section_links, links = others) }
    }
}

// The kinds of tie that have a card of their own at the top
private const val PrequelType = "PREQUEL"
private const val SequelType = "SEQUEL"

// The name of a block
@Composable
internal fun BlockTitle(text: String, modifier: Modifier = Modifier) {
    Text(
        text = text,
        style = MaterialTheme.typography.titleMedium,
        color = MaterialTheme.colorScheme.onBackground,
        modifier = modifier
    )
}

// The places the title has among the best, as pills with a star: #81 most popular of all time
@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun RankingsBlock(rankings: List<TitleRanking>) {
    val dimens = KaisouTheme.dimens

    FlowRow(
        modifier = Modifier.fillMaxWidth().padding(horizontal = dimens.screenPadding).padding(top = TitleDimens.SynopsisTop),
        horizontalArrangement = Arrangement.spacedBy(dimens.spaceSmall),
        verticalArrangement = Arrangement.spacedBy(dimens.spaceSmall)
    ) {
        rankings.forEach { ranking ->
            Chip(
                text = ranking.text,
                leading = {
                    Icon(
                        imageVector = KaisouIcons.Star,
                        contentDescription = null,
                        tint = rankColor(ranking.rank),
                        modifier = Modifier.size(TitleDimens.LinkIcon)
                    )
                }
            )
        }
    }
}

// The color of the star of a place: gold, silver and bronze for the first three, one for the top 10, one for the top 50, one for the top
// 100, and the violet of the app for the rest
@Composable
private fun rankColor(rank: Int): Color {
    val colors = KaisouTheme.colors
    return when {
        rank == 1 -> colors.medalGold
        rank == 2 -> colors.medalSilver
        rank == 3 -> colors.medalBronze
        rank <= RankTopTen -> colors.rankTopTen
        rank <= RankTopFifty -> colors.rankTopFifty
        rank <= RankTopHundred -> colors.rankTopHundred
        else -> MaterialTheme.colorScheme.primary
    }
}

private const val RankTopTen = 10
private const val RankTopFifty = 50
private const val RankTopHundred = 100

// The prequel and the sequel as two wide pictures side by side, each opens its page, with the name of the tie in the tab of the cards at
// the top corner and the title at the bottom. When there is only one it keeps the height of one half, a little longer, in the middle
@Composable
private fun SeriesBlock(prequel: TitleRelation?, sequel: TitleRelation?, onOpenTitle: (Int) -> Unit) {
    val dimens = KaisouTheme.dimens
    val landscape = LocalWindowInfo.current.containerSize.let { it.width > it.height }

    // Lying down the block is as wide as it can be and stays in the middle, and standing up it takes all the width
    Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.TopCenter) {
        Row(
            modifier = Modifier
                .then(if (landscape) Modifier.widthIn(max = TitleDimens.SeriesLandscapeMax) else Modifier)
                .fillMaxWidth()
                .padding(horizontal = dimens.screenPadding)
                .padding(top = TitleDimens.BlockTop),
            horizontalArrangement = Arrangement.spacedBy(dimens.spaceSmall, Alignment.CenterHorizontally)
        ) {
            val relations = listOfNotNull(prequel, sequel)
            relations.forEach { relation ->
                run {
                    Box(
                        modifier = Modifier
                            .then(if (relations.size == 1) Modifier.fillMaxWidth(SeriesAloneFraction) else Modifier.weight(1f))
                            .aspectRatio(if (relations.size == 1) TrailerRatio * SeriesAloneFraction / SeriesHalfFraction else TrailerRatio)
                            .clip(MaterialTheme.shapes.medium)
                            .background(KaisouTheme.colors.skeleton)
                            .clickable(role = Role.Button) { onOpenTitle(relation.card.anilistId) }
                    ) {
                        relation.image?.let {
                            AsyncImage(model = it, contentDescription = null, contentScale = ContentScale.Crop, modifier = Modifier.matchParentSize())
                        }
                        Box(
                            modifier = Modifier
                                .matchParentSize()
                                .background(Brush.verticalGradient(listOf(Color.Transparent, KaisouTheme.colors.veil.copy(alpha = TitleDimens.SeriesScrimAlpha))))
                        )
                        CornerTab(shape = RoundedCornerShape(bottomEnd = HomeDimens.TabCorner), modifier = Modifier.align(Alignment.TopStart)) {
                            Text(text = relation.label, style = MaterialTheme.typography.labelSmall, color = KaisouTheme.colors.onAccentFill)
                        }
                        Text(
                            text = relation.card.title,
                            style = MaterialTheme.typography.bodySmall,
                            color = KaisouTheme.colors.onMedia,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.align(Alignment.BottomStart).padding(dimens.spaceSmall)
                        )
                    }
                }
            }
        }
    }
}

// How much of the width the prequel or the sequel takes when it is alone, and when it has the other beside it
private const val SeriesAloneFraction = 0.75f
private const val SeriesHalfFraction = 0.5f

// The openings and the endings, a line each with which one it is, the song and who sings it, and the episodes it plays in
@Composable
private fun ThemesBlock(themes: List<TitleTheme>) {
    val dimens = KaisouTheme.dimens
    val context = LocalContext.current
    val separator = stringResource(R.string.home_card_separator)

    Column(modifier = Modifier.fillMaxWidth().padding(horizontal = dimens.screenPadding).padding(top = TitleDimens.BlockTop)) {
        BlockTitle(stringResource(R.string.title_section_themes))
        themes.forEach { theme ->
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = dimens.spaceSmall)
                    // The song is looked for on YouTube, with who sings it, in the browser that stays over the app
                    .clickable(role = Role.Button) { InAppBrowser.open(context, themeSearch(theme)) },
                horizontalArrangement = Arrangement.spacedBy(dimens.spaceMedium),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = theme.label,
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.width(TitleDimens.ThemeLabelWidth)
                )
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = theme.song ?: theme.label,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onBackground,
                        textDecoration = TextDecoration.Underline,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    val line = listOfNotNull(
                        theme.artist,
                        theme.episodes?.let { stringResource(R.string.title_theme_episodes, it) }
                    ).joinToString(separator)
                    if (line.isNotEmpty()) {
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

// The address of the search for a song on YouTube, by its name and who sings it
private fun themeSearch(theme: TitleTheme): String =
    "https://www.youtube.com/results?search_query=" + URLEncoder.encode(listOfNotNull(theme.song, theme.artist).joinToString(" "), Charsets.UTF_8.name())

// The order of the tags in use, which opens the menu of the two: alphabetical and by the percentage. What is chosen is kept on the phone
@Composable
private fun TagOrderMenu(selected: TagOrder) {
    val dimens = KaisouTheme.dimens
    var open by remember { mutableStateOf(false) }
    val labels = mapOf(TagOrder.Alphabetical to R.string.tag_order_alphabetical, TagOrder.Percentage to R.string.tag_order_percentage)
    val icons = mapOf(TagOrder.Alphabetical to KaisouIcons.SortAlphabetical, TagOrder.Percentage to KaisouIcons.Percent)

    Box {
        Row(
            modifier = Modifier
                .clip(CircleShape)
                .background(KaisouTheme.colors.elevated)
                .clickable(role = Role.DropdownList) { open = true }
                .padding(start = dimens.spaceMedium, end = dimens.spaceSmall, top = dimens.spaceExtraSmall, bottom = dimens.spaceExtraSmall),
            horizontalArrangement = Arrangement.spacedBy(dimens.spaceExtraSmall),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(imageVector = icons.getValue(selected), contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(TitleDimens.SortIcon))
            Text(text = stringResource(labels.getValue(selected)), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurface)
            Icon(imageVector = KaisouIcons.ChevronDown, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(TitleDimens.SortIcon))
        }
        DropdownMenu(
            expanded = open,
            onDismissRequest = { open = false },
            containerColor = MaterialTheme.colorScheme.surfaceContainer,
            shape = MaterialTheme.shapes.large
        ) {
            TagOrder.entries.forEach { order ->
                DropdownMenuItem(
                    leadingIcon = {
                        Icon(
                            imageVector = icons.getValue(order),
                            contentDescription = null,
                            tint = if (order == selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(TitleDimens.ListIcon)
                        )
                    },
                    text = {
                        Text(
                            text = stringResource(labels.getValue(order)),
                            style = MaterialTheme.typography.bodyMedium,
                            color = if (order == selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onBackground
                        )
                    },
                    trailingIcon = if (order == selected) {
                        { Icon(imageVector = KaisouIcons.Check, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(TitleDimens.ListIcon)) }
                    } else {
                        null
                    },
                    onClick = {
                        open = false
                        TagOrderPreference.set(order)
                    }
                )
            }
        }
    }
}

// A small question mark next to the name of a datum: a touch opens the page that explains it and pressing and holding says in a few words
// what it is
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun HintIcon(help: DatumHelp) {
    val context = LocalContext.current

    TooltipBox(
        positionProvider = TooltipDefaults.rememberTooltipPositionProvider(TooltipAnchorPosition.Above, TitleDimens.TagTipGap),
        tooltip = { PlainTooltip { Text(text = help.text) } },
        state = rememberTooltipState()
    ) {
        Icon(
            imageVector = KaisouIcons.QuestionCircle,
            contentDescription = stringResource(R.string.cd_help),
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier
                .padding(start = TitleDimens.HintGap)
                .size(TitleDimens.HintIcon)
                .clip(CircleShape)
                .clickable(role = Role.Button) { InAppBrowser.open(context, help.url) }
        )
    }
}

// The episodes that are still to air, each with the day and the hour it airs in the time of the phone and how long until it
@Composable
private fun ScheduleBlock(schedule: List<NextAiring>) {
    val dimens = KaisouTheme.dimens
    val format = remember { DateTimeFormatter.ofPattern(SchedulePattern, Locale.getDefault()).withZone(ZoneId.systemDefault()) }

    Column(modifier = Modifier.fillMaxWidth().padding(horizontal = dimens.screenPadding).padding(top = TitleDimens.BlockTop)) {
        BlockTitle(stringResource(R.string.title_schedule))
        schedule.forEach { next ->
            Row(modifier = Modifier.fillMaxWidth().padding(top = TitleDimens.DataLineTop), verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = stringResource(R.string.title_schedule_row, next.episode, format.format(Instant.ofEpochSecond(next.airingAt))),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onBackground,
                    modifier = Modifier.weight(1f)
                )
                Text(
                    text = DateUtils.getRelativeTimeSpanString(next.airingAt * 1000L, System.currentTimeMillis(), DateUtils.MINUTE_IN_MILLIS).toString(),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.primary
                )
            }
        }
    }
}

private const val SchedulePattern = "EEE, d MMM · HH:mm"

// How many people a page of the characters or of the staff has
private const val PeoplePage = 20

// The data of the title in lines: what it is on the left and what it says on the right, one under the other when it says several things
@Composable
internal fun DataBlock(data: List<TitleDatum>, onOpen: (ProfileRef) -> Unit = {}) {
    val dimens = KaisouTheme.dimens
    val context = LocalContext.current

    Column(modifier = Modifier.fillMaxWidth().padding(horizontal = dimens.screenPadding).padding(top = TitleDimens.BlockTop)) {
        BlockTitle(stringResource(R.string.title_section_data))
        data.forEach { datum ->
            Row(modifier = Modifier.fillMaxWidth().padding(top = TitleDimens.DataLineTop)) {
                Row(modifier = Modifier.fillMaxWidth(TitleDimens.DataLabelFraction), verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = datum.label,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    datum.help?.let { HintIcon(it) }
                }
                Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(dimens.spaceExtraSmall)) {
                    datum.values.forEachIndexed { index, value ->
                        val ref = datum.refs.getOrNull(index)
                        val url = datum.urls.getOrNull(index)
                        if (url != null) {
                            // A value that opens an address in the browser that stays over the app, in the violet
                            Text(
                                text = value,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.clickable(role = Role.Button) { InAppBrowser.open(context, url) }
                            )
                        } else if (ref != null) {
                            // A name that opens its page, in the violet
                            Text(
                                text = value,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.clickable(role = Role.Button) { onOpen(ref) }
                            )
                        } else if ("~!" in value) {
                            SpoilerText(
                                text = value,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onBackground,
                                linkColor = MaterialTheme.colorScheme.primary,
                                headingSize = MaterialTheme.typography.bodySmall.fontSize
                            )
                        } else {
                            Text(text = value, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onBackground)
                        }
                    }
                }
            }
        }
    }
}

// All the tags with how much of each the title has, in alphabetical order. The ones that give away the story are covered, like the spoilers
// of the text of AniList, and each one shows itself, in the accent, when it is touched
@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun TagsBlock(tags: List<TitleTag>) {
    val dimens = KaisouTheme.dimens
    var shown by rememberSaveable { mutableStateOf(listOf<String>()) }
    val order by TagOrderPreference.order.collectAsState()
    val collator = remember { Collator.getInstance(Locale.getDefault()) }
    val sorted = remember(tags, order) {
        if (order == TagOrder.Percentage) tags.sortedByDescending { it.rank } else tags.sortedWith(compareBy(collator) { it.name })
    }

    Column(modifier = Modifier.fillMaxWidth().padding(horizontal = dimens.screenPadding).padding(top = TitleDimens.BlockTop)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            BlockTitle(stringResource(R.string.title_section_tags), modifier = Modifier.weight(1f))
            TagOrderMenu(order)
        }
        FlowRow(
            modifier = Modifier.padding(top = dimens.spaceSmall),
            horizontalArrangement = Arrangement.spacedBy(dimens.spaceSmall),
            verticalArrangement = Arrangement.spacedBy(dimens.spaceSmall)
        ) {
            sorted.forEach { tag ->
                val covered = tag.spoiler && tag.name !in shown
                val chip: @Composable () -> Unit = {
                    Chip(
                        text = "${tag.name} ${tag.rank}%",
                        // A covered tag keeps its room, with its words in the color of the cover
                        textColor = if (covered) Color.Transparent else if (tag.spoiler) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface,
                        container = if (covered) KaisouTheme.colors.skeleton else KaisouTheme.colors.elevated,
                        onClick = if (covered) { { shown = shown + tag.name } } else null
                    )
                }
                // What a tag is says in a tip over it while it is pressed, and a covered one does not give itself away
                if (covered || tag.description.isNullOrBlank()) chip() else TagTip(tag.description, chip)
            }
        }
    }
}

// The words of a tag over it while it is pressed, in the language of the app: the ones of AniList show first and are changed when the
// translation arrives
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun TagTip(description: String, content: @Composable () -> Unit) {
    TooltipBox(
        positionProvider = TooltipDefaults.rememberTooltipPositionProvider(TooltipAnchorPosition.Above, TitleDimens.TagTipGap),
        tooltip = {
            val words by produceState(description, description) {
                if (AppLanguage.needsTranslation) {
                    GoogleTranslator.translate(description, AppLanguage.translationCode)?.let { value = it }
                }
            }
            PlainTooltip { Text(text = words) }
        },
        state = rememberTooltipState()
    ) { content() }
}

// The picture of the trailer with a play over it. A trailer of YouTube plays right there, and when it cannot, or it is of another site, it
// opens on the site that has it, in the browser that stays over the app
@Composable
private fun TrailerBlock(trailer: TitleTrailer) {
    val dimens = KaisouTheme.dimens
    val context = LocalContext.current
    var playing by remember { mutableStateOf(false) }
    val videoId = trailer.youtubeId
    val landscape = LocalWindowInfo.current.containerSize.let { it.width > it.height }

    // Lying down the block is as wide as it can be and stays in the middle, and standing up it takes all the width
    Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.TopCenter) {
        Column(
            modifier = Modifier
                .then(if (landscape) Modifier.widthIn(max = TitleDimens.TrailerLandscapeMax) else Modifier)
                .fillMaxWidth()
                .padding(horizontal = dimens.screenPadding)
                .padding(top = TitleDimens.BlockTop)
        ) {
            BlockTitle(stringResource(R.string.title_section_trailer))
            Box(
                modifier = Modifier
                    .padding(top = dimens.spaceSmall)
                    .fillMaxWidth()
                    .aspectRatio(TrailerRatio)
                    .clip(MaterialTheme.shapes.medium)
                    .background(KaisouTheme.colors.skeleton)
                    .then(
                        if (playing && videoId != null) {
                            Modifier
                        } else {
                            Modifier.clickable(role = Role.Button) { if (videoId != null) playing = true else InAppBrowser.open(context, trailer.url) }
                        }
                    ),
                contentAlignment = Alignment.Center
            ) {
                if (playing && videoId != null) {
                    TrailerPlayer(
                        videoId = videoId,
                        onFailed = {
                            playing = false
                            InAppBrowser.open(context, trailer.url)
                        }
                    )
                    return@Box
                }
                trailer.thumbnail?.let {
                    AsyncImage(model = it, contentDescription = null, contentScale = ContentScale.Crop, modifier = Modifier.matchParentSize())
                }
                if (videoId != null) {
                    // A video of YouTube has its own mark over it
                    Image(
                        painter = painterResource(R.drawable.ic_youtube),
                        contentDescription = stringResource(R.string.cd_trailer),
                        modifier = Modifier.size(TitleDimens.TrailerYoutube)
                    )
                } else {
                    Box(
                        modifier = Modifier
                            .size(TitleDimens.TrailerPlay)
                            .clip(CircleShape)
                            .background(KaisouTheme.colors.veil.copy(alpha = TitleDimens.TrailerPlayAlpha)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = KaisouIcons.PlayOutline,
                            contentDescription = stringResource(R.string.cd_trailer),
                            tint = KaisouTheme.colors.onMedia,
                            modifier = Modifier.size(TitleDimens.TrailerPlayIcon)
                        )
                    }
                }
            }
        }
    }
}

private const val TrailerRatio = 16f / 9f

// The links to other sites, each one opens in the browser that stays over the app
@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun LinksBlock(title: Int, links: List<TitleLink>) {
    val dimens = KaisouTheme.dimens
    val context = LocalContext.current

    Column(modifier = Modifier.fillMaxWidth().padding(horizontal = dimens.screenPadding).padding(top = TitleDimens.BlockTop)) {
        BlockTitle(stringResource(title))
        FlowRow(
            modifier = Modifier.padding(top = dimens.spaceSmall),
            horizontalArrangement = Arrangement.spacedBy(dimens.spaceSmall),
            verticalArrangement = Arrangement.spacedBy(dimens.spaceSmall)
        ) {
            links.forEach { link ->
                Chip(
                    text = link.site,
                    onClick = { InAppBrowser.open(context, link.url) },
                    leading = {
                        Icon(
                            imageVector = KaisouIcons.ExternalLink,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(TitleDimens.LinkIcon)
                        )
                    }
                )
            }
        }
    }
}

// The placeholder of the details, a name and some lines, with the light of the others
@Composable
internal fun DetailsSkeleton() {
    val dimens = KaisouTheme.dimens

    SkeletonHost(modifier = Modifier.fillMaxWidth().padding(horizontal = dimens.screenPadding).padding(top = TitleDimens.BlockTop)) {
        Box(modifier = Modifier.fillMaxWidth(DetailsSkeletonTitle).height(KaisouTheme.dimens.spaceLarge).skeleton())
        SkeletonLines(lines = TitleDimens.DetailsSkeletonLines, modifier = Modifier.padding(top = dimens.spaceMedium))
    }
}

private const val DetailsSkeletonTitle = 0.3f

// The placeholder of the data and the tags: the lines of data with a name at the start and a value, then the name of the tags with the
// button of the order at its end and some pills
@Composable
internal fun DetailsInfoSkeleton() {
    val dimens = KaisouTheme.dimens
    val values = listOf(0.5f, 0.3f, 0.6f, 0.4f, 0.5f, 0.35f)

    SkeletonHost(modifier = Modifier.fillMaxWidth().padding(horizontal = dimens.screenPadding).padding(top = TitleDimens.BlockTop)) {
        Box(modifier = Modifier.fillMaxWidth(DetailsSkeletonTitle).height(dimens.spaceLarge).skeleton())
        values.forEach { value ->
            Row(modifier = Modifier.fillMaxWidth().padding(top = TitleDimens.DataLineTop), verticalAlignment = Alignment.CenterVertically) {
                Box(modifier = Modifier.fillMaxWidth(TitleDimens.DataLabelFraction)) {
                    Box(modifier = Modifier.fillMaxWidth(DataNameFraction).height(ComponentDimens.SkeletonLineHeight).skeleton())
                }
                Box(modifier = Modifier.fillMaxWidth(value).height(ComponentDimens.SkeletonLineHeight).skeleton())
            }
        }
        Row(modifier = Modifier.fillMaxWidth().padding(top = TitleDimens.BlockTop), verticalAlignment = Alignment.CenterVertically) {
            Box(modifier = Modifier.weight(1f)) {
                Box(modifier = Modifier.fillMaxWidth(DetailsSkeletonTitle).height(dimens.spaceLarge).skeleton())
            }
            Box(modifier = Modifier.size(TitleDimens.SkeletonSortWidth, TitleDimens.SkeletonSortHeight).skeleton(CircleShape))
        }
        Row(modifier = Modifier.padding(top = dimens.spaceSmall), horizontalArrangement = Arrangement.spacedBy(dimens.spaceSmall)) {
            TitleDimens.SkeletonPills.forEach { width -> Box(modifier = Modifier.size(width, TitleDimens.SkeletonPillHeight).skeleton(CircleShape)) }
        }
    }
}

private const val DataNameFraction = 0.6f

// The placeholder of what is under the hero before the details: the synopsis with its name and some lines, the strip of facts as pills and
// the tabs with the line under them
@Composable
internal fun TitleBodySkeleton() {
    val dimens = KaisouTheme.dimens

    SkeletonHost(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(horizontal = dimens.screenPadding).padding(top = TitleDimens.SynopsisTop)) {
            Box(modifier = Modifier.fillMaxWidth(DetailsSkeletonTitle).height(dimens.spaceLarge).skeleton())
            SkeletonLines(lines = TitleDimens.SynopsisLines, modifier = Modifier.padding(top = dimens.spaceSmall))
            Row(modifier = Modifier.padding(top = dimens.spaceMedium), horizontalArrangement = Arrangement.spacedBy(dimens.spaceSmall)) {
                TitleDimens.SkeletonFacts.forEach { width -> Box(modifier = Modifier.size(width, TitleDimens.SkeletonPillHeight).skeleton(CircleShape)) }
            }
        }
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = dimens.screenPadding).padding(top = TitleDimens.BlockTop),
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {
            repeat(TabsSkeletonCount) { Box(modifier = Modifier.size(TitleDimens.SkeletonTabWidth, dimens.spaceLarge).skeleton()) }
        }
    }
}

private const val TabsSkeletonCount = 3

// The placeholder of a row of cards that slide, with the name of the block, as the ones of the relations, of the order and of what is
// recommended: a picture, a bar for the name and under it the square of the rating with a bar for the line
@Composable
internal fun CardsRowSkeleton() {
    val dimens = KaisouTheme.dimens

    SkeletonHost(modifier = Modifier.fillMaxWidth().padding(top = TitleDimens.BlockTop)) {
        Box(modifier = Modifier.padding(horizontal = dimens.screenPadding).fillMaxWidth(DetailsSkeletonTitle).height(dimens.spaceLarge).skeleton())
        LazyRow(
            contentPadding = PaddingValues(horizontal = dimens.screenPadding),
            horizontalArrangement = Arrangement.spacedBy(dimens.spaceSmall),
            userScrollEnabled = false,
            modifier = Modifier.padding(top = dimens.spaceSmall)
        ) {
            items(HomeDimens.SkeletonCards) {
                Column(modifier = Modifier.width(HomeDimens.PosterWidth)) {
                    Box(modifier = Modifier.fillMaxWidth().aspectRatio(HomeDimens.PosterRatio).skeleton(MaterialTheme.shapes.medium))
                    CardTextSkeleton()
                }
            }
        }
    }
}

// The placeholder of the row of friends: the name of the block and round pictures with a bar for the name and one for the state under each
@Composable
internal fun FriendsRowSkeleton() {
    val dimens = KaisouTheme.dimens

    SkeletonHost(modifier = Modifier.fillMaxWidth().padding(top = TitleDimens.BlockTop)) {
        Box(modifier = Modifier.padding(horizontal = dimens.screenPadding).fillMaxWidth(DetailsSkeletonTitle).height(dimens.spaceLarge).skeleton())
        LazyRow(
            contentPadding = PaddingValues(horizontal = dimens.screenPadding),
            horizontalArrangement = Arrangement.spacedBy(dimens.spaceSmall),
            userScrollEnabled = false,
            modifier = Modifier.padding(top = dimens.spaceSmall)
        ) {
            items(HomeDimens.SkeletonFriends) {
                Column(modifier = Modifier.width(TitleDimens.FriendWidth), horizontalAlignment = Alignment.CenterHorizontally) {
                    Box(modifier = Modifier.size(TitleDimens.FriendAvatar).skeleton(CircleShape))
                    Box(
                        modifier = Modifier
                            .padding(top = TitleDimens.FriendBadgeOffset + dimens.spaceExtraSmall)
                            .fillMaxWidth(HomeDimens.SkeletonTitleFraction)
                            .height(ComponentDimens.SkeletonLineHeight)
                            .skeleton()
                    )
                    Box(
                        modifier = Modifier
                            .padding(top = dimens.spaceExtraSmall)
                            .fillMaxWidth(HomeDimens.SkeletonDetailsFraction)
                            .height(ComponentDimens.SkeletonLineHeight)
                            .skeleton()
                    )
                }
            }
        }
    }
}

// The placeholder of a row of people that slide, with the name of the block: a picture, a bar for the name and one for what they do
@Composable
internal fun PeopleRowSkeleton() {
    val dimens = KaisouTheme.dimens

    SkeletonHost(modifier = Modifier.fillMaxWidth().padding(top = TitleDimens.BlockTop)) {
        Box(modifier = Modifier.padding(horizontal = dimens.screenPadding).fillMaxWidth(DetailsSkeletonTitle).height(dimens.spaceLarge).skeleton())
        LazyRow(
            contentPadding = PaddingValues(horizontal = dimens.screenPadding),
            horizontalArrangement = Arrangement.spacedBy(dimens.spaceSmall),
            userScrollEnabled = false,
            modifier = Modifier.padding(top = dimens.spaceSmall)
        ) {
            items(HomeDimens.SkeletonCards) {
                Column(modifier = Modifier.width(TitleDimens.PersonWidth)) {
                    Box(modifier = Modifier.fillMaxWidth().aspectRatio(TitleDimens.PersonRatio).skeleton(MaterialTheme.shapes.medium))
                    Box(
                        modifier = Modifier
                            .padding(top = dimens.spaceExtraSmall)
                            .fillMaxWidth(HomeDimens.SkeletonTitleFraction)
                            .height(ComponentDimens.SkeletonLineHeight)
                            .skeleton()
                    )
                    Box(
                        modifier = Modifier
                            .padding(top = dimens.spaceExtraSmall)
                            .fillMaxWidth(HomeDimens.SkeletonDetailsFraction)
                            .height(ComponentDimens.SkeletonLineHeight)
                            .skeleton()
                    )
                }
            }
        }
    }
}

// Shown when the details could not be asked, a tap asks again
@Composable
internal fun DetailsError(onRetry: () -> Unit) {
    val dimens = KaisouTheme.dimens

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onRetry)
            .padding(dimens.screenPadding)
            .padding(top = TitleDimens.BlockTop),
        verticalArrangement = Arrangement.spacedBy(dimens.spaceSmall),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = stringResource(R.string.title_details_error),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Text(
            text = stringResource(R.string.home_retry),
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.primary
        )
    }
}
