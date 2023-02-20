package com.tari.android.wallet.ui.fragment.contact_book.details.adapter.profile

import com.tari.android.wallet.databinding.ItemContactProfileBinding
import com.tari.android.wallet.model.Contact
import com.tari.android.wallet.ui.common.recyclerView.CommonViewHolder
import com.tari.android.wallet.ui.common.recyclerView.ViewHolderBuilder
import com.tari.android.wallet.ui.component.fullEmojiId.EmojiIdSummaryViewController
import com.tari.android.wallet.ui.extension.setVisible
import com.tari.android.wallet.util.extractEmojis

class ContactProfileViewHolder(view: ItemContactProfileBinding) :
    CommonViewHolder<ContactProfileViewHolderItem, ItemContactProfileBinding>(view) {

    private val emojiIdSummaryController = EmojiIdSummaryViewController(ui.participantEmojiIdView)

    override fun bind(item: ContactProfileViewHolderItem) {
        super.bind(item)
        val emojiId = item.contactDto.user.walletAddress.emojiId
        ui.firstEmojiTextView.text = emojiId.extractEmojis()[0]
        val alias = (item.contactDto.user as? Contact)?.alias.orEmpty()
        ui.alias.text = alias
        ui.alias.setVisible(alias.isNotEmpty())
        emojiIdSummaryController.display(emojiId)
    }

    companion object {
        fun getBuilder(): ViewHolderBuilder = ViewHolderBuilder(ItemContactProfileBinding::inflate, ContactProfileViewHolderItem::class.java) {
            ContactProfileViewHolder(it as ItemContactProfileBinding)
        }
    }
}