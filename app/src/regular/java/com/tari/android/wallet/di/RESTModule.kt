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
import com.tari.android.wallet.service.notification.NotificationService
import com.tari.android.wallet.service.notification.PushNotificationRESTGateway
import com.tari.android.wallet.service.notification.PushNotificationRESTService
import com.tari.android.wallet.util.Constants
import dagger.Module
import dagger.Provides
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import javax.inject.Named
import javax.inject.Singleton

/**
 * Dagger module to inject REST service dependencies.
 *
 * @author The Tari Development Team
 */
@Module
class RESTModule {

    object FieldName {
        const val pushNotificationHttpClient = "push_notification_http_client"
        const val pushNotificationRetrofit = "push_notification_retrofit"
    }

    private val interceptor = HttpLoggingInterceptor()


    @Provides
    @Named(FieldName.pushNotificationHttpClient)
    @Singleton
    fun providePushNotificationHttpClient(): OkHttpClient {
        interceptor.level = HttpLoggingInterceptor.Level.BODY
        return OkHttpClient.Builder().addInterceptor(interceptor).build()
    }

    @Provides
    @Named(FieldName.pushNotificationRetrofit)
    @Singleton
    fun providePushNotificationRetrofit(
        @Named(FieldName.pushNotificationHttpClient) okHttpClient: OkHttpClient
    ): Retrofit = Retrofit.Builder()
        .baseUrl(Constants.Wallet.pushNotificationServerUrl)
        .client(okHttpClient)
        .addConverterFactory(GsonConverterFactory.create())
        .build()

    @Provides
    @Singleton
    fun providePushNotificationRESTService(
        @Named(FieldName.pushNotificationRetrofit) retrofit: Retrofit
    ): PushNotificationRESTGateway = retrofit.create(PushNotificationRESTGateway::class.java)

    @Provides
    @Singleton
    fun provideNotificationService(gateway: PushNotificationRESTGateway): NotificationService =
        PushNotificationRESTService(gateway, BuildConfig.NOTIFICATIONS_API_KEY)

}
