package com.tari.android.wallet.ui.screen.profile.walletInfo

import com.tari.android.wallet.model.EmojiId
import com.tari.android.wallet.model.TariWalletAddress

object WalletInfoModel {
    data class UiState(
        val walletAddress: TariWalletAddress,
        val yat: EmojiId?,
        val alias: String,
        val yatShowing: Boolean = false,
        val yatDisconnected: Boolean = false,
    )
}