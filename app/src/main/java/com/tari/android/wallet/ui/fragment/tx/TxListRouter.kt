package com.tari.android.wallet.ui.fragment.tx

import com.tari.android.wallet.model.Tx
import com.tari.android.wallet.model.TxId

interface TxListRouter {
    fun toTxDetails(tx: Tx? = null, txId: TxId? = null)

    fun toTTLStore()

    fun toAllSettings()

    fun toUtxos()
}