package com.carmelitasoftworks.motointercom.core.rtc.di

import com.carmelitasoftworks.motointercom.core.rtc.RtcClient
import com.carmelitasoftworks.motointercom.core.rtc.UnimplementedRtcClient
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent

@Module
@InstallIn(SingletonComponent::class)
internal interface RtcModule {
    @Binds
    fun bindRtcClient(impl: UnimplementedRtcClient): RtcClient
}
