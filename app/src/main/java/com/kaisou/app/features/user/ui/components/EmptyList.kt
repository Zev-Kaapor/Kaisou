package com.kaisou.app.features.user.ui.components

// Compose layouts and foundations
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.CircleShape
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
import androidx.compose.ui.res.stringArrayResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.style.TextAlign
// Project resources
import com.kaisou.app.R
import com.kaisou.app.core.designsystem.theme.KaisouTheme

// What a page of a list says with no title: a face that changes, a friendly line, and a way to take the filters back
@Composable
fun EmptyList(onClear: (() -> Unit)?, modifier: Modifier = Modifier) {
    val dimens = KaisouTheme.dimens
    val faces = stringArrayResource(R.array.user_list_kaomoji)
    val picked = remember(faces) { faces.random() }

    Column(
        modifier = modifier.fillMaxWidth().padding(horizontal = dimens.screenPadding, vertical = dimens.spaceExtraLarge),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(dimens.spaceSmall)
    ) {
        Text(text = picked, style = MaterialTheme.typography.headlineMedium, color = MaterialTheme.colorScheme.primary, textAlign = TextAlign.Center)
        Text(
            text = stringResource(R.string.user_list_empty_silly),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center
        )
        if (onClear != null) {
            Text(
                text = stringResource(R.string.user_list_reset),
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.clip(CircleShape).clickable(role = Role.Button, onClick = onClear).padding(horizontal = dimens.spaceMedium, vertical = dimens.spaceSmall)
            )
        }
    }
}
