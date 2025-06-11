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
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
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

    private val _txs = MutableStateFlow(TxListData())
    val txs = _txs.asStateFlow()

    private val _txsInitialized = MutableStateFlow(false)
    val txsInitialized = _txsInitialized.asStateFlow()

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

    fun findTxById(txId: TxId): TxDto? {
        return txs.value.allTxs.firstOrNull { it.tx.id == txId }
    }

    /**
     * Re-download the transaction list from FFI
     */
    private fun refreshTxList() {
        applicationScope.launch(Dispatchers.IO) {
            walletManager.doOnWalletRunning { wallet ->
                _txs.value = TxListData(
                    cancelledTxs = wallet.getCancelledTxs().map { it.toDto() },
                    completedTxs = wallet.getCompletedTxs().map { it.toDto() },
                    pendingInboundTxs = wallet.getPendingInboundTxs().map { it.toDto() },
                    pendingOutboundTxs = wallet.getPendingOutboundTxs().map { it.toDto() },
                )
                _txsInitialized.value = true

                logger.i(
                    "Refreshed tx list: ${_txs.value.completedTxs.size} completed, " +
                            "${_txs.value.pendingInboundTxs.size} pending inbound, " +
                            "${_txs.value.pendingOutboundTxs.size} pending outbound, " +
                            "${_txs.value.cancelledTxs.size} cancelled"
                )
            }
        }
    }

    private fun Tx.toDto() = TxDto(
        tx = this,
        contact = contactsRepository.findOrCreateContact(this.tariContact.walletAddress),
    )
}
