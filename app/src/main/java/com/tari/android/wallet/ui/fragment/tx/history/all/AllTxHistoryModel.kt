package com.tari.android.wallet.ui.fragment.tx.history.all

import com.tari.android.wallet.ui.common.recyclerView.CommonViewHolderItem
import com.tari.android.wallet.ui.fragment.tx.adapter.TxViewHolderItem

class AllTxHistoryModel {
    data class UiState(
        private val allTxList: List<CommonViewHolderItem> = emptyList(),
        val searchQuery: String = "",
    ) {
        val sortedTxList
            get() = searchQuery.trim().takeIf { it.isNotBlank() }?.let { query ->
                allTxList.filterIsInstance<TxViewHolderItem>()
                    .filter { it.contains(query) }
            } ?: allTxList

        val searchBarVisible
            get() = searchQuery.trim().isNotBlank() || sortedTxList.isNotEmpty()
        val txEmptyStateVisible
            get() = searchQuery.trim().isBlank() && sortedTxList.isEmpty()
        val txListVisible
            get() = sortedTxList.isNotEmpty()
    }
}