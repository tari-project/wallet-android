package com.tari.android.wallet.ui.screen.contactBook.contacts.adapter.contact

import com.tari.android.wallet.databinding.ItemContactBinding
import com.tari.android.wallet.model.TariWalletAddress
import com.tari.android.wallet.ui.common.recyclerView.CommonViewHolder
import com.tari.android.wallet.ui.common.recyclerView.ViewHolderBuilder
import com.tari.android.wallet.util.extension.gone
import com.tari.android.wallet.util.extension.setVisible
import com.tari.android.wallet.util.extension.visible
import com.tari.android.wallet.data.contacts.model.FFIContactInfo
import com.tari.android.wallet.data.contacts.model.MergedContactInfo
import com.tari.android.wallet.data.contacts.model.PhoneContactInfo
import com.tari.android.wallet.util.addressFirstEmojis
import com.tari.android.wallet.util.addressLastEmojis
import com.tari.android.wallet.util.addressPrefixEmojis

class ContactItemViewHolder(view: ItemContactBinding) : CommonViewHolder<ContactItemViewHolderItem, ItemContactBinding>(view) {

    override fun bind(item: ContactItemViewHolderItem) {
        super.bind(item)

        when (val contactInfo = item.contact.contactInfo) {
            is FFIContactInfo -> {
                if (contactInfo.getAlias().isEmpty()) {
                    displayEmojiId(contactInfo.walletAddress)
                } else {
                    displayAlias(contactInfo.getAlias())
                }
            }

            is MergedContactInfo -> {
                displayAlias(contactInfo.phoneContactInfo.getAlias())
            }

            is PhoneContactInfo -> {
                displayAlias(contactInfo.getAlias())
            }
        }

        ui.contactIconType.setImageResource(item.contact.getTypeIcon())
        ui.starred.setVisible(item.contact.contactInfo.isFavorite)
        ui.checkbox.setVisible(item.isSelectionState && item.contact.getFFIContactInfo() != null)
        ui.checkbox.isChecked = item.isSelected
    }

    private fun displayAlias(alias: String) {
        ui.alias.text = alias
        ui.alias.visible()
        ui.emojiIdViewContainer.root.gone()
    }

    private fun displayEmojiId(address: TariWalletAddress) {
        ui.emojiIdViewContainer.root.visible()
        ui.alias.gone()
        ui.emojiIdViewContainer.textViewEmojiPrefix.text = address.addressPrefixEmojis()
        ui.emojiIdViewContainer.textViewEmojiFirstPart.text = address.addressFirstEmojis()
        ui.emojiIdViewContainer.textViewEmojiLastPart.text = address.addressLastEmojis()
    }

    companion object {
        fun getBuilder(): ViewHolderBuilder =
            ViewHolderBuilder(ItemContactBinding::inflate, ContactItemViewHolderItem::class.java) { ContactItemViewHolder(it as ItemContactBinding) }
    }
}