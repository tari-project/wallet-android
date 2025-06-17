package com.tari.android.wallet.ui.screen.chat.chatDetails

import com.tari.android.wallet.data.contacts.Contact
import com.tari.android.wallet.model.TariWalletAddress
import com.tari.android.wallet.ui.common.recyclerView.CommonViewHolderItem

object ChatDetailsModel {
    const val WALLET_ADDRESS = "WALLET_ADDRESS"

    data class UiState(
        val walletAddress: TariWalletAddress,
        val contact: Contact,
        val messages: List<CommonViewHolderItem> = emptyList(),
    ) {
        val showEmptyState: Boolean
            get() = messages.isEmpty()
    }
}