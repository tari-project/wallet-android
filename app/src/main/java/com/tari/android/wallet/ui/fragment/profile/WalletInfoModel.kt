package com.tari.android.wallet.ui.fragment.profile

import com.tari.android.wallet.model.TariWalletAddress
import com.tari.android.wallet.model.EmojiId

object WalletInfoModel {
    data class UiState(
        val walletAddress: TariWalletAddress,
        val yat: EmojiId?,
        val alias: String,
        val yatShowing: Boolean = false,
        val yatDisconnected: Boolean = false,
    )
}