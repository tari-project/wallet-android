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

import java.io.InvalidObjectException
import java.math.BigInteger

/**
 * Completed transaction wrapper.
 *
 * @author The Tari Development Team
 */
typealias CompletedTransactionPtr = Long

class CompletedTransaction constructor(pointer: CompletedTransactionPtr): FinalizerBase() {

    private external fun jniGetId(ptr: CompletedTransactionPtr, libError: LibError): ByteArray
    private external fun jniGetDestinationPublicKey(
        ptr: CompletedTransactionPtr,
        libError: LibError
    ): PublicKeyPtr

    private external fun jniGetSourcePublicKey(
        ptr: CompletedTransactionPtr,
        libError: LibError
    ): PublicKeyPtr

    private external fun jniGetAmount(ptr: CompletedTransactionPtr, libError: LibError): ByteArray
    private external fun jniGetFee(ptr: CompletedTransactionPtr, libError: LibError): ByteArray
    private external fun jniGetTimestamp(
        ptr: CompletedTransactionPtr,
        libError: LibError
    ): ByteArray

    private external fun jniGetMessage(ptr: CompletedTransactionPtr, libError: LibError): String
    private external fun jniGetStatus(ptr: CompletedTransactionPtr, libError: LibError): Int
    private external fun jniDestroy(ptr: CompletedTransactionPtr)

    enum class Status {
        TX_NULL_ERROR,
        COMPLETED,
        BROADCAST,
        MINED
    }

    private var ptr = nullptr

    init {
        ptr = pointer
    }

    fun getPointer(): CompletedTransactionPtr {
        return ptr
    }

    fun getId(): BigInteger {
        val error = LibError()
        val bytes = jniGetId(ptr, error)
        if (error.code != 0) {
            throw RuntimeException()
        }
        return BigInteger(1, bytes)
    }

    fun getDestinationPublicKey(): PublicKey {
        val error = LibError()
        val result = PublicKey(jniGetDestinationPublicKey(ptr, error))
        if (error.code != 0) {
            throw RuntimeException()
        }
        return result
    }

    fun getSourcePublicKey(): PublicKey {
        val error = LibError()
        val result = PublicKey(jniGetSourcePublicKey(ptr, error))
        if (error.code != 0) {
            throw RuntimeException()
        }
        return result
    }

    fun getAmount(): BigInteger {
        val error = LibError()
        val bytes = jniGetAmount(ptr, error)
        if (error.code != 0) {
            throw RuntimeException()
        }
        return BigInteger(1, bytes)
    }

    fun getFee(): BigInteger {
        val error = LibError()
        val bytes = jniGetFee(ptr, error)
        if (error.code != 0) {
            throw RuntimeException()
        }
        return BigInteger(1, bytes)
    }

    fun getTimestamp(): BigInteger {
        val error = LibError()
        val bytes = jniGetTimestamp(ptr, error)
        if (error.code != 0) {
            throw RuntimeException()
        }
        return BigInteger(1, bytes)
    }

    fun getMessage(): String {
        val error = LibError()
        val result = jniGetMessage(ptr, error)
        if (error.code != 0) {
            throw RuntimeException()
        }
        return result
    }

    fun getStatus(): Status {
        val error = LibError()
        val status = jniGetStatus(ptr, error)
        if (error.code != 0) {
            throw RuntimeException()
        }
        return when (status) {
            -1 -> Status.TX_NULL_ERROR
            0 -> Status.COMPLETED
            1 -> Status.BROADCAST
            2 -> Status.MINED
            else -> throw InvalidObjectException("Unexpected status")
        }
    }

    override fun destroy() {
        jniDestroy(ptr)
        ptr = nullptr
    }

}
