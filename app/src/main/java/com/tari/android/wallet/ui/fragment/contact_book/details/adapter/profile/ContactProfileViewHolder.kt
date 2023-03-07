package com.tari.android.wallet.ui.fragment.contact_book.details.adapter.profile

import com.tari.android.wallet.databinding.ItemContactProfileBinding
import com.tari.android.wallet.ui.common.recyclerView.CommonViewHolder
import com.tari.android.wallet.ui.common.recyclerView.ViewHolderBuilder
import com.tari.android.wallet.ui.component.fullEmojiId.EmojiIdSummaryViewController
import com.tari.android.wallet.ui.extension.gone
import com.tari.android.wallet.ui.extension.setVisible
import com.tari.android.wallet.ui.fragment.contact_book.data.contacts.FFIContactDto
import com.tari.android.wallet.ui.fragment.contact_book.data.contacts.MergedContactDto
import com.tari.android.wallet.ui.fragment.contact_book.data.contacts.PhoneContactDto
import com.tari.android.wallet.ui.fragment.contact_book.data.contacts.YatContactDto
import com.tari.android.wallet.util.extractEmojis

class ContactProfileViewHolder(view: ItemContactProfileBinding) :
    CommonViewHolder<ContactProfileViewHolderItem, ItemContactProfileBinding>(view) {

    private val emojiIdSummaryController = EmojiIdSummaryViewController(ui.participantEmojiIdView)

    override fun bind(item: ContactProfileViewHolderItem) {
        super.bind(item)

        when (val dto = item.contactDto.contact) {
            is YatContactDto -> {
                showFirstChar(dto.walletAddress.emojiId.extractEmojis()[0])
                showEmojiId(dto.walletAddress.emojiId)
                showAlias(dto.localAlias)
            }

            is FFIContactDto -> {
                showFirstChar(dto.walletAddress.emojiId.extractEmojis()[0])
                showEmojiId(dto.walletAddress.emojiId)
                showAlias(dto.localAlias)
            }

            is MergedContactDto -> {
                showFirstChar(dto.ffiContactDto.walletAddress.emojiId.extractEmojis()[0])
                showEmojiId(dto.ffiContactDto.walletAddress.emojiId)
                showAlias(dto.phoneContactDto.getAlias())
            }

            is PhoneContactDto -> {
                showFirstChar(dto.getAlias().firstOrNull()?.toString() ?: "C")
                showEmojiId("")
                showAlias(dto.getAlias())
            }
        }
    }

    private fun showFirstChar(firstChar: String) {
        ui.firstEmojiTextView.text = firstChar
    }

    private fun showEmojiId(emojiId: String) {
        if (emojiId.isEmpty()) {
            ui.participantEmojiIdView.emojiIdSummaryContainer.gone()
            return
        }
        emojiIdSummaryController.display(emojiId)
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

