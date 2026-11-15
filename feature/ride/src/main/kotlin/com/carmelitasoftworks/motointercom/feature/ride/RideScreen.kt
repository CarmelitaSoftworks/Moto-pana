package com.carmelitasoftworks.motointercom.feature.ride

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.carmelitasoftworks.motointercom.core.ui.PlaceholderScreen

/** Placeholder; the real screen arrives with its work order step. */
@Composable
fun RideScreen(modifier: Modifier = Modifier) {
    PlaceholderScreen(
        titleRes = R.string.ride_title,
        bodyRes = R.string.ride_placeholder,
        modifier = modifier,
    )
}
