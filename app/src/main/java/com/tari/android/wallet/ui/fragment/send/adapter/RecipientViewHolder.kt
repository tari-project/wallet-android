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
package com.tari.android.wallet.ui.fragment.send.adapter

import android.view.View
import androidx.recyclerview.widget.RecyclerView
import com.tari.android.wallet.databinding.AddRecipientListItemBinding
import com.tari.android.wallet.model.Contact
import com.tari.android.wallet.model.User
import com.tari.android.wallet.ui.component.EmojiIdSummaryViewController
import com.tari.android.wallet.ui.extension.gone
import com.tari.android.wallet.ui.extension.temporarilyDisableClick
import com.tari.android.wallet.ui.extension.visible
import java.lang.ref.WeakReference
import java.util.*

/**
 * Tx recipient view holder class.
 *
 * @author The Tari Development Team
 */
class RecipientViewHolder(view: View, listener: Listener) :
    RecyclerView.ViewHolder(view), View.OnClickListener {

    private val ui = AddRecipientListItemBinding.bind(view)
    private val listenerWR: WeakReference<Listener> = WeakReference(listener)
    private var emojiIdSummaryController = EmojiIdSummaryViewController(ui.emojiSummaryView)
    private lateinit var userWR: WeakReference<User>

    init {
        ui.rootView.setOnClickListener(this)
    }

    override fun onClick(view: View) {
        view.temporarilyDisableClick()
        listenerWR.get()?.onRecipientSelected(userWR.get()!!)
    }

    fun bind(user: User) {
        userWR = WeakReference(user)
        if (user is Contact) {
            ui.aliasTextView.visible()
            ui.emojiSummaryView.root.gone()
            ui.profileIconImageView.gone()
            ui.initialTextView.visible()

            ui.initialTextView.text = user.alias.take(1).toUpperCase(Locale.getDefault())
            ui.aliasTextView.text = user.alias
        } else {
            ui.aliasTextView.gone()
            ui.emojiSummaryView.root.visible()
            ui.profileIconImageView.visible()
            ui.initialTextView.gone()

            emojiIdSummaryController.display(
                user.publicKey.emojiId
            )
        }
    }

    interface Listener {

        fun onRecipientSelected(recipient: User)

    }

}
