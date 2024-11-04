package com.tari.android.wallet.service.service

import com.tari.android.wallet.application.walletManager.WalletNotificationManager
import com.tari.android.wallet.ffi.Base58String
import com.tari.android.wallet.ffi.FFIContact
import com.tari.android.wallet.ffi.FFIError
import com.tari.android.wallet.ffi.FFIException
import com.tari.android.wallet.ffi.FFITariWalletAddress
import com.tari.android.wallet.ffi.FFIWallet
import com.tari.android.wallet.ffi.runWithDestroy
import com.tari.android.wallet.model.BalanceInfo
import com.tari.android.wallet.model.CancelledTx
import com.tari.android.wallet.model.CompletedTx
import com.tari.android.wallet.model.MicroTari
import com.tari.android.wallet.model.PendingInboundTx
import com.tari.android.wallet.model.PendingOutboundTx
import com.tari.android.wallet.model.TariCoinPreview
import com.tari.android.wallet.model.TariContact
import com.tari.android.wallet.model.TariUnblindedOutput
import com.tari.android.wallet.model.TariUtxo
import com.tari.android.wallet.model.TariVector
import com.tari.android.wallet.model.TariWalletAddress
import com.tari.android.wallet.model.TxId
import com.tari.android.wallet.model.WalletError
import com.tari.android.wallet.service.TariWalletService
import com.tari.android.wallet.util.Constants
import java.math.BigInteger

class TariWalletServiceStubImpl(
    private val wallet: FFIWallet,
    private val walletNotificationManager: WalletNotificationManager,
) : TariWalletService.Stub() {

    private var _cachedTariContacts: List<TariContact>? = null
    private val cachedTariContacts: List<TariContact>
        @Synchronized get() {
            _cachedTariContacts?.let { return it }
            val contactsFFI = wallet.getContacts()
            val tariContacts = mutableListOf<TariContact>()
            for (i in 0 until contactsFFI.getLength()) {
                val contactFFI = contactsFFI.getAt(i)
                tariContacts.add(TariContact(contactFFI))
                // destroy native objects
                contactFFI.destroy()
            }
            // destroy native collection
            contactsFFI.destroy()
            return tariContacts.sortedWith(compareBy { it.alias }).also { _cachedTariContacts = it }
        }

    override fun getBalanceInfo(error: WalletError): BalanceInfo? = runMapping(error) { wallet.getBalance() }

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

    override fun sendTari(
        tariContact: TariContact,
        amount: MicroTari,
        feePerGram: MicroTari,
        message: String,
        isOneSidePayment: Boolean,
        paymentId: String,
        error: WalletError,
    ): TxId? = runMapping(error) {
        val recipientAddress = FFITariWalletAddress(Base58String(tariContact.walletAddress.fullBase58))
        val txId = wallet.sendTx(recipientAddress, amount.value, feePerGram.value, message, isOneSidePayment, paymentId)

        walletNotificationManager.addOutboundTxNotification(txId, recipientAddress)

        recipientAddress.destroy()
        TxId(txId)
    }

    override fun removeContact(walletAddress: TariWalletAddress, error: WalletError): Boolean = runMapping(error) {
        val contactsFFI = wallet.getContacts()
        for (i in 0 until contactsFFI.getLength()) {
            val contactFFI = contactsFFI.getAt(i)
            val ffiTariWalletAddress = contactFFI.getWalletAddress()
            if (TariWalletAddress(ffiTariWalletAddress) == walletAddress) {
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

    override fun updateContact(walletAddress: TariWalletAddress, alias: String, isFavorite: Boolean, error: WalletError): Boolean =
        runMapping(error) {
            val ffiTariWalletAddress = FFITariWalletAddress(Base58String(walletAddress.fullBase58))
            val contact = FFIContact(alias, ffiTariWalletAddress, isFavorite)
            wallet.addUpdateContact(contact).also {
                ffiTariWalletAddress.destroy()
                contact.destroy()
                _cachedTariContacts = null
            }
        } ?: false

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
        wallet.joinUtxos(utxos.map { it.commitment }.toTypedArray(), Constants.Wallet.DEFAULT_FEE_PER_GRAM.value, ffiError)
        walletError.code = ffiError.code
    } ?: Unit

    override fun splitUtxos(utxos: List<TariUtxo>, splitCount: Int, walletError: WalletError) = runMapping(walletError) {
        val ffiError = FFIError()
        wallet.splitUtxos(utxos.map { it.commitment }.toTypedArray(), splitCount, Constants.Wallet.DEFAULT_FEE_PER_GRAM.value, ffiError)
        walletError.code = ffiError.code
    } ?: Unit

    override fun previewJoinUtxos(utxos: List<TariUtxo>, walletError: WalletError): TariCoinPreview? = runMapping(walletError) {
        val ffiError = FFIError()
        val result = wallet.joinPreviewUtxos(utxos.map { it.commitment }.toTypedArray(), Constants.Wallet.DEFAULT_FEE_PER_GRAM.value, ffiError)
        walletError.code = ffiError.code
        result
    }

    override fun previewSplitUtxos(utxos: List<TariUtxo>, splitCount: Int, walletError: WalletError): TariCoinPreview? = runMapping(walletError) {
        val ffiError = FFIError()
        val result = wallet.splitPreviewUtxos(
            utxos.map { it.commitment }.toTypedArray(), splitCount, Constants.Wallet.DEFAULT_FEE_PER_GRAM.value, ffiError
        )
        walletError.code = ffiError.code
        result
    }

    override fun getUnbindedOutputs(error: WalletError): List<TariUnblindedOutput> {
        return runMapping(error) {
            val ffiError = FFIError()
            val outputs = wallet.getUnbindedOutputs(ffiError)
            error.code = ffiError.code
            outputs
        }.orEmpty()
    }

    override fun restoreWithUnbindedOutputs(jsons: List<String>, address: TariWalletAddress, message: String, error: WalletError) {
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