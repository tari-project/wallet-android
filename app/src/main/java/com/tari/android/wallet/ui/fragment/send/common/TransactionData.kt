package com.tari.android.wallet.ui.fragment.send.common

import android.os.Parcelable
import com.tari.android.wallet.model.MicroTari
import com.tari.android.wallet.ui.fragment.contact_book.data.contacts.ContactDto
import kotlinx.parcelize.Parcelize

@Parcelize
data class TransactionData(
    val recipientContact: ContactDto?,
    val amount: MicroTari?,
    val note: String?,
    val feePerGram: MicroTari?,
    val isOneSidePayment: Boolean,
) : Parcelable