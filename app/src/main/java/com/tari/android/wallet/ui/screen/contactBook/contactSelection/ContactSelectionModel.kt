package com.tari.android.wallet.ui.screen.contactBook.contactSelection

import com.tari.android.wallet.model.TariWalletAddress
import com.tari.android.wallet.model.EmojiId

object ContactSelectionModel {

    data class YatState(
        val yatUser: YatUser? = null,
        val eyeOpened: Boolean = false,
    ) {
        val showYatIcons: Boolean
            get() = yatUser != null

        fun toggleEye() = this.copy(eyeOpened = !eyeOpened)

        data class YatUser(
            val yat: EmojiId,
            val walletAddress: TariWalletAddress,
        )
    }

    sealed interface Effect {
        data object ShowNotValidEmojiId : Effect
        data object ShowNextButton : Effect
        data object GoToNext : Effect
    }
}