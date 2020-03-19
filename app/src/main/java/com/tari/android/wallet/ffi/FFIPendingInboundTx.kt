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

import java.math.BigInteger

/**
 * Pending inbound transaction wrapper.
 *
 * @author The Tari Development Team
 */

internal typealias FFIPendingInboundTxPtr = Long

internal class FFIPendingInboundTx constructor(pointer: FFIPendingInboundTxPtr) : FFIBase() {

    // region JNI

    private external fun jniGetId(libError: FFIError): ByteArray
    private external fun jniGetSourcePublicKey(
        libError: FFIError
    ): FFIPublicKeyPtr

    private external fun jniGetAmount(
        libError: FFIError
    ): ByteArray

    private external fun jniGetTimestamp(
        libError: FFIError
    ): ByteArray

    private external fun jniGetMessage(
        libError: FFIError
    ): String

    private external fun jniGetStatus(
        libError: FFIError
    ): Int

    private external fun jniDestroy()

    // endregion

    private var ptr = nullptr

    init {
        ptr = pointer
    }

    fun getPointer(): FFIPendingInboundTxPtr {
        return ptr
    }

    fun getId(): BigInteger {
        val error = FFIError()
        val bytes = jniGetId(error)
        throwIf(error)
        return BigInteger(1, bytes)
    }

    fun getSourcePublicKey(): FFIPublicKey {
        val error = FFIError()
        val result = FFIPublicKey(jniGetSourcePublicKey(error))
        throwIf(error)
        return result
    }

    fun getAmount(): BigInteger {
        val error = FFIError()
        val bytes = jniGetAmount(error)
        throwIf(error)
        return BigInteger(1, bytes)
    }

    fun getTimestamp(): BigInteger {
        val error = FFIError()
        val bytes = jniGetTimestamp(error)
        throwIf(error)
        return BigInteger(1, bytes)
    }

    fun getMessage(): String {
        val error = FFIError()
        val result = jniGetMessage(error)
        throwIf(error)
        return result
    }

    fun getStatus(): FFIStatus {
        val error = FFIError()
        val status = jniGetStatus(error)
        throwIf(error)
        return when (status) {
            -1 -> FFIStatus.TX_NULL_ERROR
            0 -> FFIStatus.COMPLETED
            1 -> FFIStatus.BROADCAST
            2 -> FFIStatus.MINED
            3 -> FFIStatus.IMPORTED
            4 -> FFIStatus.PENDING
            5 -> FFIStatus.UNKNOWN
            else -> throw FFIException(message = "Unexpected status: $status")
        }
    }

    override fun destroy() {
        jniDestroy()
    }

}