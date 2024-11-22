package com.tari.android.wallet.data.tx

import com.tari.android.wallet.model.tx.Tx
import com.tari.android.wallet.data.contacts.model.ContactDto

data class TxDto(
    val tx: Tx,
    val contact: ContactDto?,
    val requiredConfirmationCount: Long,
)