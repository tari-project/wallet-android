package com.tari.android.wallet.ui.fragment.home.overview

import com.tari.android.wallet.model.BalanceInfo

class HomeOverviewModel {
    data class UiState(
        val balance: BalanceInfo = BalanceInfo(),
    )
}