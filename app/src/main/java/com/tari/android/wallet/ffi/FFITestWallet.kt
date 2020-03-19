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
 * Test wallet implementation. Contains only the test functions to separate concerns with
 * the actual wallet implementation.
 *
 * @author The Tari Development Team
 */
internal class FFITestWallet(commsConfig: FFICommsConfig, logPath: String) :
    FFIWallet(commsConfig, logPath) {

    companion object {
        var instance: FFITestWallet? = null
    }

    // region JNI

    private external fun jniGenerateTestData(
        datastorePath: String,
        libError: FFIError
    ): Boolean

    private external fun jniTestBroadcastTx(
        txPtr: String,
        libError: FFIError
    ): Boolean

    private external fun jniTestFinalizeReceivedTx(
        txPtr: FFIPendingInboundTx,
        libError: FFIError
    ): Boolean

    private external fun jniTestCompleteSentTx(
        txPtr: FFIPendingOutboundTx,
        libError: FFIError
    ): Boolean

    private external fun jniTestMineTx(
        txId: String,
        libError: FFIError
    ): Boolean

    private external fun jniTestReceiveTx(libError: FFIError): Boolean

    // endregion


    // region wallet test methods

    fun generateTestData(datastorePath: String): Boolean {
        val error = FFIError()
        val result = jniGenerateTestData(datastorePath, error)
        throwIf(error)
        return result
    }

    fun testBroadcastTx(tx: BigInteger): Boolean {
        val error = FFIError()
        val result = jniTestBroadcastTx(tx.toString(), error)
        throwIf(error)
        return result
    }

    fun testCompleteSentTx(tx: FFIPendingOutboundTx): Boolean {
        val error = FFIError()
        val result = jniTestCompleteSentTx(tx, error)
        throwIf(error)
        return result
    }

    fun testMineTx(tx: BigInteger): Boolean {
        val error = FFIError()
        val result = jniTestMineTx(tx.toString(), error)
        throwIf(error)
        return result
    }

    fun testFinalizeReceivedTx(tx: FFIPendingInboundTx): Boolean {
        val error = FFIError()
        val result = jniTestFinalizeReceivedTx(tx, error)
        throwIf(error)
        return result
    }

    fun testReceiveTx(): Boolean {
        val error = FFIError()
        val result = jniTestReceiveTx(error)
        throwIf(error)
        return result
    }

    // endregion

}