package com.carmelitasoftworks.motointercom.feature.settings

import android.Manifest
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.StringRes
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.carmelitasoftworks.motointercom.core.audio.AudioRoute
import com.carmelitasoftworks.motointercom.core.ui.theme.MotoIntercomTheme

@Composable
fun SettingsScreen(modifier: Modifier = Modifier, viewModel: SettingsViewModel = hiltViewModel()) {
    val route by viewModel.route.collectAsStateWithLifecycle()
    val testing by viewModel.testing.collectAsStateWithLifecycle()
    val sound by viewModel.sound.collectAsStateWithLifecycle()

    val context = LocalContext.current
    val micPermission = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        if (granted) viewModel.startTest()
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(24.dp),
        verticalArrangement = Arrangement.spacedBy(32.dp),
    ) {
        HeadsetTest(route = route, testing = testing, onToggleTest = {
            val granted = context.checkSelfPermission(Manifest.permission.RECORD_AUDIO) ==
                PackageManager.PERMISSION_GRANTED
            when {
                testing -> viewModel.stopTest()
                granted -> viewModel.startTest()
                else -> micPermission.launch(Manifest.permission.RECORD_AUDIO)
            }
        })
        SoundSection(settings = sound, onChange = viewModel::updateSound)
    }
}

@Composable
private fun HeadsetTest(route: AudioRoute, testing: Boolean, onToggleTest: () -> Unit, modifier: Modifier = Modifier) {
    Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(16.dp)) {
        Text(
            text = stringResource(R.string.headset_test_title),
            style = MaterialTheme.typography.headlineMedium,
        )
        Text(
            text = stringResource(R.string.headset_test_description),
            style = MaterialTheme.typography.bodyLarge,
        )
        if (testing) {
            Text(
                text = stringResource(R.string.headset_test_route, stringResource(route.labelRes)),
                style = MaterialTheme.typography.titleLarge,
                color = MaterialTheme.colorScheme.primary,
            )
            if (route == AudioRoute.SPEAKER) {
                Text(
                    text = stringResource(R.string.headset_test_no_headset),
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.error,
                )
            }
        }
        // Glove-sized target (CLAUDE.md §6).
        Button(
            onClick = onToggleTest,
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = 72.dp),
        ) {
            Text(
                text = stringResource(if (testing) R.string.headset_test_stop else R.string.headset_test_start),
                style = MaterialTheme.typography.titleLarge,
            )
        }
    }
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
private fun HeadsetTestPreview() {
    MotoIntercomTheme {
        HeadsetTest(route = AudioRoute.BLUETOOTH_SCO, testing = true, onToggleTest = {})
    }
}
