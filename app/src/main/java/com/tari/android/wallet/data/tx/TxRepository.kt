package com.tari.android.wallet.data.tx

import com.orhanobut.logger.Logger
import com.tari.android.wallet.application.walletManager.WalletManager
import com.tari.android.wallet.application.walletManager.WalletManager.WalletEvent
import com.tari.android.wallet.application.walletManager.doOnWalletRunning
import com.tari.android.wallet.data.contacts.ContactsRepository
import com.tari.android.wallet.di.ApplicationScope
import com.tari.android.wallet.model.TxId
import com.tari.android.wallet.model.tx.Tx
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.zip
import kotlinx.coroutines.launch
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class TxRepository @Inject constructor(
    private val contactsRepository: ContactsRepository,
    private val walletManager: WalletManager,
    @ApplicationScope private val applicationScope: CoroutineScope,
) {
    private val logger
        get() = Logger.t(TxRepository::class.java.simpleName)

    private val _txs = MutableStateFlow(TxListData(confirmationCount = DEFAULT_CONFIRMATION_COUNT))
    val txs = _txs.asStateFlow()
    val pendingTxs: Flow<List<TxDto>> = txs.map { txs ->
        (txs.pendingInboundTxs + txs.pendingOutboundTxs + txs.minedUnconfirmedTxs)
            .sortedWith(compareByDescending(Tx::timestamp).thenByDescending { it.id })
            .map { it.toDto() }
    }
    val nonPendingTxs: Flow<List<TxDto>> = txs.map { txs ->
        (txs.cancelledTxs + txs.nonMinedUnconfirmedCompletedTxs)
            .sortedWith(compareByDescending(Tx::timestamp).thenByDescending { it.id })
            .map { it.toDto() }
    }
    val allTxs: Flow<List<TxDto>> = pendingTxs.zip(nonPendingTxs) { pending, nonPending -> pending + nonPending }

    init {
        applicationScope.launch(Dispatchers.IO) {
            contactsRepository.contactList.collect {
                // need to refresh tx list to update contact info
                refreshTxList()
            }
        }

        applicationScope.launch(Dispatchers.IO) {
            walletManager.walletEvent.collect { event ->
                when (event) {
                    is WalletEvent.Tx.TxReceived,
                    is WalletEvent.Tx.TxReplyReceived,
                    is WalletEvent.Tx.TxFinalized,
                    is WalletEvent.Tx.InboundTxBroadcast,
                    is WalletEvent.Tx.OutboundTxBroadcast,
                    is WalletEvent.Tx.TxMinedUnconfirmed,
                    is WalletEvent.Tx.TxMined,
                    is WalletEvent.Tx.TxFauxMinedUnconfirmed,
                    is WalletEvent.Tx.TxFauxConfirmed,
                    is WalletEvent.Tx.TxCancelled,
                    is WalletEvent.TxSend.TxSendSuccessful,
                    is WalletEvent.UtxosSplit -> refreshTxList()

                    else -> Unit
                }
            }
        }

        refreshTxList()
    }

    fun findTxById(txId: TxId): Tx? {
        return txs.value.allTxs.firstOrNull { it.id == txId }
    }

    /**
     * Re-download the transaction list from FFI
     */
    private fun refreshTxList() {
        applicationScope.launch(Dispatchers.IO) {
            walletManager.doOnWalletRunning { wallet ->
                _txs.value = TxListData(
                    cancelledTxs = wallet.getCancelledTxs(),
                    completedTxs = wallet.getCompletedTxs(),
                    pendingInboundTxs = wallet.getPendingInboundTxs(),
                    pendingOutboundTxs = wallet.getPendingOutboundTxs(),
                    confirmationCount = wallet.getRequiredConfirmationCount(),
                )
                logger.i(
                    "Refreshed tx list: ${_txs.value.completedTxs.size} completed, " +
                            "${_txs.value.pendingInboundTxs.size} pending inbound, " +
                            "${_txs.value.pendingOutboundTxs.size} pending outbound, " +
                            "${_txs.value.cancelledTxs.size} cancelled (required confirmation count: " +
                            "${_txs.value.confirmationCount})"
                )
            }
        }
    }

    private fun Tx.toDto() = TxDto(
        tx = this,
        contact = contactsRepository.getContactForTx(this),
        requiredConfirmationCount = _txs.value.confirmationCount,
    )

    companion object {
        private const val DEFAULT_CONFIRMATION_COUNT = 3L
    }
}
