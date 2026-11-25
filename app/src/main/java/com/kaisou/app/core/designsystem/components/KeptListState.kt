package com.kaisou.app.core.designsystem.components

// Compose layouts and foundations
import androidx.compose.foundation.lazy.LazyListState
// Compose state and runtime
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.runtime.snapshotFlow

// The state of a list that starts where it was left and keeps the position, so turning the phone keeps the place
@Composable
fun rememberKeptListState(position: ScrollPosition): LazyListState {
    // Not saved by the screen: what it saves is kept for each way the phone is held and would come over the position that was kept
    val state = remember { LazyListState(position.index, position.offset) }

    LaunchedEffect(state) {
        snapshotFlow { state.firstVisibleItemIndex to state.firstVisibleItemScrollOffset }.collect { (index, offset) ->
            position.index = index
            position.offset = offset
        }
    }
    return state
}
