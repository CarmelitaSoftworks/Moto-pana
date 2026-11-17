package com.carmelitasoftworks.motointercom.core.service.di

import com.carmelitasoftworks.motointercom.core.service.SessionScope
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob

@Module
@InstallIn(SingletonComponent::class)
internal object SessionModule {
    @Provides
    @Singleton
    @SessionScope
    fun provideSessionScope(): CoroutineScope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
}
