package com.tari.android.wallet.data.tx

import com.orhanobut.logger.Logger
import com.tari.android.wallet.application.walletManager.WalletManager
import com.tari.android.wallet.application.walletManager.WalletManager.WalletEvent
import com.tari.android.wallet.application.walletManager.doOnWalletRunning
import com.tari.android.wallet.di.ApplicationScope
import com.tari.android.wallet.extension.replaceItem
import com.tari.android.wallet.extension.replaceOrAddItem
import com.tari.android.wallet.extension.withItem
import com.tari.android.wallet.extension.withoutItem
import com.tari.android.wallet.model.CancelledTx
import com.tari.android.wallet.model.CompletedTx
import com.tari.android.wallet.model.PendingInboundTx
import com.tari.android.wallet.model.PendingOutboundTx
import com.tari.android.wallet.model.Tx
import com.tari.android.wallet.model.TxId
import com.tari.android.wallet.model.TxStatus
import com.tari.android.wallet.ui.fragment.contactBook.data.ContactsRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.update
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

    private val _txs = MutableStateFlow(
        TxListData(
            confirmationCount = DEFAULT_CONFIRMATION_COUNT,
        )
    )
    val txs = _txs.asStateFlow()
    val pendingTxs = txs.map { txs ->
        (txs.pendingInboundTxs + txs.pendingOutboundTxs + txs.minedUnconfirmedTxs)
            .sortedWith(compareByDescending(Tx::timestamp).thenByDescending { it.id })
            .map { it.toDto() }
    }
    val nonPendingTxs = txs.map { txs ->
        (txs.cancelledTxs + txs.nonMinedUnconfirmedCompletedTxs)
            .sortedWith(compareByDescending(Tx::timestamp).thenByDescending { it.id })
            .map { it.toDto() }
    }
    val allTxs = pendingTxs.zip(nonPendingTxs) { pending, nonPending -> pending + nonPending }

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
                    is WalletEvent.Tx.TxReceived -> onTxReceived(event.tx)
                    is WalletEvent.Tx.TxReplyReceived -> onTxReplyReceived(event.tx)
                    is WalletEvent.Tx.TxFinalized -> onTxFinalized(event.tx)
                    is WalletEvent.Tx.InboundTxBroadcast -> onInboundTxBroadcast(event.tx)
                    is WalletEvent.Tx.OutboundTxBroadcast -> onOutboundTxBroadcast(event.tx)
                    is WalletEvent.Tx.TxMinedUnconfirmed -> onTxMinedUnconfirmed(event.tx)
                    is WalletEvent.Tx.TxMined -> onTxMined(event.tx)
                    is WalletEvent.Tx.TxFauxMinedUnconfirmed -> onTxFauxMinedUnconfirmed(event.tx)
                    is WalletEvent.Tx.TxFauxConfirmed -> onFauxTxMined(event.tx)
                    is WalletEvent.Tx.TxCancelled -> onTxCancelled(event.tx)

                    is WalletEvent.UtxosSplit -> refreshTxList()

                    is WalletEvent.TxSend.TxSendSuccessful -> onTxSendSuccessful(event.txId)

                    else -> Unit
                }
            }
        }

        refreshTxList()
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

    private fun onTxReceived(tx: PendingInboundTx) {
        _txs.update { it.copy(pendingInboundTxs = it.pendingInboundTxs.withItem(tx)) }
    }

    private fun onTxReplyReceived(tx: PendingOutboundTx) {
        _txs.update { txs ->
            txs.copy(pendingOutboundTxs = txs.pendingOutboundTxs.replaceItem({ it.id == tx.id }, { it.copy(status = tx.status) }))
        }
    }

    private fun onTxFinalized(tx: PendingInboundTx) {
        _txs.update { txs ->
            txs.copy(pendingInboundTxs = txs.pendingInboundTxs.replaceItem({ it.id == tx.id }, { it.copy(status = tx.status) }))
        }
    }

    private fun onInboundTxBroadcast(tx: PendingInboundTx) {
        _txs.update { txs ->
            txs.copy(pendingInboundTxs = txs.pendingInboundTxs.replaceItem({ it.id == tx.id }, { it.copy(status = TxStatus.BROADCAST) }))
        }
    }

    private fun onOutboundTxBroadcast(tx: PendingOutboundTx) {
        _txs.update { txs ->
            txs.copy(pendingOutboundTxs = txs.pendingOutboundTxs.replaceItem({ it.id == tx.id }, { it.copy(status = TxStatus.BROADCAST) }))
        }
    }

    private fun onTxMinedUnconfirmed(tx: CompletedTx) {
        _txs.update { txs ->
            txs.copy(
                completedTxs = txs.completedTxs.replaceOrAddItem({ it.id == tx.id }, tx),
                pendingInboundTxs = if (tx.isInbound) txs.pendingInboundTxs.withoutItem { it.id == tx.id } else txs.pendingInboundTxs,
                pendingOutboundTxs = if (tx.isOutbound) txs.pendingOutboundTxs.withoutItem { it.id == tx.id } else txs.pendingOutboundTxs,
            )
        }
    }

    private fun onTxMined(tx: CompletedTx) {
        _txs.update { txs ->
            txs.copy(
                completedTxs = txs.completedTxs.replaceOrAddItem({ it.id == tx.id }, tx),
                pendingInboundTxs = txs.pendingInboundTxs.withoutItem { it.id == tx.id },
                pendingOutboundTxs = txs.pendingOutboundTxs.withoutItem { it.id == tx.id },
            )
        }
    }

    private fun onTxFauxMinedUnconfirmed(tx: CompletedTx) {
        _txs.update { txs ->
            txs.copy(
                completedTxs = txs.completedTxs.replaceOrAddItem({ it.id == tx.id }, tx),
                pendingInboundTxs = if (tx.isInbound) txs.pendingInboundTxs.withoutItem { it.id == tx.id } else txs.pendingInboundTxs,
                pendingOutboundTxs = if (tx.isOutbound) txs.pendingOutboundTxs.withoutItem { it.id == tx.id } else txs.pendingOutboundTxs,
            )
        }
    }

    private fun onFauxTxMined(tx: CompletedTx) {
        _txs.update { txs ->
            txs.copy(
                completedTxs = txs.completedTxs.replaceOrAddItem({ it.id == tx.id }, tx),
                pendingInboundTxs = txs.pendingInboundTxs.withoutItem { it.id == tx.id },
                pendingOutboundTxs = txs.pendingOutboundTxs.withoutItem { it.id == tx.id },
            )
        }
    }

    private fun onTxCancelled(tx: CancelledTx) {
        _txs.update { txs ->
            txs.copy(
                cancelledTxs = txs.cancelledTxs.withItem(tx),
                pendingInboundTxs = if (tx.isInbound) txs.pendingInboundTxs.withoutItem { it.id == tx.id } else txs.pendingInboundTxs,
                pendingOutboundTxs = if (tx.isOutbound) txs.pendingOutboundTxs.withoutItem { it.id == tx.id } else txs.pendingOutboundTxs,
            )
        }
    }

    private fun onTxSendSuccessful(txId: TxId) {
        try {
            val tx = walletManager.requireWalletInstance.getPendingOutboundTxById(txId)
            _txs.update { it.copy(pendingOutboundTxs = it.pendingOutboundTxs.withItem(tx)) }
        } catch (e: Exception) {
            logger.i("onTxSendSuccessful: error getting tx by id")
            refreshTxList()
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
