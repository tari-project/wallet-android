/**
 * Copyright 2019 The Tari Project
 *
 * Redistribution and use in source and binary forms, with or
 * without modification, are permitted provided that the
 * following conditions are met:

 * 1. Redistributions of source code must retain the above copyright notice,
 * this list of conditions and the following disclaimer.

 * 2. Redistributions in binary form must reproduce the above
 * copyright notice, this list of conditions and the following disclaimer in the
 * documentation and/or other materials provided with the distribution.

 * 3. Neither the name of the copyright holder nor the names of
 * its contributors may be used to endorse or promote products
 * derived from this software without specific prior written permission.

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

import com.tari.android.wallet.ffi.Contact
import com.tari.android.wallet.ffi.NULL_POINTER
import com.tari.android.wallet.ffi.PublicKey
import org.junit.Test
import org.junit.Assert.*

/**
 * FFI byte vector tests.
 *
 * @author Kutsal Kaan Bilgin
 */
class ContactTests {

    @Test
    fun testCreateAndDestroyContact() {
        val alias = TestUtil.generateRandomAlphanumericString(16)
        val publicKey = PublicKey.fromHex(TestUtil.PUBLIC_KEY_HEX_STRING)
        val contact = Contact.create(alias, publicKey)
        assertTrue(contact.ptr != NULL_POINTER)
        contact.destroy()
        assertTrue(contact.ptr == NULL_POINTER)
        // free resources
        publicKey.destroy()
    }

    @Test
    fun testGetContactAlias() {
        val alias = TestUtil.generateRandomAlphanumericString(16)
        val publicKey = PublicKey.fromHex(TestUtil.PUBLIC_KEY_HEX_STRING)
        val contact = Contact.create(alias, publicKey)
        assertEquals(alias, contact.alias)
        // free resources
        contact.destroy()
        publicKey.destroy()
    }

    @Test
    fun testGetContactPublicKey() {
        val alias = TestUtil.generateRandomAlphanumericString(16)
        val publicKey = PublicKey.fromHex(TestUtil.PUBLIC_KEY_HEX_STRING)
        val contact = Contact.create(alias, publicKey)
        val contactPublicKey = contact.publicKey
        val contactPublicKeyBytes = contactPublicKey.bytes
        assertEquals(TestUtil.PUBLIC_KEY_HEX_STRING, contactPublicKeyBytes.hexString)
        // free resources
        contactPublicKeyBytes.destroy()
        contactPublicKey.destroy()
        contact.destroy()
        publicKey.destroy()
    }

    @Test
    fun testCreateContactWithEmptyAlias() {
        val publicKey = PublicKey.fromHex(TestUtil.PUBLIC_KEY_HEX_STRING)
        val contact = Contact.create("", publicKey)
        assertTrue(contact.ptr != NULL_POINTER)
        assertEquals("", contact.alias)
        // free resources
        contact.destroy()
        publicKey.destroy()
    }


}