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

package com.tari.android.wallet.ffi

/**
 * Wrapper for native private key type.
 *
 * @author Kutsal Kaan Bilgin
 */
class PrivateKey(ptr: PrivateKeyPtr) {

    /**
     * JNI functions.
     */
    private external fun privateKeyGetBytesJNI(privateKeyPtr: PrivateKeyPtr): ByteVectorPtr
    private external fun privateKeyDestroyJNI(privateKeyPtr: PrivateKeyPtr)

    var ptr: PrivateKeyPtr
        private set

    init {
        this.ptr = ptr
    }

    companion object {

        /**
         * JNI static functions.
         */
        @JvmStatic
        private external fun privateKeyCreateJNI(byteVectorPtr: ByteVectorPtr): PrivateKeyPtr
        @JvmStatic
        private external fun privateKeyGenerateJNI(): PrivateKeyPtr
        @JvmStatic
        private external fun privateKeyFromHexJNI(hexStr: String): PrivateKeyPtr

        fun create(byteVector: ByteVector): PrivateKey {
            return PrivateKey(privateKeyCreateJNI(byteVector.ptr))
        }

        fun generate(): PrivateKey {
            return PrivateKey(privateKeyGenerateJNI())
        }

        fun fromHex(hexStr: String): PrivateKey {
            return PrivateKey(privateKeyFromHexJNI(hexStr))
        }

    }

    val bytes: ByteVector
        get() {
            return ByteVector(privateKeyGetBytesJNI(ptr))
        }

    fun destroy() {
        privateKeyDestroyJNI(ptr)
        ptr = NULL_POINTER
    }

    protected fun finalize() {
        destroy()
    }

}
