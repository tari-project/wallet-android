package com.tari.android.wallet.ui.fragment.contact_book.link.adapter.link_header

import android.app.Activity
import androidx.appcompat.widget.SearchView
import com.tari.android.wallet.R
import com.tari.android.wallet.databinding.ItemContactLinkHeaderBinding
import com.tari.android.wallet.ui.common.recyclerView.CommonViewHolder
import com.tari.android.wallet.ui.common.recyclerView.ViewHolderBuilder
import com.tari.android.wallet.ui.extension.showKeyboard

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

        ui.searchView.requestFocus()
        (ui.searchView.context as? Activity)?.showKeyboard(ui.searchView)

        ui.linkContactMessage.text = itemView.context.getString(R.string.contact_book_contacts_book_link_message, item.walletAddress.emojiId)
    }

    companion object {
        fun getBuilder(): ViewHolderBuilder =
            ViewHolderBuilder(
                ItemContactLinkHeaderBinding::inflate,
                ContactLinkHeaderViewHolderItem::class.java
            ) { ContactLinkHeaderViewHolder(it as ItemContactLinkHeaderBinding) }
    }
}