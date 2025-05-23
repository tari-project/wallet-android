package com.tari.android.wallet.ui.screen.contactBook.link.adapter.linkHeader

import android.app.Activity
import androidx.appcompat.widget.SearchView
import com.tari.android.wallet.databinding.ItemContactLinkHeaderBinding
import com.tari.android.wallet.ui.common.recyclerView.CommonViewHolder
import com.tari.android.wallet.ui.common.recyclerView.ViewHolderBuilder
import com.tari.android.wallet.util.extension.showKeyboard
import com.tari.android.wallet.util.addressFirstEmojis
import com.tari.android.wallet.util.addressLastEmojis
import com.tari.android.wallet.util.addressPrefixEmojis

class ContactLinkHeaderViewHolder(view: ItemContactLinkHeaderBinding) :
    CommonViewHolder<ContactLinkHeaderViewHolderItem, ItemContactLinkHeaderBinding>(view) {

    override fun bind(item: ContactLinkHeaderViewHolderItem) {
        super.bind(item)

        ui.searchView.setOnQueryTextListener(object : SearchView.OnQueryTextListener {
            override fun onQueryTextSubmit(query: String?): Boolean = true

            override fun onQueryTextChange(newText: String?): Boolean {
                item.searchAction.invoke(newText.orEmpty())
                return true
            }
        })

        ui.searchView.setIconifiedByDefault(false)

        ui.searchView.requestFocus()
        (ui.searchView.context as? Activity)?.showKeyboard(ui.searchView)

        ui.emojiIdViewContainer.textViewEmojiPrefix.text = item.walletAddress.addressPrefixEmojis()
        ui.emojiIdViewContainer.textViewEmojiFirstPart.text = item.walletAddress.addressFirstEmojis()
        ui.emojiIdViewContainer.textViewEmojiLastPart.text = item.walletAddress.addressLastEmojis()
    }

    companion object {
        fun getBuilder(): ViewHolderBuilder =
            ViewHolderBuilder(
                ItemContactLinkHeaderBinding::inflate,
                ContactLinkHeaderViewHolderItem::class.java
            ) { ContactLinkHeaderViewHolder(it as ItemContactLinkHeaderBinding) }
    }
}