package com.tari.android.wallet.ui.fragment.send.addAmount

import com.tari.android.wallet.ui.fragment.contactBook.data.contacts.ContactDto

object AddAmountModel {
    data class UiState(
        val isOneSidedPaymentEnabled: Boolean,
        val isOneSidedPaymentForced: Boolean = false,
        val feePerGrams: FeePerGramOptions? = null,
        val serviceConnected: Boolean = false,

        val amount: Double,
        val contactDto: ContactDto,
        val note: String,
    )
}