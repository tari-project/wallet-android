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
import androidx.test.core.app.ApplicationProvider
import com.tari.android.wallet.di.TorModule
import com.tari.android.wallet.di.WalletModule
import com.tari.android.wallet.ffi.*
import com.tari.android.wallet.util.Constants
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

/**
 * FFI comms config tests.
 *
 * @author The Tari Development Team
 */
class FFICommsConfigTests {

    private val dbName = "tari_test_db"

    @Test
    fun testMemoryTransport()
    {
        val transport = FFITransportType()
        assertTrue(transport.getPointer() != nullptr)
        assertTrue(transport.getAddress().isNotEmpty())
        transport.destroy()
    }

    @Test
    fun testTCPTransport()
    {
        val transport = FFITransportType(FFITestUtil.address)
        assertTrue(transport.getPointer() != nullptr)
        transport.destroy()
    }

    @Test
    fun testTorTransport()
    {
        val torMod = TorModule()
        val context = ApplicationProvider.getApplicationContext<Context>()
        val cookieFile = File(torMod.provideTorCookieFilePath(context))
        val cookie = if (cookieFile.exists()) {
            FFIByteVector(cookieFile.readBytes())
        } else
        {
            FFIByteVector(nullptr)
        }
        val addressString = NetAddressString(torMod.provideTorControlAddress(),torMod.provideTorControlPort())
        // TODO Extend to test identity as well
        val transport = FFITransportType(addressString,torMod.provideConnectionPort(), cookie,
            FFIByteVector(nullptr),
            torMod.provideTorSock5Username(), torMod.provideTorSock5Password())
        assertTrue(transport.getPointer() != nullptr)
        transport.destroy()
    }

    @Test
    fun testCommsConfig() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val walletMod = WalletModule()
        FFITestUtil.clearTestFiles(walletMod.provideWalletFilesDirPath(context))
        val privateKey = FFIPrivateKey(HexString(FFITestUtil.PRIVATE_KEY_HEX_STRING))
        val transport = FFITransportType()
        val commsConfig = FFICommsConfig(
            transport.getAddress(),
            transport,
            dbName,
            walletMod.provideWalletFilesDirPath(context),
            privateKey,
            Constants.Wallet.discoveryTimeoutSec
        )
        assertTrue(commsConfig.getPointer() != nullptr)
        commsConfig.destroy()
        transport.destroy()
        privateKey.destroy()
    }

    @Test(expected = FFIException::class)
    fun testByteVectorException() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val walletMod = WalletModule()
        FFITestUtil.clearTestFiles(StringBuilder().append(walletMod.provideWalletFilesDirPath(context)).toString())
        val privateKey = FFIPrivateKey(HexString(FFITestUtil.PRIVATE_KEY_HEX_STRING))
        val transport = FFITransportType()
        val commsConfig = FFICommsConfig(
            transport.getAddress(),
            transport,
            dbName,
            StringBuilder().append(walletMod.provideWalletFilesDirPath(context)).append("bad_dir").toString(),
            privateKey,
            Constants.Wallet.discoveryTimeoutSec
        )
        commsConfig.destroy()
        transport.destroy()
    }

}