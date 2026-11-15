package com.carmelitasoftworks.motointercom.feature.settings

import androidx.annotation.StringRes
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Button
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.carmelitasoftworks.motointercom.core.audio.SoundPreset
import com.carmelitasoftworks.motointercom.core.audio.SoundSettings
import com.carmelitasoftworks.motointercom.core.ui.theme.MotoIntercomTheme

/**
 * Volume boost plus a three-band equalizer for what the rider hears.
 *
 * [onUpdate] takes a transform of the latest settings, so two quick taps never
 * overwrite each other with a stale snapshot.
 */
@Composable
internal fun SoundSection(
    settings: SoundSettings,
    onUpdate: ((SoundSettings) -> SoundSettings) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(16.dp)) {
        Text(
            text = stringResource(R.string.sound_title),
            style = MaterialTheme.typography.headlineMedium,
        )
        Text(
            text = stringResource(R.string.sound_description),
            style = MaterialTheme.typography.bodyLarge,
        )
        DbStepper(
            labelRes = R.string.sound_boost,
            nameRes = R.string.sound_boost_name,
            value = settings.boostDb,
            onStep = { delta -> onUpdate { it.copy(boostDb = it.boostDb + delta * BOOST_STEP_DB) } },
        )
        SoundPreset.entries.forEach { preset ->
            PresetButton(
                preset = preset,
                selected = preset.matches(settings),
                onClick = { onUpdate(preset::applyTo) },
            )
        }
        DbStepper(
            labelRes = R.string.sound_bass,
            nameRes = R.string.sound_bass_name,
            value = settings.bassDb,
            onStep = { delta -> onUpdate { it.copy(bassDb = it.bassDb + delta) } },
        )
        DbStepper(
            labelRes = R.string.sound_mid,
            nameRes = R.string.sound_mid_name,
            value = settings.midDb,
            onStep = { delta -> onUpdate { it.copy(midDb = it.midDb + delta) } },
        )
        DbStepper(
            labelRes = R.string.sound_treble,
            nameRes = R.string.sound_treble_name,
            value = settings.trebleDb,
            onStep = { delta -> onUpdate { it.copy(trebleDb = it.trebleDb + delta) } },
        )
    }
}

@Composable
private fun PresetButton(preset: SoundPreset, selected: Boolean, onClick: () -> Unit, modifier: Modifier = Modifier) {
    // Full-width, glove-sized target (CLAUDE.md §6); the filled button marks the active preset.
    val sized = modifier
        .fillMaxWidth()
        .heightIn(min = GloveTarget)
    val label = @Composable {
        Text(text = stringResource(preset.labelRes), style = MaterialTheme.typography.titleMedium)
    }
    if (selected) {
        Button(onClick = onClick, modifier = sized) { label() }
    } else {
        OutlinedButton(onClick = onClick, modifier = sized) { label() }
    }
}

/**
 * A decibel value with big minus / plus buttons; easier to hit with gloves than
 * a slider. [onStep] receives -1 or +1; out-of-range results are clamped by the repository.
 */
@Composable
private fun DbStepper(
    @StringRes labelRes: Int,
    @StringRes nameRes: Int,
    value: Int,
    onStep: (Int) -> Unit,
    modifier: Modifier = Modifier,
) {
    val name = stringResource(nameRes)
    val decrease = stringResource(R.string.sound_decrease, name)
    val increase = stringResource(R.string.sound_increase, name)
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        FilledTonalButton(
            onClick = { onStep(-1) },
            modifier = Modifier
                .size(GloveTarget)
                .semantics { contentDescription = decrease },
        ) {
            Text(text = "−", style = MaterialTheme.typography.headlineSmall)
        }
        Text(
            text = stringResource(labelRes, value.signed()),
            style = MaterialTheme.typography.titleMedium,
            modifier = Modifier.weight(1f),
        )
        FilledTonalButton(
            onClick = { onStep(1) },
            modifier = Modifier
                .size(GloveTarget)
                .semantics { contentDescription = increase },
        ) {
            Text(text = "+", style = MaterialTheme.typography.headlineSmall)
        }
    }
}

private val GloveTarget = 64.dp
private const val BOOST_STEP_DB = 2

/** "+3", "0", "-3": a sign only where it carries information. */
private fun Int.signed(): String = if (this > 0) "+$this" else toString()

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
        SoundSection(settings = SoundPreset.CLEAR_VOICE.applyTo(SoundSettings(boostDb = 6)), onUpdate = {})
    }
}
