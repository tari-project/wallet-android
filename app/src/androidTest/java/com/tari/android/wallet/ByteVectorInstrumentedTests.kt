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

import androidx.test.ext.junit.runners.AndroidJUnit4
import com.tari.android.wallet.ffi.ByteVector
import com.tari.android.wallet.ffi.NULL_POINTER
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith

/**
 * FFI byte vector tests.
 *
 * @author Kutsal Kaan Bilgin
 */
@RunWith(AndroidJUnit4::class)
class ByteVectorInstrumentedTests {

    private val str = "ABCDEF"

    @Test
    fun testCreateAndDestroyByteVector() {
        val byteVector = ByteVector.create(str)
        assertTrue(byteVector.ptr != NULL_POINTER)
        byteVector.destroy()
        assertTrue(byteVector.ptr == NULL_POINTER)
    }

    @Test
    fun testByteVectorGetLength() {
        val byteVector = ByteVector.create(str)
        assertTrue(byteVector.length == str.length)
        // release resource
        byteVector.destroy()
    }

    @Test
    fun testByteVectorGetAt() {
        val byteVector = ByteVector.create(str)
        val index = 3
        assert(byteVector.getAt(index) == str[index])
        // release resource
        byteVector.destroy()
    }

    @Test
    fun testDestroyedByteVector() {
        val byteVector = ByteVector.create(str)
        assert(byteVector.length == str.length)
        byteVector.destroy()
        assert(byteVector.length == 0)
        // ERR :: still returns 'D'
        val index = 3
        assert(byteVector.getAt(index) == str[index])
    }

}