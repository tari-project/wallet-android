package com.tari.android.wallet.model

import android.os.Parcelable
import com.tari.android.wallet.data.contacts.Contact
import kotlinx.parcelize.Parcelize

@Parcelize
data class TransactionData(
    val recipientContact: Contact,
    val amount: MicroTari,
    val note: String?,
    val fee: MicroTari,
    val feePerGram: MicroTari? = null,
) : Parcelable {
    val message: String
        get() = note.orEmpty()
}