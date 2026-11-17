package com.carmelitasoftworks.motointercom.core.nearby.di

import com.carmelitasoftworks.motointercom.core.nearby.GmsNearbyIntercom
import com.carmelitasoftworks.motointercom.core.nearby.NearbyIntercom
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent

@Module
@InstallIn(SingletonComponent::class)
internal interface NearbyModule {
    @Binds
    fun bindNearbyIntercom(impl: GmsNearbyIntercom): NearbyIntercom
}
