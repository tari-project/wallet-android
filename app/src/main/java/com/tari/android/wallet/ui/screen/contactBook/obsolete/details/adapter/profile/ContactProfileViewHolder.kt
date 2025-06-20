package com.tari.android.wallet.ui.screen.contactBook.obsolete.details.adapter.profile

import android.view.View
import com.tari.android.wallet.R
import com.tari.android.wallet.databinding.ItemContactProfileBinding
import com.tari.android.wallet.ui.common.recyclerView.CommonViewHolder
import com.tari.android.wallet.ui.common.recyclerView.ViewHolderBuilder
import com.tari.android.wallet.util.addressFirstEmojis
import com.tari.android.wallet.util.addressLastEmojis
import com.tari.android.wallet.util.addressPrefixEmojis
import com.tari.android.wallet.util.extension.drawable
import com.tari.android.wallet.util.extension.setVisible

class ContactProfileViewHolder(view: ItemContactProfileBinding) :
    CommonViewHolder<ContactProfileViewHolderItem, ItemContactProfileBinding>(view) {

    private var showYat = false

    override fun bind(item: ContactProfileViewHolderItem) {
        super.bind(item)

        // Show yat first if wallet address is null
//        showYat = item.contact.walletAddress == null && item.contact.yat != null

        ui.alias.setVisible(item.contact.alias.orEmpty().isNotEmpty())
        ui.alias.text = item.contact.alias

//        ui.emojiIdSummaryContainer.setVisible(item.contact.walletAddress != null || item.contact.yat != null)
        ui.emojiIdSummaryContainer.setVisible(true)
        ui.emojiIdView.textViewEmojiPrefix.text = item.contact.walletAddress.addressPrefixEmojis()
        ui.emojiIdView.textViewEmojiFirstPart.text = item.contact.walletAddress.addressFirstEmojis()
        ui.emojiIdView.textViewEmojiLastPart.text = item.contact.walletAddress.addressLastEmojis()
//        ui.yatAddressText.text = item.contact.yat

        ui.yatButton.setVisible(false)
        ui.yatButton.setOnClickListener {
            showYat = !showYat
            setAddressVisibility(showYat)
        }
        setAddressVisibility(showYat)

        item.contact.walletAddress.let { address ->
            ui.emojiIdSummaryContainerView.setOnClickListener { item.onAddressClick(address) }
        }
    }

    private fun setAddressVisibility(showYat: Boolean) {
        ui.emojiIdAddressText.setVisible(!showYat, View.INVISIBLE)
        ui.yatAddressText.setVisible(showYat, View.INVISIBLE)
        ui.yatButton.setImageDrawable(drawable(if (showYat) R.drawable.vector_tari_yat_open else R.drawable.vector_tari_yat_close))
    }

    companion object {
        fun getBuilder(): ViewHolderBuilder = ViewHolderBuilder(ItemContactProfileBinding::inflate, ContactProfileViewHolderItem::class.java) {
            ContactProfileViewHolder(it as ItemContactProfileBinding)
        }
    }
}

