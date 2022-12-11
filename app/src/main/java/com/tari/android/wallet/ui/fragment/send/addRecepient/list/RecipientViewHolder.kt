/**
 * Copyright 2020 The Tari Project
 *
 * Redistribution and use in source and binary forms, with or
 * without modification, are permitted provided that the
 * following conditions are met:
 *
 * 1. Redistributions of source code must retain the above copyright notice,
 * this list of conditions and the following disclaimer.
 *
 * 2. Redistributions in binary form must reproduce the above
 * copyright notice, this list of conditions and the following disclaimer in the
 * documentation and/or other materials provided with the distribution.
 *
 * 3. Neither the name of the copyright holder nor the names of
 * its contributors may be used to endorse or promote products
 * derived from this software without specific prior written permission.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND
 * CONTRIBUTORS "AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES,
 * INCLUDING, BUT NOT LIMITED TO, THE IMPLIED WARRANTIES
 * OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 * DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDER OR
 * CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL,
 * SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT
 * NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES;
 * LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION)
 * HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN
 * CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE
 * OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS
 * SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */
package com.tari.android.wallet.ui.fragment.send.addRecepient.list

import com.tari.android.wallet.R
import com.tari.android.wallet.databinding.ItemAddRecipientListBinding
import com.tari.android.wallet.model.Contact
import com.tari.android.wallet.ui.common.recyclerView.CommonViewHolder
import com.tari.android.wallet.ui.common.recyclerView.ViewHolderBuilder
import com.tari.android.wallet.ui.component.fullEmojiId.EmojiIdSummaryViewController
import com.tari.android.wallet.ui.extension.setVisible
import com.tari.android.wallet.yat.YatUser

class RecipientViewHolder(view: ItemAddRecipientListBinding) : CommonViewHolder<RecipientViewHolderItem, ItemAddRecipientListBinding>(view) {

    private var emojiIdSummaryController = EmojiIdSummaryViewController(ui.emojiSummaryView)

    override fun bind(item: RecipientViewHolderItem) {
        super.bind(item)

        val isContact = item.user is Contact
        ui.aliasTextView.setVisible(isContact)
        ui.emojiSummaryView.root.setVisible(!isContact)
        ui.profileIconImageView.setVisible(!isContact)
        ui.initialTextView.setVisible(isContact)

        when (item.user) {
            is Contact -> {
                ui.initialTextView.text = item.user.alias.take(1).uppercase()
                ui.aliasTextView.text = item.user.alias
            }
            is YatUser -> {
                ui.profileIconImageView.setImageResource(R.drawable.yat_logo)
                emojiIdSummaryController.display(item.user.walletAddress.emojiId)
            }
            else -> {
                ui.profileIconImageView.setImageResource(R.drawable.recipient_profile_icon)
                emojiIdSummaryController.display(item.user.walletAddress.emojiId)
            }
        }
    }

    companion object {
        fun getBuilder(): ViewHolderBuilder = ViewHolderBuilder(ItemAddRecipientListBinding::inflate, RecipientViewHolderItem::class.java) {
            RecipientViewHolder(it as ItemAddRecipientListBinding)
        }
    }
}