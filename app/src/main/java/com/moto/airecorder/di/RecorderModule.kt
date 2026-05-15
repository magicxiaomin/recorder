package com.moto.airecorder.di

import com.moto.airecorder.service.MediaRecorderEngine
import com.moto.airecorder.service.RecorderEngine
import com.moto.airecorder.service.TimeProvider
import android.os.SystemClock
import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class RecorderBindings {
    @Binds
    @Singleton
    abstract fun bindRecorderEngine(engine: MediaRecorderEngine): RecorderEngine
}

@Module
@InstallIn(SingletonComponent::class)
object RecorderModule {
    @Provides
    @Singleton
    fun provideRecorderScope(): CoroutineScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    @Provides
    fun provideRecorderDispatcher(): CoroutineDispatcher = Dispatchers.Default

    @Provides
    fun provideTimeProvider(): TimeProvider = TimeProvider { SystemClock.elapsedRealtime() }
}
