package com.tari.android.wallet.ui.fragment.contactBook.details.adapter.profile

import com.tari.android.wallet.databinding.ItemContactProfileBinding
import com.tari.android.wallet.model.TariWalletAddress
import com.tari.android.wallet.ui.common.recyclerView.CommonViewHolder
import com.tari.android.wallet.ui.common.recyclerView.ViewHolderBuilder
import com.tari.android.wallet.ui.extension.gone
import com.tari.android.wallet.ui.extension.setVisible
import com.tari.android.wallet.ui.extension.visible
import com.tari.android.wallet.ui.fragment.contactBook.data.contacts.YatDto
import com.tari.android.wallet.util.addressFirstEmojis
import com.tari.android.wallet.util.addressLastEmojis
import com.tari.android.wallet.util.addressPrefixEmojis
import com.tari.android.wallet.util.containsNonEmoji
import com.tari.android.wallet.util.extractEmojis

class ContactProfileViewHolder(view: ItemContactProfileBinding) :
    CommonViewHolder<ContactProfileViewHolderItem, ItemContactProfileBinding>(view) {

    override fun bind(item: ContactProfileViewHolderItem) {
        super.bind(item)

        showEmojiId(item.contactDto.walletAddress)
        showAlias(item.contactDto.contactInfo.getAlias())
        showYat(item.contactDto.yatDto)

        item.contactDto.walletAddress?.let { address ->
            ui.emojiIdSummaryContainerView.setOnClickListener { item.onAddressClick(address) }
        }
    }

    private fun showYat(yatDto: YatDto?) = yatDto?.let { dto ->
        val extracted = dto.yat.extractEmojis()
        if (dto.yat.isNotEmpty() && !dto.yat.containsNonEmoji() && extracted.isNotEmpty()) {
            // TODO show Yat
        }
    }

    private fun showEmojiId(walletAddress: TariWalletAddress?) {
        walletAddress?.let {
            ui.emojiIdSummaryContainer.visible()
            ui.emojiIdView.textViewEmojiPrefix.text = it.addressPrefixEmojis()
            ui.emojiIdView.textViewEmojiFirstPart.text = it.addressFirstEmojis()
            ui.emojiIdView.textViewEmojiLastPart.text = it.addressLastEmojis()
        } ?: ui.emojiIdSummaryContainer.gone()
    }

    private fun showAlias(alias: String) {
        if (alias.isEmpty()) {
            ui.alias.gone()
            return
        }
        ui.alias.text = alias
        ui.alias.setVisible(alias.isNotEmpty())
    }

    companion object {
        fun getBuilder(): ViewHolderBuilder = ViewHolderBuilder(ItemContactProfileBinding::inflate, ContactProfileViewHolderItem::class.java) {
            ContactProfileViewHolder(it as ItemContactProfileBinding)
        }
    }
}

