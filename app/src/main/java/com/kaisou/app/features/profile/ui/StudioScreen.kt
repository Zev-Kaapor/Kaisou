package com.kaisou.app.features.profile.ui

// Android framework
import android.content.Intent
// Compose layouts and foundations
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.windowInsetsPadding
// Material 3 components
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
// Compose state and runtime
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
// UI styling and utilities
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.lifecycle.viewmodel.compose.viewModel
// Project resources
import com.kaisou.app.R
import com.kaisou.app.core.designsystem.icons.KaisouIcons
import com.kaisou.app.core.designsystem.theme.KaisouTheme
import com.kaisou.app.features.home.ui.components.HeaderAction
import com.kaisou.app.features.profile.data.ProfileRef
import com.kaisou.app.features.title.data.Load
// Java text and utilities
import java.text.NumberFormat
import java.util.Locale

// The page of a studio: back, name, heart and share, and under them all its productions by year, with no picture
@Composable
fun StudioScreen(
    onBack: () -> Unit,
    onOpenTitle: (Int) -> Unit,
    onOpenProfile: (ProfileRef) -> Unit,
    modifier: Modifier = Modifier,
    profileModel: ProfileViewModel = viewModel(),
    mediaModel: ProfileMediaViewModel = viewModel()
) {
    val dimens = KaisouTheme.dimens
    val state by profileModel.state.collectAsState()
    val studio = (state as? Load.Done)?.value
    val context = LocalContext.current
    val format = remember { NumberFormat.getIntegerInstance(Locale.getDefault()) }

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

            // The name, and how many people have it as a favourite under it
            Column(modifier = Modifier.weight(1f).padding(horizontal = dimens.spaceSmall)) {
                Text(
                    text = studio?.name.orEmpty(),
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onBackground,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                if (studio != null && studio.favourites > 0) {
                    Text(
                        text = pluralStringResource(R.plurals.profile_favourites, studio.favourites, format.format(studio.favourites)),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            if (studio != null) {
                HeaderAction(
                    active = studio.isFavourite,
                    icon = if (studio.isFavourite) KaisouIcons.HeartFilled else KaisouIcons.Heart,
                    contentDescription = stringResource(if (studio.isFavourite) R.string.cd_unfavourite else R.string.cd_favourite),
                    onClick = profileModel::toggleFavourite
                )
                studio.siteUrl?.let { url ->
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
        ProfileMediaContent(mediaModel, onOpenTitle, onOpenProfile)
    }
}
