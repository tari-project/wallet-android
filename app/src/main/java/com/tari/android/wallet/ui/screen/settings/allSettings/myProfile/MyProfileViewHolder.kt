package com.tari.android.wallet.ui.screen.settings.allSettings.myProfile

import com.tari.android.wallet.databinding.ItemMyProfileBinding
import com.tari.android.wallet.ui.common.recyclerView.CommonViewHolder
import com.tari.android.wallet.ui.common.recyclerView.ViewHolderBuilder
import com.tari.android.wallet.util.extension.setVisible
import com.tari.android.wallet.util.DebugConfig
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
        ui.textViewYat.setVisible(item.yat.isNotBlank() && DebugConfig.isYatEnabled)
        ui.textViewYat.text = item.yat
        ui.root.setOnClickListener { item.action.invoke() }
    }

    companion object {
        fun getBuilder(): ViewHolderBuilder = ViewHolderBuilder(ItemMyProfileBinding::inflate, MyProfileViewHolderItem::class.java) {
            MyProfileViewHolder(it as ItemMyProfileBinding)
        }
    }
}