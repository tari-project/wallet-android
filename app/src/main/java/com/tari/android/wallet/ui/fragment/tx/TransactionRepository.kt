package com.tari.android.wallet.ui.fragment.tx

import androidx.lifecycle.LiveData
import androidx.lifecycle.MediatorLiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.map
import androidx.lifecycle.toLiveData
import androidx.lifecycle.viewModelScope
import com.tari.android.wallet.R
import com.tari.android.wallet.event.Event
import com.tari.android.wallet.event.EventBus
import com.tari.android.wallet.extension.debounce
import com.tari.android.wallet.extension.getWithError
import com.tari.android.wallet.extension.repopulate
import com.tari.android.wallet.model.CancelledTx
import com.tari.android.wallet.model.CompletedTx
import com.tari.android.wallet.model.MicroTari
import com.tari.android.wallet.model.PendingInboundTx
import com.tari.android.wallet.model.PendingOutboundTx
import com.tari.android.wallet.model.TariContact
import com.tari.android.wallet.model.TariWalletAddress
import com.tari.android.wallet.model.Tx
import com.tari.android.wallet.model.TxId
import com.tari.android.wallet.model.TxStatus
import com.tari.android.wallet.model.WalletError
import com.tari.android.wallet.ui.common.CommonViewModel
import com.tari.android.wallet.ui.common.gyphy.presentation.GIFViewModel
import com.tari.android.wallet.ui.common.gyphy.repository.GIFRepository
import com.tari.android.wallet.ui.common.recyclerView.CommonViewHolderItem
import com.tari.android.wallet.ui.common.recyclerView.items.TitleViewHolderItem
import com.tari.android.wallet.ui.fragment.contact_book.data.ContactsRepository
import com.tari.android.wallet.ui.fragment.tx.adapter.TransactionItem
import com.tari.android.wallet.util.Build.MOCKED
import io.reactivex.BackpressureStrategy
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.math.BigInteger
import java.util.concurrent.CopyOnWriteArrayList
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class TransactionRepository @Inject constructor() : CommonViewModel() {

    @Inject
    lateinit var contactsRepository: ContactsRepository

    @Inject
    lateinit var gifRepository: GIFRepository


    private val _list = MutableLiveData<MutableList<CommonViewHolderItem>>(mutableListOf())
    val list: LiveData<MutableList<CommonViewHolderItem>> = _list

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

        doOnConnectedToWallet { doOnConnected { runCatching { onServiceConnected() } } }
    }

    private fun onServiceConnected() {
        subscribeToEventBus()

        _listUpdateTrigger.addSource(
            contactsRepository.publishSubject.toFlowable(BackpressureStrategy.LATEST).toLiveData()
        ) { _listUpdateTrigger.postValue(Unit) }

        viewModelScope.launch(Dispatchers.IO) {
            updateTxListData()
            fetchRequiredConfirmationCount()
            updateList()
        }
    }

    private fun fetchRequiredConfirmationCount() {
        _requiredConfirmationCount.postValue(walletService.getWithError { error, service -> service.getRequiredConfirmationCount(error) })
    }

    private fun subscribeToEventBus() {
        EventBus.subscribe<Event.Transaction.Updated>(this) { refreshAllData() }
        EventBus.subscribe<Event.Transaction.TxReceived>(this) {
            onTxReceived(it.tx)
        }
        EventBus.subscribe<Event.Transaction.TxReplyReceived>(this) { onTxReplyReceived(it.tx) }
        EventBus.subscribe<Event.Transaction.TxFinalized>(this) { onTxFinalized(it.tx) }
        EventBus.subscribe<Event.Transaction.InboundTxBroadcast>(this) { onInboundTxBroadcast(it.tx) }
        EventBus.subscribe<Event.Transaction.OutboundTxBroadcast>(this) { onOutboundTxBroadcast(it.tx) }
        EventBus.subscribe<Event.Transaction.TxMinedUnconfirmed>(this) { onTxMinedUnconfirmed(it.tx) }
        EventBus.subscribe<Event.Transaction.TxMined>(this) { onTxMined(it.tx) }
        EventBus.subscribe<Event.Transaction.TxFauxMinedUnconfirmed>(this) { onTxFauxMinedUnconfirmed(it.tx) }
        EventBus.subscribe<Event.Transaction.TxFauxConfirmed>(this) { onFauxTxMined(it.tx) }
        EventBus.subscribe<Event.Transaction.TxCancelled>(this) {
            onTxCancelled(it.tx)
        }

        EventBus.subscribe<Event.Transaction.TxSendSuccessful>(this) { onTxSendSuccessful(it.txId) }
    }

    fun refreshAllData() {
        viewModelScope.launch(Dispatchers.IO) {
            updateTxListData()
            updateList()
        }
    }

    private fun updateTxListData() {
        cancelledTxs.repopulate(walletService.getWithError { error, service -> service.getCancelledTxs(error) }.orEmpty())
        completedTxs.repopulate(walletService.getWithError { error, service -> service.getCompletedTxs(error) }.orEmpty())
        pendingInboundTxs.repopulate(walletService.getWithError { error, service -> service.getPendingInboundTxs(error) }.orEmpty())
        pendingOutboundTxs.repopulate(walletService.getWithError { error, service -> service.getPendingOutboundTxs(error) }.orEmpty())
    }

    private fun updateList() = viewModelScope.launch(Dispatchers.Main) {
        val confirmationCount = requiredConfirmationCount.value!!

        val items = mutableListOf<CommonViewHolderItem>()

        val minedUnconfirmedTxs = completedTxs.filter { it.status == TxStatus.MINED_UNCONFIRMED }
        val nonMinedUnconfirmedCompletedTxs = completedTxs.filter { it.status != TxStatus.MINED_UNCONFIRMED }

        // sort and add pending txs
        val pendingTxs = (pendingInboundTxs + pendingOutboundTxs + minedUnconfirmedTxs).toMutableList()
        pendingTxs.sortWith(compareByDescending(Tx::timestamp).thenByDescending { it.id })
        if (pendingTxs.isNotEmpty()) {
            items.add(TitleViewHolderItem(resourceManager.getString(R.string.home_pending_transactions_title), true))
            items.addAll(pendingTxs.mapIndexed { index, tx ->
                TransactionItem(
                    tx,
                    contactsRepository.ffiBridge.getContactForTx(tx),
                    index,
                    GIFViewModel(gifRepository),
                    confirmationCount
                )
            })
        }

        // sort and add non-pending txs
        val nonPendingTxs = (cancelledTxs + nonMinedUnconfirmedCompletedTxs).toMutableList()
        nonPendingTxs.sortWith(compareByDescending(Tx::timestamp).thenByDescending { it.id })
        if (nonPendingTxs.isNotEmpty()) {
            items.add(TitleViewHolderItem(resourceManager.getString(R.string.home_completed_transactions_title), false))
            items.addAll(nonPendingTxs.mapIndexed { index, tx ->
                TransactionItem(
                    tx,
                    contactsRepository.ffiBridge.getContactForTx(tx),
                    index + pendingTxs.size,
                    GIFViewModel(gifRepository),
                    confirmationCount
                )
            })
        }

        if (MOCKED) {
            val item = TransactionItem(
                CompletedTx().apply {
                    direction = Tx.Direction.INBOUND
                    status = TxStatus.MINED_CONFIRMED
                    amount = MicroTari(BigInteger.valueOf(100000))
                    fee = MicroTari(BigInteger.valueOf(1000))
                    timestamp = BigInteger.valueOf(System.currentTimeMillis())
                    id = BigInteger.valueOf(1)
                    tariContact = TariContact(
                        TariWalletAddress(
                            "66961BF37F58D6277B7D2563750D338AFADEF19B058299D5CD51F15AC7799F0CFC",
                            "\uD83C\uDFE0\uD83D\uDC52\uD83C\uDF4A\uD83D\uDE95\uD83D\uDC28\uD83C\uDFB7\uD83D\uDD26\uD83C\uDF5A\uD83D\uDC1D\uD83D\uDC22\uD83C\uDF55\uD83C\uDFC6\uD83D\uDC17\uD83C\uDF35\uD83C\uDF70\uD83D\uDC35\uD83D\uDEAA\uD83D\uDD2E\uD83D\uDE92\uD83D\uDC57\uD83C\uDF0D\uD83D\uDC2C\uD83D\uDC55\uD83D\uDD25\uD83D\uDCE3\uD83C\uDFAD\uD83D\uDE92\uD83C\uDFB9\uD83D\uDCC9\uD83D\uDC1B\uD83D\uDC5B\uD83C\uDF34\uD83D\uDEB2"
                        ), "test1"
                    )
                },
                contactsRepository.ffiBridge.getContactForTx(CompletedTx()),
                0,
                GIFViewModel(gifRepository),
                confirmationCount
            )

            val tx2 = CompletedTx().apply {
                direction = Tx.Direction.INBOUND
                status = TxStatus.MINED_CONFIRMED
                amount = MicroTari(BigInteger.valueOf(110000))
                fee = MicroTari(BigInteger.valueOf(1000))
                timestamp = BigInteger.valueOf(System.currentTimeMillis())
                id = BigInteger.valueOf(1)
                tariContact = TariContact(
                    TariWalletAddress(
                        "66961BF37F58D6277B7D2563750D338AFADEF19B058299D5CD51F15AC7799F0CFC",
                        "\uD83C\uDFE0\uD83D\uDC52\uD83C\uDF4A\uD83D\uDE95\uD83D\uDC28\uD83C\uDFB7\uD83D\uDD26\uD83C\uDF5A\uD83D\uDC1D\uD83D\uDC22\uD83C\uDF55\uD83C\uDFC6\uD83D\uDC17\uD83C\uDF35\uD83C\uDF70\uD83D\uDC35\uD83D\uDEAA\uD83D\uDD2E\uD83D\uDE92\uD83D\uDC57\uD83C\uDF0D\uD83D\uDC2C\uD83D\uDC55\uD83D\uDD25\uD83D\uDCE3\uD83C\uDFAD\uD83D\uDE92\uD83C\uDFB9\uD83D\uDCC9\uD83D\uDC1B\uD83D\uDC5B\uD83C\uDF34\uD83D\uDEB2"
                    ), "test2"
                )
            }
            val item2 = TransactionItem(
                tx2,
                contactsRepository.ffiBridge.getContactForTx(tx2),
                0,
                GIFViewModel(gifRepository),
                confirmationCount
            )

            val tx3 = CompletedTx().apply {
                direction = Tx.Direction.INBOUND
                status = TxStatus.MINED_CONFIRMED
                amount = MicroTari(BigInteger.valueOf(111000))
                fee = MicroTari(BigInteger.valueOf(1000))
                timestamp = BigInteger.valueOf(System.currentTimeMillis())
                id = BigInteger.valueOf(1)
                tariContact = TariContact(
                    TariWalletAddress(
                        "66961BF37F58D6277B7D2563750D338AFADEF19B058299D5CD51F15AC7799F0CFC",
                        "\uD83C\uDFE0\uD83D\uDC52\uD83C\uDF4A\uD83D\uDE95\uD83D\uDC28\uD83C\uDFB7\uD83D\uDD26\uD83C\uDF5A\uD83D\uDC1D\uD83D\uDC22\uD83C\uDF55\uD83C\uDFC6\uD83D\uDC17\uD83C\uDF35\uD83C\uDF70\uD83D\uDC35\uD83D\uDEAA\uD83D\uDD2E\uD83D\uDE92\uD83D\uDC57\uD83C\uDF0D\uD83D\uDC2C\uD83D\uDC55\uD83D\uDD25\uD83D\uDCE3\uD83C\uDFAD\uD83D\uDE92\uD83C\uDFB9\uD83D\uDCC9\uD83D\uDC1B\uD83D\uDC5B\uD83C\uDF34\uD83D\uDEB2"
                    ), "test3"
                )
            }
            val item3 = TransactionItem(
                tx3,
                contactsRepository.ffiBridge.getContactForTx(tx3),
                0,
                GIFViewModel(gifRepository),
                confirmationCount
            )

            items.add(item)
            items.add(item2)
            items.add(item3)
        }

        _list.postValue(items)
    }

    private fun onTxReceived(tx: PendingInboundTx) {
        pendingInboundTxs.add(tx)
        _listUpdateTrigger.postValue(Unit)
    }

    private fun onTxReplyReceived(tx: PendingOutboundTx) {
        pendingOutboundTxs.firstOrNull { it.id == tx.id }?.status = tx.status
        _listUpdateTrigger.postValue(Unit)
    }

    private fun onTxFinalized(tx: PendingInboundTx) {
        pendingInboundTxs.firstOrNull { it.id == tx.id }?.status = tx.status
        _listUpdateTrigger.postValue(Unit)
    }

    private fun onInboundTxBroadcast(tx: PendingInboundTx) {
        pendingInboundTxs.firstOrNull { it.id == tx.id }?.status = TxStatus.BROADCAST
    }

    private fun onOutboundTxBroadcast(tx: PendingOutboundTx) {
        pendingOutboundTxs.firstOrNull { it.id == tx.id }?.status = TxStatus.BROADCAST
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