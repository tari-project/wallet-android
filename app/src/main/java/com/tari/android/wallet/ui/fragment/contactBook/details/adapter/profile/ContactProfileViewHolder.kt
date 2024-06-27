package com.tari.android.wallet.ui.fragment.contactBook.details.adapter.profile

import android.net.Uri
import com.tari.android.wallet.databinding.ItemContactProfileBinding
import com.tari.android.wallet.ui.common.recyclerView.CommonViewHolder
import com.tari.android.wallet.ui.common.recyclerView.ViewHolderBuilder
import com.tari.android.wallet.ui.component.fullEmojiId.EmojiIdWithYatSummaryViewController
import com.tari.android.wallet.ui.extension.gone
import com.tari.android.wallet.ui.extension.setVisible
import com.tari.android.wallet.ui.fragment.contactBook.data.contacts.FFIContactInfo
import com.tari.android.wallet.ui.fragment.contactBook.data.contacts.MergedContactInfo
import com.tari.android.wallet.ui.fragment.contactBook.data.contacts.PhoneContactInfo
import com.tari.android.wallet.ui.fragment.contactBook.data.contacts.YatDto
import com.tari.android.wallet.util.containsNonEmoji
import com.tari.android.wallet.util.extractEmojis

class ContactProfileViewHolder(view: ItemContactProfileBinding) :
    CommonViewHolder<ContactProfileViewHolderItem, ItemContactProfileBinding>(view) {

    private val emojiIdSummaryController = EmojiIdWithYatSummaryViewController(ui.participantEmojiIdView)

    override fun bind(item: ContactProfileViewHolderItem) {
        super.bind(item)

        ui.participantEmojiIdView.emojiIdSummaryContainerView.setOnClickListener {
            item.show()
        }

        item.init(ui.participantEmojiIdView)

        when (val contactInfo = item.contactDto.contactInfo) {
            is FFIContactInfo -> {
                showFirstCharOrAvatar(contactInfo.walletAddress.emojiId.extractEmojis()[0])
                showEmojiId(contactInfo.walletAddress.emojiId)
                showAlias(contactInfo.getAlias())
            }

            is MergedContactInfo -> {
                val yat = item.contactDto.yatDto?.yat.orEmpty()
                emojiIdSummaryController.yat = yat
                showFirstCharOrAvatar(contactInfo.ffiContactInfo.walletAddress.emojiId.extractEmojis()[0], contactInfo.phoneContactInfo.avatar)
                showEmojiId(contactInfo.ffiContactInfo.walletAddress.emojiId)
                showAlias(contactInfo.phoneContactInfo.getAlias())
                showYat(item.contactDto.yatDto)
            }

            is PhoneContactInfo -> {
                showFirstCharOrAvatar(contactInfo.getAlias().firstOrNull()?.toString() ?: "C", contactInfo.avatar)
                showEmojiId("")
                showAlias(contactInfo.getAlias())
                showYat(item.contactDto.yatDto)
            }
        }
    }

    private fun showYat(yatDto: YatDto?) = yatDto?.let { dto ->
        val extracted = dto.yat.extractEmojis()
        if (dto.yat.isNotEmpty() && !dto.yat.containsNonEmoji() && extracted.isNotEmpty()) {
            showFirstCharOrAvatar(extracted[0])
            emojiIdSummaryController.yat = dto.yat
        }
    }

    private fun showFirstCharOrAvatar(firstChar: String, avatar: String? = null) {
        ui.firstEmojiTextView.text = firstChar
        if (avatar.orEmpty().isNotEmpty()) {
            ui.avatar.setImageURI(Uri.parse(avatar))
        }
        ui.firstEmojiTextView.setVisible(avatar.isNullOrEmpty())
        ui.avatar.setVisible(avatar.orEmpty().isNotEmpty())
    }

    private fun showEmojiId(emojiId: String) {
        if (emojiId.isEmpty()) {
            ui.participantEmojiIdView.root.gone()
            return
        }
        emojiIdSummaryController.emojiId = emojiId
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

