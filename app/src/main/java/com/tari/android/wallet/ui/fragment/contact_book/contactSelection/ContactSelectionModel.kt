package com.tari.android.wallet.ui.fragment.contact_book.contactSelection

import com.tari.android.wallet.model.TariWalletAddress
import com.tari.android.wallet.ui.fragment.contact_book.data.contacts.YatDto

object ContactSelectionModel {

    data class YatState(
        val yatUser: YatUser? = null,
        val eyeOpened: Boolean = false,
    ) {
        val showYatIcons: Boolean
            get() = yatUser != null

        fun toggleEye() = this.copy(eyeOpened = !eyeOpened)

        data class YatUser(
            val yatName: String,
            val hexAddress: String,
            val walletAddress: TariWalletAddress,
            val connectedWallets: List<YatDto.ConnectedWallet>,
        )
    }

    sealed interface Effect {
        data object ShowNotValidEmojiId : Effect
        data object ShowNextButton : Effect
    }

}