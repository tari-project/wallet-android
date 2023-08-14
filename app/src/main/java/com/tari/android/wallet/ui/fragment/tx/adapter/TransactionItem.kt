package com.tari.android.wallet.ui.fragment.tx.adapter

import com.tari.android.wallet.model.Tx
import com.tari.android.wallet.ui.common.gyphy.presentation.GIFViewModel
import com.tari.android.wallet.ui.common.recyclerView.CommonViewHolderItem
import com.tari.android.wallet.ui.fragment.contact_book.data.contacts.ContactDto
import com.tari.android.wallet.ui.fragment.contact_book.data.contacts.HashcodeUtils

data class TransactionItem(
    val tx: Tx,
    val contact: ContactDto?,
    val position: Int,
    val viewModel: GIFViewModel,
    val requiredConfirmationCount: Long
) : CommonViewHolderItem() {

    override val viewHolderUUID: String = "TransactionItem" + tx.id

    override fun hashCode(): Int = HashcodeUtils.generate(tx.id, contact?.contact, position, requiredConfirmationCount, contact?.contact?.getAlias())

    override fun equals(other: Any?): Boolean {
        return if (other is TransactionItem) {
            tx.id == other.tx.id
                    && contact?.contact?.getAlias() == other.contact?.contact?.getAlias()
                    && contact?.contact?.extractWalletAddress() == other.contact?.contact?.extractWalletAddress()
                    && position == other.position
                    && requiredConfirmationCount == other.requiredConfirmationCount
        } else false
    }

    fun isContains(text: String): Boolean = tx.tariContact.walletAddress.emojiId.contains(text)
            || tx.tariContact.walletAddress.hexString.contains(text)
            || tx.message.contains(text)
            || contact?.contact?.getAlias()?.contains(text) ?: false
}