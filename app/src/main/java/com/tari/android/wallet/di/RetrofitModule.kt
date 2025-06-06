/**
 * Copyright 2020 The Tari Project
 *
 * Redistribution and use in source and binary forms, with or
 * without modification, are permitted provided that the
 * following conditions are met:
 *
 * 1. Redistributions of source code must retain the above copyright notice,
 * this list of conditions and the following disclaimer.
 *
 * 2. Redistributions in binary form must reproduce the above
 * copyright notice, this list of conditions and the following disclaimer in the
 * documentation and/or other materials provided with the distribution.
 *
 * 3. Neither the name of the copyright holder nor the names of
 * its contributors may be used to endorse or promote products
 * derived from this software without specific prior written permission.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND
 * CONTRIBUTORS "AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES,
 * INCLUDING, BUT NOT LIMITED TO, THE IMPLIED WARRANTIES
 * OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 * DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDER OR
 * CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL,
 * SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT
 * NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES;
 * LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION)
 * HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN
 * CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE
 * OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS
 * SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */
package com.tari.android.wallet.di

import com.tari.android.wallet.BuildConfig
import com.tari.android.wallet.data.airdrop.AirdropRetrofitService
import com.tari.android.wallet.data.push.PushRetrofitService
import dagger.Module
import dagger.Provides
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import javax.inject.Named
import javax.inject.Singleton

@Module
class RetrofitModule {

    private companion object {
        private const val AIRDROP_BASE_URL = "https://airdrop.tari.com"
        private const val PUSH_BASE_URL = "https://push.tari.com"

        private const val RETROFIT_AIRDROP = "airdrop_retrofit"
        private const val RETROFIT_PUSH = "push_retrofit"
    }

    @Provides
    @Named(RETROFIT_AIRDROP)
    @Singleton
    fun provideAirdropHttpClient(): OkHttpClient = OkHttpClient.Builder()
        .addLoggingIfDebug()
        .build()

    @Provides
    @Named(RETROFIT_AIRDROP)
    @Singleton
    fun provideAirdropRetrofit(@Named(RETROFIT_AIRDROP) client: OkHttpClient): Retrofit =
        Retrofit.Builder()
            .baseUrl(AIRDROP_BASE_URL)
            .client(client)
            .addConverterFactory(GsonConverterFactory.create())
            .build()

    @Provides
    @Singleton
    fun provideAirdropRepository(@Named(RETROFIT_AIRDROP) retrofit: Retrofit): AirdropRetrofitService =
        retrofit.create(AirdropRetrofitService::class.java)

    @Provides
    @Named(RETROFIT_PUSH)
    @Singleton
    fun providePushHttpClient(): OkHttpClient = OkHttpClient.Builder()
        .addLoggingIfDebug()
        .build()

    @Provides
    @Named(RETROFIT_PUSH)
    @Singleton
    fun providePushRetrofit(@Named(RETROFIT_PUSH) client: OkHttpClient): Retrofit =
        Retrofit.Builder()
            .baseUrl(PUSH_BASE_URL)
            .client(client)
            .addConverterFactory(GsonConverterFactory.create())
            .build()

    @Provides
    @Singleton
    fun providePushRepository(@Named(RETROFIT_PUSH) retrofit: Retrofit): PushRetrofitService =
        retrofit.create(PushRetrofitService::class.java)

    private fun OkHttpClient.Builder.addLoggingIfDebug(): OkHttpClient.Builder {
        if (BuildConfig.DEBUG) {
            val loggingInterceptor = HttpLoggingInterceptor().apply {
                level = HttpLoggingInterceptor.Level.BODY
            }
            addInterceptor(loggingInterceptor)
        }
        return this
    }
}
