package com.kaisou.app.features.settings.ui.components

// Android configuration
import android.content.res.Configuration
// Compose layouts and foundations
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
// Material 3 components
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
// Compose state and runtime
import androidx.compose.runtime.Composable
// UI styling and utilities
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
// Project resources
import com.kaisou.app.R
import com.kaisou.app.core.designsystem.components.KaisouSheet
import com.kaisou.app.core.designsystem.components.LocalSheetClose
import com.kaisou.app.core.designsystem.icons.KaisouIcons
import com.kaisou.app.core.designsystem.theme.KaisouTheme
import com.kaisou.app.features.settings.ui.SettingsDimens

// One way to choose: the words, whether it is in use or the default, its icon and what it does when chosen
class Choice(
    val title: String,
    val isSelected: Boolean,
    val isDefault: Boolean = false,
    val icon: ImageVector? = null,
    val onSelect: () -> Unit
)

// The choices of a setting in a card, the one in use has its name in bold and a check at its end
@Composable
fun ChoiceSheet(title: String, choices: List<Choice>, onDismiss: () -> Unit) {
    KaisouSheet(onDismiss = onDismiss) {
        val closeSheet = LocalSheetClose.current
        val scroll = rememberScrollState()

        // The name of the choice on top
        val head: @Composable () -> Unit = {
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                color = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.padding(horizontal = SettingsDimens.ChoiceTitleHorizontal, vertical = SettingsDimens.ChoiceTitleVertical)
            )
        }

        // The choices, a line between two of them
        val options: @Composable () -> Unit = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(SettingsDimens.ChoiceCorner))
                    .background(MaterialTheme.colorScheme.surfaceContainerHigh)
            ) {
                choices.forEachIndexed { index, choice ->
                    if (index > 0) {
                        HorizontalDivider(
                            modifier = Modifier.padding(horizontal = SettingsDimens.ChoiceRowHorizontal),
                            thickness = KaisouTheme.dimens.hairline,
                            color = MaterialTheme.colorScheme.outlineVariant
                        )
                    }
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                closeSheet()
                                choice.onSelect()
                            }
                            .padding(horizontal = SettingsDimens.ChoiceRowHorizontal, vertical = SettingsDimens.ChoiceRowVertical)
                    ) {
                        // The name of the choice, and a note beside it when it is the one the app starts with
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(SettingsDimens.ChoiceTagGap),
                            modifier = Modifier.weight(1f)
                        ) {
                            choice.icon?.let {
                                Icon(
                                    imageVector = it,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                    modifier = Modifier.size(SettingsDimens.ChoiceIcon)
                                )
                            }
                            Text(
                                text = choice.title,
                                style = MaterialTheme.typography.bodyLarge.copy(fontWeight = if (choice.isSelected) FontWeight.Bold else FontWeight.Medium),
                                color = MaterialTheme.colorScheme.onSurface,
                                modifier = Modifier.weight(1f, fill = false)
                            )
                            if (choice.isDefault) {
                                Text(
                                    text = stringResource(R.string.settings_option_default),
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                        if (choice.isSelected) {
                            Icon(
                                imageVector = KaisouIcons.Check,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(SettingsDimens.ChoiceCheck)
                            )
                        }
                    }
                }
            }
        }

        // Lying down the screen is too short for the choices, the name stays on the start and the choices scroll on the end
        if (LocalConfiguration.current.orientation == Configuration.ORIENTATION_LANDSCAPE) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(SettingsDimens.ChoiceSide),
                modifier = Modifier.padding(start = SettingsDimens.ChoiceSide, end = SettingsDimens.ChoiceSide, bottom = SettingsDimens.ChoiceBottom)
            ) {
                Column(modifier = Modifier.weight(SettingsDimens.ChoiceLandscapeTitleWeight).align(Alignment.CenterVertically)) { head() }
                Column(
                    modifier = Modifier
                        .weight(1f - SettingsDimens.ChoiceLandscapeTitleWeight)
                        .verticalScroll(scroll)
                ) { options() }
            }
        } else {
            Column(
                modifier = Modifier
                    .verticalScroll(scroll)
                    .padding(start = SettingsDimens.ChoiceSide, end = SettingsDimens.ChoiceSide, bottom = SettingsDimens.ChoiceBottom)
            ) {
                head()
                Spacer(modifier = Modifier.height(SettingsDimens.ChoiceTitleVertical))
                options()
            }
        }
    }
}
