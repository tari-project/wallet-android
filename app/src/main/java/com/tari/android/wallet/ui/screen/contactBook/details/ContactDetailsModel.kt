package com.tari.android.wallet.ui.screen.contactBook.details

import com.tari.android.wallet.application.YatAdapter.ConnectedWallet
import com.tari.android.wallet.data.contacts.Contact

object ContactDetailsModel {
    data class UiState(
        val contact: Contact,
        val connectedYatWallets: List<ConnectedWallet> = emptyList(),
    )
}