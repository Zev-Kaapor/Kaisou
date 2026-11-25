package com.kaisou.app.features.title.ui.components

// Compose layouts and foundations
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
// Material 3 components
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
// Compose state and runtime
import androidx.compose.runtime.Composable
// UI styling and utilities
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.style.TextOverflow
// Project resources
import com.kaisou.app.R
import com.kaisou.app.core.browser.InAppBrowser
import com.kaisou.app.core.designsystem.icons.KaisouIcons
import com.kaisou.app.core.designsystem.theme.KaisouTheme
import com.kaisou.app.features.title.data.TitleNews
import com.kaisou.app.features.title.ui.TitleDimens

// The first news of the title under its name, with an arrow at the end of the line that opens all of them when there are more
@Composable
internal fun NewsBlock(news: List<TitleNews>, onOpenAll: () -> Unit) {
    val dimens = KaisouTheme.dimens
    val more = news.size > TitleDimens.NewsPreview

    Column(modifier = Modifier.fillMaxWidth().padding(horizontal = dimens.screenPadding).padding(top = TitleDimens.BlockTop)) {
        BlockTitle(stringResource(R.string.title_section_news))
        news.take(TitleDimens.NewsPreview).forEach { NewsCard(it, modifier = Modifier.padding(top = dimens.spaceSmall)) }

        // A gray line under the last one with an arrow says there are more and opens all of them
        if (more) {
            Row(
                modifier = Modifier
                    .align(Alignment.CenterHorizontally)
                    .clickable(role = Role.Button, onClick = onOpenAll)
                    .padding(vertical = dimens.spaceMedium),
                horizontalArrangement = Arrangement.spacedBy(dimens.spaceExtraSmall),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = stringResource(R.string.title_news_all),
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Icon(imageVector = KaisouIcons.ChevronRight, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }
}

// One piece of news in a card with the headline, the day and who wrote it, and it opens on the site of the news
@Composable
internal fun NewsCard(item: TitleNews, modifier: Modifier = Modifier) {
    val dimens = KaisouTheme.dimens
    val context = LocalContext.current
    val separator = stringResource(R.string.home_card_separator)

    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(MaterialTheme.shapes.medium)
            .background(KaisouTheme.colors.elevated)
            .clickable(role = Role.Button) { InAppBrowser.open(context, item.url) }
            .padding(dimens.spaceMedium),
        verticalArrangement = Arrangement.spacedBy(dimens.spaceExtraSmall)
    ) {
        Text(
            text = item.title,
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurface,
            maxLines = TitleDimens.NewsLines,
            overflow = TextOverflow.Ellipsis
        )
        val line = listOfNotNull(item.date, item.author).joinToString(separator)
        if (line.isNotEmpty()) {
            Text(text = line, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}
