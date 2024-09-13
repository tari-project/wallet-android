package com.tari.android.wallet.ui.fragment.contactBook.details.adapter.profile

import android.view.View
import com.tari.android.wallet.R
import com.tari.android.wallet.databinding.ItemContactProfileBinding
import com.tari.android.wallet.ui.common.recyclerView.CommonViewHolder
import com.tari.android.wallet.ui.common.recyclerView.ViewHolderBuilder
import com.tari.android.wallet.ui.extension.drawable
import com.tari.android.wallet.ui.extension.setVisible
import com.tari.android.wallet.util.addressFirstEmojis
import com.tari.android.wallet.util.addressLastEmojis
import com.tari.android.wallet.util.addressPrefixEmojis

class ContactProfileViewHolder(view: ItemContactProfileBinding) :
    CommonViewHolder<ContactProfileViewHolderItem, ItemContactProfileBinding>(view) {

    private var showYat = false

    override fun bind(item: ContactProfileViewHolderItem) {
        super.bind(item)

        // Show yat first if wallet address is null
        showYat = item.contactDto.walletAddress == null && item.contactDto.yat != null

        ui.alias.setVisible(item.contactDto.alias.isNotEmpty())
        ui.alias.text = item.contactDto.alias

        ui.emojiIdSummaryContainer.setVisible(item.contactDto.walletAddress != null || item.contactDto.yat != null)
        ui.emojiIdView.textViewEmojiPrefix.text = item.contactDto.walletAddress?.addressPrefixEmojis()
        ui.emojiIdView.textViewEmojiFirstPart.text = item.contactDto.walletAddress?.addressFirstEmojis()
        ui.emojiIdView.textViewEmojiLastPart.text = item.contactDto.walletAddress?.addressLastEmojis()
        ui.yatAddressText.text = item.contactDto.yat

        ui.yatButton.setVisible(item.contactDto.walletAddress != null && item.contactDto.yat != null)
        ui.yatButton.setOnClickListener {
            showYat = !showYat
            setAddressVisibility(showYat)
        }
        setAddressVisibility(showYat)

        item.contactDto.walletAddress?.let { address ->
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

