package com.tari.android.wallet.ui.fragment.chat.chatDetails

import com.tari.android.wallet.model.TariWalletAddress
import com.tari.android.wallet.ui.common.recyclerView.CommonViewHolderItem
import com.tari.android.wallet.ui.fragment.contactBook.data.contacts.ContactDto


object ChatDetailsModel {
    const val WALLET_ADDRESS = "WALLET_ADDRESS"

    data class UiState(
        val walletAddress: TariWalletAddress,
        val contact: ContactDto,
        val messages: List<CommonViewHolderItem> = emptyList(),
    ) {
        val showEmptyState: Boolean
            get() = messages.isEmpty()
    }
}