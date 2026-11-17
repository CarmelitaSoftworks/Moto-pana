package com.carmelitasoftworks.motointercom.core.rtc.di

import com.carmelitasoftworks.motointercom.core.rtc.DevTokenServerProvider
import com.carmelitasoftworks.motointercom.core.rtc.LiveKitRtcClient
import com.carmelitasoftworks.motointercom.core.rtc.RtcClient
import com.carmelitasoftworks.motointercom.core.rtc.TokenProvider
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent

@Module
@InstallIn(SingletonComponent::class)
internal interface RtcModule {
    @Binds
    fun bindRtcClient(impl: LiveKitRtcClient): RtcClient

    @Binds
    fun bindTokenProvider(impl: DevTokenServerProvider): TokenProvider
}
