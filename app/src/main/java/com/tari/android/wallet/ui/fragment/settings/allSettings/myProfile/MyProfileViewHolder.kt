package com.tari.android.wallet.ui.fragment.settings.allSettings.myProfile

import com.tari.android.wallet.databinding.ItemMyProfileBinding
import com.tari.android.wallet.ui.common.recyclerView.CommonViewHolder
import com.tari.android.wallet.ui.common.recyclerView.ViewHolderBuilder
import com.tari.android.wallet.ui.component.fullEmojiId.EmojiIdSummaryViewController
import com.tari.android.wallet.ui.extension.setVisible
import com.tari.android.wallet.util.extractEmojis

class MyProfileViewHolder(view: ItemMyProfileBinding) :
    CommonViewHolder<MyProfileViewHolderItem, ItemMyProfileBinding>(view) {

    private val emojiIdSummaryController = EmojiIdSummaryViewController(ui.participantEmojiIdView)
    private val yatController = EmojiIdSummaryViewController(ui.participantYatIdView)

    override fun bind(item: MyProfileViewHolderItem) {
        super.bind(item)
        ui.firstEmojiTextView.text = item.emojiId.extractEmojis()[0]
        ui.alias.text = item.alias
        ui.alias.setVisible(item.alias.isNotBlank())
        ui.participantYatIdView.root.setVisible(item.yat.isNotBlank())
        yatController.display(item.yat)
        emojiIdSummaryController.display(item.emojiId)
        ui.root.setOnClickListener { item.action.invoke() }
    }

    companion object {
        fun getBuilder(): ViewHolderBuilder = ViewHolderBuilder(ItemMyProfileBinding::inflate, MyProfileViewHolderItem::class.java) {
            MyProfileViewHolder(it as ItemMyProfileBinding)
        }
    }
}