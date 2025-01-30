package com.tari.android.wallet.ui.screen.send.common

import android.os.Parcelable
import com.tari.android.wallet.data.contacts.model.ContactDto
import com.tari.android.wallet.model.MicroTari
import kotlinx.parcelize.Parcelize

@Parcelize
data class TransactionData(
    val recipientContact: ContactDto?,
    val amount: MicroTari?,
    val note: String?,
    val feePerGram: MicroTari?,
    val isOneSidePayment: Boolean,
) : Parcelable {
    val message: String
        get() = note.orEmpty()
}