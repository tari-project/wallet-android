package com.tari.android.wallet.ui.fragment.profile

import com.tari.android.wallet.model.TariWalletAddress

object WalletInfoModel {
    data class UiState(
        val walletAddress: TariWalletAddress,
        val yat: String,
        val alias: String,
    )
}