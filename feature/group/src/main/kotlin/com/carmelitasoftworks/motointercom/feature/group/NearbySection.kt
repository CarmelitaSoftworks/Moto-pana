package com.carmelitasoftworks.motointercom.feature.group

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.carmelitasoftworks.motointercom.core.nearby.NearbyError
import com.carmelitasoftworks.motointercom.core.nearby.NearbyGroup
import com.carmelitasoftworks.motointercom.core.ui.BigButton
import com.carmelitasoftworks.motointercom.core.ui.BigOutlinedButton
import com.carmelitasoftworks.motointercom.core.ui.GloveTarget

/** Create or join a group without internet. */
@Composable
internal fun NearbySection(
    permissionsGranted: Boolean,
    groups: List<NearbyGroup>,
    error: NearbyError?,
    onGrant: () -> Unit,
    onCreate: () -> Unit,
    onJoin: (NearbyGroup) -> Unit,
    onRetry: () -> Unit,
    modifier: Modifier = Modifier,
) {
    // Coming back from Android's settings: look again with the radios as they are now.
    val failing = rememberFailingRadioChecks(onResume = { if (permissionsGranted) onRetry() })
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
        if (failing.isNotEmpty()) RadioChecksCard(failing)
        if (error != null) {
            Text(
                text = stringResource(error.messageRes, error.statusCode),
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.error,
            )
        }
        BigButton(text = stringResource(R.string.nearby_create), onClick = onCreate)
        Text(text = stringResource(R.string.nearby_found_title), style = MaterialTheme.typography.titleLarge)
        if (groups.isEmpty()) {
            Text(
                text = stringResource(R.string.nearby_searching),
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Text(
                text = stringResource(R.string.nearby_no_pairing),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            BigOutlinedButton(text = stringResource(R.string.nearby_retry), onClick = onRetry)
        }
        groups.forEach { group ->
            BigOutlinedButton(
                text = stringResource(R.string.nearby_join, group.hostName),
                onClick = { onJoin(group) },
            )
        }
    }
}

@Composable
private fun RadioChecksCard(failing: List<RadioCheck>) {
    val context = LocalContext.current
    Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(stringResource(R.string.checks_intro), style = MaterialTheme.typography.titleMedium)
            failing.forEach { check ->
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = stringResource(check.messageRes),
                        style = MaterialTheme.typography.bodyLarge,
                        modifier = Modifier.weight(1f),
                    )
                    FilledTonalButton(
                        onClick = { check.openSettings(context) },
                        modifier = Modifier.heightIn(min = GloveTarget),
                    ) {
                        Text(stringResource(R.string.check_open))
                    }
                }
            }
        }
    }
}
