package com.tari.android.wallet.di

import android.content.Context
import com.google.gson.Gson
import com.tari.android.wallet.infrastructure.yat.PreferencesGSONUserStorage
import com.tari.android.wallet.infrastructure.yat.YatService
import com.tari.android.wallet.infrastructure.yat.YatServiceImpl
import com.tari.android.wallet.infrastructure.yat.YatUserStorage
import com.tari.android.wallet.infrastructure.yat.adapter.YatAdapter
import com.tari.android.wallet.infrastructure.yat.adapter.YatAdapterImpl
import com.tari.android.wallet.model.yat.*
import com.tari.android.wallet.util.SharedPrefsWrapper
import dagger.Binds
import dagger.Module
import dagger.Provides
import javax.inject.Singleton

@Module(includes = [YATModule.BindModule::class])
class YATModule {

    @Provides
    @Singleton
    fun provideGson() = Gson()

    @Provides
    @Singleton
    fun provideYatUserStorage(context: Context, gson: Gson): YatUserStorage =
        PreferencesGSONUserStorage(context, gson)

    @Provides
    @Singleton
    fun provideYatAdapter(
        yatUserStorage: YatUserStorage,
        sharedPrefsWrapper: SharedPrefsWrapper,
        context: Context
    ): YatAdapter =
        YatAdapterImpl(yatUserStorage, sharedPrefsWrapper, context)

    @Provides
    @Singleton
    fun provideYATService(
        yatAdapter: YatAdapter,
        userStorage: YatUserStorage,
        emojiSet: ActualizingEmojiSet
    ): YatService = YatServiceImpl(
        yatAdapter,
        userStorage,
        emojiSet
    )

    @Provides
    @Singleton
    fun provideActualizingEmojiSet(
        context: Context,
        api: YatAdapter,
        gson: Gson
    ): ActualizingEmojiSet =
        YatAPISynchronizingSet(AtomicCacheEmojiSetDecorator(PersistingEmojiSet(context, gson)), api)

    @Module
    interface BindModule {
        @Binds
        @Singleton
        fun bindSet(set: ActualizingEmojiSet): EmojiSet
    }
}
