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

import com.tari.android.wallet.util.EmojiId

/**
 * Wrapper for native private key type.
 *
 * @author The Tari Development Team
 */
class FFITariWalletAddress() : FFIBase() {

    private external fun jniGetBytes(libError: FFIError): FFIPointer
    private external fun jniDestroy()
    private external fun jniCreate(byteVectorPtr: FFIByteVector, libError: FFIError)
    private external fun jniFromBase58(base58: Base58, libError: FFIError)
    private external fun jniFromEmojiId(emoji: EmojiId, libError: FFIError)
    private external fun jniGetEmojiId(libError: FFIError): EmojiId
    private external fun jniGetNetwork(libError: FFIError): Int
    private external fun jniGetFeatures(libError: FFIError): Int
    private external fun jniGetViewKey(libError: FFIError): FFIPointer
    private external fun jniGetSpendKey(libError: FFIError): FFIPointer
    private external fun jniGetChecksum(libError: FFIError): Int

    constructor(pointer: FFIPointer) : this() {
        if (pointer.isNull()) error("Pointer must not be null")
        this.pointer = pointer
    }

    constructor(byteVector: FFIByteVector) : this() {
        runWithError { jniCreate(byteVector, it) }
    }

    constructor(base58: Base58String) : this() {
        runWithError { jniFromBase58(base58.base58, it) }
    }

    constructor(emojiId: EmojiId) : this() {
        runWithError { jniFromEmojiId(emojiId, it) }
    }

    fun getByteVector(): FFIByteVector = runWithError { FFIByteVector(jniGetBytes(it)) }

    fun getEmojiId(): EmojiId = runWithError { jniGetEmojiId(it) }

    fun getNetwork(): Int = runWithError { jniGetNetwork(it) }

    fun getFeatures(): Int = runWithError { jniGetFeatures(it) }

    fun getViewKey(): FFIPublicKey? = runWithError { jniGetViewKey(it) }.takeIf { !it.isNull() }?.let { FFIPublicKey(it) }

    fun getSpendKey(): FFIPublicKey = runWithError { FFIPublicKey(jniGetSpendKey(it)) }

    fun getChecksum(): Int = runWithError { jniGetChecksum(it) }

    fun notificationHex(): String = getSpendKey().getByteVector().hex()

    override fun toString(): String = getEmojiId()

    override fun destroy() = jniDestroy()
}
