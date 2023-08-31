package com.tari.android.wallet.ui.fragment.contact_book.contacts.adapter.contact

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
import com.tari.android.wallet.ui.fragment.contact_book.contacts.adapter.contact.badges.BadgesController
import com.tari.android.wallet.ui.fragment.contact_book.data.contacts.FFIContactDto
import com.tari.android.wallet.ui.fragment.contact_book.data.contacts.MergedContactDto
import com.tari.android.wallet.ui.fragment.contact_book.data.contacts.PhoneContactDto
import com.tari.android.wallet.util.containsNonEmoji
import com.tari.android.wallet.util.extractEmojis

class ContactItemViewHolder(view: ItemContactBinding) : CommonViewHolder<ContactItem, ItemContactBinding>(view) {

    private val emojiIdSummaryController = EmojiIdSummaryViewController(ui.participantEmojiIdView)

    private val badgesController: BadgesController by lazy { BadgesController(view) }

    override fun bind(item: ContactItem) {
        super.bind(item)
        badgesController.bind(item)

        when (val dto = item.contact.contact) {
            is FFIContactDto -> {
                displayFirstEmojiOrText(dto.walletAddress.emojiId.extractEmojis()[0])
                if (dto.getAlias().isEmpty()) {
                    displayEmojiId(dto.walletAddress.emojiId)
                } else {
                    displayAlias(dto.getAlias())
                }
            }

            is MergedContactDto -> {
                if (dto.phoneContactDto.avatar.isNotEmpty() && hasContactPermission()) displayAvatar(dto.phoneContactDto.avatar) else
                    displayFirstEmojiOrText(dto.ffiContactDto.walletAddress.emojiId.extractEmojis()[0])
                displayAlias(dto.phoneContactDto.getAlias())
                displayYat(dto.phoneContactDto.yat)
            }

            is PhoneContactDto -> {
                if (dto.avatar.isNotEmpty() && hasContactPermission()) displayAvatar(dto.avatar) else {
                    var name = ""
                    dto.firstName.firstOrNull()?.let { name += it }
                    dto.surname.firstOrNull()?.let { name += it }
                    displayFirstEmojiOrText(name.ifEmpty { "C" })
                }
                displayAlias(dto.getAlias())
                displayYat(dto.yat)
            }
        }

        ui.contactIconType.setImageResource(item.contact.getTypeIcon())
        ui.starred.setVisible(item.contact.contact.isFavorite)
        ui.checkbox.setVisible(item.isSelectionState && item.contact.getFFIDto() != null)
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