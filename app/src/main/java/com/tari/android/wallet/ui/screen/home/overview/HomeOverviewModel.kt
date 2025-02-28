package com.tari.android.wallet.ui.screen.home.overview

import com.tari.android.wallet.data.tx.TxDto
import com.tari.android.wallet.model.BalanceInfo
import com.tari.android.wallet.model.EmojiId
import com.tari.android.wallet.ui.screen.tx.adapter.TxViewHolderItem
import com.tari.android.wallet.util.extension.toMicroTari

class HomeOverviewModel {
    data class UiState(
        val txList_Old: List<TxViewHolderItem>? = null,
        val txList: List<TxDto>? = emptyList(), // FIXME: distinguish between no transactions and empty transactions
        val balance: BalanceInfo = BalanceInfo(
            availableBalance = 0.toMicroTari(),
            pendingIncomingBalance = 0.toMicroTari(),
            pendingOutgoingBalance = 0.toMicroTari(),
            timeLockedBalance = 0.toMicroTari(),
        ),
        val ticker: String,
        val activeMinersCount: Int? = null,
        val isMining: Boolean = false,

        val avatarEmoji: EmojiId, // todo remove
        val emojiMedium: EmojiId, // todo remove
    )
}