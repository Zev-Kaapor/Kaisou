package com.kaisou.app.features.title.ui.components

// Compose layouts and foundations
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.lazy.LazyItemScope
import androidx.compose.foundation.lazy.LazyListScope
// Compose state and runtime
import androidx.compose.runtime.Composable
// UI styling and utilities
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer

// Wraps a list so each item fades and slides in with the progress given, for the items of a section that comes in new
class EnterScope(private val delegate: LazyListScope, private val enterProgress: () -> Float, private val enterShift: () -> Float) : LazyListScope by delegate {
    // Gives a single item the entering movement
    override fun item(key: Any?, contentType: Any?, content: @Composable LazyItemScope.() -> Unit) {
        val progress = enterProgress
        val shift = enterShift
        delegate.item(key, contentType) {
            val scope = this
            Box(modifier = Modifier.entering(progress, shift)) { scope.content() }
        }
    }

    // Gives a group of items the entering movement
    override fun items(count: Int, key: ((index: Int) -> Any)?, contentType: (index: Int) -> Any?, itemContent: @Composable LazyItemScope.(index: Int) -> Unit) {
        val progress = enterProgress
        val shift = enterShift
        delegate.items(count, key, contentType) { index ->
            val scope = this
            Box(modifier = Modifier.entering(progress, shift)) { scope.itemContent(index) }
        }
    }
}

// The fade and the slide of an item, read when it is drawn so the animation does not recompose the item
private fun Modifier.entering(progress: () -> Float, shift: () -> Float): Modifier =
    graphicsLayer {
        val value = progress()
        alpha = value
        translationX = (1f - value) * shift()
    }
