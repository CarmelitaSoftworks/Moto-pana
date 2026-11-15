package com.carmelitasoftworks.motointercom.core.audio.di

import com.carmelitasoftworks.motointercom.core.audio.AndroidAudioRouter
import com.carmelitasoftworks.motointercom.core.audio.AndroidMicLoopback
import com.carmelitasoftworks.motointercom.core.audio.AudioRouter
import com.carmelitasoftworks.motointercom.core.audio.MicLoopback
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent

@Module
@InstallIn(SingletonComponent::class)
internal interface AudioModule {
    @Binds
    fun bindAudioRouter(impl: AndroidAudioRouter): AudioRouter

    @Binds
    fun bindMicLoopback(impl: AndroidMicLoopback): MicLoopback
}
