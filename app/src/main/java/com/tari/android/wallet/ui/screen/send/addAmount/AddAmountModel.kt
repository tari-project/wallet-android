package com.tari.android.wallet.ui.screen.send.addAmount

import com.tari.android.wallet.data.contacts.Contact
import com.tari.android.wallet.model.MicroTari

object AddAmountModel {
    data class UiState(
        val feePerGram: MicroTari? = null,

        val amount: Double,
        val recipientContact: Contact,
        val note: String,
    )

    sealed class Effect {
        data class SetupUi(val uiState: UiState) : Effect()
    }
}