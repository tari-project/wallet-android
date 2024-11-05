package com.tari.android.wallet.data

import androidx.lifecycle.LiveData
import androidx.lifecycle.MediatorLiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.map
import androidx.lifecycle.viewModelScope
import com.tari.android.wallet.R
import com.tari.android.wallet.application.walletManager.WalletManager.WalletEvent
import com.tari.android.wallet.extension.collectFlow
import com.tari.android.wallet.extension.debounce
import com.tari.android.wallet.extension.getWithError
import com.tari.android.wallet.extension.repopulate
import com.tari.android.wallet.model.CancelledTx
import com.tari.android.wallet.model.CompletedTx
import com.tari.android.wallet.model.PendingInboundTx
import com.tari.android.wallet.model.PendingOutboundTx
import com.tari.android.wallet.model.Tx
import com.tari.android.wallet.model.TxId
import com.tari.android.wallet.model.TxStatus
import com.tari.android.wallet.model.WalletError
import com.tari.android.wallet.ui.common.CommonViewModel
import com.tari.android.wallet.ui.common.gyphy.presentation.GifViewModel
import com.tari.android.wallet.ui.common.gyphy.repository.GifRepository
import com.tari.android.wallet.ui.common.recyclerView.CommonViewHolderItem
import com.tari.android.wallet.ui.common.recyclerView.items.TitleViewHolderItem
import com.tari.android.wallet.ui.fragment.contactBook.data.ContactsRepository
import com.tari.android.wallet.ui.fragment.tx.adapter.TransactionItem
import com.tari.android.wallet.util.DebugConfig
import com.tari.android.wallet.util.MockDataStub
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.util.concurrent.CopyOnWriteArrayList
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class TransactionRepository @Inject constructor() : CommonViewModel() {

    @Inject
    lateinit var contactsRepository: ContactsRepository

    @Inject
    lateinit var gifRepository: GifRepository

    // TODO Repository should not return ViewHolders!!!
    private val _list = MutableLiveData<List<CommonViewHolderItem>>(emptyList())
    val list: LiveData<List<CommonViewHolderItem>> = _list

    private val _listUpdateTrigger = MediatorLiveData<Unit>()
    val listUpdateTrigger: LiveData<Unit> = _listUpdateTrigger

    private val _requiredConfirmationCount = MutableLiveData<Long>(3)
    val requiredConfirmationCount: LiveData<Long> = _requiredConfirmationCount

    val debouncedList = listUpdateTrigger.debounce(LIST_UPDATE_DEBOUNCE).map {
        updateList()
    }

    private val cancelledTxs = CopyOnWriteArrayList<CancelledTx>()
    private val completedTxs = CopyOnWriteArrayList<CompletedTx>()
    private val pendingInboundTxs = CopyOnWriteArrayList<PendingInboundTx>()
    private val pendingOutboundTxs = CopyOnWriteArrayList<PendingOutboundTx>()

    val txListIsEmpty: Boolean
        get() = cancelledTxs.isEmpty()
                && completedTxs.isEmpty()
                && pendingInboundTxs.isEmpty()
                && pendingOutboundTxs.isEmpty()

    init {
        component.inject(this)

        doOnWalletRunning { doOnWalletServiceConnected { runCatching { onServiceConnected() } } }
    }

    private fun onServiceConnected() {
        collectFlow(contactsRepository.contactList) { _listUpdateTrigger.postValue(Unit) }

        collectFlow(walletManager.walletEvent) { event ->
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

                is WalletEvent.Updated -> refreshAllData()

                is WalletEvent.TxSend.TxSendSuccessful -> onTxSendSuccessful(event.txId)

                else -> Unit
            }
        }

        viewModelScope.launch(Dispatchers.IO) {
            updateTxListData()
            fetchRequiredConfirmationCount()
            updateList()
        }
    }

    private fun fetchRequiredConfirmationCount() {
        _requiredConfirmationCount.postValue(walletService.getWithError { error, service -> service.getRequiredConfirmationCount(error) })
    }

    fun refreshAllData() {
        viewModelScope.launch(Dispatchers.IO) {
            updateTxListData()
            updateList()
        }
    }

    private fun updateTxListData() {
        doOnWalletServiceConnected {
            cancelledTxs.repopulate(it.getWithError { error, service -> service.getCancelledTxs(error) }.orEmpty())
            completedTxs.repopulate(it.getWithError { error, service -> service.getCompletedTxs(error) }.orEmpty())
            pendingInboundTxs.repopulate(it.getWithError { error, service -> service.getPendingInboundTxs(error) }.orEmpty())
            pendingOutboundTxs.repopulate(it.getWithError { error, service -> service.getPendingOutboundTxs(error) }.orEmpty())
        }
    }

    private fun updateList() = viewModelScope.launch(Dispatchers.Main) {
        val confirmationCount = requiredConfirmationCount.value!!

        val items = mutableListOf<CommonViewHolderItem>()

        if (DebugConfig.mockTxs) {
            items.addAll(MockDataStub.createTxList(gifRepository, confirmationCount))
        } else {
            val minedUnconfirmedTxs = completedTxs.filter { it.status == TxStatus.MINED_UNCONFIRMED }
            val nonMinedUnconfirmedCompletedTxs = completedTxs.filter { it.status != TxStatus.MINED_UNCONFIRMED }

            // sort and add pending txs
            val pendingTxs = (pendingInboundTxs + pendingOutboundTxs + minedUnconfirmedTxs).toMutableList()
            pendingTxs.sortWith(compareByDescending(Tx::timestamp).thenByDescending { it.id })
            if (pendingTxs.isNotEmpty()) {
                items.add(TitleViewHolderItem(title = resourceManager.getString(R.string.home_pending_transactions_title), isFirst = true))
                items.addAll(pendingTxs.mapIndexed { index, tx ->
                    TransactionItem(
                        tx = tx,
                        contact = contactsRepository.getContactForTx(tx),
                        position = index,
                        gifViewModel = GifViewModel(gifRepository),
                        requiredConfirmationCount = confirmationCount,
                    )
                })
            }

            // sort and add non-pending txs
            val nonPendingTxs = (cancelledTxs + nonMinedUnconfirmedCompletedTxs).toMutableList()
            nonPendingTxs.sortWith(compareByDescending(Tx::timestamp).thenByDescending { it.id })
            if (nonPendingTxs.isNotEmpty()) {
                items.add(
                    TitleViewHolderItem(
                        title = resourceManager.getString(R.string.home_completed_transactions_title),
                        isFirst = pendingTxs.isEmpty(),
                    )
                )
                items.addAll(nonPendingTxs.mapIndexed { index, tx ->
                    TransactionItem(
                        tx = tx,
                        contact = contactsRepository.getContactForTx(tx),
                        position = index + pendingTxs.size,
                        gifViewModel = GifViewModel(gifRepository),
                        requiredConfirmationCount = confirmationCount,
                    )
                })
            }
        }

        _list.postValue(items)
    }

    private fun onTxReceived(tx: PendingInboundTx) {
        pendingInboundTxs.add(tx)
        _listUpdateTrigger.postValue(Unit)
    }

    private fun onTxReplyReceived(tx: PendingOutboundTx) {
        val index = pendingOutboundTxs.indexOfFirst { it.id == tx.id }
        if (index != -1) {
            pendingOutboundTxs[index] = pendingOutboundTxs[index].copy(status = tx.status)
            _listUpdateTrigger.postValue(Unit)
        } else {
            logger.i("onTxReplyReceived: tx ${tx.id} not found in pendingOutboundTxs")
        }
    }

    private fun onTxFinalized(tx: PendingInboundTx) {
        val index = pendingInboundTxs.indexOfFirst { it.id == tx.id }
        if (index != -1) {
            pendingInboundTxs[index] = pendingInboundTxs[index].copy(status = tx.status)
            _listUpdateTrigger.postValue(Unit)
        } else {
            logger.i("onTxFinalized: tx ${tx.id} not found in pendingInboundTxs")
        }
    }

    private fun onInboundTxBroadcast(tx: PendingInboundTx) {
        val index = pendingInboundTxs.indexOfFirst { it.id == tx.id }
        if (index != -1) {
            pendingInboundTxs[index] = pendingInboundTxs[index].copy(status = TxStatus.BROADCAST)
        } else {
            logger.i("onInboundTxBroadcast: tx ${tx.id} not found in pendingInboundTxs")
        }
    }

    private fun onOutboundTxBroadcast(tx: PendingOutboundTx) {
        val index = pendingOutboundTxs.indexOfFirst { it.id == tx.id }
        if (index != -1) {
            pendingOutboundTxs[index] = pendingOutboundTxs[index].copy(status = TxStatus.BROADCAST)
        } else {
            logger.i("onOutboundTxBroadcast: tx ${tx.id} not found in pendingOutboundTxs")
        }
    }

    private fun onTxMinedUnconfirmed(tx: CompletedTx) {
        when (tx.direction) {
            Tx.Direction.INBOUND -> pendingInboundTxs
            Tx.Direction.OUTBOUND -> pendingOutboundTxs
        }.removeIf { it.id == tx.id }
        val index = completedTxs.indexOfFirst { it.id == tx.id }
        if (index == -1) {
            completedTxs.add(tx)
        } else {
            completedTxs[index] = tx
        }
        _listUpdateTrigger.postValue(Unit)
    }

    private fun onTxMined(tx: CompletedTx) {
        pendingInboundTxs.removeIf { it.id == tx.id }
        pendingOutboundTxs.removeIf { it.id == tx.id }

        val index = completedTxs.indexOfFirst { it.id == tx.id }
        if (index == -1) {
            completedTxs.add(tx)
        } else {
            completedTxs[index] = tx
        }
        _listUpdateTrigger.postValue(Unit)
    }

    private fun onTxFauxMinedUnconfirmed(tx: CompletedTx) {
        when (tx.direction) {
            Tx.Direction.INBOUND -> pendingInboundTxs
            Tx.Direction.OUTBOUND -> pendingOutboundTxs
        }.removeIf { it.id == tx.id }
        val index = completedTxs.indexOfFirst { it.id == tx.id }
        if (index == -1) {
            completedTxs.add(tx)
        } else {
            completedTxs[index] = tx
        }
        _listUpdateTrigger.postValue(Unit)
    }

    private fun onFauxTxMined(tx: CompletedTx) {
        pendingInboundTxs.removeIf { it.id == tx.id }
        pendingOutboundTxs.removeIf { it.id == tx.id }

        val index = completedTxs.indexOfFirst { it.id == tx.id }
        if (index == -1) {
            completedTxs.add(tx)
        } else {
            completedTxs[index] = tx
        }
        _listUpdateTrigger.postValue(Unit)
    }

    private fun onTxCancelled(tx: CancelledTx) {
        val source = when (tx.direction) {
            Tx.Direction.INBOUND -> pendingInboundTxs
            Tx.Direction.OUTBOUND -> pendingOutboundTxs
        }
        source.find { it.id == tx.id }?.let { source.remove(it) }
        cancelledTxs.add(tx)
        _listUpdateTrigger.postValue(Unit)
    }

    private fun onTxSendSuccessful(txId: TxId) {
        viewModelScope.launch(Dispatchers.IO) {
            val error = WalletError()
            val tx = walletService.getPendingOutboundTxById(txId, error)
            if (error == WalletError.NoError) {
                pendingOutboundTxs.add(tx)
                _listUpdateTrigger.postValue(Unit)
            } else {
                refreshAllData()
            }
        }
    }

    companion object {
        private const val LIST_UPDATE_DEBOUNCE = 500L
    }
}