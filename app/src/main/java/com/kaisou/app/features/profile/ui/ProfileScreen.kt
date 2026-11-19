package com.kaisou.app.features.profile.ui

// Android framework
import android.content.Intent
// Compose layouts and foundations
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.LazyColumn
// Material 3 components
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
// Compose state and runtime
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
// UI styling and utilities
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.compose.ui.text.style.TextOverflow
import androidx.lifecycle.viewmodel.compose.viewModel
// Pictures
import coil3.compose.AsyncImage
// Java text and utilities
import java.text.NumberFormat
import java.util.Locale
// Project resources
import com.kaisou.app.R
import com.kaisou.app.core.designsystem.components.rememberKeptListState
import com.kaisou.app.core.designsystem.components.scrollIndicator
import com.kaisou.app.core.designsystem.icons.KaisouIcons
import com.kaisou.app.core.designsystem.theme.KaisouTheme
import com.kaisou.app.features.home.ui.components.HeaderAction
import com.kaisou.app.core.language.DubPreference
import com.kaisou.app.features.profile.data.MediaTab
import com.kaisou.app.features.title.ui.components.DubLanguageMenu
import com.kaisou.app.features.profile.data.ProfileRef
import com.kaisou.app.features.profile.data.ProfileUi
import com.kaisou.app.features.title.data.Load
import com.kaisou.app.features.title.ui.TitleDimens
import com.kaisou.app.features.title.ui.components.CardsBlock
import com.kaisou.app.features.title.ui.components.DataBlock
import com.kaisou.app.features.title.ui.components.DetailsError
import com.kaisou.app.features.title.ui.components.DetailsSkeleton
import com.kaisou.app.features.title.ui.components.PeopleBlock
import com.kaisou.app.features.title.ui.components.TitleSynopsis

// The page of a character, a person of the staff or a studio: back, the heart and share on top, then its picture and name, what is known
// of it, what it says of itself and the rows of titles and people that go with it
@Composable
fun ProfileScreen(
    onBack: () -> Unit,
    onOpenTitle: (Int) -> Unit,
    onOpenProfile: (ProfileRef) -> Unit,
    onOpenMedia: (ProfileRef, MediaTab) -> Unit,
    modifier: Modifier = Modifier,
    viewModel: ProfileViewModel = viewModel()
) {
    val dimens = KaisouTheme.dimens
    val state by viewModel.state.collectAsState()
    val listState = rememberKeptListState(viewModel.scroll)
    val context = LocalContext.current
    val profile = (state as? Load.Done)?.value

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
            Spacer(modifier = Modifier.weight(1f))
            if (profile != null) {
                HeaderAction(
                    icon = if (profile.isFavourite) KaisouIcons.HeartFilled else KaisouIcons.Heart,
                    contentDescription = stringResource(if (profile.isFavourite) R.string.cd_unfavourite else R.string.cd_favourite),
                    onClick = viewModel::toggleFavourite
                )
                profile.siteUrl?.let { url ->
                    HeaderAction(
                        icon = KaisouIcons.Share,
                        contentDescription = stringResource(R.string.cd_share),
                        onClick = {
                            val send = Intent(Intent.ACTION_SEND).setType("text/plain").putExtra(Intent.EXTRA_TEXT, url)
                            context.startActivity(Intent.createChooser(send, null))
                        }
                    )
                }
            }
        }
        LazyColumn(state = listState, modifier = Modifier.fillMaxSize().scrollIndicator(listState)) {
            when {
                profile != null -> {
                    item(key = "header") { ProfileHeader(profile) }
                    if (profile.facts.isNotEmpty()) item(key = "facts") { DataBlock(profile.facts) }
                    if (profile.description.isNotBlank()) item(key = "about") { TitleSynopsis(text = profile.description, heading = R.string.profile_about, hideSpoilers = true) }
                    profile.sections.forEach { section ->
                        if (section.cards.isNotEmpty()) item(key = "cards-${section.title}") { CardsBlock(section.title, section.cards, onOpenTitle, onSeeAll = section.tab?.let { tab -> { onOpenMedia(profile.ref, tab) } }) }
                        if (section.title == R.string.profile_voices) {
                            // The voices of a character come in all the languages and are shown in the one the user chose
                            item(key = "people-${section.title}") {
                                val language by DubPreference.language.collectAsState()
                                val shown = section.people.filter { it.language == null || it.language.equals(language.englishName, ignoreCase = true) }
                                PeopleBlock(R.string.profile_voices, shown, onOpen = { person -> person.ref?.let(onOpenProfile) }, trailing = { DubLanguageMenu() })
                                if (shown.isEmpty()) {
                                    Text(
                                        text = stringResource(R.string.profile_voices_none),
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        modifier = Modifier.padding(horizontal = dimens.screenPadding, vertical = dimens.spaceSmall)
                                    )
                                }
                            }
                        } else if (section.people.isNotEmpty()) {
                            item(key = "people-${section.title}") {
                                PeopleBlock(
                                    section.title,
                                    section.people,
                                    onOpen = { person -> person.ref?.let(onOpenProfile) },
                                    onSeeAll = section.tab?.let { tab -> { onOpenMedia(profile.ref, tab) } }
                                )
                            }
                        }
                    }
                    item(key = "end") { Spacer(modifier = Modifier.height(dimens.spaceLarge)) }
                }
                state is Load.Failed -> item(key = "error") { DetailsError(onRetry = viewModel::load) }
                else -> item(key = "loading") { DetailsSkeleton() }
            }
        }
    }
}

// The picture over the whole screen on black, which can be zoomed with two fingers and moved when it is zoomed. A tap closes it
@Composable
private fun ImageViewer(url: String, onDismiss: () -> Unit) {
    var scale by remember { mutableFloatStateOf(1f) }
    var offset by remember { mutableStateOf(Offset.Zero) }

    Dialog(onDismissRequest = onDismiss, properties = DialogProperties(usePlatformDefaultWidth = false)) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black)
                .pointerInput(Unit) { detectTapGestures { onDismiss() } }
                .pointerInput(Unit) {
                    detectTransformGestures { _, pan, zoom, _ ->
                        scale = (scale * zoom).coerceIn(1f, MaxZoom)
                        offset = if (scale == 1f) Offset.Zero else offset + pan
                    }
                },
            contentAlignment = Alignment.Center
        ) {
            AsyncImage(
                model = url,
                contentDescription = null,
                contentScale = ContentScale.Fit,
                modifier = Modifier
                    .fillMaxSize()
                    .graphicsLayer(scaleX = scale, scaleY = scale, translationX = offset.x, translationY = offset.y)
            )
        }
    }
}

private const val MaxZoom = 5f

// Its picture, or a mark when it has none, with its name, the name in its own writing and how many people have it as a favourite
@Composable
private fun ProfileHeader(profile: ProfileUi) {
    val dimens = KaisouTheme.dimens
    val format = remember { NumberFormat.getIntegerInstance(Locale.getDefault()) }
    var viewing by rememberSaveable { mutableStateOf(false) }
    if (viewing && profile.image != null) ImageViewer(url = profile.image, onDismiss = { viewing = false })

    Row(
        modifier = Modifier.fillMaxWidth().padding(horizontal = dimens.screenPadding).padding(top = dimens.spaceMedium),
        horizontalArrangement = Arrangement.spacedBy(dimens.spaceLarge),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(TitleDimens.ProfileImageWidth, TitleDimens.ProfileImageHeight)
                .clip(MaterialTheme.shapes.medium)
                .background(KaisouTheme.colors.skeleton)
                .clickable(enabled = profile.image != null, role = Role.Button) { viewing = true },
            contentAlignment = Alignment.Center
        ) {
            if (profile.image != null) {
                AsyncImage(model = profile.image, contentDescription = null, contentScale = ContentScale.Crop, modifier = Modifier.fillMaxSize())
            } else {
                Icon(imageVector = KaisouIcons.Video, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(TitleDimens.ProfileMark))
            }
        }
        Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(dimens.spaceExtraSmall)) {
            Text(
                text = profile.name,
                style = MaterialTheme.typography.titleLarge,
                color = MaterialTheme.colorScheme.onBackground,
                maxLines = 3,
                overflow = TextOverflow.Ellipsis
            )
            profile.nativeName?.let {
                Text(text = it, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            if (profile.favourites > 0) {
                Text(
                    text = pluralStringResource(R.plurals.profile_favourites, profile.favourites, format.format(profile.favourites)),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}
