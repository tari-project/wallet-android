package com.tari.android.wallet.service.service

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
import com.tari.android.wallet.service.TariWalletServiceListener

class TariWalletServiceStubProxy : TariWalletService.Stub() {

    private var _stub: TariWalletServiceStubImpl? = null
    var stub: TariWalletServiceStubImpl
        get() = _stub!!
        set(newStub) {
            _stub = newStub
        }

    override fun registerListener(listener: TariWalletServiceListener): Boolean = stub.registerListener(listener)

    override fun unregisterListener(listener: TariWalletServiceListener): Boolean = stub.unregisterListener(listener)

    override fun getWalletAddressHexString(error: WalletError): String? = stub.getWalletAddressHexString(error)

    override fun getBalanceInfo(error: WalletError): BalanceInfo? = stub.getBalanceInfo(error)

    override fun estimateTxFee(amount: MicroTari, error: WalletError, feePerGram: MicroTari?): MicroTari? =
        stub.estimateTxFee(amount, error, feePerGram)

    override fun getContacts(error: WalletError): List<TariContact>? = stub.getContacts(error)

    override fun getCompletedTxs(error: WalletError): List<CompletedTx>? = stub.getCompletedTxs(error)

    override fun getCompletedTxById(id: TxId, error: WalletError): CompletedTx? = stub.getCompletedTxById(id, error)

    override fun getPendingInboundTxs(error: WalletError): List<PendingInboundTx>? = stub.getPendingInboundTxs(error)

    override fun getPendingInboundTxById(id: TxId, error: WalletError): PendingInboundTx? = stub.getPendingInboundTxById(id, error)

    override fun getPendingOutboundTxs(error: WalletError): List<PendingOutboundTx>? = stub.getPendingOutboundTxs(error)

    override fun getPendingOutboundTxById(id: TxId, error: WalletError): PendingOutboundTx? = stub.getPendingOutboundTxById(id, error)

    override fun getCancelledTxs(error: WalletError): List<CancelledTx>? = stub.getCancelledTxs(error)

    override fun getCancelledTxById(id: TxId, error: WalletError): CancelledTx? = stub.getCancelledTxById(id, error)

    override fun cancelPendingTx(id: TxId, error: WalletError): Boolean = stub.cancelPendingTx(id, error)

    override fun addBaseNodePeer(baseNodePublicKey: String, baseNodeAddress: String, error: WalletError): Boolean =
        stub.addBaseNodePeer(baseNodePublicKey, baseNodeAddress, error)

    override fun startBaseNodeSync(error: WalletError): Boolean = stub.startBaseNodeSync(error)

    override fun sendTari(
        contact: TariContact,
        amount: MicroTari,
        feePerGram: MicroTari,
        message: String,
        isOneSidePayment: Boolean,
        error: WalletError
    ): TxId? = stub.sendTari(contact, amount, feePerGram, message, isOneSidePayment, error)

    override fun updateContact(contactPublicKey: TariWalletAddress, alias: String, isFavorite: Boolean, error: WalletError): Boolean =
        stub.updateContact(contactPublicKey, alias, isFavorite, error)

    override fun removeContact(tariContact: TariContact, error: WalletError): Boolean = stub.removeContact(tariContact, error)

    override fun getWalletAddressFromHexString(hex: String): TariWalletAddress? = stub.getWalletAddressFromHexString(hex)

    override fun getWalletAddressFromEmojiId(emojiId: String): TariWalletAddress? = stub.getWalletAddressFromEmojiId(emojiId)

    override fun setKeyValue(key: String, value: String, error: WalletError): Boolean = stub.setKeyValue(key, value, error)

    override fun getKeyValue(key: String, error: WalletError): String? = stub.getKeyValue(key, error)

    override fun removeKeyValue(key: String, error: WalletError): Boolean = stub.removeKeyValue(key, error)

    override fun getRequiredConfirmationCount(error: WalletError): Long = stub.getRequiredConfirmationCount(error)

    override fun setRequiredConfirmationCount(number: Long, error: WalletError) = stub.setRequiredConfirmationCount(number, error)

    override fun getSeedWords(error: WalletError): List<String>? = stub.getSeedWords(error)

    override fun getUtxos(page: Int, pageSize: Int, sorting: Int, error: WalletError): TariVector? = stub.getUtxos(page, pageSize, sorting, error)

    override fun getAllUtxos(error: WalletError): TariVector? = stub.getAllUtxos(error)

    override fun previewJoinUtxos(utxos: List<TariUtxo>, error: WalletError): TariCoinPreview? = stub.previewJoinUtxos(utxos, error)

    override fun previewSplitUtxos(utxos: List<TariUtxo>, splitCount: Int, error: WalletError): TariCoinPreview? =
        stub.previewSplitUtxos(utxos, splitCount, error)

    override fun joinUtxos(utxos: List<TariUtxo>, error: WalletError) = stub.joinUtxos(utxos, error)

    override fun splitUtxos(utxos: List<TariUtxo>, splitCount: Int, error: WalletError) = stub.splitUtxos(utxos, splitCount, error)

    override fun getUnbindedOutputs(error: WalletError): MutableList<TariUnblindedOutput> = stub.getUnbindedOutputs(error)

    override fun restoreWithUnbindedOutputs(jsons: MutableList<String>, address: TariWalletAddress, message: String, error: WalletError) =
        stub.restoreWithUnbindedOutputs(jsons, address, message, error)
}