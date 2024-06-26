package com.tari.android.wallet.ui.fragment.contactBook.address_poisoning

import com.tari.android.wallet.ui.fragment.contactBook.data.contacts.ContactDto

data class SimilarAddressDto(
    val contactDto: ContactDto,
    val numberOfTransaction: Int = 0,
    val lastTransactionTimestampMillis: Long? = null,
    val trusted: Boolean = false,
)