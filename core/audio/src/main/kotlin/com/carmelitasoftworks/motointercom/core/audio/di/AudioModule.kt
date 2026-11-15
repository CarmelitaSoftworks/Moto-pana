package com.carmelitasoftworks.motointercom.core.audio.di

import com.carmelitasoftworks.motointercom.core.audio.AudioRouter
import com.carmelitasoftworks.motointercom.core.audio.UnimplementedAudioRouter
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent

@Module
@InstallIn(SingletonComponent::class)
internal interface AudioModule {
    @Binds
    fun bindAudioRouter(impl: UnimplementedAudioRouter): AudioRouter
}
