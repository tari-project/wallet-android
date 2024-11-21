package com.tari.android.wallet.ui.screen.home

enum class HomeDeeplinkScreens {
    TxDetails;

    companion object {
        const val KEY = "HomeDeeplinkScreen"
        const val KEY_TX_DETAIL_ARGS = "HomeDeeplinkScreen_tx_id"

        fun parse(string: String?): HomeDeeplinkScreens? {
            return entries.firstOrNull { it.name == string }
        }
    }
}