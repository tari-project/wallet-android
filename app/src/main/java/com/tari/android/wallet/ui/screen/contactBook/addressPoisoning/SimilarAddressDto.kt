package com.tari.android.wallet.ui.screen.contactBook.addressPoisoning

import com.tari.android.wallet.data.contacts.model.ContactDto

data class SimilarAddressDto(
    val contactDto: ContactDto,
    val numberOfTransaction: Int = 0,
    val lastTransactionTimestampMillis: Long? = null,
    val trusted: Boolean = false,
)