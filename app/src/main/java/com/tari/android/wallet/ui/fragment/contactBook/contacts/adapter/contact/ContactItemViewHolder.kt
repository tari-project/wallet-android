package com.tari.android.wallet.ui.fragment.contactBook.contacts.adapter.contact

import android.Manifest.permission.READ_CONTACTS
import android.content.pm.PackageManager
import com.tari.android.wallet.databinding.ItemContactBinding
import com.tari.android.wallet.model.TariWalletAddress
import com.tari.android.wallet.ui.common.recyclerView.CommonViewHolder
import com.tari.android.wallet.ui.common.recyclerView.ViewHolderBuilder
import com.tari.android.wallet.ui.extension.gone
import com.tari.android.wallet.ui.extension.setVisible
import com.tari.android.wallet.ui.extension.visible
import com.tari.android.wallet.ui.fragment.contactBook.data.contacts.FFIContactInfo
import com.tari.android.wallet.ui.fragment.contactBook.data.contacts.MergedContactInfo
import com.tari.android.wallet.ui.fragment.contactBook.data.contacts.PhoneContactInfo
import com.tari.android.wallet.util.addressFirstEmojis
import com.tari.android.wallet.util.addressLastEmojis
import com.tari.android.wallet.util.addressPrefixEmojis
import com.tari.android.wallet.util.containsNonEmoji
import com.tari.android.wallet.util.extractEmojis

class ContactItemViewHolder(view: ItemContactBinding) : CommonViewHolder<ContactItem, ItemContactBinding>(view) {

//    private val badgesController: BadgesController by lazy { BadgesController(view) }

    override fun bind(item: ContactItem) {
        super.bind(item)
//        badgesController.bind(item)

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
                displayYat(item.contact.yatDto?.yat.orEmpty())
            }

            is PhoneContactInfo -> {
                displayAlias(contactInfo.getAlias())
                displayYat(item.contact.yatDto?.yat.orEmpty())
            }
        }

        ui.contactIconType.setImageResource(item.contact.getTypeIcon())
        ui.starred.setVisible(item.contact.contactInfo.isFavorite)
        ui.checkbox.setVisible(item.isSelectionState && item.contact.getFFIContactInfo() != null)
        ui.checkbox.isChecked = item.isSelected
    }

    private fun displayYat(yat: String) {
        val extracted = yat.extractEmojis()
        if (yat.isNotEmpty() && !yat.containsNonEmoji() && extracted.isNotEmpty()) {
            // TODO show Yat? There were no code for showing Yat address
        }
    }

    private fun displayAlias(alias: String) {
        ui.alias.text = alias
        ui.alias.visible()
        ui.emojiIdViewContainer.gone()
    }

    private fun displayEmojiId(address: TariWalletAddress) {
        ui.emojiIdViewContainer.visible()
        ui.alias.gone()
        ui.textViewEmojiPrefix.text = address.addressPrefixEmojis()
        ui.textViewEmojiFirstPart.text = address.addressFirstEmojis()
        ui.textViewEmojiLastPart.text = address.addressLastEmojis()
    }

    companion object {
        fun getBuilder(): ViewHolderBuilder =
            ViewHolderBuilder(ItemContactBinding::inflate, ContactItem::class.java) { ContactItemViewHolder(it as ItemContactBinding) }
    }
}