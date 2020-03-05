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

import android.content.Context
import com.tari.android.wallet.R
import dagger.Module
import dagger.Provides
import java.util.*
import javax.inject.Named
import javax.inject.Singleton

/**
 * Dagger module to inject application configuration parameters.
 *
 * @author The Tari Development Team
 */
@Module
internal class ConfigModule {

    object FieldName {
        const val deleteExistingWallet = "config_delete_existing_wallet"
        const val receiveFromAnonymous = "config_receive_from_anonymous"
        const val generateTestData = "config_generate_test_data"
    }

    @Provides
    @Singleton
    fun provideProperties(context: Context): Properties {
        val rawResource = context.resources.openRawResource(R.raw.config)
        return Properties().apply {
            load(rawResource)
            rawResource.close()
        }
    }

    @Provides
    @Named(FieldName.deleteExistingWallet)
    fun provideDeleteExistingWalletFlag(properties: Properties): Boolean {
        return properties.getProperty("wallet.delete_existing")?.toBoolean()
            ?: false
    }

    @Provides
    @Named(FieldName.receiveFromAnonymous)
    @Singleton
    fun provideReceiveFromAnonymousFlag(properties: Properties): Boolean {
        return properties.getProperty("wallet.create_new.receive_from_anonymous")?.toBoolean()
            ?: false
    }

    @Provides
    @Named(FieldName.generateTestData)
    @Singleton
    fun provideGenerarateTestDataFlag(properties: Properties): Boolean {
        return properties.getProperty("wallet.create_new.generate_test_data")?.toBoolean()
            ?: false
    }

}