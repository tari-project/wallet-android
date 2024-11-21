package com.tari.android.wallet.ui.screen.tx.history.contact

import com.tari.android.wallet.data.contacts.model.ContactDto
import com.tari.android.wallet.ui.screen.tx.adapter.TxViewHolderItem

class ContactTxHistoryModel {
    data class UiState(
        val selectedContact: ContactDto,

        val txList: List<TxViewHolderItem> = emptyList(),
    )
}