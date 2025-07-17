package com.tari.android.wallet.model

import android.os.Parcelable
import com.tari.android.wallet.data.contacts.Contact
import kotlinx.parcelize.Parcelize

@Parcelize
data class TransactionData(
    val recipientContact: Contact,
    val yat: EmojiId? = null, // always null until Yat is implemented
    val amount: MicroTari,
    val note: String?,
    val feePerGram: MicroTari,
    val isOneSidePayment: Boolean = true, // it's always true since we have only one side payments allowed
) : Parcelable {
    val message: String
        get() = note.orEmpty()
}