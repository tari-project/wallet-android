package com.tari.android.wallet.ui.fragment.settings.allSettings.myProfile

import com.tari.android.wallet.databinding.ItemMyProfileBinding
import com.tari.android.wallet.ui.common.recyclerView.CommonViewHolder
import com.tari.android.wallet.ui.common.recyclerView.ViewHolderBuilder
import com.tari.android.wallet.ui.extension.setVisible
import com.tari.android.wallet.util.addressFirstEmojis
import com.tari.android.wallet.util.addressLastEmojis
import com.tari.android.wallet.util.addressPrefixEmojis

class MyProfileViewHolder(view: ItemMyProfileBinding) : CommonViewHolder<MyProfileViewHolderItem, ItemMyProfileBinding>(view) {

    override fun bind(item: MyProfileViewHolderItem) {
        super.bind(item)
        ui.alias.text = item.alias
        ui.alias.setVisible(item.alias.isNotBlank())
        ui.emojiIdViewContainer.textViewEmojiPrefix.text = item.address.addressPrefixEmojis()
        ui.emojiIdViewContainer.textViewEmojiFirstPart.text = item.address.addressFirstEmojis()
        ui.emojiIdViewContainer.textViewEmojiLastPart.text = item.address.addressLastEmojis()
        ui.root.setOnClickListener { item.action.invoke() }
    }

    companion object {
        fun getBuilder(): ViewHolderBuilder = ViewHolderBuilder(ItemMyProfileBinding::inflate, MyProfileViewHolderItem::class.java) {
            MyProfileViewHolder(it as ItemMyProfileBinding)
        }
    }
}