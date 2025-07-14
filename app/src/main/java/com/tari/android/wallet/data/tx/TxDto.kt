package com.tari.android.wallet.data.tx

import com.tari.android.wallet.data.contacts.Contact
import com.tari.android.wallet.model.tx.Tx
import com.tari.android.wallet.util.extension.isTrue

data class TxDto(
    val tx: Tx,
    val contact: Contact,
) {
    fun contains(searchQuery: String): Boolean = this.tx.tariContact.walletAddress.fullEmojiId.contains(searchQuery, ignoreCase = true)
            || this.tx.tariContact.walletAddress.fullBase58.contains(searchQuery, ignoreCase = true)
            || this.tx.tariContact.alias.contains(searchQuery, ignoreCase = true)
            || this.tx.paymentId?.contains(searchQuery, ignoreCase = true).isTrue()
            || this.tx.amount.formattedTariValue.contains(searchQuery, ignoreCase = true)
            || this.tx.amount.formattedMicroTariValue.contains(searchQuery, ignoreCase = true)
            || this.contact.alias?.contains(searchQuery, ignoreCase = true).isTrue()
}