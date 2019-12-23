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

import android.util.Log
import java.io.InvalidObjectException
import java.math.BigInteger

/**
 * Wallet wrapper.
 *
 * @author The Tari Development Team
 */

typealias WalletPtr = Long

object Wallet: FinalizerBase() {

    private external fun jniCreate(
        commsConfig: CommsConfigPtr,
        logPath: String,
        callbackReceivedTransaction: String,
        callbackReceivedTransactionSig: String,
        callbackReceivedTransactionReply: String,
        callbackReceivedTransactionReplySig: String,
        callbackReceivedFinalizedTransaction: String,
        callbackReceivedFinalizedTransactionSig: String,
        callbackTransactionBroadcast: String,
        callbackTransactionBroadcastSig: String,
        callbackTransactionMined: String,
        callbackTransactionMinedSig: String,
        callbackDiscoveryProcessComplete: String,
        callbackDiscoveryProcessCompleteSig: String,
        libError: LibError
    ): WalletPtr
    
    private external fun jniGetPublicKey(walletPtr: WalletPtr, libError: LibError): PublicKeyPtr
    
    private external fun jniGetAvailableBalance(walletPtr: WalletPtr, libError: LibError): ByteArray
    
    private external fun jniGetPendingIncomingBalance(
        walletPtr: WalletPtr,
        libError: LibError
    ): ByteArray
    
    private external fun jniGetPendingOutgoingBalance(
        walletPtr: WalletPtr,
        libError: LibError
    ): ByteArray
    
    private external fun jniGetContacts(walletPtr: WalletPtr, libError: LibError): ContactsPtr
    
    private external fun jniAddContact(
        walletPtr: WalletPtr,
        contactPtr: ContactPtr,
        libError: LibError
    ): Boolean
    
    private external fun jniRemoveContact(
        walletPtr: WalletPtr,
        contactPtr: ContactPtr,
        libError: LibError
    ): Boolean
    
    private external fun jniGetCompletedTransactions(
        walletPtr: WalletPtr,
        libError: LibError
    ): CompletedTransactionsPtr
    
    private external fun jniGetCompletedTransactionById(
        walletPtr: WalletPtr,
        id: Long,
        libError: LibError
    ): CompletedTransactionPtr
    
    private external fun jniGetPendingOutboundTransactions(
        walletPtr: WalletPtr,
        libError: LibError
    ): PendingOutboundTransactionsPtr
    
    private external fun jniGetPendingOutboundTransactionById(
        walletPtr: WalletPtr,
        id: Long,
        libError: LibError
    ): PendingOutboundTransactionPtr
    
    private external fun jniGetPendingInboundTransactions(
        walletPtr: WalletPtr,
        libError: LibError
    ): PendingInboundTransactionsPtr
    
    private external fun jniGetPendingInboundTransactionById(
        walletPtr: WalletPtr,
        id: Long,
        libError: LibError
    ): PendingInboundTransactionPtr
    
    private external fun jniGenerateTestData(
        walletPtr: WalletPtr,
        datastorePath: String,
        libError: LibError
    ): Boolean
    
    private external fun jniTransactionBroadcast(
        walletPtr: WalletPtr,
        txId: PendingInboundTransactionPtr,
        libError: LibError
    ): Boolean
    
    private external fun jniCompleteSentTransaction(
        walletPtr: WalletPtr,
        txId: PendingOutboundTransactionPtr,
        libError: LibError
    ): Boolean
    
    private external fun jniMineCompletedTransaction(
        walletPtr: WalletPtr,
        txId: CompletedTransactionPtr,
        libError: LibError
    ): Boolean
    
    private external fun jniReceiveTransaction(walletPtr: WalletPtr, libError: LibError): Boolean
    
    private external fun jniFinalizeCompletedTransaction(
        walletPtr: WalletPtr,
        txId: PendingInboundTransactionPtr,
        libError: LibError
    ): Boolean
    
    private external fun jniSendTransaction(
        walletPtr: WalletPtr,
        publicKeyPtr: PublicKeyPtr,
        amount: Long,
        fee: Long,
        message: String,
        libError: LibError
    ): Boolean
    private external fun jniDestroy(walletPtr: WalletPtr)

    private var ptr = nullptr
    private var listenerAdapter: WalletListenerAdapter? = null

    private fun onTransactionBroadcast(ptr: CompletedTransactionPtr) {
        Log.i("onTransactionReceived", ptr.toString())
        val param = CompletedTransaction(ptr)
        listenerAdapter?.onTransactionBroadcast(param)
        param.destroy()
    }

    private fun onTransactionMined(ptr: CompletedTransactionPtr) {
        Log.i("onTransactionReceived", ptr.toString())
        val param = CompletedTransaction(ptr)
        listenerAdapter?.onTransactionMined(param)
        param.destroy()
    }

    private fun onTransactionReceived(ptr: PendingInboundTransactionPtr) {
        Log.i("onTransactionReceived", ptr.toString())
        val param = PendingInboundTransaction(ptr)
        listenerAdapter?.onTransactionReceived(param)
        param.destroy()
    }

    private fun onTransactionReplyReceived(ptr: CompletedTransactionPtr) {
        Log.i("onTransactionReplyReceived", ptr.toString())
        val param = CompletedTransaction(ptr)
        listenerAdapter?.onTransactionReplyReceived(param)
        param.destroy()
    }

    private fun onTransactionFinalized(ptr: CompletedTransactionPtr) {
        Log.i("onTransactionFinalized", ptr.toString())
        val param = CompletedTransaction(ptr)
        listenerAdapter?.onTransactionFinalized(param)
        param.destroy()
    }

    private fun onDiscoveryComplete(bytes: ByteArray, success: Boolean) {
        Log.i("onDiscoveryComplete", ptr.toString())
        val txId = BigInteger(1, bytes)
        listenerAdapter?.onDiscoveryComplete(txId, success)
    }

    fun generateTestData(datastorePath: String): Boolean {
        val error = LibError()
        val result = jniGenerateTestData(getPointer(), datastorePath, error)
        if (error.code != 0) {
            throw RuntimeException()
        }
        return result
    }

    fun transactionBroadcast(tx: CompletedTransaction): Boolean {
        val error = LibError()
        val result = jniTransactionBroadcast(getPointer(), tx.getPointer(), error)
        if (error.code != 0) {
            throw RuntimeException()
        }
        return result
    }

    fun completeSentTransaction(tx: PendingOutboundTransaction): Boolean {
        val error = LibError()
        val result = jniCompleteSentTransaction(getPointer(), tx.getPointer(), error)
        if (error.code != 0) {
            throw RuntimeException()
        }
        return result
    }

    fun minedCompletedTransaction(tx: CompletedTransaction): Boolean {
        val error = LibError()
        val result = jniMineCompletedTransaction(getPointer(), tx.getPointer(), error)
        if (error.code != 0) {
            throw RuntimeException()
        }
        return result
    }

    fun finalizeCompletedTransaction(tx: PendingInboundTransaction): Boolean {
        val error = LibError()
        val result = jniFinalizeCompletedTransaction(getPointer(), tx.getPointer(), error)
        if (error.code != 0) {
            throw RuntimeException()
        }
        return result
    }

    fun receiveTransaction(): Boolean {
        val error = LibError()
        val result = jniReceiveTransaction(getPointer(), error)
        if (error.code != 0) {
            throw RuntimeException()
        }
        return result
    }
    
    // this acts as a constructor would for a normal class since constructors are not allowed for
    // singletons
    fun init(commsConfig: CommsConfig, logPath: String, listeners: WalletListenerAdapter) {
        if (ptr == nullptr) { //so it can only be assigned once for the singleton
            listenerAdapter = listeners
            val error = LibError()
            ptr = jniCreate(
                commsConfig.getPointer(), logPath,
                this::onTransactionReceived.name, "(J)V",
                this::onTransactionReplyReceived.name, "(J)V",
                this::onTransactionFinalized.name, "(J)V",
                this::onTransactionBroadcast.name, "(J)V",
                this::onTransactionMined.name, "(J)V",
                this::onDiscoveryComplete.name, "([BZ)V",
                error
            )
            Log.i("Wallet Code", error.code.toString())
            if (error.code != 0) {
                throw InvalidObjectException("Failure to create Wallet")
            }
        }
    }

    fun getPointer(): WalletPtr {
        return ptr
    }

    fun getAvailableBalance(): BigInteger {
        val error = LibError()
        val bytes = jniGetAvailableBalance(ptr, error)
        if (error.code != 0) {
            throw RuntimeException()
        }
        return BigInteger(1, bytes)
    }

    fun getPendingIncomingBalance(): BigInteger {
        val error = LibError()
        val bytes = jniGetPendingIncomingBalance(ptr, error)
        if (error.code != 0) {
            throw RuntimeException()
        }
        return BigInteger(1, bytes)
    }

    fun getPendingOutgoingBalance(): BigInteger {
        val error = LibError()
        val bytes = jniGetPendingOutgoingBalance(ptr, error)
        if (error.code != 0) {
            throw RuntimeException()
        }
        return BigInteger(1, bytes)
    }

    fun getPublicKey(): PublicKey {
        val error = LibError()
        val result = PublicKey(jniGetPublicKey(ptr, error))
        if (error.code != 0) {
            throw RuntimeException()
        }
        return result
    }

    fun getContacts(): Contacts {
        val error = LibError()
        val result = Contacts(jniGetContacts(ptr, error))
        if (error.code != 0) {
            throw RuntimeException()
        }
        return result
    }

    fun addContact(contact: Contact): Boolean {
        val error = LibError()
        val result = jniAddContact(ptr, contact.getPointer(), error)
        if (error.code != 0) {
            throw RuntimeException()
        }
        return result
    }

    fun removeContact(contact: Contact): Boolean {
        val error = LibError()
        val result = jniRemoveContact(ptr, contact.getPointer(), error)
        if (error.code != 0) {
            throw RuntimeException()
        }
        return result
    }

    fun getCompletedTransactions(): CompletedTransactions {
        val error = LibError()
        val result = CompletedTransactions(jniGetCompletedTransactions(ptr, error))
        if (error.code != 0) {
            throw RuntimeException()
        }
        return result
    }

    fun getCompletedTransactionById(id: Long): CompletedTransaction {
        val error = LibError()
        val result = CompletedTransaction(jniGetCompletedTransactionById(ptr, id, error))
        if (error.code != 0) {
            throw RuntimeException()
        }
        return result
    }

    fun getPendingOutboundTransactions(): PendingOutboundTransactions {
        val error = LibError()
        val result = PendingOutboundTransactions(jniGetPendingOutboundTransactions(ptr, error))
        if (error.code != 0) {
            throw RuntimeException()
        }
        return result
    }

    fun getPendingOutboundTransactionById(id: Long): PendingOutboundTransaction {
        val error = LibError()
        val result =
            PendingOutboundTransaction(jniGetPendingOutboundTransactionById(ptr, id, error))
        if (error.code != 0) {
            throw RuntimeException()
        }
        return result
    }

    fun getPendingInboundTransactions(): PendingInboundTransactions {
        val error = LibError()
        val result = PendingInboundTransactions(jniGetPendingInboundTransactions(ptr, error))
        if (error.code != 0) {
            throw RuntimeException()
        }
        return result
    }

    fun getPendingInboundTransactionById(id: Long): PendingInboundTransaction {
        val error = LibError()
        val result = PendingInboundTransaction(jniGetPendingInboundTransactionById(ptr, id, error))
        if (error.code != 0) {
            throw RuntimeException()
        }
        return result
    }

    fun sendTransaction(destination: PublicKey, amount: Long, fee: Long, message: String): Boolean {
        val minimumLibFee = 100
        if (fee < minimumLibFee) {
            throw RuntimeException()
        }
        if (amount < 0) {
            throw RuntimeException()
        }
        if (destination == getPublicKey()) {
            throw  RuntimeException()
        }
        val error = LibError()
        val result = jniSendTransaction(ptr, destination.getPointer(), amount, fee, message, error)
        if (error.code != 0) {
            throw RuntimeException()
        }
        return result
    }

    override fun destroy() {
        jniDestroy(ptr)
        ptr = nullptr
    }

}