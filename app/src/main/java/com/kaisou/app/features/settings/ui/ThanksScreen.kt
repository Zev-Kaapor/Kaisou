package com.kaisou.app.features.settings.ui

// Android intents
import android.content.Intent
import androidx.core.net.toUri
// Compose layouts and foundations
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
// Material 3 components
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
// Compose UI
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalResources
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
// Pictures
import coil3.compose.AsyncImage
// Project resources
import com.kaisou.app.R
import com.kaisou.app.features.settings.ui.components.SettingsDivider
import com.kaisou.app.features.settings.ui.components.SettingsGroup
import com.kaisou.app.features.settings.ui.components.SettingsPage

// One of the projects, sites or tools the app is thankful to, the words that name it, what it gave, and the address of its page when it has one
// Its picture is one of the app, or the one of the internet when it has none
private class Thanks(
    val titleRes: Int,
    val subtitleRes: Int,
    val addressRes: Int? = null,
    val pictureRes: Int? = null,
    val iconRes: Int? = null
)

// The projects, the sites and the apps that inspired Kaisou, the order is made by their names
private val SpecialThanks = listOf(
    Thanks(R.string.thanks_youtube_player, R.string.thanks_youtube_player_subtitle, R.string.thanks_youtube_player_address, pictureRes = R.drawable.thanks_youtube_player),
    Thanks(R.string.thanks_anilist, R.string.thanks_anilist_subtitle, R.string.thanks_anilist_address, pictureRes = R.drawable.ic_anilist),
    Thanks(R.string.thanks_crunchyroll, R.string.thanks_crunchyroll_subtitle, R.string.thanks_crunchyroll_address, R.drawable.thanks_crunchyroll),
    Thanks(R.string.thanks_dantotsu, R.string.thanks_dantotsu_subtitle, R.string.thanks_dantotsu_address, R.drawable.thanks_dantotsu),
    Thanks(R.string.thanks_google_translate, R.string.thanks_google_translate_subtitle, R.string.thanks_google_translate_address, R.drawable.thanks_google_translate),
    Thanks(R.string.thanks_jikan, R.string.thanks_jikan_subtitle, R.string.thanks_jikan_address, R.drawable.thanks_jikan),
    Thanks(R.string.thanks_kuroiru, R.string.thanks_kuroiru_subtitle, R.string.thanks_kuroiru_address, R.drawable.thanks_kuroiru),
    Thanks(R.string.thanks_mangabaka, R.string.thanks_mangabaka_subtitle, R.string.thanks_mangabaka_address, R.drawable.thanks_mangabaka),
    Thanks(R.string.thanks_mangadex, R.string.thanks_mangadex_subtitle, R.string.thanks_mangadex_address, R.drawable.thanks_mangadex),
    Thanks(R.string.thanks_myanimelist, R.string.thanks_myanimelist_subtitle, R.string.thanks_myanimelist_address, R.drawable.thanks_myanimelist),
    Thanks(R.string.thanks_thetvdb, R.string.thanks_thetvdb_subtitle, R.string.thanks_thetvdb_address, R.drawable.thanks_thetvdb),
    Thanks(R.string.thanks_tmdb, R.string.thanks_tmdb_subtitle, R.string.thanks_tmdb_address, R.drawable.thanks_tmdb),
    Thanks(R.string.thanks_tomato, R.string.thanks_tomato_subtitle, R.string.thanks_tomato_address, R.drawable.thanks_tomato),
    Thanks(R.string.thanks_wavvy, R.string.thanks_wavvy_subtitle, R.string.thanks_wavvy_address, iconRes = R.string.thanks_wavvy_icon)
)

// The tools that took part in the development, in the order of their names, they are thanked apart from the projects
private val AiThanks = listOf(
    Thanks(R.string.thanks_claude, R.string.thanks_claude_subtitle, R.string.thanks_claude_address, R.drawable.thanks_claude)
)

// The projects the app is thankful to, in the order of their names, then the tools that helped, each group under a line, and each one with its picture opens its page when it has one
@Composable
fun ThanksScreen(onBack: () -> Unit, modifier: Modifier = Modifier) {
    val context = LocalContext.current
    val resources = LocalResources.current
    val ordered = SpecialThanks.sortedBy { resources.getString(it.titleRes).lowercase() }
    val open: (Thanks) -> Unit = { thanks ->
        thanks.addressRes?.let { runCatching { context.startActivity(Intent(Intent.ACTION_VIEW, resources.getString(it).toUri())) } }
    }

    SettingsPage(title = stringResource(R.string.settings_about_thanks_special), onBack = onBack, modifier = modifier) {
        SettingsGroup(title = stringResource(R.string.thanks_projects_group)) {
            ordered.forEachIndexed { index, thanks ->
                if (index > 0) SettingsDivider()
                ThanksRow(thanks = thanks, onClick = { open(thanks) })
            }
        }

        // The tools that helped in the development are thanked apart, under a line
        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
        SettingsGroup(title = stringResource(R.string.thanks_ai_group)) {
            AiThanks.forEachIndexed { index, thanks ->
                if (index > 0) SettingsDivider()
                ThanksRow(thanks = thanks, onClick = { open(thanks) })
            }
        }
    }
}

// A project, a site or a tool, its picture in a circle, its name and what it gave
@Composable
private fun ThanksRow(thanks: Thanks, onClick: () -> Unit) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = SettingsDimens.RowHeight)
            .clickable(enabled = thanks.addressRes != null, onClick = onClick)
            .padding(horizontal = SettingsDimens.RowPadding, vertical = SettingsDimens.RowVertical)
    ) {
        val pictureModifier = Modifier
            .size(SettingsDimens.ThanksIcon)
            .clip(CircleShape)
            .background(MaterialTheme.colorScheme.surfaceContainerHighest)

        // A picture of the app, or the one of the internet
        if (thanks.pictureRes != null) {
            Image(painter = painterResource(thanks.pictureRes), contentDescription = null, contentScale = ContentScale.Fit, modifier = pictureModifier)
        } else {
            AsyncImage(model = thanks.iconRes?.let { stringResource(it) }, contentDescription = null, contentScale = ContentScale.Crop, modifier = pictureModifier)
        }

        Column(modifier = Modifier.weight(1f).padding(start = SettingsDimens.RowIconGap)) {
            Text(
                text = stringResource(thanks.titleRes),
                style = MaterialTheme.typography.bodyLarge.merge(SettingsType.RowTitle),
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
            Text(
                text = stringResource(thanks.subtitleRes),
                style = MaterialTheme.typography.bodyMedium.merge(SettingsType.RowSubtitle),
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = SettingsDimens.ThanksLines,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}
