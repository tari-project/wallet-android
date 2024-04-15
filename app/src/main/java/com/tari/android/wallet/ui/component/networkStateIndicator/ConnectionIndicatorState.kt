package com.tari.android.wallet.ui.component.networkStateIndicator

import com.tari.android.wallet.R

sealed class ConnectionIndicatorState(val resId: Int) {

    data object Connected : ConnectionIndicatorState(R.drawable.vector_network_state_full)

    data object ConnectedWithIssues : ConnectionIndicatorState(R.drawable.vector_network_state_limited)

    data object Disconnected : ConnectionIndicatorState(R.drawable.vector_network_state_off)
}