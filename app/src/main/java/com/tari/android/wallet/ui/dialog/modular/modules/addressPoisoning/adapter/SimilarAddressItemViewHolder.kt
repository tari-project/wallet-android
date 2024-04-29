package com.tari.android.wallet.ui.dialog.modular.modules.addressPoisoning.adapter

import com.tari.android.wallet.R
import com.tari.android.wallet.databinding.ItemSimilarAddressBinding
import com.tari.android.wallet.ui.common.recyclerView.CommonViewHolder
import com.tari.android.wallet.ui.common.recyclerView.ViewHolderBuilder
import com.tari.android.wallet.ui.extension.setVisible
import com.tari.android.wallet.ui.extension.string
import com.tari.android.wallet.ui.fragment.contact_book.address_poisoning.SimilarAddressItem
import com.tari.android.wallet.util.EmojiUtil
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class SimilarAddressItemViewHolder(view: ItemSimilarAddressBinding) : CommonViewHolder<SimilarAddressItem, ItemSimilarAddressBinding>(view) {

    override fun bind(item: SimilarAddressItem) {
        super.bind(item)

        ui.rootViewSelected.setVisible(item.selected)
        ui.rootViewNotSelected.setVisible(!item.selected)

        val emojiId = item.contact.getFFIDto()?.let { ffiContact ->
            EmojiUtil.getFullEmojiIdSpannable(
                emojiId = ffiContact.walletAddress.emojiId,
                separator = string(R.string.emoji_id_chunk_separator),
                darkColor = paletteManager.getBlack(itemView.context),
                lightColor = paletteManager.getLightGray(itemView.context),
            )
        } ?: "" // the contact _should_ always be an FFIContactDto, because it has a wallet address
        val contactName = item.contact.contact.getAlias()
        val numberOfTransactions = string(R.string.address_poisoning_number_of_transactions, item.numberOfTransaction)
        val lastTransactionDate = item.lastTransactionDate?.let { string(R.string.address_poisoning_last_transaction, formatLastTransactionDate(it)) }
            ?: string(R.string.address_poisoning_last_transaction_never)

        if (item.selected) {
            ui.layoutSelected.textEmojiAddress.text = emojiId
            ui.layoutSelected.textContactName.text = contactName
            ui.layoutSelected.textNumberTransactions.text = numberOfTransactions
            ui.layoutSelected.textLastTransactionDate.text = lastTransactionDate
            ui.layoutSelected.iconTrusted.setVisible(item.trusted)
        } else {
            ui.layoutNotSelected.textEmojiAddress.text = emojiId
            ui.layoutNotSelected.textContactName.text = contactName
            ui.layoutNotSelected.textNumberTransactions.text = numberOfTransactions
            ui.layoutNotSelected.textLastTransactionDate.text = lastTransactionDate
            ui.layoutNotSelected.iconTrusted.setVisible(item.trusted)
        }

        ui.divider.setVisible(!item.lastItem && !item.selected)
    }

    private fun formatLastTransactionDate(date: Date): String {
        val dateFormat = SimpleDateFormat("dd MMM yyyy", Locale.getDefault())
        return dateFormat.format(date)
    }

    companion object {
        fun getBuilder() = ViewHolderBuilder(
            createView = ItemSimilarAddressBinding::inflate,
            itemJavaClass = SimilarAddressItem::class.java,
            createViewHolder = { SimilarAddressItemViewHolder(it as ItemSimilarAddressBinding) },
        )
    }
}