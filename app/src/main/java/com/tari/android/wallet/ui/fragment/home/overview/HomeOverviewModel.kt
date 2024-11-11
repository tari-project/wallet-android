package com.tari.android.wallet.ui.fragment.home.overview

import com.tari.android.wallet.model.BalanceInfo
import com.tari.android.wallet.ui.fragment.tx.adapter.TxViewHolderItem
import com.tari.android.wallet.util.EmojiId

class HomeOverviewModel {
    data class UiState(
        val txList: List<TxViewHolderItem> = emptyList(),
        val balance: BalanceInfo = BalanceInfo(),

        val avatarEmoji: EmojiId,
        val emojiMedium: EmojiId,
    )
}