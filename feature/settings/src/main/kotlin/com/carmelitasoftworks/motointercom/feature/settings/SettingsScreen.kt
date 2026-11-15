package com.carmelitasoftworks.motointercom.feature.settings

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.carmelitasoftworks.motointercom.core.ui.PlaceholderScreen

/** Placeholder; the real screen arrives with its work order step. */
@Composable
fun SettingsScreen(modifier: Modifier = Modifier) {
    PlaceholderScreen(
        titleRes = R.string.settings_title,
        bodyRes = R.string.settings_placeholder,
        modifier = modifier,
    )
}
