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

import com.tari.android.wallet.ffi.*
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * FFI byte vector tests.
 *
 * @author The Tari Development Team
 */
class FFIContactTests {

    @Test
    fun constructor_assertThatConstructedContactIsValid() {
        val publicKey = FFIPublicKey(HexString(FFITestUtil.PUBLIC_KEY_HEX_STRING))
        val contact = FFIContact(FFITestUtil.generateRandomAlphanumericString(16), publicKey)
        assertTrue(contact.getPointer() != nullptr)
        contact.destroy()
        publicKey.destroy()
    }

    @Test
    fun getPublicKey_assertThatContactPublicKeyIsEqualToTheGivenPublicKeyHexString() {
        val publicKey = FFIPublicKey(HexString(FFITestUtil.PUBLIC_KEY_HEX_STRING))
        val contact = FFIContact(FFITestUtil.generateRandomAlphanumericString(16), publicKey)
        assertEquals(FFITestUtil.PUBLIC_KEY_HEX_STRING, contact.getPublicKey().toString())
        contact.destroy()
        publicKey.destroy()
    }

    @Test(expected = FFIException::class)
    fun constructor_assertThat() {
        FFIContact("", FFIPublicKey(HexString(FFITestUtil.PUBLIC_KEY_HEX_STRING)))
    }
}
