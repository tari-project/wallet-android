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

import com.orhanobut.logger.Logger
import com.tari.android.wallet.BuildConfig
import com.tari.android.wallet.data.sharedPrefs.network.TariNetwork
import com.tari.android.wallet.model.BalanceInfo
import com.tari.android.wallet.model.CancelledTx
import com.tari.android.wallet.model.CompletedTx
import com.tari.android.wallet.model.PendingInboundTx
import com.tari.android.wallet.model.PendingOutboundTx
import com.tari.android.wallet.model.PublicKey
import com.tari.android.wallet.model.TariCoinPreview
import com.tari.android.wallet.model.TariUnblindedOutput
import com.tari.android.wallet.model.TariVector
import com.tari.android.wallet.model.TariWalletAddress
import com.tari.android.wallet.model.Tx
import com.tari.android.wallet.recovery.WalletRestorationState
import java.math.BigInteger

/**
 * Wallet wrapper.
 *
 * @author The Tari Development Team
 */

class FFIWallet(
    private val listener: FFIWalletListener,
) : FFIBase() {

    private val logger
        get() = Logger.t(FFIWallet::class.simpleName)

    companion object {
        // values for the wallet initialization
        private val LOG_VERBOSITY: Int = if (BuildConfig.BUILD_TYPE == "debug") 11 else 4
        private const val IS_DNS_SECURE_ON = false
        private const val MAX_NUMBER_OF_ROLLING_LOG_FILES = 2
        private const val ROLLING_LOG_FILE_MAX_SIZE_BYTES = 10 * 1024 * 1024
    }

    private external fun jniCreate(
        commsConfig: FFICommsConfig,
        logPath: String,
        logVerbosity: Int,
        maxNumberOfRollingLogFiles: Int,
        rollingLogFileMaxSizeBytes: Int,
        passphrase: String?,
        network: String?,
        seedWords: FFISeedWords?,
        dnsPeer: String,
        isDnsSecureOn: Boolean,
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
        callbackWalletScannedHeight: String,
        callbackWalletScannedHeightSig: String,
        callbackBaseNodeStatusStatus: String,
        callbackBaseNodeStatusSig: String,
        libError: FFIError
    )

    private external fun jniGetBalance(libError: FFIError): FFIPointer
    private external fun jniLogMessage(message: String, libError: FFIError)
    private external fun jniGetWalletAddress(libError: FFIError): FFIPointer
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
        publicKeyPtr: FFITariWalletAddress,
        amount: String,
        feePerGram: String,
        message: String,
        oneSided: Boolean,
        paymentId: String,
        libError: FFIError
    ): ByteArray

    private external fun jniSignMessage(message: String, libError: FFIError): String
    private external fun jniVerifyMessageSignature(publicKeyPtr: FFIPublicKey, message: String, signature: String, libError: FFIError): Boolean
    private external fun jniGetBaseNodePeers(libError: FFIError): FFIPointer
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
    private external fun jniStartRecovery(
        baseNodePublicKey: FFIPublicKey,
        callback: String,
        callbackSig: String,
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
    private external fun jniWalletGetUnspentOutputs(libError: FFIError): FFIPointer
    private external fun jniImportExternalUtxoAsNonRewindable(
        output: FFITariUnblindedOutput,
        sourceAddress: FFITariWalletAddress,
        message: String,
        libError: FFIError
    ): ByteArray

    private external fun jniDestroy()

    constructor(
        tariNetwork: TariNetwork,
        commsConfig: FFICommsConfig,
        logPath: String,
        passphrase: String,
        seedWords: FFISeedWords?,
        listener: FFIWalletListener,
    ) : this(listener) {
        val error = FFIError()
        logger.i("Pre jniCreate")

        try {
            jniCreate(
                commsConfig = commsConfig,
                logPath = logPath,
                logVerbosity = LOG_VERBOSITY,
                maxNumberOfRollingLogFiles = MAX_NUMBER_OF_ROLLING_LOG_FILES,
                rollingLogFileMaxSizeBytes = ROLLING_LOG_FILE_MAX_SIZE_BYTES,
                passphrase = passphrase,
                network = tariNetwork.network.uriComponent,
                seedWords = seedWords,
                dnsPeer = tariNetwork.dnsPeer,
                isDnsSecureOn = IS_DNS_SECURE_ON,
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
                this::onWalletScannedHeight.name, "([B)V",
                this::onBaseNodeStatus.name, "(J)V",
                libError = error,
            )
        } catch (e: Throwable) {
            logger.e(e, "jniCreate was failed")
            throw e
        }

        logger.i("Post jniCreate with code: ${error.code}.")
        throwIf(error)
    }

    fun getBalance(): BalanceInfo = FFIBalance(runWithError { jniGetBalance(it) }).runWithDestroy {
        BalanceInfo(it.getAvailable(), it.getIncoming(), it.getOutgoing(), it.getTimeLocked())
    }

    fun getUtxos(page: Int, pageSize: Int, sorting: Int): TariVector =
        TariVector(FFITariVector(runWithError { jniGetUtxos(page, pageSize, sorting, 0, it) }))

    fun getAllUtxos(): TariVector = TariVector(FFITariVector(runWithError { jniGetAllUtxos(it) }))

    fun getWalletAddress(): FFITariWalletAddress = runWithError { FFITariWalletAddress(jniGetWalletAddress(it)) }

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

    fun estimateTxFee(amount: BigInteger, gramFee: BigInteger, kernelCount: BigInteger, outputCount: BigInteger): BigInteger = runWithError {
        BigInteger(1, jniEstimateTxFee(amount.toString(), gramFee.toString(), kernelCount.toString(), outputCount.toString(), it))
    }

    fun sendTx(
        destination: FFITariWalletAddress,
        amount: BigInteger,
        feePerGram: BigInteger,
        message: String,
        isOneSided: Boolean,
        paymentId: String,
    ): BigInteger {
        if (amount < BigInteger.valueOf(0L)) {
            throw FFIException(message = "Amount is less than 0.")
        }
        if (destination == getWalletAddress()) {
            throw FFIException(message = "Tx source and destination are the same.")
        }
        val bytes = runWithError { jniSendTx(destination, amount.toString(), feePerGram.toString(), message, isOneSided, paymentId, it) }
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

    fun startTXOValidation(): BigInteger = runWithError { BigInteger(1, jniStartTXOValidation(it)) }

    fun startTxValidation(): BigInteger = runWithError { BigInteger(1, jniStartTxValidation(it)) }

    fun restartTxBroadcast(): BigInteger = runWithError { BigInteger(1, jniRestartTxBroadcast(it)) }

    fun setPowerModeNormal() = runWithError { jniPowerModeNormal(it) }

    fun setPowerModeLow() = runWithError { jniPowerModeLow(it) }

    fun getSeedWords(): FFISeedWords = runWithError { FFISeedWords(jniGetSeedWords(it)) }

    fun getBaseNodePeers(): List<PublicKey> = runWithError { error ->
        FFIPublicKeys(jniGetBaseNodePeers(error)).let { ffiPublicKeys ->
            List(ffiPublicKeys.getLength()) { index -> PublicKey(ffiPublicKeys.getAt(index)) }
        }
    }

    fun addBaseNodePeer(baseNodePublicKey: FFIPublicKey, baseNodeAddress: String): Boolean =
        runWithError { jniAddBaseNodePeer(baseNodePublicKey, baseNodeAddress, it) }

    fun setKeyValue(key: String, value: String): Boolean = runWithError { jniSetKeyValue(key, value, it) }

    fun getKeyValue(key: String): String = runWithError { jniGetKeyValue(key, it) }

    fun removeKeyValue(key: String): Boolean = runWithError { jniRemoveKeyValue(key, it) }

    fun logMessage(message: String) = runWithError { jniLogMessage(message, it) }

    fun getRequiredConfirmationCount(): BigInteger = runWithError { BigInteger(1, jniGetConfirmations(it)) }

    fun setRequiredConfirmationCount(number: BigInteger) = runWithError { jniSetConfirmations(number.toString(), it) }

    fun startRecovery(baseNodePublicKey: FFIPublicKey, recoveryOutputMessage: String): Boolean =
        runWithError { jniStartRecovery(baseNodePublicKey, this::onWalletRecovery.name, "(I[B[B)V", recoveryOutputMessage, it) }

    fun getFeePerGramStats(): FFIFeePerGramStats = runWithError { FFIFeePerGramStats(jniWalletGetFeePerGramStats(3, it)) }

    fun getUnbindedOutputs(error: FFIError): List<TariUnblindedOutput> {
        val outputs = FFITariUnblindedOutputs(jniWalletGetUnspentOutputs(error))
        val txs = mutableListOf<TariUnblindedOutput>()
        for (i in 0 until outputs.getLength()) {
            txs.add(TariUnblindedOutput(outputs.getAt(i)))
        }
        return txs
    }

    fun restoreWithUnbindedOutputs(jsons: List<String>, address: TariWalletAddress, message: String, error: FFIError) {
        for (json in jsons) {
            val output = FFITariUnblindedOutput(json)
            jniImportExternalUtxoAsNonRewindable(output, FFITariWalletAddress(emojiId = address.fullEmojiId), message, error)
        }
    }

    private fun onWalletRecovery(event: Int, firstArg: ByteArray, secondArg: ByteArray) {
        val state = WalletRestorationState.create(event, firstArg, secondArg)
        logger.i(
            "Wallet restoration: ${
                when (state) {
                    is WalletRestorationState.ConnectingToBaseNode -> "Connecting to base node"
                    is WalletRestorationState.ConnectedToBaseNode -> "Connected to base node"
                    is WalletRestorationState.ConnectionToBaseNodeFailed -> "Connection to base node failed: ${state.retryCount}/${state.retryLimit}"
                    is WalletRestorationState.Progress -> "Progress: ${state.currentBlock}/${state.numberOfBlocks}"
                    is WalletRestorationState.Completed -> "Completed: ${state.numberOfUTXO} UTXOs, ${state.microTari.size} MicroTari"
                    is WalletRestorationState.ScanningRoundFailed -> "Scanning round failed: ${state.retryCount}/${state.retryLimit}"
                    is WalletRestorationState.RecoveryFailed -> "Recovery failed"
                }
            }"
        )
        listener.onWalletRestoration(state)
    }

    override fun destroy() {
        jniDestroy()
    }

    /* FFI wallet callbacks */

    private fun onTxReceived(pendingInboundTxPtr: FFIPointer) {
        val tx = FFIPendingInboundTx(pendingInboundTxPtr)
        logger.i("Tx received ${tx.getId()}")
        val pendingTx = PendingInboundTx(tx)
        listener.onTxReceived(pendingTx)
    }

    private fun onTxReplyReceived(txPointer: FFIPointer) {
        val tx = FFICompletedTx(txPointer)
        logger.i("Tx reply received ${tx.getId()}")
        val pendingOutboundTx = PendingOutboundTx(tx)
        listener.onTxReplyReceived(pendingOutboundTx)
    }

    private fun onTxFinalized(completedTx: FFIPointer) {
        val tx = FFICompletedTx(completedTx)
        logger.i("Tx finalized ${tx.getId()}")
        val pendingInboundTx = PendingInboundTx(tx)
        listener.onTxFinalized(pendingInboundTx)
    }

    private fun onTxBroadcast(completedTxPtr: FFIPointer) {
        val tx = FFICompletedTx(completedTxPtr)
        logger.i("Tx broadcast ${tx.getId()}")
        when (tx.getDirection()) {
            Tx.Direction.INBOUND -> {
                val pendingInboundTx = PendingInboundTx(tx)
                listener.onInboundTxBroadcast(pendingInboundTx)
            }

            Tx.Direction.OUTBOUND -> {
                val pendingOutboundTx = PendingOutboundTx(tx)
                listener.onOutboundTxBroadcast(pendingOutboundTx)
            }
        }
    }

    private fun onTxMined(completedTxPtr: FFIPointer) {
        val completed = CompletedTx(completedTxPtr)
        logger.i("Tx mined & confirmed ${completed.id}")
        listener.onTxMined(completed)
    }

    private fun onTxMinedUnconfirmed(completedTxPtr: FFIPointer, confirmationCountBytes: ByteArray) {
        val confirmationCount = BigInteger(1, confirmationCountBytes).toInt()
        val completed = CompletedTx(completedTxPtr)
        logger.i("Tx mined & unconfirmed ${completed.id} $confirmationCount")
        listener.onTxMinedUnconfirmed(completed, confirmationCount)
    }

    private fun onTxFauxConfirmed(completedTxPtr: FFIPointer) {
        val completed = CompletedTx(completedTxPtr)
        logger.i("Tx faux confirmed ${completed.id}")
        listener.onTxMined(completed)
    }

    private fun onTxFauxUnconfirmed(completedTxPtr: FFIPointer, confirmationCountBytes: ByteArray) {
        val confirmationCount = BigInteger(1, confirmationCountBytes).toInt()
        val completed = CompletedTx(completedTxPtr)
        logger.i("Tx faux unconfirmed ${completed.id}")
        listener.onTxMinedUnconfirmed(completed, confirmationCount)
    }

    private fun onDirectSendResult(bytes: ByteArray, pointer: FFIPointer) {
        val txId = BigInteger(1, bytes)
        logger.i("Tx direct send result $txId")
        listener.onDirectSendResult(txId, FFITransactionSendStatus(pointer).getStatus())
    }

    private fun onTxCancelled(completedTx: FFIPointer, rejectionReason: ByteArray) {
        val rejectionReasonInt = BigInteger(1, rejectionReason).toInt()
        val tx = FFICompletedTx(completedTx)
        logger.i("Tx cancelled ${tx.getId()}")
        val cancelledTx = CancelledTx(tx)
        if (tx.getDirection() == Tx.Direction.OUTBOUND) {
            listener.onTxCancelled(cancelledTx, rejectionReasonInt)
        }
    }

    private fun onBaseNodeStatus(baseNodeStatePointer: FFIPointer) {
        val baseNodeState = FFITariBaseNodeState(baseNodeStatePointer)
        logger.i("Base node state updated (height of the longest chain is ${baseNodeState.getHeightOfLongestChain()})")
        listener.onBaseNodeStateChanged(baseNodeState)
    }

    private fun onConnectivityStatus(bytes: ByteArray) {
        val connectivityStatus = BigInteger(1, bytes)
        logger.i("ConnectivityStatus is [$connectivityStatus]")
        listener.onConnectivityStatus(connectivityStatus.toInt())
    }

    private fun onWalletScannedHeight(bytes: ByteArray) {
        val height = BigInteger(1, bytes)
        logger.i("Wallet scanned height is [$height]")
        listener.onWalletScannedHeight(height.toInt())
    }

    private fun onBalanceUpdated(ptr: FFIPointer) {
        logger.i("Balance Updated")
        val balance = FFIBalance(ptr).runWithDestroy { BalanceInfo(it.getAvailable(), it.getIncoming(), it.getOutgoing(), it.getTimeLocked()) }
        listener.onBalanceUpdated(balance)
    }

    private fun onTXOValidationComplete(bytes: ByteArray, statusBytes: ByteArray) {
        val requestId = BigInteger(1, bytes)
        val statusInteger = BigInteger(1, statusBytes).toInt()
        val status = TransactionValidationStatus.entries.firstOrNull { it.value == statusInteger } ?: return
        logger.i("TXO validation [$requestId] complete. Result: $status")
        listener.onTXOValidationComplete(requestId, status)
    }

    private fun onTxValidationComplete(requestIdBytes: ByteArray, statusBytes: ByteArray) {
        val requestId = BigInteger(1, requestIdBytes)
        val statusInteger = BigInteger(1, statusBytes).toInt()
        val status = TransactionValidationStatus.entries.firstOrNull { it.value == statusInteger } ?: return
        logger.i("Tx validation [$requestId] complete. Result: $status")
        listener.onTxValidationComplete(requestId, status)
    }

    private fun onContactLivenessDataUpdated(livenessUpdate: FFIPointer) {
        logger.i("OnContactLivenessDataUpdated")
    }
}
