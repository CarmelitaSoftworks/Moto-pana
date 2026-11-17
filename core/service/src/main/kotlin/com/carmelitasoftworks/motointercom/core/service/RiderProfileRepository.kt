package com.carmelitasoftworks.motointercom.core.service

import android.content.Context
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/** The name other riders see, remembered between rides. */
@Singleton
class RiderProfileRepository @Inject constructor(@ApplicationContext context: Context) {
    private val prefs = context.getSharedPreferences("rider_profile", Context.MODE_PRIVATE)

    private val mutableName = MutableStateFlow(prefs.getString(KEY_NAME, "").orEmpty())
    val name: StateFlow<String> = mutableName.asStateFlow()

    fun setName(name: String) {
        val clean = name.take(MAX_NAME_LENGTH)
        if (clean == mutableName.value) return
        mutableName.value = clean
        prefs.edit().putString(KEY_NAME, clean).apply()
    }

    companion object {
        const val MAX_NAME_LENGTH = 20
        private const val KEY_NAME = "name"
    }
}
