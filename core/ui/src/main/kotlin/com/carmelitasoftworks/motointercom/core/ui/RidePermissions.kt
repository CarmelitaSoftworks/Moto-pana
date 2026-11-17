package com.carmelitasoftworks.motointercom.core.ui

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.platform.LocalContext

/** Microphone, notification and Bluetooth permissions any ride needs, for this Android version. */
val ridePermissions: List<String> = buildList {
    add(Manifest.permission.RECORD_AUDIO)
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) add(Manifest.permission.POST_NOTIFICATIONS)
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) add(Manifest.permission.BLUETOOTH_CONNECT)
}

/** What Nearby Connections needs to find and link phones without internet, per Android version. */
val nearbyPermissions: List<String> = buildList {
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
        add(Manifest.permission.BLUETOOTH_SCAN)
        add(Manifest.permission.BLUETOOTH_ADVERTISE)
        add(Manifest.permission.BLUETOOTH_CONNECT)
    }
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
        add(Manifest.permission.NEARBY_WIFI_DEVICES)
    } else {
        add(Manifest.permission.ACCESS_FINE_LOCATION)
    }
}

fun hasPermissions(context: Context, permissions: List<String>): Boolean =
    permissions.all { context.checkSelfPermission(it) == PackageManager.PERMISSION_GRANTED }

/**
 * Returns a function that asks for [permissions] if needed and then calls
 * [onGranted] when every one in [required] is granted, or [onDenied] otherwise.
 */
@Composable
fun rememberPermissionRequest(
    permissions: List<String>,
    required: List<String>,
    onGranted: () -> Unit,
    onDenied: () -> Unit,
): () -> Unit {
    val context = LocalContext.current
    val granted by rememberUpdatedState(onGranted)
    val denied by rememberUpdatedState(onDenied)
    val launcher = rememberLauncherForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) {
        if (hasPermissions(context, required)) granted() else denied()
    }
    return {
        val missing = permissions.filter { context.checkSelfPermission(it) != PackageManager.PERMISSION_GRANTED }
        if (missing.isEmpty()) granted() else launcher.launch(missing.distinct().toTypedArray())
    }
}

/**
 * Asks for [ridePermissions] if needed, then calls [onGranted] (microphone granted)
 * or [onDenied]. Notifications and Bluetooth are optional: without them the ride
 * still works, just less comfortably.
 */
@Composable
fun rememberRidePermissionRequest(onGranted: () -> Unit, onDenied: () -> Unit): () -> Unit = rememberPermissionRequest(
    permissions = ridePermissions,
    required = listOf(Manifest.permission.RECORD_AUDIO),
    onGranted = onGranted,
    onDenied = onDenied,
)
