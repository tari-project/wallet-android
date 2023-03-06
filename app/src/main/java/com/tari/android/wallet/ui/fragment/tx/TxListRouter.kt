package com.tari.android.wallet.ui.fragment.tx

import com.tari.android.wallet.model.Tx
import com.tari.android.wallet.model.TxId
import com.tari.android.wallet.ui.fragment.contact_book.data.contacts.ContactDto
import com.tari.android.wallet.ui.fragment.contact_book.data.contacts.IContact

interface TxListRouter {
    fun toTxDetails(tx: Tx? = null, txId: TxId? = null)

    fun toSendTari(user: ContactDto?)

    fun toTTLStore()

    fun toAllSettings()

    fun toUtxos()
}