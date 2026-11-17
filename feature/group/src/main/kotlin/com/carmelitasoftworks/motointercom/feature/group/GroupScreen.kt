package com.carmelitasoftworks.motointercom.feature.group

import android.Manifest
import androidx.annotation.StringRes
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.carmelitasoftworks.motointercom.core.nearby.NearbyGroup
import com.carmelitasoftworks.motointercom.core.service.GroupCode
import com.carmelitasoftworks.motointercom.core.service.IntercomService
import com.carmelitasoftworks.motointercom.core.service.IntercomSession
import com.carmelitasoftworks.motointercom.core.service.JoinError
import com.carmelitasoftworks.motointercom.core.service.RiderProfileRepository
import com.carmelitasoftworks.motointercom.core.ui.BigButton
import com.carmelitasoftworks.motointercom.core.ui.BigOutlinedButton
import com.carmelitasoftworks.motointercom.core.ui.hasPermissions
import com.carmelitasoftworks.motointercom.core.ui.nearbyPermissions
import com.carmelitasoftworks.motointercom.core.ui.rememberPermissionRequest
import com.carmelitasoftworks.motointercom.core.ui.rememberRidePermissionRequest
import com.carmelitasoftworks.motointercom.core.ui.ridePermissions
import com.carmelitasoftworks.motointercom.core.ui.theme.MotoIntercomTheme

private enum class GroupMode { ONLINE, NEARBY }

/** What the rider asked for, kept while the permission dialog is up. */
private sealed interface PendingStart {
    data object CreateOnline : PendingStart

    data class JoinOnline(val code: String) : PendingStart

    data object HostNearby : PendingStart

    data class JoinNearby(val group: NearbyGroup) : PendingStart
}

@Composable
fun GroupScreen(onRideStarted: () -> Unit, modifier: Modifier = Modifier, viewModel: GroupViewModel = hiltViewModel()) {
    val session by viewModel.session.collectAsStateWithLifecycle()
    val name by viewModel.name.collectAsStateWithLifecycle()
    val nearbyGroups by viewModel.nearbyGroups.collectAsStateWithLifecycle()
    val context = LocalContext.current

    var mode by rememberSaveable { mutableStateOf(GroupMode.ONLINE) }
    var problem by rememberSaveable { mutableStateOf<Int?>(null) }
    var pending by remember { mutableStateOf<PendingStart?>(null) }
    var nearbyGranted by remember { mutableStateOf(hasPermissions(context, nearbyPermissions)) }

    val onGranted = {
        when (val action = pending) {
            PendingStart.CreateOnline -> viewModel.createGroup()
            is PendingStart.JoinOnline -> viewModel.join(action.code)
            PendingStart.HostNearby -> viewModel.hostNearby()
            is PendingStart.JoinNearby -> viewModel.joinNearby(action.group)
            null -> Unit
        }
        if (pending != null) {
            IntercomService.start(context)
            onRideStarted()
        }
        pending = null
    }
    val onDenied = {
        pending = null
        problem = R.string.group_mic_needed
    }
    val requestOnline = rememberRidePermissionRequest(onGranted = onGranted, onDenied = onDenied)
    val requestNearby = rememberPermissionRequest(
        permissions = ridePermissions + nearbyPermissions,
        required = listOf(Manifest.permission.RECORD_AUDIO) + nearbyPermissions,
        onGranted = {
            nearbyGranted = true
            onGranted()
        },
        onDenied = {
            nearbyGranted = hasPermissions(context, nearbyPermissions)
            if (pending != null) onDenied()
        },
    )

    fun start(action: PendingStart) {
        viewModel.dismissError()
        problem = null
        when {
            name.isBlank() -> problem = R.string.group_name_needed
            action is PendingStart.JoinOnline && !GroupCode.isValid(action.code) ->
                problem = R.string.group_code_needed
            else -> {
                pending = action
                if (action is PendingStart.HostNearby || action is PendingStart.JoinNearby) {
                    requestNearby()
                } else {
                    requestOnline()
                }
            }
        }
    }

    val active = session as? IntercomSession.Active
    // Look for nearby groups only while this form is on screen in nearby mode.
    val discovering = active == null && mode == GroupMode.NEARBY && nearbyGranted
    DisposableEffect(discovering) {
        if (discovering) viewModel.startNearbyDiscovery()
        onDispose {
            if (discovering && viewModel.session.value !is IntercomSession.Active) viewModel.stopNearbyDiscovery()
        }
    }

    val error = (session as? IntercomSession.Failed)?.error?.messageRes ?: problem
    if (active != null) {
        InRide(code = active.code, onGoToRide = onRideStarted, modifier = modifier)
    } else {
        JoinForm(
            name = name,
            onNameChange = viewModel::setName,
            mode = mode,
            onMode = { mode = it },
            error = error,
            onCreate = { start(PendingStart.CreateOnline) },
            onJoin = { start(PendingStart.JoinOnline(it)) },
            nearby = {
                NearbySection(
                    permissionsGranted = nearbyGranted,
                    groups = nearbyGroups,
                    onGrant = requestNearby,
                    onCreate = { start(PendingStart.HostNearby) },
                    onJoin = { start(PendingStart.JoinNearby(it)) },
                )
            },
            modifier = modifier,
        )
    }
}

@Composable
private fun InRide(code: String, onGoToRide: () -> Unit, modifier: Modifier = Modifier) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(24.dp, androidx.compose.ui.Alignment.CenterVertically),
    ) {
        Text(
            text = stringResource(R.string.group_in_ride, code),
            style = MaterialTheme.typography.headlineMedium,
            textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth(),
        )
        BigButton(text = stringResource(R.string.group_go_to_ride), onClick = onGoToRide)
    }
}

@Composable
private fun JoinForm(
    name: String,
    onNameChange: (String) -> Unit,
    mode: GroupMode,
    onMode: (GroupMode) -> Unit,
    @StringRes error: Int?,
    onCreate: () -> Unit,
    onJoin: (String) -> Unit,
    nearby: @Composable () -> Unit,
    modifier: Modifier = Modifier,
) {
    var code by rememberSaveable { mutableStateOf("") }
    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(20.dp),
    ) {
        Text(text = stringResource(R.string.group_title), style = MaterialTheme.typography.headlineLarge)
        OutlinedTextField(
            value = name,
            onValueChange = { onNameChange(it.take(RiderProfileRepository.MAX_NAME_LENGTH)) },
            label = { Text(stringResource(R.string.group_name_label)) },
            placeholder = { Text(stringResource(R.string.group_name_hint)) },
            singleLine = true,
            textStyle = MaterialTheme.typography.titleLarge,
            keyboardOptions = KeyboardOptions(
                capitalization = KeyboardCapitalization.Words,
                imeAction = ImeAction.Done,
            ),
            modifier = Modifier.fillMaxWidth(),
        )
        ModeSelector(mode = mode, onMode = onMode)
        if (mode == GroupMode.ONLINE) {
            OnlineSection(code = code, onCode = { code = it }, onCreate = onCreate, onJoin = onJoin)
        } else {
            nearby()
        }
        if (error != null) {
            Text(
                text = stringResource(error),
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.error,
            )
        }
        if (mode == GroupMode.ONLINE) {
            Text(
                text = stringResource(R.string.group_coverage_note),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun ModeSelector(mode: GroupMode, onMode: (GroupMode) -> Unit) {
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        GroupMode.entries.forEach { option ->
            val label = stringResource(if (option == GroupMode.ONLINE) R.string.mode_online else R.string.mode_nearby)
            if (option == mode) {
                BigButton(text = label, onClick = {}, modifier = Modifier.weight(1f))
            } else {
                BigOutlinedButton(text = label, onClick = { onMode(option) }, modifier = Modifier.weight(1f))
            }
        }
    }
}

@Composable
private fun OnlineSection(code: String, onCode: (String) -> Unit, onCreate: () -> Unit, onJoin: (String) -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(20.dp)) {
        BigButton(text = stringResource(R.string.group_create), onClick = onCreate)
        Text(
            text = stringResource(R.string.group_or),
            style = MaterialTheme.typography.titleMedium,
            textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth(),
        )
        OutlinedTextField(
            value = code,
            onValueChange = { onCode(GroupCode.normalize(it)) },
            label = { Text(stringResource(R.string.group_code_label)) },
            placeholder = { Text("ABC234") },
            singleLine = true,
            textStyle = TextStyle(fontSize = 32.sp, letterSpacing = 6.sp),
            keyboardOptions = KeyboardOptions(
                capitalization = KeyboardCapitalization.Characters,
                autoCorrectEnabled = false,
                imeAction = ImeAction.Go,
            ),
            keyboardActions = KeyboardActions(onGo = { onJoin(code) }),
            modifier = Modifier.fillMaxWidth(),
        )
        BigOutlinedButton(text = stringResource(R.string.group_join), onClick = { onJoin(code) })
    }
}

@get:StringRes
private val JoinError.messageRes: Int
    get() = when (this) {
        JoinError.SERVER_NOT_CONFIGURED -> R.string.error_server_not_configured
        JoinError.NO_NETWORK -> R.string.error_no_network
        JoinError.SERVER_REJECTED -> R.string.error_server_rejected
        JoinError.CONNECTION_FAILED -> R.string.error_connection_failed
    }

@Preview
@Composable
private fun JoinFormPreview() {
    MotoIntercomTheme {
        JoinForm(
            name = "Ana",
            onNameChange = {},
            mode = GroupMode.ONLINE,
            onMode = {},
            error = null,
            onCreate = {},
            onJoin = {},
            nearby = {},
        )
    }
}
