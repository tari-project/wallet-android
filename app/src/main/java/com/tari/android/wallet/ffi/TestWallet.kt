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
 * Test wallet - separates the test functions.
 *
 * @author Kutsal Kaan Bilgin
 */
class TestWallet(ptr: WalletPtr): Wallet(ptr) {

    /**
     * JNI test functions.
     */
    private external fun testGenerateDataJNI(
        walletPtr: WalletPtr,
        datastorePath: String
    ): Boolean
    private external fun testTransactionBroadcastJNI(
        walletPtr: WalletPtr,
        txId: PendingInboundTransactionPtr
    ): Boolean
    private external fun testCompleteSentTransactionJNI(
        walletPtr: WalletPtr,
        txId: PendingOutboundTransactionPtr
    ): Boolean
    private external fun testMinedJNI(walletPtr: WalletPtr, txId: CompletedTransactionPtr): Boolean
    private external fun testReceiveTransactionJNI(walletPtr: WalletPtr): Boolean

    companion object {

        internal fun create(walletConfig: CommsConfig, logPath: String): TestWallet {
            return TestWallet(Wallet.create(walletConfig, logPath).ptr)
        }

    }

    fun generateTestData(datastorePath: String): Boolean {
        return testGenerateDataJNI(ptr, datastorePath)
    }

    fun testTransactionBroadcast(tx: PendingInboundTransaction): Boolean {
        return testTransactionBroadcastJNI(ptr, tx.ptr)
    }

    fun testCompleteSentTransaction(tx: PendingOutboundTransaction): Boolean {
        return testCompleteSentTransactionJNI(ptr, tx.ptr)
    }

    fun testMined(tx: CompletedTransaction): Boolean {
        return testMinedJNI(ptr, tx.ptr)
    }

    fun testReceiveTransaction(): Boolean {
        return testReceiveTransactionJNI(ptr)
    }
}