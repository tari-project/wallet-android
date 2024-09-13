package com.tari.android.wallet.ui.fragment.contactBook.details

import com.tari.android.wallet.application.YatAdapter.ConnectedWallet
import com.tari.android.wallet.ui.fragment.contactBook.data.contacts.ContactDto

object ContactDetailsModel {
    data class UiState(
        val contact: ContactDto,
        val connectedYatWallets: List<ConnectedWallet> = emptyList(),
    )
}