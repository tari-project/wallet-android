package com.tari.android.wallet.data.tx

import com.tari.android.wallet.data.contacts.model.ContactDto
import com.tari.android.wallet.model.tx.Tx

data class TxDto(
    val tx: Tx,
    val contact: ContactDto?,
)