package com.tari.android.wallet.ui.screen.home.overview

import com.tari.android.wallet.data.ConnectionIndicatorState
import com.tari.android.wallet.data.tx.TxDto
import com.tari.android.wallet.model.BalanceInfo
import com.tari.android.wallet.util.extension.toMicroTari

class HomeOverviewModel {
    data class UiState(
        val txList: List<TxDto>? = emptyList(), // FIXME: distinguish between no transactions and empty transactions
        val balance: BalanceInfo = BalanceInfo(
            availableBalance = 0.toMicroTari(),
            pendingIncomingBalance = 0.toMicroTari(),
            pendingOutgoingBalance = 0.toMicroTari(),
            timeLockedBalance = 0.toMicroTari(),
        ),

        val ticker: String,
        val networkName: String,
        val ffiVersion: String,
        val connectionIndicatorState: ConnectionIndicatorState = ConnectionIndicatorState.Disconnected,

        val activeMinersCount: Int? = null,
        val activeMinersCountError: Boolean = false,
        val isMining: Boolean? = null,
        val isMiningError: Boolean = false,

        val showWalletSyncSuccessDialog: Boolean = false,
        val showWalletRestoreSuccessDialog: Boolean = false,
    )
}