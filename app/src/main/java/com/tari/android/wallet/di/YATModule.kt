package com.tari.android.wallet.di

import android.content.Context
import com.google.gson.Gson
import com.tari.android.wallet.BuildConfig
import com.tari.android.wallet.infrastructure.yat.*
import com.tari.android.wallet.infrastructure.yat.authentication.YatAuthenticationAPI
import com.tari.android.wallet.infrastructure.yat.cart.YatCartAPI
import com.tari.android.wallet.infrastructure.yat.emojiid.YatEmojiIdAPI
import com.tari.android.wallet.infrastructure.yat.user.YatUserAPI
import com.tari.android.wallet.model.yat.*
import com.tari.android.wallet.util.SharedPrefsWrapper
import dagger.Binds
import dagger.Module
import dagger.Provides
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import javax.inject.Qualifier
import javax.inject.Singleton

@Module(includes = [YATModule.BindModule::class])
class YATModule {

    @Provides
    @Singleton
    fun provideGson() = Gson()

    @Provides
    @Singleton
    fun provideYatJWTStorage(context: Context, gson: Gson): YatJWTStorage =
        PreferencesJWTStorage(context, gson)

    @Provides
    @Singleton
    fun provideYatUserStorage(context: Context, gson: Gson): YatUserStorage =
        PreferencesGSONUserStorage(context, gson)

    @Provides
    @Singleton
    @YatUnauthenticatedRetrofit
    fun provideYatUnauthenticatedRetrofit(): Retrofit = Retrofit.Builder()
        .baseUrl(YAT_API_BASE_URL)
        .client(
            OkHttpClient.Builder()
                .also { client ->
                    if (BuildConfig.DEBUG) HttpLoggingInterceptor()
                        .apply { level = HttpLoggingInterceptor.Level.BODY }
                        .also { client.addInterceptor(it) }
                }.build()
        )
        .addConverterFactory(GsonConverterFactory.create())
        .build()

    @Provides
    @Singleton
    @YatAuthenticatingRetrofit
    fun provideYatAuthenticatingRetrofit(
        storage: YatJWTStorage,
        authenticationAPI: YatAuthenticationAPI
    ): Retrofit = Retrofit.Builder()
        .baseUrl(YAT_API_BASE_URL)
        .client(
            OkHttpClient.Builder()
                .also { client ->
                    if (BuildConfig.DEBUG) HttpLoggingInterceptor()
                        .apply { level = HttpLoggingInterceptor.Level.BODY }
                        .also { client.addInterceptor(it) }
                }
                .addInterceptor(
                    YatAuthenticationInterceptor(storage, emptyList(), authenticationAPI)
                )
                .build()
        )
        .addConverterFactory(GsonConverterFactory.create())
        .build()

    @Provides
    @Singleton
    fun provideYatAuthenticationAPI(@YatUnauthenticatedRetrofit retrofit: Retrofit): YatAuthenticationAPI =
        retrofit.create(YatAuthenticationAPI::class.java)

    @Provides
    @Singleton
    fun provideYatUserAPI(@YatUnauthenticatedRetrofit retrofit: Retrofit): YatUserAPI =
        retrofit.create(YatUserAPI::class.java)

    @Provides
    @Singleton
    fun provideYatEmojiIdAPI(@YatAuthenticatingRetrofit retrofit: Retrofit): YatEmojiIdAPI =
        retrofit.create(YatEmojiIdAPI::class.java)

    @Provides
    @Singleton
    fun provideYatCartAPI(@YatAuthenticatingRetrofit retrofit: Retrofit): YatCartAPI =
        retrofit.create(YatCartAPI::class.java)

    @Provides
    @Singleton
    fun provideYATService(
        authenticationAPI: YatAuthenticationAPI,
        userAPI: YatUserAPI,
        emojiIdAPI: YatEmojiIdAPI,
        cartAPI: YatCartAPI,
        jwtStorage: YatJWTStorage,
        userStorage: YatUserStorage,
        preferences: SharedPrefsWrapper,
        set: ActualizingEmojiSet,
    ): YatService = RESTYatService(
        authenticationAPI,
        userAPI,
        emojiIdAPI,
        cartAPI,
        jwtStorage,
        userStorage,
        preferences,
        set
    )

    @Qualifier
    private annotation class YatAuthenticatingRetrofit

    @Qualifier
    private annotation class YatUnauthenticatedRetrofit

    @Provides
    @Singleton
    fun provideActualizingEmojiSet(
        context: Context,
        api: YatEmojiIdAPI,
        gson: Gson
    ): ActualizingEmojiSet =
        YatAPISynchronizingSet(AtomicCacheEmojiSetDecorator(PersistingEmojiSet(context, gson)), api)

    @Module
    interface BindModule {
        @Binds
        @Singleton
        fun bindSet(set: ActualizingEmojiSet): EmojiSet
    }

    private companion object {
        //private const val YAT_API_BASE_URL = "https://api-dev.yat.rocks/"
        private const val YAT_API_BASE_URL = "https://activated.scratch.emojid.me/api/"
    }

}
