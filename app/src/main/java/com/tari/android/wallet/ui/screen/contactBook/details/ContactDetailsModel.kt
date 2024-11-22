package com.tari.android.wallet.ui.screen.contactBook.details

import com.tari.android.wallet.application.YatAdapter.ConnectedWallet
import com.tari.android.wallet.data.contacts.model.ContactDto

object ContactDetailsModel {
    data class UiState(
        val contact: ContactDto,
        val connectedYatWallets: List<ConnectedWallet> = emptyList(),
    )
}