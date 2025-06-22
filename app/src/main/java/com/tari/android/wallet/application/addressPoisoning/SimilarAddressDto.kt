package com.tari.android.wallet.application.addressPoisoning

import com.tari.android.wallet.data.contacts.Contact

data class SimilarAddressDto(
    val contact: Contact,
    val numberOfTransaction: Int = 0,
    val lastTransactionTimestampMillis: Long? = null,
    val trusted: Boolean = false,
)