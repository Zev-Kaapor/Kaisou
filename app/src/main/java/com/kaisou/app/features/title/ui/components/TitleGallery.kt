package com.kaisou.app.features.title.ui.components

// Compose layouts and foundations
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.itemsIndexed
// Material 3 components
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
// Compose state and runtime
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
// UI styling and utilities
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalResources
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.style.TextOverflow
// Image loading
import coil3.compose.AsyncImage
// Project resources
import com.kaisou.app.R
import com.kaisou.app.core.artwork.GalleryImage
import com.kaisou.app.core.designsystem.components.ImageViewer
import com.kaisou.app.core.designsystem.components.ViewerImage
import com.kaisou.app.core.designsystem.theme.KaisouTheme
import com.kaisou.app.core.download.ImageDownload
import com.kaisou.app.features.title.data.GalleryKind
import com.kaisou.app.features.title.ui.TitleDimens

// The pictures of one kind of the gallery of an anime in a row that slides, with the arrow to the page that has all of them
@Composable
fun GalleryBlock(kind: GalleryKind, images: List<GalleryImage>, onSeeAll: () -> Unit) {
    val dimens = KaisouTheme.dimens
    var viewing by rememberSaveable(kind.path) { mutableStateOf<Int?>(null) }
    viewing?.let { GalleryViewer(images, startIndex = it, onDismiss = { viewing = null }) }
    val shown = images.take(TitleDimens.CardsShown)

    Column(modifier = Modifier.fillMaxWidth().padding(top = TitleDimens.BlockTop)) {
        Row(modifier = Modifier.fillMaxWidth().padding(horizontal = dimens.screenPadding), verticalAlignment = Alignment.CenterVertically) {
            BlockTitle(text = stringResource(kind.label), modifier = Modifier.weight(1f))
            if (images.size > shown.size) SeeAllArrow(onSeeAll)
        }
        LazyRow(
            contentPadding = PaddingValues(horizontal = dimens.screenPadding),
            horizontalArrangement = Arrangement.spacedBy(dimens.spaceSmall),
            modifier = Modifier.padding(top = dimens.spaceSmall)
        ) {
            itemsIndexed(shown, key = { _, image -> image.url }) { index, image ->
                GalleryTile(image, kind, onClick = { viewing = index }, modifier = Modifier.width(if (kind == GalleryKind.Posters) TitleDimens.CoverWidth else TitleDimens.GalleryWideWidth))
            }
        }
    }
}

// A picture of the gallery with its language and size under it, a logo being fitted whole in its box and the others filling it
@Composable
fun GalleryTile(image: GalleryImage, kind: GalleryKind, onClick: () -> Unit, modifier: Modifier = Modifier) {
    val dimens = KaisouTheme.dimens
    val ratio = if (kind == GalleryKind.Posters) TitleDimens.CoverRatio else TitleDimens.GalleryWideRatio

    Column(modifier = modifier.clickable(role = Role.Button, onClick = onClick), verticalArrangement = Arrangement.spacedBy(dimens.spaceExtraSmall)) {
        AsyncImage(
            model = image.thumbnail,
            contentDescription = null,
            contentScale = if (kind == GalleryKind.Logos) ContentScale.Fit else ContentScale.Crop,
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(ratio)
                .clip(MaterialTheme.shapes.medium)
                .background(if (kind == GalleryKind.Logos) KaisouTheme.colors.elevated else KaisouTheme.colors.skeleton)
        )
        Text(
            text = galleryName(image, stringResource(R.string.home_card_separator)),
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}

// The pictures over the whole screen, sliding between them, each with its language and size and the download of the one that is on
@Composable
fun GalleryViewer(images: List<GalleryImage>, startIndex: Int, onDismiss: () -> Unit) {
    val context = LocalContext.current
    val resources = LocalResources.current
    val separator = stringResource(R.string.home_card_separator)
    val started = stringResource(R.string.title_cover_download_started)
    val failed = stringResource(R.string.title_cover_download_failed)
    val viewerImages = remember(images, separator) {
        images.map { image ->
            val name = namePieces(image, separator) { width, height -> resources.getString(R.string.title_gallery_size, width, height) }
            ViewerImage(url = image.url, placeholder = image.thumbnail, title = name.ifEmpty { null })
        }
    }
    ImageViewer(
        images = viewerImages,
        startIndex = startIndex,
        onDismiss = onDismiss,
        onDownload = { image -> if (ImageDownload.save(context, image.url, ImageDownload.nameOf(image.url, GalleryFilePrefix))) started else failed }
    )
}

// The words under a picture: its language and its size
@Composable
private fun galleryName(image: GalleryImage, separator: String): String {
    val resources = LocalResources.current
    return namePieces(image, separator) { width, height -> resources.getString(R.string.title_gallery_size, width, height) }
}

// The language of a picture and its size, said in the form that goes under it
private fun namePieces(image: GalleryImage, separator: String, size: (Int, Int) -> String): String =
    listOfNotNull(image.language?.uppercase(), size(image.width, image.height).takeIf { image.width > 0 && image.height > 0 }).joinToString(separator)

// The prefix of the name of a picture saved to the phone
private const val GalleryFilePrefix = "Kaisou"
