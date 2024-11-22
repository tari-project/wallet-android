package com.tari.android.wallet.ui.screen.home.overview

import com.tari.android.wallet.model.BalanceInfo
import com.tari.android.wallet.ui.screen.tx.adapter.TxViewHolderItem
import com.tari.android.wallet.model.EmojiId

class HomeOverviewModel {
    data class UiState(
        val txList: List<TxViewHolderItem> = emptyList(),
        val balance: BalanceInfo = BalanceInfo(),

        val avatarEmoji: EmojiId,
        val emojiMedium: EmojiId,
    )
}