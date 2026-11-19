package com.kaisou.app.features.title.ui.components

// Compose layouts and foundations
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
// Material 3 components
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
// Compose state and runtime
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
// UI styling and utilities
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
// Project resources
import com.kaisou.app.R
import com.kaisou.app.core.designsystem.icons.KaisouIcons
import com.kaisou.app.core.designsystem.theme.KaisouTheme
import com.kaisou.app.core.language.DubLanguage
import com.kaisou.app.core.language.DubPreference
import com.kaisou.app.features.title.ui.TitleDimens

// The language of the voices in use, which opens the menu of all of them. What is chosen is the language of the voices on every page, so
// the pages that show them change together
@Composable
fun DubLanguageMenu(modifier: Modifier = Modifier) {
    val dimens = KaisouTheme.dimens
    val selected by DubPreference.language.collectAsState()
    var open by remember { mutableStateOf(false) }

    Box(modifier = modifier) {
        Row(
            modifier = Modifier
                .clip(MaterialTheme.shapes.small)
                .clickable(role = Role.DropdownList) { open = true }
                .padding(vertical = dimens.spaceExtraSmall),
            horizontalArrangement = Arrangement.spacedBy(dimens.spaceExtraSmall),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = stringResource(R.string.dub_language, selected.displayName()),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.primary
            )
            Icon(imageVector = KaisouIcons.ChevronDown, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(TitleDimens.ListIcon))
        }
        DropdownMenu(
            expanded = open,
            onDismissRequest = { open = false },
            containerColor = MaterialTheme.colorScheme.surfaceContainer,
            shape = MaterialTheme.shapes.large
        ) {
            DubLanguage.entries.forEach { language ->
                DropdownMenuItem(
                    text = {
                        Text(
                            text = language.displayName(),
                            style = MaterialTheme.typography.bodyMedium,
                            color = if (language == selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onBackground
                        )
                    },
                    trailingIcon = if (language == selected) {
                        { Icon(imageVector = KaisouIcons.Check, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(TitleDimens.ListIcon)) }
                    } else {
                        null
                    },
                    onClick = {
                        open = false
                        DubPreference.set(language)
                    }
                )
            }
        }
    }
}
