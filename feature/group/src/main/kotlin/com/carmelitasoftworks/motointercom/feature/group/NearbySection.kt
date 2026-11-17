package com.carmelitasoftworks.motointercom.feature.group

import android.os.Build
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.carmelitasoftworks.motointercom.core.nearby.NearbyGroup
import com.carmelitasoftworks.motointercom.core.ui.BigButton
import com.carmelitasoftworks.motointercom.core.ui.BigOutlinedButton

/** Create or join a group without internet. */
@Composable
internal fun NearbySection(
    permissionsGranted: Boolean,
    groups: List<NearbyGroup>,
    onGrant: () -> Unit,
    onCreate: () -> Unit,
    onJoin: (NearbyGroup) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(16.dp)) {
        Text(
            text = stringResource(R.string.nearby_explainer),
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        if (!permissionsGranted) {
            Text(
                text = stringResource(R.string.nearby_permissions_needed),
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.error,
            )
            BigButton(text = stringResource(R.string.nearby_grant), onClick = onGrant)
            return@Column
        }
        BigButton(text = stringResource(R.string.nearby_create), onClick = onCreate)
        Text(text = stringResource(R.string.nearby_found_title), style = MaterialTheme.typography.titleLarge)
        if (groups.isEmpty()) {
            Text(
                text = stringResource(R.string.nearby_searching),
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) {
                Text(
                    text = stringResource(R.string.nearby_location_hint),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
        groups.forEach { group ->
            BigOutlinedButton(
                text = stringResource(R.string.nearby_join, group.hostName),
                onClick = { onJoin(group) },
            )
        }
    }
}
