/**
 * Copyright 2020 The Tari Project
 *
 *
 * Redistribution and use in source and binary forms, with or
 * without modification, are permitted provided that the
 * following conditions are met:
 *
 *
 * 1. Redistributions of source code must retain the above copyright notice,
 * this list of conditions and the following disclaimer.
 *
 *
 * 2. Redistributions in binary form must reproduce the above
 * copyright notice, this list of conditions and the following disclaimer in the
 * documentation and/or other materials provided with the distribution.
 *
 *
 * 3. Neither the name of the copyright holder nor the names of
 * its contributors may be used to endorse or promote products
 * derived from this software without specific prior written permission.
 *
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
package com.tari.android.wallet

import android.content.Context
import androidx.test.core.app.ApplicationProvider.getApplicationContext
import com.tari.android.wallet.data.WalletConfig
import com.tari.android.wallet.data.sharedPrefs.network.NetworkPrefRepositoryImpl
import com.tari.android.wallet.di.ApplicationModule
import com.tari.android.wallet.ffi.FFICommsConfig
import com.tari.android.wallet.ffi.FFIException
import com.tari.android.wallet.ffi.FFITariTransportConfig
import com.tari.android.wallet.ffi.nullptr
import com.tari.android.wallet.util.Constants
import org.junit.After
import org.junit.Assert.assertNotEquals
import org.junit.BeforeClass
import org.junit.Test

/**
 * FFI comms config tests.
 *
 * @author The Tari Development Team
 */
class FFICommsConfigTests {

    private companion object {
        private const val DB_NAME = "tari_test_db"
        private var walletDir = ""
        private val context = getApplicationContext<Context>()
        private val prefs = context.getSharedPreferences(ApplicationModule.sharedPrefsFileName, Context.MODE_PRIVATE)
        private val networkRepository = NetworkPrefRepositoryImpl(prefs)

        @BeforeClass
        @JvmStatic
        fun fullSetup() {
            walletDir = WalletConfig(context, networkRepository).getWalletFilesDirPath()
            FFITestUtil.clearTestFiles(walletDir)
        }
    }

    @After
    fun tearDown() {
        FFITestUtil.clearTestFiles(walletDir)
    }

    @Test
    fun constructor_assertThatValidCommsConfigInstanceWasCreated() {
        val transport = FFITariTransportConfig()
        val commsConfig = FFICommsConfig(
            transport.getAddress(),
            transport,
            DB_NAME,
            walletDir,
            Constants.Wallet.DISCOVERY_TIMEOUT_SEC,
            Constants.Wallet.STORE_AND_FORWARD_MESSAGE_DURATION_SEC
        )
        assertNotEquals(nullptr, commsConfig.pointer)
        commsConfig.destroy()
        transport.destroy()
    }

    @Test(expected = FFIException::class)
    fun constructor_assertThatFFIExceptionWasThrown_ifGivenDirectoryDoesNotExist() {
        val transport = FFITariTransportConfig()
        try {
            FFICommsConfig(
                transport.getAddress(),
                transport,
                DB_NAME,
                "${walletDir}_invalid_target",
                Constants.Wallet.DISCOVERY_TIMEOUT_SEC,
                Constants.Wallet.STORE_AND_FORWARD_MESSAGE_DURATION_SEC
            )
        } catch (e: Throwable) {
            transport.destroy()
            throw e
        }
    }

}
