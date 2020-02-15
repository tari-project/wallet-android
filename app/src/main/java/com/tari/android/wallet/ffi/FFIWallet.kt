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

import android.util.Log
import com.orhanobut.logger.Logger
import java.io.InvalidObjectException
import java.math.BigInteger

/**
 * Wallet wrapper.
 *
 * @author The Tari Development Team
 */

internal typealias FFIWalletPtr = Long

internal abstract class FFIWallet(commsConfig: FFICommsConfig, logPath: String) : FFIBase() {

    // region JNI

    private external fun jniCreate(
        commsConfig: FFICommsConfigPtr,
        logPath: String,
        callbackReceivedTx: String,
        callbackReceivedTxSig: String,
        callbackReceivedTxReply: String,
        callbackReceivedTxReplySig: String,
        callbackReceivedFinalizedTx: String,
        callbackReceivedFinalizedTxSig: String,
        callbackTxBroadcast: String,
        callbackTxBroadcastSig: String,
        callbackTxMined: String,
        callbackTxMinedSig: String,
        callbackDiscoveryProcessComplete: String,
        callbackDiscoveryProcessCompleteSig: String,
        libError: FFIError
    ): FFIWalletPtr

    private external fun jniGetPublicKey(
        walletPtr: FFIWalletPtr,
        libError: FFIError
    ): FFIPublicKeyPtr

    private external fun jniGetAvailableBalance(
        walletPtr: FFIWalletPtr,
        libError: FFIError
    ): ByteArray

    private external fun jniGetPendingIncomingBalance(
        walletPtr: FFIWalletPtr,
        libError: FFIError
    ): ByteArray

    private external fun jniGetPendingOutgoingBalance(
        walletPtr: FFIWalletPtr,
        libError: FFIError
    ): ByteArray

    private external fun jniGetContacts(walletPtr: FFIWalletPtr, libError: FFIError): FFIContactsPtr

    private external fun jniAddUpdateContact(
        walletPtr: FFIWalletPtr,
        contactPtr: FFIContactPtr,
        libError: FFIError
    ): Boolean

    private external fun jniRemoveContact(
        walletPtr: FFIWalletPtr,
        contactPtr: FFIContactPtr,
        libError: FFIError
    ): Boolean

    private external fun jniGetCompletedTxs(
        walletPtr: FFIWalletPtr,
        libError: FFIError
    ): FFICompletedTxsPtr

    private external fun jniGetCompletedTxById(
        walletPtr: FFIWalletPtr,
        id: String,
        libError: FFIError
    ): FFICompletedTxPtr

    private external fun jniGetPendingOutboundTxs(
        walletPtr: FFIWalletPtr,
        libError: FFIError
    ): FFIPendingOutboundTxsPtr

    private external fun jniGetPendingOutboundTxById(
        walletPtr: FFIWalletPtr,
        id: String,
        libError: FFIError
    ): FFIPendingOutboundTxPtr

    private external fun jniGetPendingInboundTxs(
        walletPtr: FFIWalletPtr,
        libError: FFIError
    ): FFIPendingInboundTxsPtr

    private external fun jniGetPendingInboundTxById(
        walletPtr: FFIWalletPtr,
        id: String,
        libError: FFIError
    ): FFIPendingInboundTxPtr

    private external fun jniIsCompletedTxOutbound(
        walletPtr: FFIWalletPtr,
        completedTx: FFICompletedTxPtr,
        libError: FFIError
    ): Boolean

    private external fun jniSendTx(
        walletPtr: FFIWalletPtr,
        publicKeyPtr: FFIPublicKeyPtr,
        amount: String,
        fee: String,
        message: String,
        libError: FFIError
    ): Boolean

    private external fun jniSignMessage(
        walletPtr: FFIWalletPtr,
        message: String,
        libError: FFIError
    ): String

    private external fun jniVerifyMessageSignature(
        walletPtr: FFIWalletPtr,
        publicKeyPtr: FFIPublicKeyPtr,
        message: String,
        signature: String,
        libError: FFIError
    ): Boolean

    private external fun jniImportUTXO(
        walletPtr: FFIWalletPtr,
        spendingKey: FFIPrivateKeyPtr,
        sourcePublicKey: FFIPublicKeyPtr,
        amount: String,
        message: String,
        libError: FFIError
    ): ByteArray

    private external fun jniDestroy(walletPtr: FFIWalletPtr)

    // endregion

    protected var ptr = nullptr
    var listenerAdapter: FFIWalletListenerAdapter? = null

    // this acts as a constructor would for a normal class since constructors are not allowed for
    // singletons
    init {
        if (ptr == nullptr) { // so it can only be assigned once for the singleton
            val error = FFIError()
            ptr = jniCreate(
                commsConfig.getPointer(), logPath,
                this::onTxReceived.name, "(J)V",
                this::onTxReplyReceived.name, "(J)V",
                this::onTxFinalized.name, "(J)V",
                this::onTxBroadcast.name, "(J)V",
                this::onTxMined.name, "(J)V",
                this::onDiscoveryComplete.name, "([BZ)V",
                error
            )
            Log.i("Wallet Code", error.code.toString())
            if (error.code != 0) {
                throw InvalidObjectException("Failure to create Wallet")
            }
        }
    }

    fun getPointer(): FFIWalletPtr {
        return ptr
    }

    fun getAvailableBalance(): BigInteger {
        val error = FFIError()
        val bytes = jniGetAvailableBalance(ptr, error)
        if (error.code != 0) {
            throw RuntimeException()
        }
        return BigInteger(1, bytes)
    }

    fun getPendingIncomingBalance(): BigInteger {
        val error = FFIError()
        val bytes = jniGetPendingIncomingBalance(ptr, error)
        if (error.code != 0) {
            throw RuntimeException()
        }
        return BigInteger(1, bytes)
    }

    fun getPendingOutgoingBalance(): BigInteger {
        val error = FFIError()
        val bytes = jniGetPendingOutgoingBalance(ptr, error)
        if (error.code != 0) {
            throw RuntimeException()
        }
        return BigInteger(1, bytes)
    }

    fun getPublicKey(): FFIPublicKey {
        val error = FFIError()
        val result = FFIPublicKey(jniGetPublicKey(ptr, error))
        if (error.code != 0) {
            throw RuntimeException()
        }
        return result
    }

    fun getContacts(): FFIContacts {
        val error = FFIError()
        val result = FFIContacts(jniGetContacts(ptr, error))
        if (error.code != 0) {
            throw RuntimeException()
        }
        return result
    }

    fun addUpdateContact(contact: FFIContact): Boolean {
        val error = FFIError()
        val result = jniAddUpdateContact(ptr, contact.getPointer(), error)
        if (error.code != 0) {
            throw RuntimeException()
        }
        return result
    }

    fun removeContact(contact: FFIContact): Boolean {
        val error = FFIError()
        val result = jniRemoveContact(ptr, contact.getPointer(), error)
        if (error.code != 0) {
            throw RuntimeException()
        }
        return result
    }

    fun isCompletedTxOutbound(completedTx: FFICompletedTx): Boolean {
        val error = FFIError()
        val result = jniIsCompletedTxOutbound(ptr, completedTx.getPointer(), error)
        if (error.code != 0) {
            throw RuntimeException()
        }
        return result
    }

    fun getCompletedTxs(): FFICompletedTxs {
        val error = FFIError()
        val result = FFICompletedTxs(jniGetCompletedTxs(ptr, error))
        if (error.code != 0) {
            throw RuntimeException()
        }
        return result
    }

    fun getCompletedTxById(id: BigInteger): FFICompletedTx {
        val error = FFIError()
        val result = FFICompletedTx(jniGetCompletedTxById(ptr, id.toString(), error))
        if (error.code != 0) {
            throw RuntimeException()
        }
        return result
    }

    fun getPendingOutboundTxs(): FFIPendingOutboundTxs {
        val error = FFIError()
        val result = FFIPendingOutboundTxs(jniGetPendingOutboundTxs(ptr, error))
        if (error.code != 0) {
            throw RuntimeException()
        }
        return result
    }

    fun getPendingOutboundTxById(id: BigInteger): FFIPendingOutboundTx {
        val error = FFIError()
        val result =
            FFIPendingOutboundTx(jniGetPendingOutboundTxById(ptr, id.toString(), error))
        if (error.code != 0) {
            throw RuntimeException()
        }
        return result
    }

    fun getPendingInboundTxs(): FFIPendingInboundTxs {
        val error = FFIError()
        val result = FFIPendingInboundTxs(jniGetPendingInboundTxs(ptr, error))
        if (error.code != 0) {
            throw RuntimeException()
        }
        return result
    }

    fun getPendingInboundTxById(id: BigInteger): FFIPendingInboundTx {
        val error = FFIError()
        val result = FFIPendingInboundTx(jniGetPendingInboundTxById(ptr, id.toString(), error))
        if (error.code != 0) {
            throw RuntimeException()
        }
        return result
    }

    private fun onTxBroadcast(ptr: FFICompletedTxPtr) {
        Logger.i("Tx completed. Pointer: %s", ptr.toString())
        val tx = FFICompletedTx(ptr)
        val id = tx.getId()
        tx.destroy()
        listenerAdapter?.onTxBroadcast(id)
    }

    private fun onTxMined(ptr: FFICompletedTxPtr) {
        Logger.i("Tx mined. Pointer: %s", ptr.toString())
        val tx = FFICompletedTx(ptr)
        listenerAdapter?.onTxMined(tx.getId())
        tx.destroy()
    }

    private fun onTxReceived(ptr: FFIPendingInboundTxPtr) {
        Logger.i("Tx received. Pointer: %s", ptr.toString())
        val tx = FFIPendingInboundTx(ptr)
        listenerAdapter?.onTxReceived(tx.getId())
        tx.destroy()
    }

    private fun onTxReplyReceived(ptr: FFICompletedTxPtr) {
        Logger.i("Tx reply received. Pointer: %s", ptr.toString())
        val tx = FFICompletedTx(ptr)
        listenerAdapter?.onTxReplyReceived(tx.getId())
        tx.destroy()
    }

    private fun onTxFinalized(ptr: FFICompletedTxPtr) {
        Logger.i("Tx finalized. Pointer: %s", ptr.toString())
        val tx = FFICompletedTx(ptr)
        listenerAdapter?.onTxFinalized(tx.getId())
        tx.destroy()
    }

    private fun onDiscoveryComplete(bytes: ByteArray, success: Boolean) {
        Logger.i("Tx discovery complete. Success: $success")
        val txId = BigInteger(1, bytes)
        listenerAdapter?.onDiscoveryComplete(txId, success)
    }

    fun sendTx(
        destination: FFIPublicKey,
        amount: BigInteger,
        fee: BigInteger,
        message: String
    ): Boolean {
        val minimumLibFee = 100L
        if (fee < BigInteger.valueOf(minimumLibFee)) {
            throw RuntimeException("Fee is less than the minimum of $minimumLibFee taris.")
        }
        if (amount < BigInteger.valueOf(0L)) {
            throw RuntimeException("Amount is less than 0.")
        }
        if (destination == getPublicKey()) {
            throw RuntimeException("Tx source and destination are the same.")
        }
        val error = FFIError()
        val result = jniSendTx(
            ptr,
            destination.getPointer(),
            amount.toString(),
            fee.toString(),
            message,
            error
        )
        // TODO ignore error#210 until the base node is ready
        if (error.code != 0) {
            throw RuntimeException()
        }
        return result
    }

    fun signMessage(message: String): String {
        val error = FFIError()
        val result = jniSignMessage(ptr, message, error)
        if (error.code != 0) {
            throw RuntimeException()
        }
        return result
    }

    fun verifyMessageSignature(
        contactPublicKey: FFIPublicKey,
        message: String,
        signature: String
    ): Boolean {
        val error = FFIError()
        val result =
            jniVerifyMessageSignature(ptr, contactPublicKey.getPointer(), message, signature, error)
        if (error.code != 0) {
            throw RuntimeException()
        }
        return result
    }

    fun importUTXO(
        amount: BigInteger,
        message: String,
        spendingKey: FFIPrivateKey,
        sourcePublicKey: FFIPublicKey
    ): BigInteger {
        val error = FFIError()
        val bytes = jniImportUTXO(
            ptr,
            spendingKey.getPointer(),
            sourcePublicKey.getPointer(),
            amount.toString(),
            message,
            error
        )
        if (error.code != 0) {
            throw RuntimeException()
        }
        return BigInteger(1, bytes)
    }

    override fun destroy() {
        jniDestroy(ptr)
        ptr = nullptr
    }

}