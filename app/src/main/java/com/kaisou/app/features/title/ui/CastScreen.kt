package com.kaisou.app.features.title.ui

// Compose layouts and foundations
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.foundation.lazy.items
// Material 3 components
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
// Compose state and runtime
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.snapshotFlow
// UI styling and utilities
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.style.TextOverflow
import androidx.lifecycle.viewmodel.compose.viewModel
// Pictures
import coil3.compose.AsyncImage
// Project resources
import com.kaisou.app.R
import com.kaisou.app.core.designsystem.components.rememberKeptListState
import com.kaisou.app.core.designsystem.components.scrollIndicator
import com.kaisou.app.core.designsystem.icons.KaisouIcons
import com.kaisou.app.core.designsystem.theme.KaisouTheme
import com.kaisou.app.features.home.ui.components.HeaderAction
import com.kaisou.app.features.profile.data.ProfileRef
import com.kaisou.app.features.title.ui.components.DetailsError
import com.kaisou.app.features.title.ui.components.CastRowsSkeleton
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Icon
import androidx.compose.runtime.setValue
import androidx.compose.ui.text.font.FontWeight
import com.kaisou.app.features.title.data.TitlePerson
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import com.kaisou.app.features.home.ui.components.SpoilerText

// All the characters or all the staff of a title a page at a time, the next asked near the end, each opening its page
@Composable
fun CastScreen(onBack: () -> Unit, onOpenProfile: (ProfileRef) -> Unit, modifier: Modifier = Modifier, viewModel: CastViewModel = viewModel()) {
    val dimens = KaisouTheme.dimens
    val state by viewModel.state.collectAsState()
    val listState = rememberKeptListState(viewModel.scroll)

    // The next page is asked for when the last rows are on the screen
    LaunchedEffect(listState, state.people.size) {
        snapshotFlow { listState.layoutInfo.visibleItemsInfo.lastOrNull()?.index ?: 0 }.collect { last ->
            if (last >= state.people.size - LoadAhead) viewModel.loadMore()
        }
    }

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
                text = stringResource(if (viewModel.staff) R.string.title_section_staff else R.string.title_section_characters),
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onBackground,
                modifier = Modifier.padding(horizontal = dimens.spaceSmall)
            )
        }
        LazyColumn(
            state = listState,
            modifier = Modifier.fillMaxSize().scrollIndicator(listState),
            contentPadding = PaddingValues(horizontal = dimens.screenPadding, vertical = dimens.spaceSmall),
            verticalArrangement = Arrangement.spacedBy(dimens.spaceSmall)
        ) {
            items(state.people, key = { it.url }) { person -> CastCard(person, onOpenProfile) }
            when {
                state.failed -> item(key = "error") { DetailsError(onRetry = viewModel::loadMore) }
                state.loading || state.hasMore -> item(key = if (state.people.isEmpty()) "loading-first" else "loading-more") { CastRowsSkeleton(if (state.people.isEmpty()) TitleDimens.CastSkeletonRows else 2) }
            }
        }
    }
}

// How many rows before the end of the list the next page is asked for
private const val LoadAhead = 4

// A person of the title in a card: the picture at full height, the name, the part, the start of what it says, and a button
@Composable
internal fun CastCard(person: TitlePerson, onOpenProfile: (ProfileRef) -> Unit) {
    val dimens = KaisouTheme.dimens

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(MaterialTheme.shapes.large)
            .background(KaisouTheme.colors.elevated)
            .clickable(enabled = person.ref != null, role = Role.Button) { person.ref?.let(onOpenProfile) }
            .height(TitleDimens.CastCardHeight)
    ) {
        Box(modifier = Modifier.width(TitleDimens.CastCardImageWidth).fillMaxHeight().background(KaisouTheme.colors.skeleton)) {
            person.image?.let { AsyncImage(model = it, contentDescription = null, contentScale = ContentScale.Crop, modifier = Modifier.matchParentSize()) }
        }
        Column(modifier = Modifier.weight(1f).fillMaxHeight().padding(dimens.spaceMedium)) {
            Text(
                text = person.name,
                style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.Bold),
                color = MaterialTheme.colorScheme.onBackground,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            person.role?.let { role ->
                Text(
                    text = role,
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.primary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
            person.about?.let { about ->
                SpoilerText(
                    text = about,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    linkColor = MaterialTheme.colorScheme.primary,
                    headingSize = MaterialTheme.typography.bodySmall.fontSize,
                    maxLines = CastAboutLines,
                    modifier = Modifier.padding(top = dimens.spaceExtraSmall)
                )
            }
            if (person.ref != null) {
                Spacer(modifier = Modifier.weight(1f))
                Row(
                    modifier = Modifier
                        .align(Alignment.End)
                        .clip(CircleShape)
                        .clickable(role = Role.Button) { onOpenProfile(person.ref) }
                        .padding(vertical = dimens.spaceExtraSmall),
                    horizontalArrangement = Arrangement.spacedBy(dimens.spaceExtraSmall),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(text = stringResource(R.string.cast_details), style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.primary)
                    Icon(imageVector = KaisouIcons.ChevronRight, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(TitleDimens.SortIcon))
                }
            }
        }
    }
}

// How many lines of what a person says of itself a card shows
private const val CastAboutLines = 2
