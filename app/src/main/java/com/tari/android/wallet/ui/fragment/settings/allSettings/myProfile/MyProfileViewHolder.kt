package com.tari.android.wallet.ui.fragment.settings.allSettings.myProfile

import com.tari.android.wallet.databinding.ItemMyProfileBinding
import com.tari.android.wallet.ui.common.recyclerView.CommonViewHolder
import com.tari.android.wallet.ui.common.recyclerView.ViewHolderBuilder
import com.tari.android.wallet.ui.component.fullEmojiId.EmojiIdSummaryViewController
import com.tari.android.wallet.ui.fragment.contact_book.details.adapter.profile.ContactProfileViewHolder
import com.tari.android.wallet.ui.fragment.contact_book.details.adapter.profile.ContactProfileViewHolderItem
import com.tari.android.wallet.util.extractEmojis

class MyProfileViewHolder(view: ItemMyProfileBinding) :
    CommonViewHolder<MyProfileViewHolderItem, ItemMyProfileBinding>(view) {

    private val emojiIdSummaryController = EmojiIdSummaryViewController(ui.participantEmojiIdView)

    override fun bind(item: MyProfileViewHolderItem) {
        super.bind(item)
        ui.firstEmojiTextView.text = item.emojiId.extractEmojis()[0]
        emojiIdSummaryController.display(item.emojiId)
        ui.root.setOnClickListener { item.action.invoke() }
    }

    companion object {
        fun getBuilder(): ViewHolderBuilder = ViewHolderBuilder(ItemMyProfileBinding::inflate, MyProfileViewHolderItem::class.java) {
            MyProfileViewHolder(it as ItemMyProfileBinding)
        }
    }
}