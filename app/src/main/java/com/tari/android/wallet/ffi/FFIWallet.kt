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

import com.tari.android.wallet.data.sharedPrefs.SharedPrefsRepository
import com.tari.android.wallet.data.sharedPrefs.network.NetworkRepository
import com.tari.android.wallet.model.*
import com.tari.android.wallet.model.recovery.WalletRestorationResult
import com.tari.android.wallet.service.seedPhrase.SeedPhraseRepository
import com.tari.android.wallet.util.Constants
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import java.math.BigInteger
import java.util.concurrent.atomic.AtomicReference

/**
 * Wallet wrapper.
 *
 * @author The Tari Development Team
 */

class FFIWallet(
    val sharedPrefsRepository: SharedPrefsRepository,
    val seedPhraseRepository: SeedPhraseRepository,
    val networkRepository: NetworkRepository,
    val commsConfig: FFICommsConfig,
    val logPath: String
) : FFIBase() {

    private val coroutineContext = Job()
    private var localScope = CoroutineScope(coroutineContext)

    companion object {
        private var atomicInstance = AtomicReference<FFIWallet>()
        var instance: FFIWallet?
            get() = atomicInstance.get()
            set(value) = atomicInstance.set(value)
    }

    private external fun jniCreate(
        commsConfig: FFICommsConfig,
        logPath: String,
        maxNumberOfRollingLogFiles: Int,
        rollingLogFileMaxSizeBytes: Int,
        passphrase: String?,
        network: String?,
        seedWords: FFISeedWords?,
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
        callbackTxFauxConfirmed: String,
        callbackTxFauxConfirmedSig: String,
        callbackTxFauxUnconfirmed: String,
        callbackTxFauxUnconfirmedSig: String,
        callbackTxMinedUnconfirmed: String,
        callbackTxMinedUnconfirmedSig: String,
        callbackDirectSendResult: String,
        callbackDirectSendResultSig: String,
        callbackTxCancellation: String,
        callbackTxCancellationSig: String,
        callbackTXOValidationComplete: String,
        callbackTXOValidationCompleteSig: String,
        callbackContactsLivenessDataUpdated: String,
        callbackContactsLivenessDataUpdatedSig: String,
        callbackBalanceUpdated: String,
        callbackBalanceUpdatedSig: String,
        callbackTransactionValidationComplete: String,
        callbackTransactionValidationCompleteSig: String,
        callbackConnectivityStatus: String,
        callbackConnectivityStatusSig: String,
        libError: FFIError
    )

    private external fun jniGetBalance(libError: FFIError): FFIPointer

    private external fun jniLogMessage(message: String, libError: FFIError)

    private external fun jniGetPublicKey(libError: FFIError): FFIPointer

    private external fun jniGetContacts(libError: FFIError): FFIPointer

    private external fun jniAddUpdateContact(contactPtr: FFIContact, libError: FFIError): Boolean

    private external fun jniRemoveContact(contactPtr: FFIContact, libError: FFIError): Boolean

    private external fun jniGetCompletedTxs(libError: FFIError): FFIPointer

    private external fun jniGetCancelledTxs(libError: FFIError): FFIPointer

    private external fun jniGetCompletedTxById(id: String, libError: FFIError): FFIPointer

    private external fun jniGetCancelledTxById(id: String, libError: FFIError): FFIPointer

    private external fun jniGetPendingOutboundTxs(libError: FFIError): FFIPointer

    private external fun jniGetPendingOutboundTxById(id: String, libError: FFIError): FFIPointer

    private external fun jniGetPendingInboundTxs(libError: FFIError): FFIPointer

    private external fun jniGetPendingInboundTxById(id: String, libError: FFIError): FFIPointer

    private external fun jniCancelPendingTx(id: String, libError: FFIError): Boolean

    private external fun jniSendTx(
        publicKeyPtr: FFIPublicKey,
        amount: String,
        feePerGram: String,
        message: String,
        oneSided: Boolean,
        libError: FFIError
    ): ByteArray

    private external fun jniSignMessage(message: String, libError: FFIError): String

    private external fun jniVerifyMessageSignature(publicKeyPtr: FFIPublicKey, message: String, signature: String, libError: FFIError): Boolean

    private external fun jniImportUTXO(
        amount: String,
        spendingKey: FFIPrivateKey,
        sourcePublicKey: FFIPublicKey,
        tariCommitmentSignature: FFITariCommitmentSignature,
        sourceSenderPublicKey: FFIPublicKey,
        scriptPrivateKey: FFIPrivateKey,
        message: String,
        libError: FFIError
    ): ByteArray

    private external fun jniAddBaseNodePeer(publicKey: FFIPublicKey, address: String, libError: FFIError): Boolean

    private external fun jniStartTXOValidation(libError: FFIError): ByteArray

    private external fun jniStartTxValidation(libError: FFIError): ByteArray

    private external fun jniRestartTxBroadcast(libError: FFIError): ByteArray

    private external fun jniPowerModeNormal(libError: FFIError)

    private external fun jniPowerModeLow(libError: FFIError)

    private external fun jniGetSeedWords(libError: FFIError): FFIPointer

    private external fun jniSetKeyValue(key: String, value: String, libError: FFIError): Boolean

    private external fun jniGetKeyValue(key: String, libError: FFIError): String

    private external fun jniRemoveKeyValue(key: String, libError: FFIError): Boolean

    private external fun jniGetConfirmations(libError: FFIError): ByteArray

    private external fun jniSetConfirmations(number: String, libError: FFIError)

    private external fun jniEstimateTxFee(amount: String, gramFee: String, kernelCount: String, outputCount: String, libError: FFIError): ByteArray

    private external fun jniApplyEncryption(passphrase: String, libError: FFIError)

    private external fun jniRemoveEncryption(libError: FFIError)

    private external fun jniStartRecovery(
        base_node_public_key: FFIPublicKey,
        callback: String,
        callback_sig: String,
        recoveryOutputMessage: String,
        libError: FFIError
    ): Boolean

    private external fun jniWalletGetFeePerGramStats(count: Int, libError: FFIError): FFIPointer

    private external fun jniGetUtxos(page: Int, pageSize: Int, sorting: Int, dustThreshold: Long, libError: FFIError): FFIPointer

    private external fun jniGetAllUtxos(libError: FFIError): FFIPointer

    private external fun jniJoinUtxos(commitments: Array<String>, feePerGram: String, libError: FFIError): FFIPointer

    private external fun jniSplitUtxos(commitments: Array<String>, splitCount: String, feePerGram: String, libError: FFIError): FFIPointer

    private external fun jniPreviewJoinUtxos(commitments: Array<String>, feePerGram: String, libError: FFIError): FFIPointer

    private external fun jniPreviewSplitUtxos(commitments: Array<String>, splitCount: String, feePerGram: String, libError: FFIError): FFIPointer

    private external fun jniDestroy()


    var listener: FFIWalletListener? = null

    // this acts as a constructor would for a normal class since constructors are not allowed for
    // singletons
    init {
        if (pointer == nullptr) { // so it can only be assigned once for the singleton
            init()
        }
    }

    private fun init() {
        val error = FFIError()
        logger.i("Pre jniCreate")
        try {
            jniCreate(
                commsConfig,
                logPath,
                Constants.Wallet.maxNumberOfRollingLogFiles,
                Constants.Wallet.rollingLogFileMaxSizeBytes,
                sharedPrefsRepository.databasePassphrase,
                networkRepository.currentNetwork?.network?.uriComponent,
                seedPhraseRepository.getPhrase()?.ffiSeedWords,
                this::onTxReceived.name, "(J)V",
                this::onTxReplyReceived.name, "(J)V",
                this::onTxFinalized.name, "(J)V",
                this::onTxBroadcast.name, "(J)V",
                this::onTxMined.name, "(J)V",
                this::onTxMinedUnconfirmed.name, "(J[B)V",
                this::onTxFauxConfirmed.name, "(J)V",
                this::onTxFauxUnconfirmed.name, "(J[B)V",
                this::onDirectSendResult.name, "([BJ)V",
                this::onTxCancelled.name, "(J[B)V",
                this::onTXOValidationComplete.name, "([B[B)V",
                this::onContactLivenessDataUpdated.name, "(J)V",
                this::onBalanceUpdated.name, "(J)V",
                this::onTxValidationComplete.name, "([B[B)V",
                this::onConnectivityStatus.name, "([B)V",
                error
            )
        } catch (e: Throwable) {
            logger.e(e, "jniCreate was failed")
            throw e
        }

        logger.i("Post jniCreate with code: %d.", error.code)
        throwIf(error)

        enableEncryption()
    }

    @Synchronized
    fun enableEncryption(databasePhrase: String? = null) {
        if (sharedPrefsRepository.databasePassphrase == null) {
            try {
                val databasePassphrase = databasePhrase ?: sharedPrefsRepository.generateDatabasePassphrase()
                setEncryption(databasePassphrase)
                sharedPrefsRepository.databasePassphrase = databasePassphrase
                logger.i("Database encryption enabled")
            } catch (e: Throwable) {
                sharedPrefsRepository.databasePassphrase = null
                logger.e(e, "Database encryption failed")
            }
        }
    }

    fun getBalance(): BalanceInfo = FFIBalance(runWithError { jniGetBalance(it) }).runWithDestroy {
        BalanceInfo(it.getAvailable(), it.getIncoming(), it.getOutgoing(), it.getTimeLocked())
    }

    fun getUtxos(page: Int, pageSize: Int, sorting: Int): TariVector =
        TariVector(FFITariVector(runWithError { jniGetUtxos(page, pageSize, sorting, 0, it) }))

    fun getAllUtxos(): TariVector = TariVector(FFITariVector(runWithError { jniGetAllUtxos(it) }))

    fun getPublicKey(): FFIPublicKey = runWithError { FFIPublicKey(jniGetPublicKey(it)) }

    fun getContacts(): FFIContacts = runWithError { FFIContacts(jniGetContacts(it)) }

    fun addUpdateContact(contact: FFIContact): Boolean = runWithError { jniAddUpdateContact(contact, it) }

    fun removeContact(contact: FFIContact): Boolean = runWithError { jniRemoveContact(contact, it) }

    fun getCompletedTxs(): FFICompletedTxs = runWithError { FFICompletedTxs(jniGetCompletedTxs(it)) }

    fun getCancelledTxs(): FFICompletedTxs = runWithError { FFICompletedTxs(jniGetCancelledTxs(it)) }

    fun getCompletedTxById(id: BigInteger): FFICompletedTx = runWithError { FFICompletedTx(jniGetCompletedTxById(id.toString(), it)) }

    fun getCancelledTxById(id: BigInteger): FFICompletedTx = runWithError { FFICompletedTx(jniGetCancelledTxById(id.toString(), it)) }

    fun getPendingOutboundTxs(): FFIPendingOutboundTxs = runWithError { FFIPendingOutboundTxs(jniGetPendingOutboundTxs(it)) }

    fun getPendingOutboundTxById(id: BigInteger): FFIPendingOutboundTx =
        runWithError { FFIPendingOutboundTx(jniGetPendingOutboundTxById(id.toString(), it)) }

    fun getPendingInboundTxs(): FFIPendingInboundTxs = runWithError { FFIPendingInboundTxs(jniGetPendingInboundTxs(it)) }

    fun getPendingInboundTxById(id: BigInteger): FFIPendingInboundTx =
        runWithError { FFIPendingInboundTx(jniGetPendingInboundTxById(id.toString(), it)) }

    fun cancelPendingTx(id: BigInteger): Boolean = runWithError { jniCancelPendingTx(id.toString(), it) }

    @Suppress("MemberVisibilityCanBePrivate")
    fun onTxReceived(pendingInboundTxPtr: FFIPointer) {
        val tx = FFIPendingInboundTx(pendingInboundTxPtr)
        logger.i("Tx received ${tx.getId()}")
        val pendingTx = PendingInboundTx(tx)
        localScope.launch { listener?.onTxReceived(pendingTx) }
    }

    /**
     * This callback function cannot be private due to JNI behaviour
     */
    @Suppress("MemberVisibilityCanBePrivate")
    fun onTxReplyReceived(txPointer: FFIPointer) {
        val tx = FFICompletedTx(txPointer)
        logger.i("Tx reply received ${tx.getId()}")
        val pendingOutboundTx = PendingOutboundTx(tx)
        localScope.launch { listener?.onTxReplyReceived(pendingOutboundTx) }
    }

    /**
     * This callback function cannot be private due to JNI behaviour.
     */
    @Suppress("MemberVisibilityCanBePrivate")
    fun onTxFinalized(completedTx: FFIPointer) {
        val tx = FFICompletedTx(completedTx)
        logger.i("Tx finalized ${tx.getId()}")
        val pendingInboundTx = PendingInboundTx(tx)
        localScope.launch { listener?.onTxFinalized(pendingInboundTx) }
    }

    /**
     * This callback function cannot be private due to JNI behaviour.
     */
    @Suppress("MemberVisibilityCanBePrivate")
    fun onTxBroadcast(completedTxPtr: FFIPointer) {
        val tx = FFICompletedTx(completedTxPtr)
        logger.i("Tx broadcast ${tx.getId()}")
        when (tx.getDirection()) {
            Tx.Direction.INBOUND -> {
                val pendingInboundTx = PendingInboundTx(tx)
                localScope.launch { listener?.onInboundTxBroadcast(pendingInboundTx) }
            }
            Tx.Direction.OUTBOUND -> {
                val pendingOutboundTx = PendingOutboundTx(tx)
                localScope.launch { listener?.onOutboundTxBroadcast(pendingOutboundTx) }
            }
        }
    }

    /**
     * This callback function cannot be private due to JNI behaviour.
     */
    @Suppress("MemberVisibilityCanBePrivate")
    fun onTxMined(completedTxPtr: FFIPointer) {
        val completed = CompletedTx(completedTxPtr)
        logger.i("Tx mined & confirmed ${completed.id}")
        localScope.launch { listener?.onTxMined(completed) }
    }

    /**
     * This callback function cannot be private due to JNI behaviour.
     */
    @Suppress("MemberVisibilityCanBePrivate")
    fun onTxMinedUnconfirmed(completedTxPtr: FFIPointer, confirmationCountBytes: ByteArray) {
        val confirmationCount = BigInteger(1, confirmationCountBytes).toInt()
        val completed = CompletedTx(completedTxPtr)
        logger.i("Tx mined & unconfirmed ${completed.id} $confirmationCount")
        localScope.launch { listener?.onTxMinedUnconfirmed(completed, confirmationCount) }
    }

    /**
     * This callback function cannot be private due to JNI behaviour.
     */
    @Suppress("MemberVisibilityCanBePrivate")
    fun onTxFauxConfirmed(completedTxPtr: FFIPointer) {
        val completed = CompletedTx(completedTxPtr)
        logger.i("Tx faux confirmed ${completed.id}")
        localScope.launch { listener?.onTxMined(completed) }
    }

    /**
     * This callback function cannot be private due to JNI behaviour.
     */
    @Suppress("MemberVisibilityCanBePrivate")
    fun onTxFauxUnconfirmed(completedTxPtr: FFIPointer, confirmationCountBytes: ByteArray) {
        val confirmationCount = BigInteger(1, confirmationCountBytes).toInt()
        val completed = CompletedTx(completedTxPtr)
        logger.i("Tx faux unconfirmed ${completed.id}")
        localScope.launch { listener?.onTxMinedUnconfirmed(completed, confirmationCount) }
    }

    /**
     * This callback function cannot be private due to JNI behaviour.
     */
    @Suppress("MemberVisibilityCanBePrivate")
    fun onDirectSendResult(bytes: ByteArray, pointer: FFIPointer) {
        val txId = BigInteger(1, bytes)
        logger.i("Tx direct send result $txId")
        localScope.launch { listener?.onDirectSendResult(txId, FFITransactionSendStatus(pointer).getStatus()) }
    }

    /**
     * This callback function cannot be private due to JNI behaviour.
     */
    @Suppress("MemberVisibilityCanBePrivate")
    fun onTxCancelled(completedTx: FFIPointer, rejectionReason: ByteArray) {
        val rejectionReasonInt = BigInteger(1, rejectionReason).toInt()
        val tx = FFICompletedTx(completedTx)
        logger.i("Tx cancelled ${tx.getId()}")

        if (tx.getDirection() == Tx.Direction.OUTBOUND) {
            localScope.launch { listener?.onTxCancelled(CancelledTx(tx), rejectionReasonInt) }
        }
    }

    /**
     * This callback function cannot be private due to JNI behaviour.
     */
    @Suppress("MemberVisibilityCanBePrivate")
    fun onConnectivityStatus(bytes: ByteArray) {
        val connectivityStatus = BigInteger(1, bytes)
        localScope.launch { listener?.onConnectivityStatus(connectivityStatus.toInt()) }
        logger.i("ConnectivityStatus is [$connectivityStatus]")
    }

    /**
     * This callback function cannot be private due to JNI behaviour.
     */
    @Suppress("MemberVisibilityCanBePrivate")
    fun onBalanceUpdated(ptr: FFIPointer) {
        logger.i("Balance Updated")
        val balance = FFIBalance(ptr).runWithDestroy { BalanceInfo(it.getAvailable(), it.getIncoming(), it.getOutgoing(), it.getTimeLocked()) }
        localScope.launch { listener?.onBalanceUpdated(balance) }
    }

    /**
     * This callback function cannot be private due to JNI behaviour.
     */
    @Suppress("MemberVisibilityCanBePrivate")
    fun onTXOValidationComplete(bytes: ByteArray, statusBytes: ByteArray) {
        val requestId = BigInteger(1, bytes)
        val statusInteger = BigInteger(1, statusBytes).toInt()
        val status = TransactionValidationStatus.values().firstOrNull { it.value == statusInteger } ?: return
        logger.i("TXO validation [$requestId] complete. Result: $status")
        localScope.launch { listener?.onTXOValidationComplete(requestId, status) }
    }

    /**
     * This callback function cannot be private due to JNI behaviour.
     */
    @Suppress("MemberVisibilityCanBePrivate")
    fun onTxValidationComplete(requestIdBytes: ByteArray, statusBytes: ByteArray) {
        val requestId = BigInteger(1, requestIdBytes)
        val statusInteger = BigInteger(1, statusBytes).toInt()
        val status = TransactionValidationStatus.values().firstOrNull { it.value == statusInteger } ?: return
        logger.i("Tx validation [$requestId] complete. Result: $status")
        localScope.launch { listener?.onTxValidationComplete(requestId, status) }
    }

    /**
     * This callback function cannot be private due to JNI behaviour.
     */
    @Suppress("MemberVisibilityCanBePrivate", "UNUSED_PARAMETER")
    fun onContactLivenessDataUpdated(livenessUpdate: FFIPointer) {
        logger.i("OnContactLivenessDataUpdated")
    }

    fun estimateTxFee(amount: BigInteger, gramFee: BigInteger, kernelCount: BigInteger, outputCount: BigInteger): BigInteger = runWithError {
        BigInteger(1, jniEstimateTxFee(amount.toString(), gramFee.toString(), kernelCount.toString(), outputCount.toString(), it))
    }

    fun sendTx(destination: FFIPublicKey, amount: BigInteger, feePerGram: BigInteger, message: String, isOneSided: Boolean): BigInteger {
        if (amount < BigInteger.valueOf(0L)) {
            throw FFIException(message = "Amount is less than 0.")
        }
        if (destination == getPublicKey()) {
            throw FFIException(message = "Tx source and destination are the same.")
        }
        val bytes = runWithError { jniSendTx(destination, amount.toString(), feePerGram.toString(), message, isOneSided, it) }
        return BigInteger(1, bytes)
    }

    fun joinUtxos(commitments: Array<String>, feePerGram: BigInteger, error: FFIError) {
        jniJoinUtxos(commitments, feePerGram.toString(), error)
    }

    fun splitUtxos(commitments: Array<String>, count: Int, feePerGram: BigInteger, error: FFIError) {
        jniSplitUtxos(commitments, count.toString(), feePerGram.toString(), error)
    }

    fun joinPreviewUtxos(commitments: Array<String>, feePerGram: BigInteger, error: FFIError): TariCoinPreview =
        TariCoinPreview(FFITariCoinPreview(jniPreviewJoinUtxos(commitments, feePerGram.toString(), error)))

    fun splitPreviewUtxos(commitments: Array<String>, count: Int, feePerGram: BigInteger, error: FFIError): TariCoinPreview =
        TariCoinPreview(FFITariCoinPreview(jniPreviewSplitUtxos(commitments, count.toString(), feePerGram.toString(), error)))

    fun signMessage(message: String): String = runWithError { jniSignMessage(message, it) }

    fun verifyMessageSignature(contactPublicKey: FFIPublicKey, message: String, signature: String): Boolean =
        runWithError { jniVerifyMessageSignature(contactPublicKey, message, signature, it) }

    fun importUTXO(
        amount: BigInteger,
        message: String,
        spendingKey: FFIPrivateKey,
        sourcePublicKey: FFIPublicKey,
        tariCommitmentSignature: FFITariCommitmentSignature,
        senderPublicKey: FFIPublicKey,
        scriptPrivateKey: FFIPrivateKey,
    ): BigInteger = runWithError {
        BigInteger(
            jniImportUTXO(
                amount.toString(),
                spendingKey,
                sourcePublicKey,
                tariCommitmentSignature,
                senderPublicKey,
                scriptPrivateKey,
                message,
                it
            )
        )
    }

    fun startTXOValidation(): BigInteger = runWithError { BigInteger(1, jniStartTXOValidation(it)) }

    fun startTxValidation(): BigInteger = runWithError { BigInteger(1, jniStartTxValidation(it)) }

    fun restartTxBroadcast(): BigInteger = runWithError { BigInteger(1, jniRestartTxBroadcast(it)) }

    fun setPowerModeNormal() = runWithError { jniPowerModeNormal(it) }

    fun setPowerModeLow() = runWithError { jniPowerModeLow(it) }

    fun getSeedWords(): FFISeedWords = runWithError { FFISeedWords(jniGetSeedWords(it)) }

    fun addBaseNodePeer(baseNodePublicKey: FFIPublicKey, baseNodeAddress: String): Boolean =
        runWithError { jniAddBaseNodePeer(baseNodePublicKey, baseNodeAddress, it) }

    fun setKeyValue(key: String, value: String): Boolean = runWithError { jniSetKeyValue(key, value, it) }

    fun getKeyValue(key: String): String = runWithError { jniGetKeyValue(key, it) }

    fun removeKeyValue(key: String): Boolean = runWithError { jniRemoveKeyValue(key, it) }

    fun logMessage(message: String) = runWithError { jniLogMessage(message, it) }

    fun getRequiredConfirmationCount(): BigInteger = runWithError { BigInteger(1, jniGetConfirmations(it)) }

    fun setRequiredConfirmationCount(number: BigInteger) = runWithError { jniSetConfirmations(number.toString(), it) }

    fun setEncryption(passphrase: String) = runWithError { jniApplyEncryption(passphrase, it) }

    fun removeEncryption() = runWithError { jniRemoveEncryption(it) }

    fun startRecovery(baseNodePublicKey: FFIPublicKey, recoveryOutputMessage: String): Boolean =
        runWithError { jniStartRecovery(baseNodePublicKey, this::onWalletRecovery.name, "(I[B[B)V", recoveryOutputMessage, it) }

    fun getFeePerGramStats(): FFIFeePerGramStats = runWithError { FFIFeePerGramStats(jniWalletGetFeePerGramStats(3, it)) }

    /**
     * This callback function cannot be private due to JNI behaviour.
     */
    @Suppress("MemberVisibilityCanBePrivate")
    fun onWalletRecovery(event: Int, firstArg: ByteArray, secondArg: ByteArray) {
        val result = WalletRestorationResult.create(event, firstArg, secondArg)
        logger.i("Wallet restored with $result")
        localScope.launch { listener?.onWalletRestoration(result) }
    }

    override fun destroy() {
        listener = null
        jniDestroy()
    }
}

