package com.tari.android.wallet.di

import dagger.Module
import dagger.Provides
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.plus
import javax.inject.Named
import javax.inject.Qualifier
import javax.inject.Singleton

@Retention(AnnotationRetention.RUNTIME)
@Qualifier
annotation class ApplicationScope

@Module
class CoroutinesDispatchersModule {
    private companion object {
        private const val DEFAULT = "DEFAULT"
        private const val IO = "IO"
        private const val MAIN = "MAIN"
        private const val MAIN_IMMEDIATE = "MAIN_IMMEDIATE"
    }

    @Named(DEFAULT)
    @Provides
    fun providesDefaultDispatcher(): CoroutineDispatcher = Dispatchers.Default

    @Named(IO)
    @Provides
    fun providesIoDispatcher(): CoroutineDispatcher = Dispatchers.IO

    @Named(MAIN)
    @Provides
    fun providesMainDispatcher(): CoroutineDispatcher = Dispatchers.Main

    @Named(MAIN_IMMEDIATE)
    @Provides
    fun providesMainImmediateDispatcher(): CoroutineDispatcher = Dispatchers.Main.immediate

    @Singleton
    @ApplicationScope
    @Provides
    fun providesCoroutineScope(
        @Named(DEFAULT) defaultDispatcher: CoroutineDispatcher
    ): CoroutineScope = MainScope() + defaultDispatcher
}
