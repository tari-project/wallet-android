package com.tari.android.wallet.ui.fragment.tx

import com.tari.android.wallet.model.Tx
import com.tari.android.wallet.model.TxId
import com.tari.android.wallet.model.User
import com.tari.android.wallet.ui.fragment.contact_book.data.IContact

interface TxListRouter {
    fun toTxDetails(tx: Tx? = null, txId: TxId? = null)

    fun toSendTari(user: IContact?)

    fun toTTLStore()

    fun toAllSettings()

    fun toUtxos()
}