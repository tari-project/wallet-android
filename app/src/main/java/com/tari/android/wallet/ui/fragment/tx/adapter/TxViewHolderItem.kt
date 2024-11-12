package com.tari.android.wallet.ui.fragment.tx.adapter

import com.tari.android.wallet.data.tx.TxDto
import com.tari.android.wallet.ui.common.gyphy.presentation.GifViewModel
import com.tari.android.wallet.ui.common.recyclerView.CommonViewHolderItem
import com.tari.android.wallet.ui.fragment.contactBook.data.contacts.HashcodeUtils

data class TxViewHolderItem(
    val txDto: TxDto,
    val gifViewModel: GifViewModel, // TODO try not to pass the repo as a param, but as an injection. This possible causes memory leaks!!
) : CommonViewHolderItem() {

    override val viewHolderUUID: String = "TransactionItem" + txDto.tx.id

    override fun hashCode(): Int = HashcodeUtils.generate(txDto)

    override fun equals(other: Any?): Boolean {
        return if (other is TxViewHolderItem) {
            txDto == other.txDto
        } else false
    }

    fun contains(searchQuery: String): Boolean = txDto.tx.tariContact.walletAddress.fullEmojiId.contains(searchQuery, ignoreCase = true)
            || txDto.tx.tariContact.walletAddress.fullBase58.contains(searchQuery, ignoreCase = true)
            || txDto.tx.tariContact.alias.contains(searchQuery, ignoreCase = true)
            || txDto.tx.message.contains(searchQuery, ignoreCase = true)
            || txDto.tx.paymentId.contains(searchQuery, ignoreCase = true)
            || txDto.tx.amount.formattedTariValue.contains(searchQuery, ignoreCase = true)
            || txDto.tx.amount.formattedValue.contains(searchQuery, ignoreCase = true)
            || txDto.contact?.contactInfo?.getAlias()?.contains(searchQuery, ignoreCase = true) ?: false
}