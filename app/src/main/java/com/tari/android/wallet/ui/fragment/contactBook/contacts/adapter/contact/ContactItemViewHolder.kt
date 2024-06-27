package com.tari.android.wallet.ui.fragment.contactBook.contacts.adapter.contact

import android.Manifest.permission.READ_CONTACTS
import android.content.pm.PackageManager
import android.net.Uri
import com.tari.android.wallet.databinding.ItemContactBinding
import com.tari.android.wallet.ui.common.recyclerView.CommonViewHolder
import com.tari.android.wallet.ui.common.recyclerView.ViewHolderBuilder
import com.tari.android.wallet.ui.component.fullEmojiId.EmojiIdSummaryViewController
import com.tari.android.wallet.ui.extension.gone
import com.tari.android.wallet.ui.extension.setVisible
import com.tari.android.wallet.ui.extension.visible
import com.tari.android.wallet.ui.fragment.contactBook.contacts.adapter.contact.badges.BadgesController
import com.tari.android.wallet.ui.fragment.contactBook.data.contacts.FFIContactInfo
import com.tari.android.wallet.ui.fragment.contactBook.data.contacts.MergedContactInfo
import com.tari.android.wallet.ui.fragment.contactBook.data.contacts.PhoneContactInfo
import com.tari.android.wallet.util.containsNonEmoji
import com.tari.android.wallet.util.extractEmojis

class ContactItemViewHolder(view: ItemContactBinding) : CommonViewHolder<ContactItem, ItemContactBinding>(view) {

    private val emojiIdSummaryController = EmojiIdSummaryViewController(ui.participantEmojiIdView)

    private val badgesController: BadgesController by lazy { BadgesController(view) }

    override fun bind(item: ContactItem) {
        super.bind(item)
        badgesController.bind(item)

        when (val contactInfo = item.contact.contactInfo) {
            is FFIContactInfo -> {
                displayFirstEmojiOrText(contactInfo.walletAddress.emojiId.extractEmojis()[0])
                if (contactInfo.getAlias().isEmpty()) {
                    displayEmojiId(contactInfo.walletAddress.emojiId)
                } else {
                    displayAlias(contactInfo.getAlias())
                }
            }

            is MergedContactInfo -> {
                if (contactInfo.phoneContactInfo.avatar.isNotEmpty() && hasContactPermission()) displayAvatar(contactInfo.phoneContactInfo.avatar) else
                    displayFirstEmojiOrText(contactInfo.ffiContactInfo.walletAddress.emojiId.extractEmojis()[0])
                displayAlias(contactInfo.phoneContactInfo.getAlias())
                displayYat(item.contact.yatDto?.yat.orEmpty())
            }

            is PhoneContactInfo -> {
                if (contactInfo.avatar.isNotEmpty() && hasContactPermission()) displayAvatar(contactInfo.avatar) else {
                    var name = ""
                    contactInfo.firstName.firstOrNull()?.let { name += it }
                    contactInfo.lastName.firstOrNull()?.let { name += it }
                    displayFirstEmojiOrText(name.ifEmpty { "C" })
                }
                displayAlias(contactInfo.getAlias())
                displayYat(item.contact.yatDto?.yat.orEmpty())
            }
        }

        ui.contactIconType.setImageResource(item.contact.getTypeIcon())
        ui.starred.setVisible(item.contact.contactInfo.isFavorite)
        ui.checkbox.setVisible(item.isSelectionState && item.contact.getFFIContactInfo() != null)
        ui.checkbox.isChecked = item.isSelected
    }

    private fun hasContactPermission(): Boolean = ui.root.context.checkSelfPermission(READ_CONTACTS) == PackageManager.PERMISSION_GRANTED

    private fun displayYat(yat: String) {
        val extracted = yat.extractEmojis()
        if (yat.isNotEmpty() && !yat.containsNonEmoji() && extracted.isNotEmpty()) {
            ui.firstEmojiTextView.text = extracted[0]
        }
    }

    private fun displayAvatar(url: String) {
        ui.avatar.setImageURI(Uri.parse(url))
        ui.avatar.visible()
        ui.firstEmojiTextView.gone()
    }

    private fun displayFirstEmojiOrText(string: String) {
        ui.firstEmojiTextView.visible()
        ui.avatar.gone()
        ui.firstEmojiTextView.text = string
    }

    private fun displayAlias(alias: String) {
        ui.alias.text = alias
        ui.alias.visible()
        ui.participantEmojiIdView.root.gone()
    }

    private fun displayEmojiId(emojiId: String) {
        ui.participantEmojiIdView.root.visible()
        ui.alias.gone()
        emojiIdSummaryController.display(emojiId, showEmojisFromEachEnd = 3)
    }

    companion object {
        fun getBuilder(): ViewHolderBuilder =
            ViewHolderBuilder(ItemContactBinding::inflate, ContactItem::class.java) { ContactItemViewHolder(it as ItemContactBinding) }
    }
}