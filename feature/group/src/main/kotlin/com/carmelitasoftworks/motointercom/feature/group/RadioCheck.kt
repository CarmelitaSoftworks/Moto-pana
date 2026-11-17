package com.carmelitasoftworks.motointercom.feature.group

import android.bluetooth.BluetoothManager
import android.content.Context
import android.content.Intent
import android.location.LocationManager
import android.net.wifi.WifiManager
import android.os.Build
import android.provider.Settings
import androidx.annotation.StringRes
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner

/** A phone setting that must be on for nearby mode, with where to turn it on. */
internal enum class RadioCheck(@StringRes val messageRes: Int, val settingsAction: String) {
    BLUETOOTH(R.string.check_bluetooth, Settings.ACTION_BLUETOOTH_SETTINGS),
    WIFI(R.string.check_wifi, Settings.ACTION_WIFI_SETTINGS),
    LOCATION(R.string.check_location, Settings.ACTION_LOCATION_SOURCE_SETTINGS),
    ;

    fun openSettings(context: Context) = context.startActivity(Intent(settingsAction))
}

/** The checks that currently fail; re-evaluated every time the screen comes back to the front. */
@Composable
internal fun rememberFailingRadioChecks(onResume: () -> Unit): List<RadioCheck> {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val resumed by rememberUpdatedState(onResume)
    var failing by remember { mutableStateOf(failingChecks(context)) }
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                failing = failingChecks(context)
                resumed()
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }
    return failing
}

private fun failingChecks(context: Context): List<RadioCheck> = buildList {
    val bluetooth = context.getSystemService(BluetoothManager::class.java)?.adapter
    if (bluetooth?.isEnabled != true) add(RadioCheck.BLUETOOTH)
    val wifi = context.applicationContext.getSystemService(WifiManager::class.java)
    if (wifi?.isWifiEnabled != true) add(RadioCheck.WIFI)
    if (!isLocationOn(context)) add(RadioCheck.LOCATION)
}

@Suppress("DEPRECATION") // LOCATION_MODE is the only way below API 28.
private fun isLocationOn(context: Context): Boolean =
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
        context.getSystemService(LocationManager::class.java)?.isLocationEnabled == true
    } else {
        Settings.Secure.getInt(context.contentResolver, Settings.Secure.LOCATION_MODE, 0) !=
            Settings.Secure.LOCATION_MODE_OFF
    }
