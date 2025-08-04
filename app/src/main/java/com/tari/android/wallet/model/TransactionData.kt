package com.tari.android.wallet.model

import android.os.Parcelable
import com.tari.android.wallet.data.contacts.Contact
import kotlinx.parcelize.Parcelize

@Parcelize
data class TransactionData(
    val recipientContact: Contact,
    val amount: MicroTari,
    val note: String?,
    val feePerGram: MicroTari,
) : Parcelable {
    val message: String
        get() = note.orEmpty()
}