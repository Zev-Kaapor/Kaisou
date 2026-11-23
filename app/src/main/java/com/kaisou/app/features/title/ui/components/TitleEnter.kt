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

// Wraps a list so that each of its items comes in with the progress it is given, from nothing to all: it fades in and slides in from the
// side it is given. It is for the items of a section, which come in new when the section is changed, and the ones that scroll into view later
// are already there
class EnterScope(private val delegate: LazyListScope, private val enterProgress: () -> Float, private val enterShift: () -> Float) : LazyListScope by delegate {
    override fun item(key: Any?, contentType: Any?, content: @Composable LazyItemScope.() -> Unit) {
        val progress = enterProgress
        val shift = enterShift
        delegate.item(key, contentType) {
            val scope = this
            Box(modifier = Modifier.entering(progress, shift)) { scope.content() }
        }
    }

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
