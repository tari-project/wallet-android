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
import com.tari.android.wallet.ui.common.giphy.repository.GiphyRestService
import com.tari.android.wallet.ui.common.giphy.repository.GiphyRepository
import dagger.Module
import dagger.Provides
import okhttp3.Interceptor
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.create
import javax.inject.Named
import javax.inject.Singleton

@Module
class GiphyRestModule {

    private companion object {
        private const val GIPHY_BASE_URL = "https://api.giphy.com"
        private const val GIPHY_HTTP_CLIENT = "giphy_http_client"
        private const val GIPHY_RETROFIT = "giphy_retrofit"
        private const val GIPHY_QUERY_PARAM_API_KEY = "api_key"
    }

    @Provides
    @Named(GIPHY_HTTP_CLIENT)
    @Singleton
    fun provideGiphyHttpClient(): OkHttpClient = OkHttpClient.Builder()
        .also { client ->
            if (BuildConfig.DEBUG) HttpLoggingInterceptor()
                .apply { level = HttpLoggingInterceptor.Level.BODY }
                .also { client.addInterceptor(it) }
        }
        .addInterceptor { chain: Interceptor.Chain ->
            chain.request().url.newBuilder()
                .addQueryParameter(GIPHY_QUERY_PARAM_API_KEY, BuildConfig.GIPHY_KEY).build()
                .let { chain.request().newBuilder().url(it).build() }
                .let(chain::proceed)
        }
        .build()

    @Provides
    @Named(GIPHY_RETROFIT)
    @Singleton
    fun provideTestnetFaucetRetrofit(@Named(GIPHY_HTTP_CLIENT) client: OkHttpClient): Retrofit =
        Retrofit.Builder()
            .baseUrl(GIPHY_BASE_URL)
            .client(client)
            .addConverterFactory(GsonConverterFactory.create())
            .build()

    @Provides
    @Singleton
    fun provideGifRepository(@Named(GIPHY_RETROFIT) retrofit: Retrofit): GiphyRestService =
        GiphyRepository(retrofit.create())

}
