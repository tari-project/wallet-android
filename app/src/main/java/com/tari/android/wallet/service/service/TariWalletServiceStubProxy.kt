package com.tari.android.wallet.service.service

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

class TariWalletServiceStubProxy : TariWalletService.Stub() {

    private var _stub: TariWalletServiceStubImpl? = null

    var stub: TariWalletServiceStubImpl?
        get() = _stub
        set(newStub) {
            _stub = newStub
        }

    override fun getContacts(error: WalletError): List<TariContact>? = stub?.getContacts(error)

    override fun getCompletedTxs(error: WalletError): List<CompletedTx>? = stub?.getCompletedTxs(error)

    override fun getCompletedTxById(id: TxId, error: WalletError): CompletedTx? = stub?.getCompletedTxById(id, error)

    override fun getPendingInboundTxs(error: WalletError): List<PendingInboundTx>? = stub?.getPendingInboundTxs(error)

    override fun getPendingInboundTxById(id: TxId, error: WalletError): PendingInboundTx? = stub?.getPendingInboundTxById(id, error)

    override fun getPendingOutboundTxs(error: WalletError): List<PendingOutboundTx>? = stub?.getPendingOutboundTxs(error)

    override fun getPendingOutboundTxById(id: TxId, error: WalletError): PendingOutboundTx? = stub?.getPendingOutboundTxById(id, error)

    override fun getCancelledTxs(error: WalletError): List<CancelledTx>? = stub?.getCancelledTxs(error)

    override fun getCancelledTxById(id: TxId, error: WalletError): CancelledTx? = stub?.getCancelledTxById(id, error)

    override fun cancelPendingTx(id: TxId, error: WalletError): Boolean = stub?.cancelPendingTx(id, error) ?: false

    override fun sendTari(
        contact: TariContact,
        amount: MicroTari,
        feePerGram: MicroTari,
        message: String,
        isOneSidePayment: Boolean,
        paymentId: String,
        error: WalletError,
    ): TxId? = stub?.sendTari(contact, amount, feePerGram, message, isOneSidePayment, paymentId, error)

    override fun updateContact(contactPublicKey: TariWalletAddress, alias: String, isFavorite: Boolean, error: WalletError): Boolean =
        stub?.updateContact(contactPublicKey, alias, isFavorite, error) ?: false

    override fun removeContact(contactPublicKey: TariWalletAddress, error: WalletError): Boolean =
        stub?.removeContact(contactPublicKey, error) ?: false

    override fun getRequiredConfirmationCount(error: WalletError): Long = stub?.getRequiredConfirmationCount(error) ?: 3

    override fun setRequiredConfirmationCount(number: Long, error: WalletError) = stub?.setRequiredConfirmationCount(number, error) ?: Unit

    override fun getSeedWords(error: WalletError): List<String>? = stub?.getSeedWords(error)

    override fun getUtxos(page: Int, pageSize: Int, sorting: Int, error: WalletError): TariVector? = stub?.getUtxos(page, pageSize, sorting, error)

    override fun getAllUtxos(error: WalletError): TariVector? = stub?.getAllUtxos(error)

    override fun previewJoinUtxos(utxos: List<TariUtxo>, error: WalletError): TariCoinPreview? = stub?.previewJoinUtxos(utxos, error)

    override fun previewSplitUtxos(utxos: List<TariUtxo>, splitCount: Int, error: WalletError): TariCoinPreview? =
        stub?.previewSplitUtxos(utxos, splitCount, error)

    override fun joinUtxos(utxos: List<TariUtxo>, error: WalletError) = stub?.joinUtxos(utxos, error) ?: Unit

    override fun splitUtxos(utxos: List<TariUtxo>, splitCount: Int, error: WalletError) = stub?.splitUtxos(utxos, splitCount, error) ?: Unit

    override fun getUnbindedOutputs(error: WalletError): List<TariUnblindedOutput> = stub?.getUnbindedOutputs(error) ?: listOf()

    override fun restoreWithUnbindedOutputs(jsons: List<String>, address: TariWalletAddress, message: String, error: WalletError) =
        stub?.restoreWithUnbindedOutputs(jsons, address, message, error) ?: Unit
}