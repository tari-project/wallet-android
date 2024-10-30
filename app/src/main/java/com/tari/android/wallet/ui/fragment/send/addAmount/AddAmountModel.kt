package com.tari.android.wallet.ui.fragment.send.addAmount

import com.tari.android.wallet.ui.fragment.contactBook.data.contacts.ContactDto

object AddAmountModel {
    data class UiState(
        val isOneSidedPaymentEnabled: Boolean,
        val isOneSidedPaymentForced: Boolean = false,
        val feePerGrams: FeePerGramOptions? = null,

        val amount: Double,
        val contactDto: ContactDto,
        val note: String,
    )

    sealed class Effect {
        data class OnServiceConnected(val uiState: UiState) : Effect()
    }
}