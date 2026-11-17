package com.carmelitasoftworks.motointercom.feature.group

import androidx.annotation.StringRes
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
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
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
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
import com.carmelitasoftworks.motointercom.core.service.GroupCode
import com.carmelitasoftworks.motointercom.core.service.IntercomService
import com.carmelitasoftworks.motointercom.core.service.IntercomSession
import com.carmelitasoftworks.motointercom.core.service.JoinError
import com.carmelitasoftworks.motointercom.core.service.RiderProfileRepository
import com.carmelitasoftworks.motointercom.core.ui.BigButton
import com.carmelitasoftworks.motointercom.core.ui.BigOutlinedButton
import com.carmelitasoftworks.motointercom.core.ui.rememberRidePermissionRequest
import com.carmelitasoftworks.motointercom.core.ui.theme.MotoIntercomTheme

@Composable
fun GroupScreen(onRideStarted: () -> Unit, modifier: Modifier = Modifier, viewModel: GroupViewModel = hiltViewModel()) {
    val session by viewModel.session.collectAsStateWithLifecycle()
    val name by viewModel.name.collectAsStateWithLifecycle()
    val context = LocalContext.current

    var problem by rememberSaveable { mutableStateOf<Int?>(null) }
    var pendingCode by rememberSaveable { mutableStateOf<String?>(null) }

    val requestPermissions = rememberRidePermissionRequest(
        onGranted = {
            val code = pendingCode
            if (code == null) viewModel.createGroup() else viewModel.join(code)
            IntercomService.start(context)
            onRideStarted()
        },
        onDenied = { problem = R.string.group_mic_needed },
    )

    fun start(code: String?) {
        viewModel.dismissError()
        problem = null
        when {
            name.isBlank() -> problem = R.string.group_name_needed
            code != null && !GroupCode.isValid(code) -> problem = R.string.group_code_needed
            else -> {
                pendingCode = code
                requestPermissions()
            }
        }
    }

    val active = session as? IntercomSession.Active
    val error = (session as? IntercomSession.Failed)?.error?.messageRes ?: problem
    if (active != null) {
        InRide(code = active.code, onGoToRide = onRideStarted, modifier = modifier)
    } else {
        JoinForm(
            name = name,
            onNameChange = viewModel::setName,
            error = error,
            onCreate = { start(code = null) },
            onJoin = { start(code = it) },
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
    @StringRes error: Int?,
    onCreate: () -> Unit,
    onJoin: (String) -> Unit,
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
        BigButton(text = stringResource(R.string.group_create), onClick = onCreate)
        Text(
            text = stringResource(R.string.group_or),
            style = MaterialTheme.typography.titleMedium,
            textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth(),
        )
        OutlinedTextField(
            value = code,
            onValueChange = { code = GroupCode.normalize(it) },
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
        if (error != null) {
            Text(
                text = stringResource(error),
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.error,
            )
        }
        Text(
            text = stringResource(R.string.group_coverage_note),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
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
        JoinForm(name = "Ana", onNameChange = {}, error = null, onCreate = {}, onJoin = {})
    }
}
