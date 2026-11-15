package com.carmelitasoftworks.motointercom.feature.settings

import androidx.annotation.StringRes
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.carmelitasoftworks.motointercom.core.audio.SoundPreset
import com.carmelitasoftworks.motointercom.core.audio.SoundSettings
import com.carmelitasoftworks.motointercom.core.ui.theme.MotoIntercomTheme
import kotlin.math.roundToInt

/** Volume boost plus a three-band equalizer for what the rider hears. */
@Composable
internal fun SoundSection(settings: SoundSettings, onChange: (SoundSettings) -> Unit, modifier: Modifier = Modifier) {
    Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(16.dp)) {
        Text(
            text = stringResource(R.string.sound_title),
            style = MaterialTheme.typography.headlineMedium,
        )
        Text(
            text = stringResource(R.string.sound_description),
            style = MaterialTheme.typography.bodyLarge,
        )
        DbSlider(
            labelRes = R.string.sound_boost,
            value = settings.boostDb,
            range = SoundSettings.BOOST_RANGE_DB,
            onChange = { onChange(settings.copy(boostDb = it)) },
        )
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            SoundPreset.entries.forEach { preset ->
                PresetButton(
                    preset = preset,
                    selected = preset.matches(settings),
                    onClick = { onChange(preset.applyTo(settings)) },
                    modifier = Modifier.weight(1f),
                )
            }
        }
        DbSlider(
            labelRes = R.string.sound_bass,
            value = settings.bassDb,
            range = SoundSettings.EQ_RANGE_DB,
            onChange = { onChange(settings.copy(bassDb = it)) },
        )
        DbSlider(
            labelRes = R.string.sound_mid,
            value = settings.midDb,
            range = SoundSettings.EQ_RANGE_DB,
            onChange = { onChange(settings.copy(midDb = it)) },
        )
        DbSlider(
            labelRes = R.string.sound_treble,
            value = settings.trebleDb,
            range = SoundSettings.EQ_RANGE_DB,
            onChange = { onChange(settings.copy(trebleDb = it)) },
        )
    }
}

@Composable
private fun PresetButton(preset: SoundPreset, selected: Boolean, onClick: () -> Unit, modifier: Modifier = Modifier) {
    // Glove-sized target (CLAUDE.md §6); the filled button marks the active preset.
    val sized = modifier.heightIn(min = 64.dp)
    val label = @Composable {
        Text(text = stringResource(preset.labelRes), textAlign = TextAlign.Center)
    }
    if (selected) {
        Button(onClick = onClick, modifier = sized) { label() }
    } else {
        OutlinedButton(onClick = onClick, modifier = sized) { label() }
    }
}

/** A whole-decibel slider with its current value in the label. */
@Composable
private fun DbSlider(
    @StringRes labelRes: Int,
    value: Int,
    range: IntRange,
    onChange: (Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier) {
        Text(
            text = stringResource(labelRes, value),
            style = MaterialTheme.typography.titleMedium,
        )
        Slider(
            value = value.toFloat(),
            onValueChange = { onChange(it.roundToInt()) },
            valueRange = range.first.toFloat()..range.last.toFloat(),
            steps = range.last - range.first - 1,
            modifier = Modifier.fillMaxWidth(),
        )
    }
}

@get:StringRes
private val SoundPreset.labelRes: Int
    get() = when (this) {
        SoundPreset.NORMAL -> R.string.sound_preset_normal
        SoundPreset.CLEAR_VOICE -> R.string.sound_preset_clear_voice
        SoundPreset.MORE_BASS -> R.string.sound_preset_more_bass
    }

@Preview
@Composable
private fun SoundSectionPreview() {
    MotoIntercomTheme {
        SoundSection(settings = SoundPreset.CLEAR_VOICE.applyTo(SoundSettings(boostDb = 6)), onChange = {})
    }
}
