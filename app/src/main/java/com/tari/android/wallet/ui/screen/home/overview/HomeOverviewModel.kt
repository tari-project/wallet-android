package com.tari.android.wallet.ui.screen.home.overview

import com.tari.android.wallet.data.ConnectionState
import com.tari.android.wallet.data.tx.TxDto
import com.tari.android.wallet.model.BalanceInfo
import com.tari.android.wallet.util.extension.toMicroTari

class HomeOverviewModel {
    data class UiState(
        val txList: List<TxDto> = emptyList(),
        val txListInitialized: Boolean = false,
        val balance: BalanceInfo = BalanceInfo(
            availableBalance = 0.toMicroTari(),
            pendingIncomingBalance = 0.toMicroTari(),
            pendingOutgoingBalance = 0.toMicroTari(),
            timeLockedBalance = 0.toMicroTari(),
        ),

        val balanceHidden: Boolean = false,

        val ticker: String,
        val networkName: String,
        val ffiVersion: String,
        val connectionState: ConnectionState = ConnectionState(),

        val activeMinersCount: Int? = null,
        val activeMinersCountError: Boolean = false,
        val isMining: Boolean? = null,
        val isMiningError: Boolean = false,

        val showWalletSyncSuccessDialog: Boolean = false,
        val showWalletRestoreSuccessDialog: Boolean = false,
        val showBalanceInfoDialog: Boolean = false,
    )
}