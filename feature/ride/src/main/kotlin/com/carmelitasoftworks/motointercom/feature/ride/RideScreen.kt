package com.carmelitasoftworks.motointercom.feature.ride

import android.content.Context
import android.content.Intent
import androidx.annotation.StringRes
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.carmelitasoftworks.motointercom.core.audio.AudioRoute
import com.carmelitasoftworks.motointercom.core.rtc.RemoteRider
import com.carmelitasoftworks.motointercom.core.rtc.RtcConnectionState
import com.carmelitasoftworks.motointercom.core.service.IntercomSession
import com.carmelitasoftworks.motointercom.core.service.TalkMode
import com.carmelitasoftworks.motointercom.core.ui.BigButton
import com.carmelitasoftworks.motointercom.core.ui.BigOutlinedButton
import com.carmelitasoftworks.motointercom.core.ui.GloveTarget
import com.carmelitasoftworks.motointercom.core.ui.theme.MotoIntercomTheme

@Composable
fun RideScreen(onGoToGroup: () -> Unit, modifier: Modifier = Modifier, viewModel: RideViewModel = hiltViewModel()) {
    val session by viewModel.session.collectAsStateWithLifecycle()
    val context = LocalContext.current
    when (val current = session) {
        is IntercomSession.Active -> ActiveRide(
            session = current,
            actions = RideActions(
                onSelfMuted = viewModel::setSelfMuted,
                onTalkMode = viewModel::setTalkMode,
                onPushToTalk = viewModel::setPushToTalkHeld,
                onRiderMuted = viewModel::setRiderMuted,
                onShare = { shareCode(context, current.code) },
                onLeave = viewModel::leave,
            ),
            modifier = modifier,
        )
        is IntercomSession.Failed -> NoRide(
            message = R.string.ride_failed,
            onGoToGroup = onGoToGroup,
            modifier = modifier,
        )
        IntercomSession.Idle -> NoRide(message = R.string.ride_no_group, onGoToGroup = onGoToGroup, modifier = modifier)
    }
}

private class RideActions(
    val onSelfMuted: (Boolean) -> Unit,
    val onTalkMode: (TalkMode) -> Unit,
    val onPushToTalk: (Boolean) -> Unit,
    val onRiderMuted: (String, Boolean) -> Unit,
    val onShare: () -> Unit,
    val onLeave: () -> Unit,
)

@Composable
private fun NoRide(@StringRes message: Int, onGoToGroup: () -> Unit, modifier: Modifier = Modifier) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(24.dp, Alignment.CenterVertically),
    ) {
        Text(
            text = stringResource(message),
            style = MaterialTheme.typography.headlineSmall,
            textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth(),
        )
        BigButton(text = stringResource(R.string.ride_go_to_group), onClick = onGoToGroup)
    }
}

@Composable
private fun ActiveRide(session: IntercomSession.Active, actions: RideActions, modifier: Modifier = Modifier) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Header(session = session, onShare = actions.onShare)
        BatteryCard()
        when (session.talkMode) {
            TalkMode.OPEN -> MuteButton(
                muted = session.selfMuted,
                speaking = session.meSpeaking,
                onToggle = { actions.onSelfMuted(!session.selfMuted) },
            )
            TalkMode.PUSH_TO_TALK -> PushToTalkButton(talking = session.transmitting, onHeld = actions.onPushToTalk)
        }
        ModeSelector(mode = session.talkMode, onMode = actions.onTalkMode)
        Riders(riders = session.riders, onRiderMuted = actions.onRiderMuted)
        Spacer(Modifier.heightIn(min = 8.dp))
        BigOutlinedButton(text = stringResource(R.string.ride_leave), onClick = actions.onLeave)
    }
}

@Composable
private fun Header(session: IntercomSession.Active, onShare: () -> Unit) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Column(Modifier.weight(1f)) {
            Text(
                text = stringResource(R.string.ride_code, session.code),
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold,
            )
            Text(
                text = stringResource(session.connection.labelRes),
                style = MaterialTheme.typography.titleMedium,
                color = when (session.connection) {
                    RtcConnectionState.CONNECTED -> MaterialTheme.colorScheme.secondary
                    RtcConnectionState.RECONNECTING -> MaterialTheme.colorScheme.error
                    else -> MaterialTheme.colorScheme.onSurfaceVariant
                },
            )
            Text(
                text = stringResource(R.string.route_label, stringResource(session.route.labelRes)),
                style = MaterialTheme.typography.bodyLarge,
                color = if (session.route == AudioRoute.SPEAKER) {
                    MaterialTheme.colorScheme.error
                } else {
                    MaterialTheme.colorScheme.onSurfaceVariant
                },
            )
        }
        FilledTonalButton(onClick = onShare, modifier = Modifier.heightIn(min = GloveTarget)) {
            Text(stringResource(R.string.ride_share))
        }
    }
}

@Composable
private fun MuteButton(muted: Boolean, speaking: Boolean, onToggle: () -> Unit) {
    val colors = if (muted) {
        ButtonDefaults.buttonColors(
            containerColor = MaterialTheme.colorScheme.error,
            contentColor = MaterialTheme.colorScheme.onError,
        )
    } else {
        ButtonDefaults.buttonColors()
    }
    BigButton(
        text = stringResource(if (muted) R.string.mic_off else R.string.mic_on),
        onClick = onToggle,
        colors = colors,
        modifier = Modifier.heightIn(min = 120.dp),
    )
    if (speaking && !muted) {
        Text(
            text = stringResource(R.string.you_speaking),
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.secondary,
        )
    }
}

@Composable
private fun PushToTalkButton(talking: Boolean, onHeld: (Boolean) -> Unit) {
    val container = if (talking) MaterialTheme.colorScheme.secondary else MaterialTheme.colorScheme.primary
    val label = stringResource(if (talking) R.string.ptt_talking else R.string.ptt_hold)
    Box(
        contentAlignment = Alignment.Center,
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 160.dp)
            .clip(RoundedCornerShape(24.dp))
            .background(container)
            .semantics { contentDescription = label }
            .pointerInput(Unit) {
                detectTapGestures(
                    onPress = {
                        onHeld(true)
                        try {
                            tryAwaitRelease()
                        } finally {
                            onHeld(false)
                        }
                    },
                )
            },
    ) {
        Text(text = label, style = MaterialTheme.typography.headlineSmall, color = Color.Black)
    }
}

@Composable
private fun ModeSelector(mode: TalkMode, onMode: (TalkMode) -> Unit) {
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        TalkMode.entries.forEach { option ->
            val selected = option == mode
            val label = stringResource(if (option == TalkMode.OPEN) R.string.mode_open else R.string.mode_ptt)
            if (selected) {
                BigButton(text = label, onClick = {}, modifier = Modifier.weight(1f))
            } else {
                BigOutlinedButton(text = label, onClick = { onMode(option) }, modifier = Modifier.weight(1f))
            }
        }
    }
}

@Composable
private fun Riders(riders: List<RemoteRider>, onRiderMuted: (String, Boolean) -> Unit) {
    Text(text = stringResource(R.string.riders_title), style = MaterialTheme.typography.titleLarge)
    if (riders.isEmpty()) {
        Text(
            text = stringResource(R.string.riders_empty),
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
    riders.forEach { rider ->
        val description = stringResource(
            if (rider.mutedByMe) R.string.rider_unmute else R.string.rider_mute,
            rider.name,
        )
        Card(
            onClick = { onRiderMuted(rider.id, !rider.mutedByMe) },
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer),
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = GloveTarget)
                .semantics { contentDescription = description },
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.padding(16.dp),
            ) {
                Box(
                    Modifier
                        .size(18.dp)
                        .clip(CircleShape)
                        .background(
                            if (rider.speaking && !rider.mutedByMe) {
                                MaterialTheme.colorScheme.secondary
                            } else {
                                MaterialTheme.colorScheme.surfaceVariant
                            },
                        ),
                )
                Spacer(Modifier.width(16.dp))
                Text(text = rider.name, style = MaterialTheme.typography.titleLarge, modifier = Modifier.weight(1f))
                val status = when {
                    rider.mutedByMe -> stringResource(R.string.rider_muted)
                    rider.speaking -> stringResource(R.string.rider_speaking)
                    else -> ""
                }
                Text(
                    text = status,
                    style = MaterialTheme.typography.titleMedium,
                    color = if (rider.mutedByMe) {
                        MaterialTheme.colorScheme.error
                    } else {
                        MaterialTheme.colorScheme.secondary
                    },
                )
            }
        }
    }
}

private fun shareCode(context: Context, code: String) {
    val send = Intent(Intent.ACTION_SEND)
        .setType("text/plain")
        .putExtra(Intent.EXTRA_TEXT, context.getString(R.string.ride_share_text, code))
    context.startActivity(Intent.createChooser(send, null))
}

@get:StringRes
private val RtcConnectionState.labelRes: Int
    get() = when (this) {
        RtcConnectionState.CONNECTED -> R.string.status_connected
        RtcConnectionState.RECONNECTING -> R.string.status_reconnecting
        RtcConnectionState.CONNECTING, RtcConnectionState.DISCONNECTED -> R.string.status_connecting
    }

@get:StringRes
private val AudioRoute.labelRes: Int
    get() = when (this) {
        AudioRoute.NONE -> R.string.route_none
        AudioRoute.BLUETOOTH_SCO -> R.string.route_bluetooth_sco
        AudioRoute.BLUETOOTH_LE -> R.string.route_bluetooth_le
        AudioRoute.WIRED_HEADSET -> R.string.route_wired_headset
        AudioRoute.SPEAKER -> R.string.route_speaker
    }

@Preview
@Composable
private fun ActiveRidePreview() {
    MotoIntercomTheme {
        ActiveRide(
            session = IntercomSession.Active(
                code = "ABC234",
                connection = RtcConnectionState.CONNECTED,
                route = AudioRoute.BLUETOOTH_SCO,
                talkMode = TalkMode.OPEN,
                selfMuted = false,
                transmitting = true,
                meSpeaking = false,
                riders = listOf(
                    RemoteRider(id = "1", name = "Ana", speaking = true, mutedByMe = false),
                    RemoteRider(id = "2", name = "Luis", speaking = false, mutedByMe = true),
                ),
            ),
            actions = RideActions({}, {}, {}, { _, _ -> }, {}, {}),
        )
    }
}
