package com.tari.android.wallet.data.tx

import com.tari.android.wallet.model.Tx
import com.tari.android.wallet.ui.fragment.contactBook.data.contacts.ContactDto

data class TxDto(
    val tx: Tx,
    val contact: ContactDto?,
    val requiredConfirmationCount: Long,
)