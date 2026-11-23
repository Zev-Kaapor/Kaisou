package com.kaisou.app.features.user.ui.components

// Compose gestures, canvas and layouts
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import android.content.ClipData
import android.content.ClipboardManager
import com.kaisou.app.core.browser.InAppBrowser
import com.kaisou.app.core.designsystem.components.Toasts
import com.kaisou.app.core.designsystem.icons.KaisouIcons
import com.kaisou.app.features.home.ui.components.ActivityUrl
import com.kaisou.app.features.title.ui.ActionButton
import com.kaisou.app.features.title.ui.LikeButton
import androidx.compose.foundation.lazy.LazyListScope
// Material 3 components
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
// Compose state and runtime
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
// UI styling and utilities
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.layout
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.withStyle
// Images
import coil3.compose.AsyncImage
// Java time and text
import java.text.NumberFormat
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.temporal.TemporalAdjusters
import java.time.temporal.WeekFields
import java.util.Locale
// Project resources
import com.kaisou.app.R
import com.kaisou.app.core.designsystem.theme.KaisouTheme
import com.kaisou.app.features.home.ui.HomeDimens
import com.kaisou.app.features.home.ui.components.MarkdownContent
import com.kaisou.app.features.home.ui.components.SpoilerText
import com.kaisou.app.features.profile.data.ProfileRef
import com.kaisou.app.features.title.ui.TitleDimens
import com.kaisou.app.features.title.ui.components.BlockTitle
import com.kaisou.app.features.title.ui.components.CardsBlock
import com.kaisou.app.features.title.ui.components.Chip
import com.kaisou.app.features.title.ui.components.PeopleBlock
import com.kaisou.app.features.title.ui.components.SeeAllArrow
import com.kaisou.app.features.title.ui.components.relativeTime
import com.kaisou.app.features.title.ui.components.rememberShortDay
import com.kaisou.app.features.user.data.ActivityDay
import com.kaisou.app.features.user.data.FavouriteKind
import com.kaisou.app.features.user.data.UserActivity
import com.kaisou.app.features.user.data.UserGenre
import com.kaisou.app.features.user.data.UserTotals
import com.kaisou.app.features.user.data.UserUi
import com.kaisou.app.features.user.ui.UserDimens

// The overview of a person: what they say of themselves, their totals, the genres they like, the days they were active, the last things they
// did and their favourites
@OptIn(ExperimentalLayoutApi::class)
fun LazyListScope.userOverviewItems(
    user: UserUi,
    recent: List<UserActivity>?,
    onOpenTitle: (Int) -> Unit,
    onOpenProfile: (ProfileRef) -> Unit,
    onOpenActivity: () -> Unit,
    onOpenFavourites: (FavouriteKind) -> Unit
) {
    if (user.about.isNotBlank()) item(key = "about") { AboutBlock(user.about) }
    if (user.anime.count > 0 || user.manga.count > 0) item(key = "totals") { TotalsBlock(user.anime, user.manga) }
    if (user.animeGenres.isNotEmpty() || user.mangaGenres.isNotEmpty()) item(key = "genres") { GenresBlock(user.animeGenres, user.mangaGenres) }
    if (user.activity.isNotEmpty()) item(key = "activity") { ActivityBlock(user.activity) }
    if (recent == null) item(key = "recent-loading") { UserRecentSkeleton() }
    else if (recent.isNotEmpty()) item(key = "recent") { RecentBlock(recent, onOpenTitle, onOpenActivity) }

    val favourites = user.favourites
    // A kind that has more than came has the arrow to its whole list
    fun seeAll(kind: FavouriteKind, shown: Int): (() -> Unit)? = { onOpenFavourites(kind) }.takeIf { kind in favourites.more || shown > TitleDimens.CardsShown }
    if (favourites.anime.isNotEmpty()) {
        item(key = "fav-anime") { CardsBlock(R.string.user_fav_anime, favourites.anime, onOpenTitle, onSeeAll = seeAll(FavouriteKind.Anime, favourites.anime.size)) }
    }
    if (favourites.manga.isNotEmpty()) {
        item(key = "fav-manga") { CardsBlock(R.string.user_fav_manga, favourites.manga, onOpenTitle, onSeeAll = seeAll(FavouriteKind.Manga, favourites.manga.size)) }
    }
    if (favourites.characters.isNotEmpty()) {
        item(key = "fav-characters") {
            PeopleBlock(
                R.string.user_fav_characters,
                favourites.characters.take(TitleDimens.PeopleShown),
                onOpen = { person -> person.ref?.let(onOpenProfile) },
                onSeeAll = seeAll(FavouriteKind.Characters, favourites.characters.size).takeIf { FavouriteKind.Characters in favourites.more || favourites.characters.size > TitleDimens.PeopleShown },
                keepRoom = false
            )
        }
    }
    if (favourites.staff.isNotEmpty()) {
        item(key = "fav-staff") {
            PeopleBlock(
                R.string.user_fav_staff,
                favourites.staff.take(TitleDimens.PeopleShown),
                onOpen = { person -> person.ref?.let(onOpenProfile) },
                onSeeAll = seeAll(FavouriteKind.Staff, favourites.staff.size).takeIf { FavouriteKind.Staff in favourites.more || favourites.staff.size > TitleDimens.PeopleShown },
                keepRoom = false
            )
        }
    }
    if (favourites.studios.isNotEmpty()) {
        item(key = "fav-studios") {
            val dimens = KaisouTheme.dimens
            Column(modifier = Modifier.fillMaxWidth().padding(horizontal = dimens.screenPadding).padding(top = TitleDimens.BlockTop)) {
                Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                    BlockTitle(text = stringResource(R.string.user_fav_studios), modifier = Modifier.weight(1f))
                    if (FavouriteKind.Studios in favourites.more) SeeAllArrow { onOpenFavourites(FavouriteKind.Studios) }
                }
                FlowRow(
                    modifier = Modifier.fillMaxWidth().padding(top = dimens.spaceSmall),
                    horizontalArrangement = Arrangement.spacedBy(dimens.spaceSmall),
                    verticalArrangement = Arrangement.spacedBy(dimens.spaceSmall)
                ) {
                    favourites.studios.forEach { studio -> Chip(text = studio.name, onClick = { onOpenProfile(studio.ref) }) }
                }
            }
        }
    }
}

// What the person says of themselves, all of it: it is the text they wrote for their page, so it is not cut
@Composable
private fun AboutBlock(text: String) {
    val dimens = KaisouTheme.dimens

    Column(modifier = Modifier.fillMaxWidth().padding(horizontal = dimens.screenPadding).padding(top = TitleDimens.BlockTop)) {
        MarkdownContent(text = text, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onBackground, hideSpoilers = true)
    }
}

// The totals of anime and of manga, one card for each with its name over three numbers: how many titles, how much of them and the mean score
@Composable
private fun TotalsBlock(anime: UserTotals, manga: UserTotals) {
    val dimens = KaisouTheme.dimens
    val whole = remember { NumberFormat.getIntegerInstance(Locale.getDefault()) }
    val decimal = remember { NumberFormat.getNumberInstance(Locale.getDefault()).apply { minimumFractionDigits = 1; maximumFractionDigits = 1 } }

    Column(
        modifier = Modifier.fillMaxWidth().padding(horizontal = dimens.screenPadding).padding(top = TitleDimens.BlockTop),
        verticalArrangement = Arrangement.spacedBy(dimens.spaceSmall)
    ) {
        if (anime.count > 0) {
            TotalsCard(
                R.string.user_total_anime to whole.format(anime.count),
                R.string.user_days_watched to decimal.format(anime.amount),
                R.string.user_mean_score to decimal.format(anime.meanScore),
                amount = anime.amount
            )
        }
        if (manga.count > 0) {
            TotalsCard(
                R.string.user_total_manga to whole.format(manga.count),
                R.string.user_chapters_read to whole.format(manga.amount.toLong()),
                R.string.user_mean_score to decimal.format(manga.meanScore),
                amount = manga.amount
            )
        }
    }
}

// A card as AniList has it: three numbers in the accent side by side, each over what it counts, and under them a darker strip with a scale
// and a bar that fills as far as the second number goes on it
@Composable
private fun TotalsCard(first: Pair<Int, String>, second: Pair<Int, String>, third: Pair<Int, String>, amount: Double) {
    val dimens = KaisouTheme.dimens
    val whole = remember { NumberFormat.getNumberInstance(Locale.getDefault()).apply { maximumFractionDigits = 1 } }
    val top = remember(amount) { scaleTop(amount) }

    Column(modifier = Modifier.fillMaxWidth().clip(MaterialTheme.shapes.medium).background(KaisouTheme.colors.elevated)) {
        Row(modifier = Modifier.fillMaxWidth().padding(vertical = dimens.spaceLarge)) {
            listOf(first, second, third).forEach { (label, value) ->
                Column(modifier = Modifier.weight(1f), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(dimens.spaceExtraSmall)) {
                    Text(text = value, style = MaterialTheme.typography.titleMedium, color = MaterialTheme.colorScheme.primary, maxLines = 1)
                    Text(text = stringResource(label), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 1, overflow = TextOverflow.Ellipsis)
                }
            }
        }
        Column(modifier = Modifier.fillMaxWidth().background(MaterialTheme.colorScheme.background.copy(alpha = UserDimens.ScaleShade))) {
            // The three marks of the scale sit in the middle of three equal parts, and the bar reaches the last one when the number is the top
            Row(modifier = Modifier.fillMaxWidth().padding(top = dimens.spaceSmall)) {
                ScaleMarks.forEach { share ->
                    Text(
                        text = whole.format(top * share),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.weight(1f)
                    )
                }
            }
            // A small line under each number, in the middle of its part, joins it to the bar
            Row(modifier = Modifier.fillMaxWidth().padding(top = dimens.spaceExtraSmall)) {
                ScaleMarks.forEach { _ ->
                    Box(modifier = Modifier.weight(1f), contentAlignment = Alignment.Center) {
                        Box(modifier = Modifier.width(UserDimens.ScaleTickWidth).height(UserDimens.ScaleTickHeight).background(MaterialTheme.colorScheme.onSurfaceVariant))
                    }
                }
            }
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(UserDimens.TotalsBarHeight)
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxHeight()
                        .fillMaxWidth((amount / top).toFloat().coerceIn(0f, 1f) * UserDimens.TotalsBarReach)
                        .clip(MaterialTheme.shapes.extraSmall)
                        .background(Brush.horizontalGradient(listOf(KaisouTheme.colors.avatarRingStart, KaisouTheme.colors.buttonEnd)))
                )
            }
        }
    }
}

// The marks of the scale as shares of its top
private val ScaleMarks = listOf(0.5, 0.75, 1.0)

// The top of the scale for a number: the first of 1, 2 and 5 times a power of ten that is not under it, like 100 for 99.6 and 10000 for 8981
private fun scaleTop(amount: Double): Double {
    var base = 1.0
    while (true) {
        for (step in listOf(1.0, 2.0, 5.0)) if (base * step >= amount) return base * step
        base *= 10.0
    }
}

// The genres of what the person watched or read, in a card: the first ones named, each in its color with how many titles it has, and under
// them a bar split in the pieces of the most of them. Anime and manga each have their own, both always in view
@Composable
private fun GenresBlock(anime: List<UserGenre>, manga: List<UserGenre>) {
    val dimens = KaisouTheme.dimens

    Column(modifier = Modifier.fillMaxWidth().padding(horizontal = dimens.screenPadding).padding(top = TitleDimens.BlockTop)) {
        BlockTitle(stringResource(R.string.user_section_genres))
        if (anime.isNotEmpty()) GenresCard(R.string.profile_type_anime, anime)
        if (manga.isNotEmpty()) GenresCard(R.string.profile_type_manga, manga)
    }
}

// One card of genres with the name of the kind over it
@Composable
private fun GenresCard(kind: Int, genres: List<UserGenre>) {
    val dimens = KaisouTheme.dimens
    val whole = remember { NumberFormat.getIntegerInstance(Locale.getDefault()) }
    val palette = KaisouTheme.colors.chart
    val titles = stringResource(R.string.user_titles)
    val pieces = genres.take(UserDimens.GenreBarPieces)
    val total = pieces.sumOf { it.count.coerceAtLeast(1) }.toFloat()
    // The piece of the bar that is held, which is said in a card of its color over the bar, as high as the tip of a tag, so the finger does not
    // cover it
    var held by remember { mutableStateOf<Int?>(null) }
    val gap = TitleDimens.TagTipGap

    Text(
        text = stringResource(kind),
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = Modifier.padding(top = dimens.spaceSmall)
    )
    Box(modifier = Modifier.padding(top = dimens.spaceExtraSmall).fillMaxWidth()) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(MaterialTheme.shapes.medium)
            .background(KaisouTheme.colors.elevated)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(dimens.spaceMedium),
            horizontalArrangement = Arrangement.spacedBy(dimens.spaceSmall)
        ) {
            genres.take(UserDimens.GenreNamed).forEachIndexed { index, genre ->
                val color = palette[index % palette.size]
                Column(modifier = Modifier.weight(1f), horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(dimens.spaceExtraSmall)) {
                    Text(
                        text = genre.name,
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.background,
                        textAlign = TextAlign.Center,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(MaterialTheme.shapes.small)
                            .background(color)
                            .padding(vertical = dimens.spaceExtraSmall, horizontal = dimens.spaceSmall)
                    )
                    Text(
                        text = buildAnnotatedString {
                            withStyle(SpanStyle(color = color)) { append(whole.format(genre.count)) }
                            append(" ")
                            append(titles)
                        },
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1
                    )
                }
            }
        }
        // Holding a piece of the bar says which genre it is and how many titles it has
        Row(modifier = Modifier.fillMaxWidth().height(UserDimens.GenreBarHeight)) {
            pieces.forEachIndexed { index, genre ->
                Box(
                    modifier = Modifier
                        .weight(genre.count.coerceAtLeast(1).toFloat())
                        .fillMaxHeight()
                        .background(palette[index % palette.size])
                        .pointerInput(index) { detectTapGestures(onPress = { held = index; tryAwaitRelease(); held = null }) }
                )
            }
        }
    }
    held?.let { index ->
        val genre = pieces[index]
        val center = (pieces.take(index).sumOf { it.count.coerceAtLeast(1) } + genre.count.coerceAtLeast(1) / 2f) / total
        Text(
            text = "${genre.name} \u00b7 ${whole.format(genre.count)}",
            style = MaterialTheme.typography.titleSmall,
            color = MaterialTheme.colorScheme.background,
            maxLines = 1,
            modifier = Modifier
                .align(Alignment.BottomStart)
                .layout { measurable, constraints ->
                    // Over the middle of the piece, kept inside the width of the card, and above the bar as far as the tip of a tag is
                    val label = measurable.measure(constraints.copy(minWidth = 0, minHeight = 0))
                    layout(0, 0) {
                        val x = (constraints.maxWidth * center - label.width / 2f).toInt().coerceIn(0, (constraints.maxWidth - label.width).coerceAtLeast(0))
                        label.place(x, -(label.height + gap.roundToPx() + UserDimens.GenreBarHeight.roundToPx()))
                    }
                }
                .clip(MaterialTheme.shapes.small)
                .background(palette[index % palette.size])
                .padding(vertical = dimens.spaceExtraSmall, horizontal = dimens.spaceMedium)
        )
    }
    }
}

// The days AniList keeps of the person, about half a year, as a grid of weeks in a card as wide as the page, each day painted as strong as it
// was busy, with the names of the months over it. A touch picks a day and the line over the grid says what happened on it
@Composable
private fun ActivityBlock(days: List<ActivityDay>) {
    val dimens = KaisouTheme.dimens
    val whole = remember { NumberFormat.getIntegerInstance(Locale.getDefault()) }
    val shortDay = rememberShortDay()
    val monthName = remember { DateTimeFormatter.ofPattern("MMM", Locale.getDefault()) }
    val today = remember { LocalDate.now() }
    // The grid starts on the first day of the week of the language, as many weeks back as it shows
    val start = remember { today.with(TemporalAdjusters.previousOrSame(WeekFields.of(Locale.getDefault()).firstDayOfWeek)).minusWeeks((UserDimens.ActivityWeeks - 1).toLong()) }
    val byDay = remember(days) { days.associateBy { it.date } }
    val total = remember(days) { days.filter { !it.date.isBefore(start) }.sumOf { it.amount } }
    // The weeks where a month begins carry its name, the first one only when the next month is not too close to it
    val months = remember(start) {
        (0 until UserDimens.ActivityWeeks).filter { week ->
            val month = start.plusWeeks(week.toLong()).month
            if (week == 0) start.plusWeeks(MonthRoom).month == month else start.plusWeeks(week - 1L).month != month
        }
    }
    var picked by remember(days) { mutableStateOf<LocalDate?>(null) }
    val empty = MaterialTheme.colorScheme.background
    val full = MaterialTheme.colorScheme.primary
    val outline = MaterialTheme.colorScheme.onBackground

    Column(modifier = Modifier.fillMaxWidth().padding(horizontal = dimens.screenPadding).padding(top = TitleDimens.BlockTop)) {
        BlockTitle(stringResource(R.string.user_section_activity))
        Column(
            modifier = Modifier
                .padding(top = dimens.spaceSmall)
                .fillMaxWidth()
                .clip(MaterialTheme.shapes.medium)
                .background(KaisouTheme.colors.elevated)
                .padding(dimens.spaceMedium)
        ) {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text(
                    text = pluralStringResource(R.plurals.user_activity_total, total, whole.format(total)),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                picked?.let { day ->
                    val amount = byDay[day]?.amount ?: 0
                    Text(
                        text = pluralStringResource(R.plurals.user_activity_day, amount, shortDay.format(day), whole.format(amount)),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            }
            BoxWithConstraints(modifier = Modifier.fillMaxWidth().padding(top = dimens.spaceSmall)) {
                val pitch = maxWidth / UserDimens.ActivityWeeks
                Column {
                    Box(modifier = Modifier.fillMaxWidth()) {
                        months.forEach { week ->
                            Text(
                                text = monthName.format(start.plusWeeks(week.toLong())),
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                maxLines = 1,
                                softWrap = false,
                                modifier = Modifier.offset(x = pitch * week)
                            )
                        }
                    }
                    Canvas(
                        modifier = Modifier
                            .padding(top = dimens.spaceExtraSmall)
                            .fillMaxWidth()
                            .aspectRatio(UserDimens.ActivityWeeks / UserDimens.ActivityDaysInWeek.toFloat())
                            .pointerInput(start) {
                                detectTapGestures { picked = dayAt(it, size.width.toFloat() / UserDimens.ActivityWeeks, start, today) }
                            }
                    ) {
                        val pitchPx = size.width / UserDimens.ActivityWeeks
                        val side = Size(pitchPx * (1f - UserDimens.ActivityGap), pitchPx * (1f - UserDimens.ActivityGap))
                        val corner = CornerRadius(side.width * UserDimens.ActivityCorner)
                        for (week in 0 until UserDimens.ActivityWeeks) {
                            for (row in 0 until UserDimens.ActivityDaysInWeek) {
                                val day = start.plusDays((week * UserDimens.ActivityDaysInWeek + row).toLong())
                                if (day.isAfter(today)) continue
                                val level = byDay[day]?.level ?: 0
                                val color = if (level <= 0) empty else lerp(empty, full, UserDimens.ActivityFloor + (1f - UserDimens.ActivityFloor) * (level / UserDimens.ActivityTopLevel).coerceAtMost(1f))
                                val at = Offset(week * pitchPx, row * pitchPx)
                                drawRoundRect(color = color, topLeft = at, size = side, cornerRadius = corner)
                                if (day == picked) drawRoundRect(color = outline, topLeft = at, size = side, cornerRadius = corner, style = Stroke(width = UserDimens.ActivityPickWidth.toPx()))
                            }
                        }
                    }
                }
            }
        }
    }
}

// How many weeks ahead the first month of the grid is looked at, to know whether its name has room
private const val MonthRoom = 3L

// The day under a touch on the grid, null when it is out of it or in the days that did not come yet
private fun dayAt(at: Offset, pitch: Float, start: LocalDate, today: LocalDate): LocalDate? {
    val week = (at.x / pitch).toInt()
    val row = (at.y / pitch).toInt()
    if (week !in 0 until UserDimens.ActivityWeeks || row !in 0 until UserDimens.ActivityDaysInWeek) return null
    return start.plusDays((week * UserDimens.ActivityDaysInWeek + row).toLong()).takeIf { !it.isAfter(today) }
}

// The last things the person did, with the arrow of the title that opens all of them
@Composable
private fun RecentBlock(recent: List<UserActivity>, onOpenTitle: (Int) -> Unit, onOpenActivity: () -> Unit) {
    val dimens = KaisouTheme.dimens

    Column(modifier = Modifier.fillMaxWidth().padding(horizontal = dimens.screenPadding).padding(top = TitleDimens.BlockTop)) {
        Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            BlockTitle(text = stringResource(R.string.user_section_recent), modifier = Modifier.weight(1f))
            SeeAllArrow(onOpenActivity)
        }
        recent.forEach { item -> UserActivityCard(item, onOpenTitle, modifier = Modifier.padding(top = dimens.spaceSmall)) }
    }
}

// What the buttons of an activity do, and whether the activities are the user's own, which they can delete
class ActivityActions(
    val isOwn: Boolean,
    val onLike: (Int) -> Unit,
    val onFollow: (Int) -> Unit,
    val onOpenReplies: (Int) -> Unit,
    val onDelete: (Int) -> Unit
)

// Something the person did, in a card: the cover of the title with what they did to it, which opens the title, or the beginning of a text
// they wrote, with when it was. When the actions are given there is a line under it with the bell, the menu, the comments and the heart
@Composable
internal fun UserActivityCard(item: UserActivity, onOpenTitle: (Int) -> Unit, modifier: Modifier = Modifier, actions: ActivityActions? = null) {
    val dimens = KaisouTheme.dimens
    val card = item.card
    val time = relativeTime(item.createdAt)

    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(MaterialTheme.shapes.medium)
            .background(KaisouTheme.colors.elevated)
            .padding(dimens.spaceSmall)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().clickable(enabled = card != null, role = Role.Button) { card?.let { onOpenTitle(it.anilistId) } },
            horizontalArrangement = Arrangement.spacedBy(dimens.spaceMedium),
            verticalAlignment = Alignment.CenterVertically
        ) {
            card?.let {
                AsyncImage(
                    model = it.imageUrl,
                    contentDescription = null,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier
                        .width(UserDimens.RecentCover)
                        .aspectRatio(HomeDimens.PosterRatio)
                        .clip(MaterialTheme.shapes.small)
                        .background(KaisouTheme.colors.skeleton)
                )
            }
            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(dimens.spaceExtraSmall)) {
                if (card != null) {
                    Text(text = item.text, style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Medium), color = MaterialTheme.colorScheme.onSurface, maxLines = 2, overflow = TextOverflow.Ellipsis)
                } else {
                    SpoilerText(
                        text = item.text,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurface,
                        linkColor = MaterialTheme.colorScheme.primary,
                        headingSize = MaterialTheme.typography.bodySmall.fontSize,
                        maxLines = TextLines
                    )
                }
                Text(text = time, style = MaterialTheme.typography.bodySmall.copy(fontWeight = FontWeight.SemiBold), color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
        if (actions != null) {
            Row(modifier = Modifier.fillMaxWidth().padding(top = dimens.spaceExtraSmall), horizontalArrangement = Arrangement.End, verticalAlignment = Alignment.CenterVertically) {
                ActionButton(
                    icon = if (item.isSubscribed) KaisouIcons.BellRinging else KaisouIcons.Bell,
                    description = stringResource(if (item.isSubscribed) R.string.cd_activity_unfollow else R.string.cd_activity_follow),
                    onClick = { actions.onFollow(item.id) },
                    active = item.isSubscribed
                )
                ActivityMenu(item.id, actions)
                CommentButton(count = item.replyCount, onClick = { actions.onOpenReplies(item.id) })
                LikeButton(liked = item.isLiked, count = item.likeCount, onClick = { actions.onLike(item.id) })
            }
        }
    }
}

// The three dots of an activity: the link to copy or to open, and, when it is the user's, deleting it
@Composable
private fun ActivityMenu(id: Int, actions: ActivityActions) {
    val context = LocalContext.current
    var open by remember { mutableStateOf(false) }
    val url = ActivityUrl + id
    val copied = stringResource(R.string.activity_link_copied)

    Box {
        ActionButton(KaisouIcons.More, stringResource(R.string.cd_thread_more), { open = true })
        DropdownMenu(
            expanded = open,
            onDismissRequest = { open = false },
            containerColor = MaterialTheme.colorScheme.surfaceContainer,
            shape = MaterialTheme.shapes.large
        ) {
            ActivityMenuLine(KaisouIcons.Copy, R.string.activity_copy_link, MaterialTheme.colorScheme.onSurfaceVariant) {
                open = false
                context.getSystemService(ClipboardManager::class.java)?.setPrimaryClip(ClipData.newPlainText(null, url))
                Toasts.show(message = copied, icon = KaisouIcons.Copy)
            }
            ActivityMenuLine(KaisouIcons.ExternalLink, R.string.story_open_anilist, MaterialTheme.colorScheme.onSurfaceVariant) {
                open = false
                InAppBrowser.open(context, url)
            }
            if (actions.isOwn) {
                ActivityMenuLine(KaisouIcons.Trash, R.string.story_delete, MaterialTheme.colorScheme.error) {
                    open = false
                    actions.onDelete(id)
                }
            }
        }
    }
}

// A line of the menu: its icon and its name, in the color it is given
@Composable
private fun ActivityMenuLine(icon: ImageVector, label: Int, tint: Color, onClick: () -> Unit) {
    DropdownMenuItem(
        leadingIcon = { Icon(imageVector = icon, contentDescription = null, tint = tint, modifier = Modifier.size(TitleDimens.ListIcon)) },
        text = { Text(text = stringResource(label), style = MaterialTheme.typography.bodyMedium, color = if (tint == MaterialTheme.colorScheme.error) tint else MaterialTheme.colorScheme.onBackground) },
        onClick = onClick
    )
}

// The bubble of the comments with how many there are, which opens them
@Composable
private fun CommentButton(count: Int, onClick: () -> Unit) {
    val dimens = KaisouTheme.dimens

    Row(
        modifier = Modifier
            .clip(CircleShape)
            .clickable(role = Role.Button, onClick = onClick)
            .padding(vertical = dimens.spaceExtraSmall, horizontal = dimens.spaceSmall),
        horizontalArrangement = Arrangement.spacedBy(dimens.spaceExtraSmall),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = KaisouIcons.Comment,
            contentDescription = stringResource(R.string.story_replies_title),
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.size(TitleDimens.ThreadActionIcon)
        )
        Text(text = count.toString(), style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

// How many lines of a text the person wrote show in its card
private const val TextLines = 4
