package com.tari.android.wallet.ui.screen.send.addAmount

import com.tari.android.wallet.data.contacts.model.ContactDto

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
        data class SetupUi(val uiState: UiState) : Effect()
    }
}