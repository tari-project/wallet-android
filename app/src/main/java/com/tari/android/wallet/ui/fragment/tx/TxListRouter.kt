package com.tari.android.wallet.ui.fragment.tx

import com.tari.android.wallet.model.Tx

interface TxListRouter {
    fun toTxDetails(tx: Tx)

    fun toTTLStore()

    fun toAllSettings()
}