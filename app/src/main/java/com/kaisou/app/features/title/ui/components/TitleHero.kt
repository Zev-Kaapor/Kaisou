package com.kaisou.app.features.title.ui.components

// Compose animation
import androidx.annotation.StringRes
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.animateIntAsState
import androidx.compose.animation.core.tween
// Compose layouts and foundations
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsIgnoringVisibility
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.wrapContentHeight
// Material 3 components
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
// Compose state and runtime
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
// UI styling and utilities
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.withStyle
// Image loading
import coil3.compose.AsyncImage
import coil3.size.Size
// Java utilities
import java.text.NumberFormat
import java.util.Locale
import kotlin.math.roundToInt
// Project resources
import com.kaisou.app.R
import com.kaisou.app.core.artwork.artRequest
import com.kaisou.app.core.artwork.logoRequest
import com.kaisou.app.core.designsystem.components.AgeRatingBadge
import com.kaisou.app.core.designsystem.components.ButtonIcon
import com.kaisou.app.core.designsystem.components.ComponentDimens
import com.kaisou.app.core.designsystem.components.OutlineButton
import com.kaisou.app.core.designsystem.components.OutlineIconButton
import com.kaisou.app.core.designsystem.components.TitleLogo
import com.kaisou.app.core.designsystem.components.WeeklyReleaseBadge
import com.kaisou.app.core.designsystem.icons.KaisouIcons
import com.kaisou.app.core.designsystem.theme.KaisouMotion
import com.kaisou.app.core.designsystem.theme.KaisouTheme
import com.kaisou.app.features.home.data.HeroArt
import com.kaisou.app.features.home.ui.HomeDimens
import com.kaisou.app.features.home.ui.components.HeroArtEdgeFade
import com.kaisou.app.features.home.ui.components.HeroFade
import com.kaisou.app.features.home.ui.components.MarkdownContent
import com.kaisou.app.features.title.data.ListStatus
import com.kaisou.app.features.title.data.TitleUi
import com.kaisou.app.features.title.ui.TitleDimens

// The top of the page of a title: the art fading into the page, with the name, the details, the score and the buttons anchored to its
// bottom, on the left side when lying down. Prototype, built on the same look as the hero of the Home
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun TitleHero(
    title: TitleUi,
    art: HeroArt?,
    posterSize: Size,
    backdropSize: Size,
    logoSize: Size,
    landscape: Boolean,
    onListButton: () -> Unit,
    onEditList: () -> Unit,
    onFavourite: () -> Unit,
    modifier: Modifier = Modifier
) {
    val dimens = KaisouTheme.dimens

    Box(modifier = modifier) {
        // The picture is asked the way the preload did, so it comes from memory. Until it is ready the cover shows
        val context = LocalContext.current
        // Lying down the picture is the wide one, with the poster as the second choice. Standing up it is the poster
        val hasBackdrop = landscape && art?.backdropUrl != null
        val model = remember(art, title.coverUrl, landscape) {
            val backdrop = if (landscape) art?.backdropUrl?.let { artRequest(context, it, backdropSize) } else null
            backdrop ?: art?.posterUrl?.let { artRequest(context, it, posterSize) } ?: title.coverUrl
        }
        Box(
            modifier = (if (landscape) {
                Modifier
                    .align(Alignment.CenterEnd)
                    .fillMaxWidth(if (hasBackdrop) HomeDimens.HeroLandscapeArtFraction else HomeDimens.HeroLandscapePosterFraction)
            } else {
                Modifier.fillMaxWidth()
            }).fillMaxHeight()
        ) {
            // The gradient of the cover stays under the picture, which covers it when it arrives
            Box(modifier = Modifier.fillMaxSize().background(Brush.linearGradient(title.artColors)))
            model?.let {
                AsyncImage(
                    model = it,
                    contentDescription = null,
                    contentScale = ContentScale.Crop,
                    alignment = if (hasBackdrop) Alignment.Center else Alignment.TopCenter,
                    modifier = Modifier.fillMaxSize()
                )
            }
            if (landscape) HeroArtEdgeFade()
        }
        // Veil only as tall as the bar and a little more, so the icons read and the rest of the art stays clear
        val black = KaisouTheme.colors.veil
        val veilHeight = WindowInsets.statusBarsIgnoringVisibility.asPaddingValues().calculateTopPadding() +
            HomeDimens.HeaderAction + HomeDimens.HeroVeilExtra
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(veilHeight)
                .background(
                    Brush.verticalGradient(
                        0f to black.copy(alpha = HomeDimens.HeroVeilStrength),
                        0.6f to black.copy(alpha = HomeDimens.HeroVeilStrength * 0.6f),
                        1f to Color.Transparent
                    )
                )
        )
        HeroFade(landscape = landscape)

        Column(
            modifier = Modifier
                .align(Alignment.BottomStart)
                .fillMaxWidth(if (landscape) HomeDimens.HeroLandscapeInfoWidth else 1f)
                .padding(horizontal = dimens.screenPadding)
                .padding(bottom = HomeDimens.HeroInfoBottom)
        ) {
            // The logo is asked the way the preload did, so it comes from memory. Without one the name shows in text
            val logo = remember(art, logoSize) { art?.logoUrl?.let { logoRequest(context, it, logoSize) } }
            TitleLogo(
                title = title.title,
                logo = logo,
                maxHeight = if (landscape) HomeDimens.HeroLandscapeTitleHeight else ComponentDimens.TitleLogoMaxHeight,
                modifier = Modifier.fillMaxWidth()
            )
            Row(
                modifier = Modifier.padding(top = if (landscape) dimens.spaceSmall else dimens.spaceMedium),
                horizontalArrangement = Arrangement.spacedBy(dimens.spaceSmall),
                verticalAlignment = Alignment.CenterVertically
            ) {
                ScoreChip(score = title.score)
                art?.rating?.let { AgeRatingBadge(it) }
                title.releaseDay?.let { WeeklyReleaseBadge(it) }
                // Year and format, then a faint divider and the genres, as in 2026 • TV | Action, Mystery
                val details = if (title.year != null) stringResource(R.string.home_hero_meta, title.year, title.format) else title.format
                val divider = stringResource(R.string.home_hero_meta_divider)
                val dividerColor = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = HomeDimens.MetaDividerAlpha)
                val meta = remember(details, divider, dividerColor, title.genres) {
                    buildAnnotatedString {
                        append(details)
                        if (title.genres.isNotBlank()) {
                            withStyle(SpanStyle(color = dividerColor)) { append(divider) }
                            append(title.genres)
                        }
                    }
                }
                Text(
                    text = meta,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
            Row(
                modifier = Modifier.padding(top = if (landscape) HomeDimens.HeroLandscapeButtonsTop else HomeDimens.HeroButtonsTop),
                horizontalArrangement = Arrangement.spacedBy(dimens.spaceMedium),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // The button says where the title is on the list of the user and what comes next
                OutlineButton(
                    // Lying down the button is not stretched over the whole column
                    modifier = if (landscape) {
                        Modifier.weight(1f, fill = false).widthIn(max = HomeDimens.HeroLandscapeButtonMax)
                    } else {
                        Modifier.weight(1f)
                    },
                    text = listButtonText(title, art?.season),
                    onClick = onListButton,
                    leading = { ButtonIcon(icon = KaisouIcons.PlayOutline, tint = MaterialTheme.colorScheme.primary) },
                    grouped = true
                )
                // The list of the user: it opens the editor, and the icon says where the title is on the list, a plus when it is not on it
                OutlineIconButton(
                    icon = listStatusIcon(title.entry?.status, title.manga),
                    contentDescription = stringResource(R.string.cd_list),
                    onClick = onEditList
                )
                OutlineIconButton(
                    icon = if (title.isFavourite) KaisouIcons.HeartFilled else KaisouIcons.Heart,
                    contentDescription = stringResource(if (title.isFavourite) R.string.cd_unfavourite else R.string.cd_favourite),
                    onClick = onFavourite
                )
            }
        }
    }
}

// The score of the title with its star, first on the line of details. A title with no score yet has dashes in its place, since zero would
// say people did not like it
@Composable
private fun ScoreChip(score: Float?, modifier: Modifier = Modifier) {
    val dimens = KaisouTheme.dimens
    val format = remember { NumberFormat.getNumberInstance(Locale.getDefault()).apply { minimumFractionDigits = 1; maximumFractionDigits = 1 } }

    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(dimens.spaceExtraSmall),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = KaisouIcons.Star,
            contentDescription = null,
            tint = KaisouTheme.colors.medalGold,
            modifier = Modifier.size(TitleDimens.ScoreStar)
        )
        Text(
            text = score?.let(format::format) ?: stringResource(R.string.home_card_no_score),
            style = MaterialTheme.typography.titleSmall,
            color = MaterialTheme.colorScheme.onBackground
        )
    }
}

// The icon of the list for where the title is on it: a plus when it is not on the list, then watching or reading, planned, done, done again,
// paused and dropped
internal fun listStatusIcon(status: ListStatus?, manga: Boolean): ImageVector =
    when (status) {
        null -> KaisouIcons.ListPlus
        ListStatus.Current -> if (manga) KaisouIcons.BookOpen else KaisouIcons.ListVideo
        ListStatus.Planning -> KaisouIcons.Clock
        ListStatus.Completed -> KaisouIcons.ListChecks
        ListStatus.Repeating -> KaisouIcons.Repeat
        ListStatus.Paused -> KaisouIcons.CirclePause
        ListStatus.Dropped -> KaisouIcons.CircleX
    }

// What the button of the list says: what to do next with the title, by where it is on the list of the user. A title that is done is
// done again from the start, a movie has no episode and a manga has chapters
@Composable
private fun listButtonText(title: TitleUi, season: Int?): String {
    val unit = when {
        title.manga -> stringResource(R.string.title_unit_chapter, title.nextUnit)
        season != null -> stringResource(R.string.title_unit_season_episode, season, title.nextUnit)
        else -> stringResource(R.string.title_unit_episode, title.nextUnit)
    }
    return when {
        title.listStatus == ListStatus.Completed ->
            stringResource(if (title.manga) R.string.title_cta_read_again else R.string.title_cta_watch_again)
        title.query.movie && !title.manga -> stringResource(R.string.home_cta_watch_movie)
        title.listStatus == ListStatus.Current || title.listStatus == ListStatus.Repeating -> stringResource(R.string.title_cta_continue, unit)
        title.listStatus == ListStatus.Paused || title.listStatus == ListStatus.Dropped -> stringResource(R.string.title_cta_resume, unit)
        else -> stringResource(if (title.manga) R.string.title_cta_read else R.string.title_cta_watch, unit)
    }
}

// The synopsis under the top, with its name, in the small text of the app and with the formats and the breaks that AniList wrote. A few
// lines of it show, and a link in the accent opens all of it, which is only there when there is more to show. The text is always laid out
// whole inside a box whose height is what moves, so it opens and closes the same way
@Composable
fun TitleSynopsis(text: String, modifier: Modifier = Modifier, @StringRes heading: Int = R.string.title_synopsis, hideSpoilers: Boolean = false) {
    val dimens = KaisouTheme.dimens
    val density = LocalDensity.current
    val style = MaterialTheme.typography.bodySmall
    var expanded by rememberSaveable { mutableStateOf(false) }
    // How tall all the text is once laid out, which it is whatever room the box gives it
    var fullHeight by remember { mutableIntStateOf(0) }
    val lineHeight = with(density) { style.lineHeight.toPx() }
    val collapsedHeight = (lineHeight * TitleDimens.SynopsisLines).roundToInt()
    val canExpand = fullHeight > collapsedHeight
    val closedHeight = if (fullHeight in 1 until collapsedHeight) fullHeight else collapsedHeight
    val height by animateIntAsState(
        targetValue = if (expanded && canExpand) fullHeight else closedHeight,
        animationSpec = tween(KaisouMotion.ExpandMillis),
        label = "synopsisHeight"
    )
    // The end of the last line fades out into the page while it is closed
    val fade by animateFloatAsState(
        targetValue = if (expanded) 0f else 1f,
        animationSpec = tween(KaisouMotion.ExpandMillis),
        label = "synopsisFade"
    )

    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = dimens.screenPadding)
            .padding(top = TitleDimens.SynopsisTop)
    ) {
        Text(
            text = stringResource(heading),
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onBackground
        )
        Box(
            modifier = Modifier
                .padding(top = dimens.spaceSmall)
                .fillMaxWidth()
                .height(with(density) { height.toDp() })
                .clipToBounds()
                .clickable(enabled = canExpand, interactionSource = null, indication = null) { expanded = !expanded }
        ) {
            MarkdownContent(
                text = text,
                style = style,
                color = MaterialTheme.colorScheme.onBackground,
                modifier = Modifier
                    .wrapContentHeight(align = Alignment.Top, unbounded = true)
                    .onSizeChanged { fullHeight = it.height },
                hideSpoilers = hideSpoilers
            )
            if (canExpand) {
                Box(
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .fillMaxWidth()
                        .height(with(density) { lineHeight.toDp() })
                        .alpha(fade)
                        .background(Brush.verticalGradient(listOf(Color.Transparent, MaterialTheme.colorScheme.background)))
                )
            }
        }
        if (canExpand) {
            // The link is as big as the text it opens and closes
            Text(
                text = stringResource(if (expanded) R.string.title_show_less else R.string.title_show_more),
                style = style,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier
                    .padding(top = dimens.spaceSmall)
                    .clickable { expanded = !expanded }
            )
        }
    }
}
