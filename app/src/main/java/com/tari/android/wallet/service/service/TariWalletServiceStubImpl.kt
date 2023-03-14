package com.tari.android.wallet.service.service

import com.orhanobut.logger.Logger
import com.tari.android.wallet.data.sharedPrefs.baseNode.BaseNodeSharedRepository
import com.tari.android.wallet.event.EventBus
import com.tari.android.wallet.ffi.*
import com.tari.android.wallet.model.*
import com.tari.android.wallet.service.TariWalletService
import com.tari.android.wallet.service.TariWalletServiceListener
import com.tari.android.wallet.service.baseNode.BaseNodeSyncState
import com.tari.android.wallet.util.Constants
import java.math.BigInteger
import java.util.Locale

class TariWalletServiceStubImpl(
    private val wallet: FFIWallet,
    private val baseNodeSharedPrefsRepository: BaseNodeSharedRepository,
    private val walletServiceListener: FFIWalletListenerImpl
) : TariWalletService.Stub() {

    private val logger
        get() = Logger.t(WalletService::class.simpleName)

    private var _cachedTariContacts: List<TariContact>? = null
    private val cachedTariContacts: List<TariContact>
        @Synchronized get() {
            _cachedTariContacts?.let { return it }
            val contactsFFI = wallet.getContacts()
            val tariContacts = mutableListOf<TariContact>()
            for (i in 0 until contactsFFI.getLength()) {
                val contactFFI = contactsFFI.getAt(i)
                val ffiTariWalletAddress = contactFFI.getWalletAddress()
                tariContacts.add(TariContact(walletAddressFromFFI(ffiTariWalletAddress), contactFFI.getAlias(), contactFFI.getIsFavorite()))
                // destroy native objects
                ffiTariWalletAddress.destroy()
                contactFFI.destroy()
            }
            // destroy native collection
            contactsFFI.destroy()
            return tariContacts.sortedWith(compareBy { it.alias }).also { _cachedTariContacts = it }
        }

    override fun registerListener(listener: TariWalletServiceListener): Boolean {
        walletServiceListener.listeners.add(listener)
        listener.asBinder().linkToDeath({ walletServiceListener.listeners.remove(listener) }, 0)
        return true
    }

    override fun unregisterListener(listener: TariWalletServiceListener): Boolean = walletServiceListener.listeners.remove(listener)

    override fun getWalletAddressHexString(error: WalletError): String? = runMapping(error) { wallet.getWalletAddress().toString() }

    override fun getBalanceInfo(error: WalletError): BalanceInfo? = runMapping(error) { wallet.getBalance() }

    override fun estimateTxFee(amount: MicroTari, error: WalletError, feePerGram: MicroTari?): MicroTari? = runMapping(error) {
        val defaultKernelCount = BigInteger("1")
        val defaultOutputCount = BigInteger("2")
        val gram = feePerGram?.value ?: Constants.Wallet.defaultFeePerGram.value
        MicroTari(wallet.estimateTxFee(amount.value, gram, defaultKernelCount, defaultOutputCount))
    }

    /**
     * Get all contacts.
     */
    override fun getContacts(error: WalletError): List<TariContact>? = runMapping(error) { cachedTariContacts }

    /**
     * Get all completed transactions.
     * Client-facing function.
     */
    override fun getCompletedTxs(error: WalletError): List<CompletedTx>? = runMapping(error) {
        wallet.getCompletedTxs().runWithDestroy { txs -> (0 until txs.getLength()).map { CompletedTx(txs.getAt(it)) } }
    }

    /**
     * Get all cancelledTxs transactions.
     * Client-facing function.
     */
    override fun getCancelledTxs(error: WalletError): List<CancelledTx>? = runMapping(error) {
        wallet.getCancelledTxs().runWithDestroy { txs -> (0 until txs.getLength()).map { CancelledTx(txs.getAt(it)) } }
    }

    /**
     * Get completed transaction by id.
     * Client-facing function.
     */
    override fun getCancelledTxById(id: TxId, error: WalletError): CancelledTx? =
        runMapping(error) { CancelledTx(wallet.getCancelledTxById(id.value)) }

    /**
     * Get completed transaction by id.
     * Client-facing function.
     */
    override fun getCompletedTxById(id: TxId, error: WalletError): CompletedTx? =
        runMapping(error) { CompletedTx(wallet.getCompletedTxById(id.value)) }

    /**
     * Get all pending inbound transactions.
     * Client-facing function.
     */
    override fun getPendingInboundTxs(error: WalletError): List<PendingInboundTx>? = runMapping(error) {
        wallet.getPendingInboundTxs().runWithDestroy { txs -> (0 until txs.getLength()).map { PendingInboundTx(txs.getAt(it)) } }
    }

    /**
     * Get pending inbound transaction by id.
     * Client-facing function.
     */
    override fun getPendingInboundTxById(id: TxId, error: WalletError): PendingInboundTx? = runMapping(error) {
        PendingInboundTx(wallet.getPendingInboundTxById(id.value))
    }

    /**
     * Get all pending outbound transactions.
     * Client-facing function.
     */
    override fun getPendingOutboundTxs(error: WalletError): List<PendingOutboundTx>? = runMapping(error) {
        wallet.getPendingOutboundTxs().runWithDestroy { txs -> (0 until txs.getLength()).map { PendingOutboundTx(txs.getAt(it)) } }
    }

    /**
     * Get pending outbound transaction by id.
     * Client-facing function.
     */
    override fun getPendingOutboundTxById(id: TxId, error: WalletError): PendingOutboundTx? = runMapping(error) {
        PendingOutboundTx(wallet.getPendingOutboundTxById(id.value))
    }

    override fun cancelPendingTx(id: TxId, error: WalletError): Boolean = runMapping(error) { wallet.cancelPendingTx(id.value) } ?: false

    override fun addBaseNodePeer(baseNodePublicKey: String, baseNodeAddress: String, error: WalletError): Boolean = runMapping(error) {
        val result = FFIPublicKey(HexString(baseNodePublicKey)).runWithDestroy { wallet.addBaseNodePeer(it, baseNodeAddress) }
        if (result) {
            walletServiceListener.baseNodeValidationStatusMap.clear()
            EventBus.baseNodeSyncState.post(BaseNodeSyncState.NotStarted)
        }
        result
    } ?: false

    override fun startBaseNodeSync(error: WalletError): Boolean = runMapping(error, {
        logger.e(it, "Base node sync failed")
        baseNodeSharedPrefsRepository.baseNodeLastSyncResult = false
        walletServiceListener.baseNodeValidationStatusMap.clear()
        EventBus.baseNodeSyncState.post(BaseNodeSyncState.Failed)
    }) {
        walletServiceListener.baseNodeValidationStatusMap.clear()
        walletServiceListener.baseNodeValidationStatusMap[BaseNodeValidationType.TXO] = Pair(wallet.startTXOValidation(), null)
        walletServiceListener.baseNodeValidationStatusMap[BaseNodeValidationType.TX] = Pair(wallet.startTxValidation(), null)
        baseNodeSharedPrefsRepository.baseNodeLastSyncResult = null
        true
    } ?: false

    override fun sendTari(
        tariContact: TariContact, amount: MicroTari, feePerGram: MicroTari, message: String, isOneSidePayment: Boolean, error: WalletError
    ): TxId? = runMapping(error) {
        val recipientAddressHex = tariContact.walletAddress.hexString
        val recipientAddress = FFITariWalletAddress(HexString(recipientAddressHex)).runWithDestroy {
            wallet.sendTx(it, amount.value, feePerGram.value, message, isOneSidePayment)
        }
        walletServiceListener.outboundTxIdsToBePushNotified.add(Pair(recipientAddress, recipientAddressHex.lowercase(Locale.ENGLISH)))
        TxId(recipientAddress)
    }

    override fun removeContact(tariContact: TariContact, error: WalletError): Boolean = runMapping(error) {
        val contactsFFI = wallet.getContacts()
        for (i in 0 until contactsFFI.getLength()) {
            val contactFFI = contactsFFI.getAt(i)
            val ffiTariWalletAddress = contactFFI.getWalletAddress()
            if (ffiTariWalletAddress.toString() == tariContact.walletAddress.hexString) {
                return@runMapping wallet.removeContact(contactFFI).also {
                    ffiTariWalletAddress.destroy()
                    contactFFI.destroy()
                    contactsFFI.destroy()
                    _cachedTariContacts = null
                }
            }
            ffiTariWalletAddress.destroy()
            contactFFI.destroy()
        }
        contactsFFI.destroy()
        false
    } ?: false

    override fun updateContactAlias(walletAddress: TariWalletAddress, alias: String, error: WalletError): Boolean = runMapping(error) {
        val ffiTariWalletAddress = FFITariWalletAddress(HexString(walletAddress.hexString))
        val contact = FFIContact(alias, ffiTariWalletAddress)
        wallet.addUpdateContact(contact).also {
            ffiTariWalletAddress.destroy()
            contact.destroy()
            _cachedTariContacts = null
        }
    } ?: false

    override fun getWalletAddressFromEmojiId(emojiId: String?): TariWalletAddress? =
        runCatching { FFITariWalletAddress(emojiId.orEmpty()).runWithDestroy { walletAddressFromFFI(it) } }.getOrNull()

    override fun getWalletAddressFromHexString(walletAddressHex: String?): TariWalletAddress? =
        runCatching { FFITariWalletAddress(HexString(walletAddressHex ?: "")).runWithDestroy { walletAddressFromFFI(it) } }.getOrNull()

    override fun setKeyValue(key: String, value: String, error: WalletError): Boolean = runMapping(error) { wallet.setKeyValue(key, value) } ?: false

    override fun getKeyValue(key: String, error: WalletError): String? = runMapping(error) { wallet.getKeyValue(key) }

    override fun removeKeyValue(key: String, error: WalletError): Boolean = runMapping(error) { wallet.removeKeyValue(key) } ?: false

    override fun getRequiredConfirmationCount(error: WalletError): Long = runMapping(error) { wallet.getRequiredConfirmationCount().toLong() } ?: 0

    override fun setRequiredConfirmationCount(number: Long, error: WalletError) {
        runMapping(error) { wallet.setRequiredConfirmationCount(BigInteger.valueOf(number)) }
    }

    override fun getSeedWords(error: WalletError): List<String>? = runMapping(error) {
        wallet.getSeedWords().runWithDestroy { seedWords -> (0 until seedWords.getLength()).map { seedWords.getAt(it) } }
    }

    override fun getUtxos(page: Int, pageSize: Int, sorting: Int, error: WalletError): TariVector? =
        runMapping(error) { wallet.getUtxos(page, pageSize, sorting) }

    override fun getAllUtxos(error: WalletError): TariVector? = runMapping(error) { wallet.getAllUtxos() }

    override fun joinUtxos(utxos: List<TariUtxo>, walletError: WalletError) = runMapping(walletError) {
        val ffiError = FFIError()
        wallet.joinUtxos(utxos.map { it.commitment }.toTypedArray(), Constants.Wallet.defaultFeePerGram.value, ffiError)
        walletError.code = ffiError.code
    } ?: Unit

    override fun splitUtxos(utxos: List<TariUtxo>, splitCount: Int, walletError: WalletError) = runMapping(walletError) {
        val ffiError = FFIError()
        wallet.splitUtxos(utxos.map { it.commitment }.toTypedArray(), splitCount, Constants.Wallet.defaultFeePerGram.value, ffiError)
        walletError.code = ffiError.code
    } ?: Unit

    override fun previewJoinUtxos(utxos: List<TariUtxo>, walletError: WalletError): TariCoinPreview? = runMapping(walletError) {
        val ffiError = FFIError()
        val result = wallet.joinPreviewUtxos(utxos.map { it.commitment }.toTypedArray(), Constants.Wallet.defaultFeePerGram.value, ffiError)
        walletError.code = ffiError.code
        result
    }

    override fun previewSplitUtxos(utxos: List<TariUtxo>, splitCount: Int, walletError: WalletError): TariCoinPreview? = runMapping(walletError) {
        val ffiError = FFIError()
        val result = wallet.splitPreviewUtxos(
            utxos.map { it.commitment }.toTypedArray(), splitCount, Constants.Wallet.defaultFeePerGram.value, ffiError
        )
        walletError.code = ffiError.code
        result
    }

    override fun getUnbindedOutputs(error: WalletError): MutableList<TariUnblindedOutput> {
        return runMapping(error) {
            val ffiError = FFIError()
            val outputs = wallet.getUnbindedOutputs(ffiError)
            error.code = ffiError.code
            outputs
        }.orEmpty().toMutableList()
    }

    override fun restoreWithUnbindedOutputs(jsons: MutableList<String>, address: TariWalletAddress, message: String, error: WalletError) {
        runMapping(error) {
            val ffiError = FFIError()
            wallet.restoreWithUnbindedOutputs(jsons, address, message, ffiError)
            error.code = ffiError.code
        }
    }

    private fun mapThrowableIntoError(walletError: WalletError, throwable: Throwable) {
        if (throwable is FFIException) {
            if (throwable.error != null) {
                walletError.code = throwable.error.code
                return
            }
        }
        walletError.code = WalletError.UnknownError.code
    }

    private fun walletAddressFromFFI(ffiTariWalletAddress: FFITariWalletAddress): TariWalletAddress =
        TariWalletAddress(ffiTariWalletAddress.toString(), ffiTariWalletAddress.getEmojiId())

    private fun <T> runMapping(walletError: WalletError, onError: (Throwable) -> (Unit) = {}, action: () -> T?): T? {
        return try {
            action()
        } catch (throwable: Throwable) {
            onError(throwable)
            mapThrowableIntoError(walletError, throwable)
            null
        }
    }
}