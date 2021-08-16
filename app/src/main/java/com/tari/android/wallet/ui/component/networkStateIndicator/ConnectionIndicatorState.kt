package com.tari.android.wallet.ui.component.networkStateIndicator

import com.tari.android.wallet.R
import com.tari.android.wallet.application.TariWalletApplication

sealed class ConnectionIndicatorState(val resId: Int, val messageId: Int) {

    class Connected(messageId: Int) : ConnectionIndicatorState(R.drawable.network_indicator_ok, messageId)

    class ConnectedWithIssues(messageId: Int) : ConnectionIndicatorState(R.drawable.network_indicator_warning, messageId)

    class Disconnected(messageId: Int) : ConnectionIndicatorState(R.drawable.network_indicator_error, messageId)
}