package com.tari.android.wallet.data.tx

import com.tari.android.wallet.data.contacts.model.ContactDto
import com.tari.android.wallet.model.tx.Tx

data class TxDto(
    val tx: Tx,
    val contact: ContactDto?,
) {
    fun contains(searchQuery: String): Boolean = this.tx.tariContact.walletAddress.fullEmojiId.contains(searchQuery, ignoreCase = true)
            || this.tx.tariContact.walletAddress.fullBase58.contains(searchQuery, ignoreCase = true)
            || this.tx.tariContact.alias.contains(searchQuery, ignoreCase = true)
            || this.tx.paymentId.contains(searchQuery, ignoreCase = true)
            || this.tx.amount.formattedTariValue.contains(searchQuery, ignoreCase = true)
            || this.tx.amount.formattedValue.contains(searchQuery, ignoreCase = true)
            || this.contact?.contactInfo?.getAlias()?.contains(searchQuery, ignoreCase = true) == true
}