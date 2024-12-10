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
import com.tari.android.wallet.application.walletManager.WalletCallbacks
import com.tari.android.wallet.data.sharedPrefs.network.TariNetwork
import com.tari.android.wallet.model.BalanceInfo
import com.tari.android.wallet.model.MicroTari
import com.tari.android.wallet.model.PublicKey
import com.tari.android.wallet.model.TariCoinPreview
import com.tari.android.wallet.model.TariUnblindedOutput
import com.tari.android.wallet.model.TariUtxo
import com.tari.android.wallet.model.TariVector
import com.tari.android.wallet.model.TariWalletAddress
import com.tari.android.wallet.model.TxId
import com.tari.android.wallet.model.tx.CancelledTx
import com.tari.android.wallet.model.tx.CompletedTx
import com.tari.android.wallet.model.tx.PendingInboundTx
import com.tari.android.wallet.model.tx.PendingOutboundTx
import com.tari.android.wallet.ui.screen.send.addAmount.FeePerGramOptions
import com.tari.android.wallet.util.Constants
import com.tari.android.wallet.util.DebugConfig
import java.math.BigInteger

/**
 * Wallet wrapper.
 *
 * @author The Tari Development Team
 */
class FFIWallet(
    private val walletCallbacks: WalletCallbacks,
) : FFIBase() {

    private val logger
        get() = Logger.t(FFIWallet::class.simpleName)

    companion object {
        // values for the wallet initialization
        private val LOG_VERBOSITY: Int = if (DebugConfig.isDebug()) 11 else 4
        private const val IS_DNS_SECURE_ON = false
        private val MAX_NUMBER_OF_ROLLING_LOG_FILES = if (DebugConfig.isDebug()) 10 else 2
        private val ROLLING_LOG_FILE_MAX_SIZE_BYTES = (if (DebugConfig.isDebug()) 4 else 10) * 1024 * 1024
    }

    private external fun jniCreate(
        walletContextId: Int,
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
        walletCallbacks: WalletCallbacks,
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
        walletCallbacks: WalletCallbacks,
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
        walletContextId: Int,
        tariNetwork: TariNetwork,
        commsConfig: FFICommsConfig,
        logPath: String,
        passphrase: String,
        seedWords: FFISeedWords?,
        walletCallbacks: WalletCallbacks,
    ) : this(walletCallbacks) {
        val error = FFIError()
        logger.i("Pre jniCreate")

        try {
            jniCreate(
                walletContextId = walletContextId,
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
                walletCallbacks = walletCallbacks,
                WalletCallbacks::onTxReceived.name, "([BJ)V",
                WalletCallbacks::onTxReplyReceived.name, "([BJ)V",
                WalletCallbacks::onTxFinalized.name, "([BJ)V",
                WalletCallbacks::onTxBroadcast.name, "([BJ)V",
                WalletCallbacks::onTxMined.name, "([BJ)V",
                WalletCallbacks::onTxMinedUnconfirmed.name, "([BJ[B)V",
                WalletCallbacks::onTxFauxConfirmed.name, "([BJ)V",
                WalletCallbacks::onTxFauxUnconfirmed.name, "([BJ[B)V",
                WalletCallbacks::onDirectSendResult.name, "([B[BJ)V",
                WalletCallbacks::onTxCancelled.name, "([BJ[B)V",
                WalletCallbacks::onTXOValidationComplete.name, "([B[B[B)V",
                WalletCallbacks::onContactLivenessDataUpdated.name, "([BJ)V",
                WalletCallbacks::onBalanceUpdated.name, "([BJ)V",
                WalletCallbacks::onTxValidationComplete.name, "([B[B[B)V",
                WalletCallbacks::onConnectivityStatus.name, "([B[B)V",
                WalletCallbacks::onWalletScannedHeight.name, "([B[B)V",
                WalletCallbacks::onBaseNodeStatus.name, "([BJ)V",
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

    fun getUtxos(page: Int, pageSize: Int, sorting: Int): TariVector = runWithError { error ->
        TariVector(FFITariVector(jniGetUtxos(page, pageSize, sorting, 0, error)))
    }

    fun getAllUtxos(): TariVector = runWithError { TariVector(FFITariVector(jniGetAllUtxos(it))) }

    fun getWalletAddress(): FFITariWalletAddress = runWithError { FFITariWalletAddress(jniGetWalletAddress(it)) }

    fun getContacts(): FFIContacts = runWithError { FFIContacts(jniGetContacts(it)) }

    fun findContactByWalletAddress(walletAddress: TariWalletAddress): FFIContact? = getContacts().find { ffiContact ->
        ffiContact.getWalletAddress().runWithDestroy { TariWalletAddress(it) } == walletAddress
    }

    fun addUpdateContact(walletAddress: TariWalletAddress, alias: String, isFavorite: Boolean): Boolean = runWithError {
        FFITariWalletAddress(Base58String(walletAddress.fullBase58)).runWithDestroy { ffiTariWalletAddress ->
            FFIContact(alias, ffiTariWalletAddress, isFavorite).runWithDestroy { contactToUpdate ->
                jniAddUpdateContact(contactToUpdate, it)
            }
        }
    }

    fun removeContact(walletAddress: TariWalletAddress): Boolean = runWithError {
        findContactByWalletAddress(walletAddress)?.runWithDestroy { contact ->
            jniRemoveContact(contact, it)
        } == true
    }

    fun getCompletedTxs(): List<CompletedTx> = runWithError { FFICompletedTxs(jniGetCompletedTxs(it)).iterateWithDestroy { tx -> CompletedTx(tx) } }

    fun getCancelledTxs(): List<CancelledTx> = runWithError { FFICompletedTxs(jniGetCancelledTxs(it)).iterateWithDestroy { tx -> CancelledTx(tx) } }

    fun getPendingOutboundTxs(): List<PendingOutboundTx> = runWithError {
        FFIPendingOutboundTxs(jniGetPendingOutboundTxs(it)).iterateWithDestroy { tx -> PendingOutboundTx(tx) }
    }

    fun getPendingInboundTxs(): List<PendingInboundTx> = runWithError {
        FFIPendingInboundTxs(jniGetPendingInboundTxs(it)).iterateWithDestroy { tx -> PendingInboundTx(tx) }
    }

    fun getCompletedTxById(id: TxId): CompletedTx = runWithError {
        FFICompletedTx(jniGetCompletedTxById(id.toString(), it)).runWithDestroy { tx -> CompletedTx(tx) }
    }

    fun getCancelledTxById(id: TxId): CompletedTx = runWithError {
        FFICompletedTx(jniGetCancelledTxById(id.toString(), it)).runWithDestroy { tx -> CompletedTx(tx) }
    }

    fun getPendingOutboundTxById(id: TxId): PendingOutboundTx = runWithError {
        FFIPendingOutboundTx(jniGetPendingOutboundTxById(id.toString(), it)).runWithDestroy { tx -> PendingOutboundTx(tx) }
    }

    fun getPendingInboundTxById(id: TxId): PendingInboundTx = runWithError {
        FFIPendingInboundTx(jniGetPendingInboundTxById(id.toString(), it)).runWithDestroy { tx -> PendingInboundTx(tx) }
    }

    fun cancelPendingTx(id: BigInteger): Boolean = runWithError { jniCancelPendingTx(id.toString(), it) }

    fun estimateTxFee(amount: MicroTari, feePerGram: MicroTari?): MicroTari = runWithError { error ->
        val defaultKernelCount = BigInteger("1")
        val defaultOutputCount = BigInteger("2")
        val gram = feePerGram?.value ?: Constants.Wallet.DEFAULT_FEE_PER_GRAM.value
        MicroTari(
            BigInteger(
                1, jniEstimateTxFee(
                    amount = amount.value.toString(),
                    gramFee = gram.toString(),
                    kernelCount = defaultKernelCount.toString(),
                    outputCount = defaultOutputCount.toString(),
                    libError = error,
                )
            )
        )
    }

    fun sendTx(
        destination: FFITariWalletAddress,
        amount: BigInteger,
        feePerGram: BigInteger,
        message: String,
        isOneSided: Boolean,
        paymentId: String,
    ): TxId {
        if (amount < BigInteger.valueOf(0L)) {
            throw FFIException(message = "Amount is less than 0.")
        }
        if (destination == getWalletAddress()) {
            throw FFIException(message = "Tx source and destination are the same.")
        }
        val txIdBytes = runWithError { jniSendTx(destination, amount.toString(), feePerGram.toString(), message, isOneSided, paymentId, it) }
        return BigInteger(1, txIdBytes)
    }

    fun joinUtxos(utxos: List<TariUtxo>) = runWithError { error ->
        jniJoinUtxos(
            commitments = utxos.map { it.commitment }.toTypedArray(),
            feePerGram = Constants.Wallet.DEFAULT_FEE_PER_GRAM.value.toString(),
            libError = error,
        )
    }

    fun splitUtxos(utxos: List<TariUtxo>, splitCount: Int) = runWithError { error ->
        jniSplitUtxos(
            commitments = utxos.map { it.commitment }.toTypedArray(),
            splitCount = splitCount.toString(),
            feePerGram = Constants.Wallet.DEFAULT_FEE_PER_GRAM.value.toString(),
            libError = error,
        )
    }

    fun joinPreviewUtxos(utxos: List<TariUtxo>): TariCoinPreview = runWithError { error ->
        FFITariCoinPreview(
            jniPreviewJoinUtxos(
                commitments = utxos.map { it.commitment }.toTypedArray(),
                feePerGram = Constants.Wallet.DEFAULT_FEE_PER_GRAM.value.toString(),
                libError = error,
            )
        ).runWithDestroy { TariCoinPreview(it) }
    }

    fun splitPreviewUtxos(utxos: List<TariUtxo>, splitCount: Int): TariCoinPreview = runWithError { error ->
        FFITariCoinPreview(
            jniPreviewSplitUtxos(
                commitments = utxos.map { it.commitment }.toTypedArray(),
                splitCount = splitCount.toString(),
                feePerGram = Constants.Wallet.DEFAULT_FEE_PER_GRAM.value.toString(),
                libError = error,
            )
        ).runWithDestroy { TariCoinPreview(it) }
    }

    fun signMessage(message: String): String = runWithError { jniSignMessage(message, it) }

    fun verifyMessageSignature(contactPublicKey: FFIPublicKey, message: String, signature: String): Boolean =
        runWithError { jniVerifyMessageSignature(contactPublicKey, message, signature, it) }

    fun startTXOValidation(): BigInteger = runWithError { BigInteger(1, jniStartTXOValidation(it)) }

    fun startTxValidation(): BigInteger = runWithError { BigInteger(1, jniStartTxValidation(it)) }

    fun restartTxBroadcast(): BigInteger = runWithError { BigInteger(1, jniRestartTxBroadcast(it)) }

    fun setPowerModeNormal() = runWithError { jniPowerModeNormal(it) }

    fun setPowerModeLow() = runWithError { jniPowerModeLow(it) }

    fun getSeedWords(): List<String> = runWithError { error ->
        FFISeedWords(jniGetSeedWords(error)).runWithDestroy { seedWords -> (0 until seedWords.getLength()).map { seedWords.getAt(it) } }
    }

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

    fun getRequiredConfirmationCount(): Long = runWithError { BigInteger(1, jniGetConfirmations(it)).toLong() }

    fun setRequiredConfirmationCount(number: BigInteger) = runWithError { jniSetConfirmations(number.toString(), it) }

    fun startRecovery(recoveryOutputMessage: String): Boolean =
        runWithError {
            jniStartRecovery(
                walletCallbacks = walletCallbacks,
                callback = walletCallbacks::onWalletRecovery.name,
                callbackSig = "([BI[B[B)V",
                recoveryOutputMessage = recoveryOutputMessage,
                libError = it,
            )
        }

    fun getFeePerGramStats(): FeePerGramOptions = runWithError { error ->
        FFIFeePerGramStats(jniWalletGetFeePerGramStats(3, error)).runWithDestroy { FeePerGramOptions(it) }
    }

    fun getUnbindedOutputs(): List<TariUnblindedOutput> = runWithError { error ->
        FFITariUnblindedOutputs(jniWalletGetUnspentOutputs(error)).iterateWithDestroy { TariUnblindedOutput(it) }
    }

    fun restoreWithUnbindedOutputs(jsons: List<String>, address: TariWalletAddress, message: String) = runWithError { error ->
        for (json in jsons) {
            FFITariUnblindedOutput(json).runWithDestroy { output ->
                FFITariWalletAddress(emojiId = address.fullEmojiId).runWithDestroy { sourceAddress ->
                    jniImportExternalUtxoAsNonRewindable(output, sourceAddress, message, error)
                }
            }
        }
    }

    override fun destroy() {
        jniDestroy()
    }
}
