package com.kaisou.app.features.title.ui.components

// Compose layouts and foundations
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.aspectRatio
// Material 3 components
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
// Compose state and runtime
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
// UI styling and utilities
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
// Pictures
import coil3.compose.AsyncImage
// Project resources
import com.kaisou.app.core.designsystem.theme.KaisouTheme
import com.kaisou.app.features.home.ui.components.MarkdownContent
import com.kaisou.app.features.title.data.ForumLinks
import com.kaisou.app.features.title.data.ForumPart
import com.kaisou.app.features.title.data.LinkedTitle
import com.kaisou.app.features.title.data.ThreadTag
import com.kaisou.app.features.title.ui.TitleDimens

// A text of the forum as AniList writes it, with the links to a known title alone in a line shown as a card
@Composable
internal fun ForumText(text: String, cards: Map<Int, LinkedTitle>, onOpenTitle: (Int) -> Unit, style: TextStyle, color: Color, modifier: Modifier = Modifier) {
    val parts = remember(text, cards.keys) { ForumLinks.split(text, cards.keys) }

    Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(KaisouTheme.dimens.spaceSmall)) {
        parts.forEach { part ->
            when (part) {
                is ForumPart.Text -> MarkdownContent(text = part.text, style = style, color = color, hideSpoilers = true)
                is ForumPart.Card -> cards[part.id]?.let { LinkedTitleCard(it, onOpenTitle) }
            }
        }
    }
}

// A title that a text links to: its cover, its name and a line about it, and it opens its page
@Composable
private fun LinkedTitleCard(title: LinkedTitle, onOpen: (Int) -> Unit) {
    val dimens = KaisouTheme.dimens

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(MaterialTheme.shapes.medium)
            .background(KaisouTheme.colors.elevated)
            .clickable(role = Role.Button) { onOpen(title.id) },
        horizontalArrangement = Arrangement.spacedBy(dimens.spaceMedium),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.width(TitleDimens.LinkedCoverWidth).aspectRatio(TitleDimens.LinkedCoverRatio).background(KaisouTheme.colors.skeleton)) {
            title.cover?.let { AsyncImage(model = it, contentDescription = null, contentScale = ContentScale.Crop, modifier = Modifier.fillMaxWidth().weight(1f)) }
        }
        Column(modifier = Modifier.weight(1f).padding(end = dimens.spaceMedium), verticalArrangement = Arrangement.spacedBy(dimens.spaceExtraSmall)) {
            Text(
                text = title.title,
                style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.Bold),
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
            if (title.info.isNotBlank()) {
                Text(text = title.info, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }
}

// The tags of a thread in a row that breaks into lines, the titles opening when touched
@OptIn(ExperimentalLayoutApi::class)
@Composable
internal fun ThreadTags(tags: List<ThreadTag>, onOpenTitle: (Int) -> Unit) {
    val dimens = KaisouTheme.dimens

    FlowRow(horizontalArrangement = Arrangement.spacedBy(dimens.spaceSmall), verticalArrangement = Arrangement.spacedBy(dimens.spaceSmall)) {
        tags.forEach { tag ->
            Text(
                text = tag.label,
                style = MaterialTheme.typography.labelMedium,
                color = if (tag.mediaId != null) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier
                    .clip(MaterialTheme.shapes.small)
                    .background(KaisouTheme.colors.elevated)
                    .then(if (tag.mediaId != null) Modifier.clickable(role = Role.Button) { onOpenTitle(tag.mediaId) } else Modifier)
                    .padding(horizontal = dimens.spaceSmall, vertical = dimens.spaceExtraSmall)
            )
        }
    }
}
