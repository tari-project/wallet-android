package com.tari.android.wallet.ui.fragment.tx.history.contact

import com.tari.android.wallet.ui.fragment.contactBook.data.contacts.ContactDto
import com.tari.android.wallet.ui.fragment.tx.adapter.TxViewHolderItem

class ContactTxHistoryModel {
    data class UiState(
        val selectedContact: ContactDto,

        val txList: List<TxViewHolderItem> = emptyList(),
    )
}