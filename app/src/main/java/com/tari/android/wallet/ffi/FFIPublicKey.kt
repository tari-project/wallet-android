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
package com.tari.android.wallet.ffi

internal typealias FFIPublicKeyPtr = Long

/**
 * Wrapper for native private key type.
 *
 * @author The Tari Development Team
 */
internal class FFIPublicKey constructor(pointer: FFIPublicKeyPtr): FFIBase() {

    // region JNI

    private external fun jniGetBytes(publicKeyPtr: FFIPublicKeyPtr, libError: FFIError): FFIByteVectorPtr
    private external fun jniDestroy(publicKeyPtr: FFIPublicKeyPtr)
    private external fun jniCreate(byteVectorPtr: FFIByteVectorPtr, libError: FFIError): FFIPublicKeyPtr
    private external fun jniFromHex(hexStr: String, libError: FFIError): FFIPublicKeyPtr
    private external fun jniGetEmojiNodeId(publicKeyPtr: FFIPublicKeyPtr, libError: FFIError): String
    private external fun jniFromPrivateKey(
        privateKeyPtr: FFIPrivateKeyPtr,
        libError: FFIError
    ): FFIPublicKeyPtr

    // endregion

    private var ptr = nullptr

    init {
        ptr = pointer
    }

    constructor(byteVector: FFIByteVector) : this(nullptr) {
        val error = FFIError()
        ptr = jniCreate(byteVector.getPointer(), error)
        throwIf(error)
    }

    constructor(hex: HexString) : this(nullptr) {
        if (hex.toString().length == 64) {
            val error = FFIError()
            ptr = jniFromHex(hex.hex, error)
            throwIf(error)
        } else {
            throw FFIException(message = "HexString is not a valid PublicKey")
        }
    }

    constructor(privateKey: FFIPrivateKey) : this(nullptr) {
        val error = FFIError()
        ptr = jniFromPrivateKey(privateKey.getPointer(), error)
        throwIf(error)
    }

    fun getPointer(): FFIPublicKeyPtr {
        return ptr
    }

    fun getBytes(): FFIByteVector {
        val error = FFIError()
        val result = FFIByteVector(jniGetBytes(ptr, error))
        throwIf(error)
        return result
    }

    fun getEmojiNodeId(): String {
        val error = FFIError()
        val result = jniGetEmojiNodeId(ptr, error)
        throwIf(error)
        return result
    }

    override fun toString(): String {
        val error = FFIError()
        val result = FFIByteVector(jniGetBytes(ptr, error)).toString()
        throwIf(error)
        return result
    }

    override fun destroy() {
        jniDestroy(ptr)
        ptr = nullptr
    }

}
