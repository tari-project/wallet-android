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
import com.tari.android.wallet.model.WalletErrorCode.*
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
        commsConfig: FFICommsConfig,
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
    )

    private external fun jniGetPublicKey(
        libError: FFIError
    ): FFIPublicKeyPtr

    private external fun jniGetAvailableBalance(
        libError: FFIError
    ): ByteArray

    private external fun jniGetPendingIncomingBalance(
        libError: FFIError
    ): ByteArray

    private external fun jniGetPendingOutgoingBalance(
        libError: FFIError
    ): ByteArray

    private external fun jniGetContacts(libError: FFIError): FFIContactsPtr

    private external fun jniAddUpdateContact(
        contactPtr: FFIContact,
        libError: FFIError
    ): Boolean

    private external fun jniRemoveContact(
        contactPtr: FFIContact,
        libError: FFIError
    ): Boolean

    private external fun jniGetCompletedTxs(
        libError: FFIError
    ): FFICompletedTxsPtr

    private external fun jniGetCompletedTxById(
        id: String,
        libError: FFIError
    ): FFICompletedTxPtr

    private external fun jniGetPendingOutboundTxs(
        libError: FFIError
    ): FFIPendingOutboundTxsPtr

    private external fun jniGetPendingOutboundTxById(
        id: String,
        libError: FFIError
    ): FFIPendingOutboundTxPtr

    private external fun jniGetPendingInboundTxs(
        libError: FFIError
    ): FFIPendingInboundTxsPtr

    private external fun jniGetPendingInboundTxById(
        id: String,
        libError: FFIError
    ): FFIPendingInboundTxPtr

    private external fun jniIsCompletedTxOutbound(
        completedTx: FFICompletedTx,
        libError: FFIError
    ): Boolean

    private external fun jniSendTx(
        publicKeyPtr: FFIPublicKey,
        amount: String,
        fee: String,
        message: String,
        libError: FFIError
    ): Boolean

    private external fun jniSignMessage(
        message: String,
        libError: FFIError
    ): String

    private external fun jniVerifyMessageSignature(
        publicKeyPtr: FFIPublicKey,
        message: String,
        signature: String,
        libError: FFIError
    ): Boolean

    private external fun jniImportUTXO(
        spendingKey: FFIPrivateKey,
        sourcePublicKey: FFIPublicKey,
        amount: String,
        message: String,
        libError: FFIError
    ): ByteArray

    private external fun jniAddBaseNodePeer(
        publicKeyPtr: FFIPublicKeyPtr,
        address: String,
        libError: FFIError
    ): Boolean

    private external fun jniSyncBaseNode(
        libError: FFIError
    ): Boolean

    private external fun jniGetTorPrivateKey(
        libError: FFIError
    ): FFIByteVectorPtr

    private external fun jniDestroy()

    // endregion

    protected var ptr = nullptr
    var listenerAdapter: FFIWalletListenerAdapter? = null

    // this acts as a constructor would for a normal class since constructors are not allowed for
    // singletons
    init {
        if (ptr == nullptr) { // so it can only be assigned once for the singleton
            val error = FFIError()
            Logger.i("Pre jniCreate.")
            jniCreate(
                commsConfig, logPath,
                this::onTxReceived.name, "(J)V",
                this::onTxReplyReceived.name, "(J)V",
                this::onTxFinalized.name, "(J)V",
                this::onTxBroadcast.name, "(J)V",
                this::onTxMined.name, "(J)V",
                this::onDiscoveryComplete.name, "([BZ)V",
                error
            )
            Logger.i("Post jniCreate.")
            Log.i("Wallet Code", error.code.toString())
            throwIf(error)
        }
    }

    fun getPointer(): FFIWalletPtr {
        return ptr
    }

    fun getAvailableBalance(): BigInteger {
        val error = FFIError()
        val bytes = jniGetAvailableBalance(error)
        throwIf(error)
        return BigInteger(1, bytes)
    }

    fun getPendingIncomingBalance(): BigInteger {
        val error = FFIError()
        val bytes = jniGetPendingIncomingBalance(error)
        throwIf(error)
        return BigInteger(1, bytes)
    }

    fun getPendingOutgoingBalance(): BigInteger {
        val error = FFIError()
        val bytes = jniGetPendingOutgoingBalance(error)
        throwIf(error)
        return BigInteger(1, bytes)
    }

    fun getPublicKey(): FFIPublicKey {
        val error = FFIError()
        val result = FFIPublicKey(jniGetPublicKey(error))
        throwIf(error)
        return result
    }

    fun getContacts(): FFIContacts {
        val error = FFIError()
        val result = FFIContacts(jniGetContacts(error))
        throwIf(error)
        return result
    }

    fun addUpdateContact(contact: FFIContact): Boolean {
        val error = FFIError()
        val result = jniAddUpdateContact(contact, error)
        throwIf(error)
        return result
    }

    fun removeContact(contact: FFIContact): Boolean {
        val error = FFIError()
        val result = jniRemoveContact(contact, error)
        throwIf(error)
        return result
    }

    fun isCompletedTxOutbound(completedTx: FFICompletedTx): Boolean {
        val error = FFIError()
        val result = jniIsCompletedTxOutbound(completedTx, error)
        throwIf(error)
        return result
    }

    fun getCompletedTxs(): FFICompletedTxs {
        val error = FFIError()
        val result = FFICompletedTxs(jniGetCompletedTxs(error))
        throwIf(error)
        return result
    }

    fun getCompletedTxById(id: BigInteger): FFICompletedTx {
        val error = FFIError()
        val result = FFICompletedTx(jniGetCompletedTxById(id.toString(), error))
        throwIf(error)
        return result
    }

    fun getPendingOutboundTxs(): FFIPendingOutboundTxs {
        val error = FFIError()
        val result = FFIPendingOutboundTxs(jniGetPendingOutboundTxs(error))
        throwIf(error)
        return result
    }

    fun getPendingOutboundTxById(id: BigInteger): FFIPendingOutboundTx {
        val error = FFIError()
        val result =
            FFIPendingOutboundTx(jniGetPendingOutboundTxById(id.toString(), error))
        throwIf(error)
        return result
    }

    fun getPendingInboundTxs(): FFIPendingInboundTxs {
        val error = FFIError()
        val result = FFIPendingInboundTxs(jniGetPendingInboundTxs(error))
        throwIf(error)
        return result
    }

    fun getPendingInboundTxById(id: BigInteger): FFIPendingInboundTx {
        val error = FFIError()
        val result = FFIPendingInboundTx(jniGetPendingInboundTxById(id.toString(), error))
        throwIf(error)
        return result
    }

    protected fun onTxBroadcast(ptr: FFICompletedTxPtr) {
        Logger.i("Tx completed. Pointer: %s", ptr.toString())
        val tx = FFICompletedTx(ptr)
        val id = tx.getId()
        tx.destroy()
        listenerAdapter?.onTxBroadcast(id)
    }

    protected fun onTxMined(ptr: FFICompletedTxPtr) {
        Logger.i("Tx mined. Pointer: %s", ptr.toString())
        val tx = FFICompletedTx(ptr)
        listenerAdapter?.onTxMined(tx.getId())
        tx.destroy()
    }

    protected fun onTxReceived(ptr: FFIPendingInboundTxPtr) {
        Logger.i("Tx received. Pointer: %s", ptr.toString())
        val tx = FFIPendingInboundTx(ptr)
        listenerAdapter?.onTxReceived(tx.getId())
        tx.destroy()
    }

    protected fun onTxReplyReceived(ptr: FFICompletedTxPtr) {
        Logger.i("Tx reply received. Pointer: %s", ptr.toString())
        val tx = FFICompletedTx(ptr)
        listenerAdapter?.onTxReplyReceived(tx.getId())
        tx.destroy()
    }

    protected fun onTxFinalized(ptr: FFICompletedTxPtr) {
        Logger.i("Tx finalized. Pointer: %s", ptr.toString())
        val tx = FFICompletedTx(ptr)
        listenerAdapter?.onTxFinalized(tx.getId())
        tx.destroy()
    }

    protected fun onDiscoveryComplete(bytes: ByteArray, success: Boolean) {
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
            throw FFIException(message = "Fee is less than the minimum of $minimumLibFee taris.")
        }
        if (amount < BigInteger.valueOf(0L)) {
            throw FFIException(message = "Amount is less than 0.")
        }
        if (destination == getPublicKey()) {
            throw FFIException(message = "Tx source and destination are the same.")
        }
        val error = FFIError()
        val result = jniSendTx(
            destination,
            amount.toString(),
            fee.toString(),
            message,
            error
        )
        if (error.code == OUTBOUND_SEND_DISCOVERY_IN_PROGRESS.code) {
            // TODO store the placeholder transaction in local storage until the discovery is complete
            return true
        }
        throwIf(error)
        return result
    }

    fun signMessage(message: String): String {
        val error = FFIError()
        val result = jniSignMessage(message, error)
        throwIf(error)
        return result
    }

    fun verifyMessageSignature(
        contactPublicKey: FFIPublicKey,
        message: String,
        signature: String
    ): Boolean {
        val error = FFIError()
        val result =
            jniVerifyMessageSignature(contactPublicKey, message, signature, error)
        throwIf(error)
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
            spendingKey,
            sourcePublicKey,
            amount.toString(),
            message,
            error
        )
        throwIf(error)
        return BigInteger(1, bytes)
    }

    fun syncBaseNode(): Boolean {
        val error = FFIError()
        val result = jniSyncBaseNode(error)
        throwIf(error)
        return result
    }

    fun getTorPrivateKey(): String {
        val error = FFIError()
        val resultPtr = jniGetTorPrivateKey(error)
        throwIf(error)
        val bytes = FFIByteVector(resultPtr)
        throwIf(error)
        return bytes.toString()
    }

    fun addBaseNodePeer(
        baseNodePublicKey: FFIPublicKey,
        baseNodeAddress: String
    ): Boolean {
        val error = FFIError()
        val result = jniAddBaseNodePeer(baseNodePublicKey.getPointer(), baseNodeAddress, error)
        throwIf(error)
        return result
    }

    override fun destroy() {
        jniDestroy()
    }

}