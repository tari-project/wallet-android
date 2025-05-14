package com.tari.android.wallet.ui.screen.tx.history.all

import com.tari.android.wallet.R
import com.tari.android.wallet.data.tx.TxRepository
import com.tari.android.wallet.model.tx.Tx
import com.tari.android.wallet.navigation.Navigation
import com.tari.android.wallet.ui.common.CommonViewModel
import com.tari.android.wallet.ui.common.giphy.presentation.GifViewModel
import com.tari.android.wallet.ui.common.giphy.repository.GiphyRestService
import com.tari.android.wallet.ui.common.recyclerView.items.TitleViewHolderItem
import com.tari.android.wallet.ui.screen.tx.adapter.TxViewHolderItem
import com.tari.android.wallet.util.extension.collectFlow
import com.tari.android.wallet.util.extension.zipToPair
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import javax.inject.Inject

class AllTxHistoryViewModel : CommonViewModel() {

    @Inject
    lateinit var txRepository: TxRepository

    @Inject
    lateinit var giphyRestService: GiphyRestService

    private val _uiState = MutableStateFlow(AllTxHistoryModel.UiState())
    val uiState = _uiState.asStateFlow()

    init {
        component.inject(this)

        collectFlow(txRepository.pendingTxs.zipToPair(txRepository.nonPendingTxs)) { (pendingTxs, nonPendingTxs) ->
            _uiState.update { uiState ->
                uiState.copy(
                    allTxList = listOfNotNull(
                        TitleViewHolderItem(
                            title = resourceManager.getString(R.string.home_pending_transactions_title),
                            isFirst = true,
                        ).takeIf { pendingTxs.isNotEmpty() },
                        *pendingTxs.map { txDto ->
                            TxViewHolderItem(
                                txDto = txDto,
                                gifViewModel = GifViewModel(giphyRestService),
                            )
                        }.toTypedArray(),

                        TitleViewHolderItem(
                            title = resourceManager.getString(R.string.home_completed_transactions_title),
                            isFirst = pendingTxs.isEmpty(),
                        ).takeIf { nonPendingTxs.isNotEmpty() },
                        *nonPendingTxs.map { txDto ->
                            TxViewHolderItem(
                                txDto = txDto,
                                gifViewModel = GifViewModel(giphyRestService),
                            )
                        }.toTypedArray(),
                    )
                )
            }
        }
    }

    fun doSearch(query: String) {
        _uiState.update { it.copy(searchQuery = query) }
    }

    fun onTransactionClick(tx: Tx) {
        tariNavigator.navigate(Navigation.TxList.ToTxDetails(tx))
    }

    fun onRequestTariClick() {
        tariNavigator.navigate(Navigation.AllSettings.ToRequestTari)
    }
}