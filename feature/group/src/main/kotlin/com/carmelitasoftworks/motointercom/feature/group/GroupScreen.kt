package com.carmelitasoftworks.motointercom.feature.group

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.carmelitasoftworks.motointercom.core.ui.PlaceholderScreen

/** Placeholder; the real screen arrives with its work order step. */
@Composable
fun GroupScreen(modifier: Modifier = Modifier) {
    PlaceholderScreen(
        titleRes = R.string.group_title,
        bodyRes = R.string.group_placeholder,
        modifier = modifier,
    )
}
