package com.tari.android.wallet.ui.fragment.send.common

import android.os.Parcelable
import com.tari.android.wallet.model.MicroTari
import com.tari.android.wallet.ui.fragment.contactBook.data.contacts.ContactDto
import kotlinx.parcelize.Parcelize

@Parcelize
data class TransactionData(
    val recipientContact: ContactDto?,
    val amount: MicroTari?,
    private val note: String?,
    val feePerGram: MicroTari?,
    val isOneSidePayment: Boolean,
) : Parcelable {
    val message: String
        get() = if (isOneSidePayment) "" else note.orEmpty()

    val paymentId: String
        get() = if (isOneSidePayment) note.orEmpty() else ""
}