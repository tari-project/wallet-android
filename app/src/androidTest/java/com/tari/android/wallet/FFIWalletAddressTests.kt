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
package com.tari.android.wallet

import com.tari.android.wallet.ffi.Base58String
import com.tari.android.wallet.ffi.FFITariWalletAddress
import com.tari.android.wallet.ffi.nullptr
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Test

/**
 * FFI private key tests.
 *
 * @author The Tari Development Team
 */
class FFIWalletAddressTests {

    @Test
    fun constructorFromByteVector_assertThatValidPublicKeyInstanceWasCreated() {
        val ffiTariWalletAddress = FFITariWalletAddress(Base58String(FFITestUtil.WALLET_ADDRESS_HEX_STRING))
        assertNotEquals(nullptr, ffiTariWalletAddress.pointer)
        assertEquals(FFITestUtil.WALLET_ADDRESS_HEX_STRING, ffiTariWalletAddress.toString())
        ffiTariWalletAddress.destroy()
    }

    @Test
    fun constructorFromHexString_assertThatValidPublicKeyInstanceWasCreated() {
        val ffiTariWalletAddress = FFITariWalletAddress(Base58String(FFITestUtil.WALLET_ADDRESS_HEX_STRING))
        assertNotEquals(nullptr, ffiTariWalletAddress.pointer)
        assertEquals(FFITestUtil.WALLET_ADDRESS_HEX_STRING, ffiTariWalletAddress.toString())
        ffiTariWalletAddress.destroy()
    }

    @Test
    fun constructorFromEmojiId_assertThatValidPublicKeyInstanceWasCreated() {
        val origin = FFITariWalletAddress(Base58String(FFITestUtil.WALLET_ADDRESS_HEX_STRING))
        val ffiTariWalletAddress = FFITariWalletAddress(FFITestUtil.WALLET_EMOJI_ID)
        assertEquals(origin.toString(), ffiTariWalletAddress.toString())
        ffiTariWalletAddress.destroy()
        origin.destroy()
    }

}
