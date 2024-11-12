package com.tari.android.wallet.data.tx

import com.tari.android.wallet.model.Tx
import com.tari.android.wallet.ui.fragment.contactBook.data.contacts.ContactDto
import com.tari.android.wallet.ui.fragment.contactBook.data.contacts.HashcodeUtils

data class TxDto(
    val tx: Tx,
    val contact: ContactDto?,
    val requiredConfirmationCount: Long,
) {
    override fun equals(other: Any?): Boolean {
        return if (other is TxDto) {
            tx.id == other.tx.id
                    && contact?.contactInfo?.getAlias() == other.contact?.contactInfo?.getAlias()
                    && contact?.contactInfo?.extractWalletAddress() == other.contact?.contactInfo?.extractWalletAddress()
                    && requiredConfirmationCount == other.requiredConfirmationCount
        } else false
    }

    override fun hashCode(): Int = HashcodeUtils.generate(
        tx.id,
        contact?.contactInfo?.getAlias(),
        contact?.contactInfo,
        requiredConfirmationCount,
    )
}