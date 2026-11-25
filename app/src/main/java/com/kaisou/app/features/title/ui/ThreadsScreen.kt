package com.kaisou.app.features.title.ui

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
import androidx.compose.ui.res.stringResource
import androidx.lifecycle.viewmodel.compose.viewModel
// Project resources
import com.kaisou.app.R
import com.kaisou.app.core.designsystem.components.rememberKeptListState
import com.kaisou.app.core.designsystem.components.scrollIndicator
import com.kaisou.app.core.designsystem.icons.KaisouIcons
import com.kaisou.app.core.designsystem.theme.KaisouTheme
import com.kaisou.app.features.home.ui.components.HeaderAction
import com.kaisou.app.features.title.ui.components.DetailsError
import com.kaisou.app.features.title.ui.components.DetailsSkeleton
import com.kaisou.app.features.title.ui.components.ThreadRow

// All the threads of the forum about a title a page at a time, the next asked near the end, each opening to be read
@Composable
fun ThreadsScreen(onBack: () -> Unit, onOpenThread: (Int) -> Unit, modifier: Modifier = Modifier, viewModel: ThreadsViewModel = viewModel()) {
    val dimens = KaisouTheme.dimens
    val state by viewModel.state.collectAsState()
    val listState = rememberKeptListState(viewModel.scroll)

    // The next page is asked for when the last rows are on the screen
    LaunchedEffect(listState, state.threads.size) {
        snapshotFlow { listState.layoutInfo.visibleItemsInfo.lastOrNull()?.index ?: 0 }.collect { last ->
            if (last >= state.threads.size - LoadAhead) viewModel.loadMore()
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
                text = stringResource(R.string.title_section_threads),
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onBackground,
                modifier = Modifier.padding(horizontal = dimens.spaceSmall)
            )
        }
        LazyColumn(
            state = listState,
            modifier = Modifier.fillMaxSize().scrollIndicator(listState),
            contentPadding = PaddingValues(horizontal = dimens.screenPadding, vertical = dimens.spaceSmall),
            verticalArrangement = Arrangement.spacedBy(dimens.spaceMedium)
        ) {
            items(state.threads, key = { it.id }) { thread -> ThreadRow(thread = thread, onClick = { onOpenThread(thread.id) }) }
            when {
                state.failed -> item(key = "error") { DetailsError(onRetry = viewModel::loadMore) }
                state.loading || state.hasMore -> item(key = "loading") { DetailsSkeleton() }
            }
        }
    }
}

// How many rows before the end of the list the next page is asked for
private const val LoadAhead = 4
