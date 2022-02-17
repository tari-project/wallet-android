package com.tari.android.wallet.ui.activity.home

enum class HomeDeeplinkScreens {
    TxDetails;

    companion object {
        const val Key = "HomeDeeplinkScreen"
        const val KeyTxDetailsArgs = "HomeDeeplinkScreen_tx_id"

        fun parse(string: String?) : HomeDeeplinkScreens? {
            return values().firstOrNull { it.name == string }
        }
    }
}