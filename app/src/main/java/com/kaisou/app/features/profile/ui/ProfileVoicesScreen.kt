package com.kaisou.app.features.profile.ui

// Compose layouts and foundations
import androidx.compose.foundation.background
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
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
// Material 3 components
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
// Compose state and runtime
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
// UI styling and utilities
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.lifecycle.viewmodel.compose.viewModel
// Project resources
import com.kaisou.app.R
import com.kaisou.app.core.designsystem.components.scrollIndicator
import com.kaisou.app.core.designsystem.icons.KaisouIcons
import com.kaisou.app.core.designsystem.theme.KaisouTheme
import com.kaisou.app.core.language.DubLanguage
import com.kaisou.app.features.home.ui.components.HeaderAction
import com.kaisou.app.features.profile.data.ProfileRef
import com.kaisou.app.features.title.data.Load
import com.kaisou.app.features.title.ui.CastCard
import com.kaisou.app.features.title.ui.TitleDimens
import com.kaisou.app.features.title.ui.components.CastRowsSkeleton
import com.kaisou.app.features.title.ui.components.DetailsError
import androidx.compose.runtime.LaunchedEffect

// All the voices of a character as cards: the language of each voice, the note of its part, and what the person says of itself
@Composable
fun ProfileVoicesScreen(onBack: () -> Unit, onOpenProfile: (ProfileRef) -> Unit, modifier: Modifier = Modifier, viewModel: ProfileViewModel = viewModel()) {
    val dimens = KaisouTheme.dimens
    val state by viewModel.state.collectAsState()
    val voices = (state as? Load.Done)?.value?.sections?.firstOrNull { it.title == R.string.profile_voices }?.people.orEmpty()
    val abouts by viewModel.abouts.collectAsState()

    // What the voices say of themselves is asked when they are on the screen
    LaunchedEffect(voices) { viewModel.loadAbouts(voices.mapNotNull { it.ref?.id }) }

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
                text = stringResource(R.string.profile_voices),
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onBackground,
                modifier = Modifier.padding(horizontal = dimens.spaceSmall)
            )
        }
        when (state) {
            is Load.Failed -> DetailsError(onRetry = viewModel::load)
            is Load.Done -> {
                val listState = rememberLazyListState()
                LazyColumn(
                    state = listState,
                    modifier = Modifier.fillMaxSize().scrollIndicator(listState),
                    contentPadding = PaddingValues(horizontal = dimens.screenPadding, vertical = dimens.spaceSmall),
                    verticalArrangement = Arrangement.spacedBy(dimens.spaceSmall)
                ) {
                    items(voices, key = { it.url }) { person ->
                        CastCard(
                            person.copy(role = listOfNotNull(person.language?.let { DubLanguage.nameOf(it) }, person.role).joinToString(" · ").ifEmpty { null }, about = person.ref?.let { abouts[it.id] }),
                            onOpenProfile
                        )
                    }
                }
            }
            else -> CastRowsSkeleton(TitleDimens.CastSkeletonRows)
        }
    }
}
