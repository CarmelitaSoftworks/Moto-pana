package com.carmelitasoftworks.motointercom.core.nearby

import androidx.annotation.StringRes
import com.google.android.gms.common.api.ApiException
import com.google.android.gms.nearby.connection.ConnectionsStatusCodes

/** Why advertising, discovery or connecting failed, in terms the rider can act on. */
data class NearbyError(val kind: Kind, val statusCode: Int) {
    enum class Kind { LOCATION_OFF, RADIO, PERMISSION, BUSY, OTHER }

    @get:StringRes
    val messageRes: Int
        get() = when (kind) {
            Kind.LOCATION_OFF -> R.string.nearby_error_location_off
            Kind.RADIO -> R.string.nearby_error_radio
            Kind.PERMISSION -> R.string.nearby_error_permission
            Kind.BUSY -> R.string.nearby_error_busy
            Kind.OTHER -> R.string.nearby_error_other
        }

    companion object {
        private val permissionCodes = setOf(
            ConnectionsStatusCodes.MISSING_PERMISSION_NEARBY_WIFI_DEVICES,
            ConnectionsStatusCodes.MISSING_PERMISSION_BLUETOOTH,
            ConnectionsStatusCodes.MISSING_PERMISSION_BLUETOOTH_ADMIN,
            ConnectionsStatusCodes.MISSING_PERMISSION_ACCESS_WIFI_STATE,
            ConnectionsStatusCodes.MISSING_PERMISSION_CHANGE_WIFI_STATE,
            ConnectionsStatusCodes.MISSING_PERMISSION_ACCESS_COARSE_LOCATION,
            ConnectionsStatusCodes.MISSING_PERMISSION_RECORD_AUDIO,
            ConnectionsStatusCodes.MISSING_PERMISSION_ACCESS_FINE_LOCATION,
            ConnectionsStatusCodes.MISSING_PERMISSION_BLUETOOTH_SCAN,
            ConnectionsStatusCodes.MISSING_PERMISSION_BLUETOOTH_ADVERTISE,
            ConnectionsStatusCodes.MISSING_PERMISSION_BLUETOOTH_CONNECT,
        )

        // Older Play services still report the deprecated codes, so keep mapping them.
        @Suppress("DEPRECATION")
        fun fromStatus(code: Int): NearbyError = NearbyError(
            kind = when (code) {
                ConnectionsStatusCodes.MISSING_SETTING_LOCATION_MUST_BE_ON -> Kind.LOCATION_OFF
                ConnectionsStatusCodes.STATUS_BLUETOOTH_ERROR, ConnectionsStatusCodes.STATUS_RADIO_ERROR -> Kind.RADIO
                in permissionCodes -> Kind.PERMISSION
                ConnectionsStatusCodes.API_CONNECTION_FAILED_ALREADY_IN_USE -> Kind.BUSY
                else -> Kind.OTHER
            },
            statusCode = code,
        )

        fun from(exception: Exception): NearbyError =
            fromStatus((exception as? ApiException)?.statusCode ?: ConnectionsStatusCodes.STATUS_ERROR)
    }
}
