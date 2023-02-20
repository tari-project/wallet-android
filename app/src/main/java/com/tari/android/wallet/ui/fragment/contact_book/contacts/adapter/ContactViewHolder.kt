package com.tari.android.wallet.ui.fragment.contact_book.contacts.adapter

import com.tari.android.wallet.databinding.ItemContactBinding
import com.tari.android.wallet.model.Contact
import com.tari.android.wallet.ui.common.recyclerView.CommonViewHolder
import com.tari.android.wallet.ui.common.recyclerView.ViewHolderBuilder
import com.tari.android.wallet.ui.component.fullEmojiId.EmojiIdSummaryViewController
import com.tari.android.wallet.ui.extension.gone
import com.tari.android.wallet.ui.extension.setVisible
import com.tari.android.wallet.ui.extension.visible
import com.tari.android.wallet.ui.fragment.contact_book.data.ContactDto
import com.tari.android.wallet.util.extractEmojis
import com.tari.android.wallet.yat.YatUser

class ContactViewHolder(view: ItemContactBinding) : CommonViewHolder<ContactItem, ItemContactBinding>(view) {

    private val emojiIdSummaryController = EmojiIdSummaryViewController(ui.participantEmojiIdView)

    override fun bind(item: ContactItem) {
        super.bind(item)

        with(item.contact) {
            displayFirstEmoji(this)
            displayAliasOrEmojiId(this)
            ui.starred.setVisible(item.contact.isFavorite)
        }
    }

    private fun displayFirstEmoji(contactDto: ContactDto) {
        // display first emoji of emoji id
        val firstEmoji = contactDto.user.walletAddress.emojiId.extractEmojis()[0]
        ui.firstEmojiTextView.text = firstEmoji
    }

    private fun displayAliasOrEmojiId(contactDto: ContactDto) {
        // display contact name or emoji id
        when (val txUser = contactDto.user) {
            is Contact -> {
                val alias = txUser.alias
                ui.alias.text = alias
                ui.alias.visible()
                ui.participantEmojiIdView.root.gone()
            }

            is YatUser -> {
                val alias = txUser.yat
                ui.alias.text = alias
                ui.alias.visible()
                ui.participantEmojiIdView.root.gone()
            }

            else -> {
                ui.participantEmojiIdView.root.visible()
                ui.alias.gone()
                emojiIdSummaryController.display(txUser.walletAddress.emojiId, showEmojisFromEachEnd = 3)
            }
        }
    }

    companion object {
        fun getBuilder(): ViewHolderBuilder =
            ViewHolderBuilder(ItemContactBinding::inflate, ContactItem::class.java) { ContactViewHolder(it as ItemContactBinding) }
    }
}