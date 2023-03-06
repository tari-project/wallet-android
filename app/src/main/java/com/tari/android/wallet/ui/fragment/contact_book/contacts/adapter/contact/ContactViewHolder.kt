package com.tari.android.wallet.ui.fragment.contact_book.contacts.adapter.contact

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
import com.tari.android.wallet.ui.fragment.contact_book.data.contacts.YatContactDto
import com.tari.android.wallet.util.extractEmojis

class ContactViewHolder(view: ItemContactBinding) : CommonViewHolder<ContactItem, ItemContactBinding>(view) {

    private val emojiIdSummaryController = EmojiIdSummaryViewController(ui.participantEmojiIdView)

    private val badgesController: BadgesController by lazy { BadgesController(view) }

    override fun bind(item: ContactItem) {
        item.toggleBadges = { }

        super.bind(item)

        item.toggleBadges = badgesController::process
        badgesController.notifyAction = item.notifyToggling

        badgesController.bind(item)

        when (val dto = item.contact.contact) {
            is YatContactDto -> {
                displayFirstEmojiOrText(dto.walletAddress.emojiId.extractEmojis()[0])
                if (dto.localAlias.isEmpty()) {
                    displayEmojiId(dto.walletAddress.emojiId)
                } else {
                    displayAlias(dto.localAlias)
                }
            }

            is FFIContactDto -> {
                displayFirstEmojiOrText(dto.walletAddress.emojiId.extractEmojis()[0])
                if (dto.localAlias.isEmpty()) {
                    displayEmojiId(dto.walletAddress.emojiId)
                } else {
                    displayAlias(dto.localAlias)
                }
            }

            is MergedContactDto -> {
                displayFirstEmojiOrText(dto.ffiContactDto.walletAddress.emojiId.extractEmojis()[0])
                displayAlias(dto.phoneContactDto.name)
            }

            is PhoneContactDto -> {
                displayFirstEmojiOrText(dto.name.firstOrNull()?.toString() ?: "C")
                displayAlias(dto.name)
            }
        }

        ui.starred.setVisible(item.contact.isFavorite)
    }

    private fun displayFirstEmojiOrText(string: String) {
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
            ViewHolderBuilder(ItemContactBinding::inflate, ContactItem::class.java) { ContactViewHolder(it as ItemContactBinding) }
    }
}