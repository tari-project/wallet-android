package com.tari.android.wallet.ui.screen.send.addAmount

import com.tari.android.wallet.data.contacts.Contact

object AddAmountModel {
    data class UiState(
        val feePerGrams: FeePerGramOptions? = null,

        val amount: Double,
        val recipientContact: Contact,
        val note: String,
    )

    sealed class Effect {
        data class SetupUi(val uiState: UiState) : Effect()
    }
}