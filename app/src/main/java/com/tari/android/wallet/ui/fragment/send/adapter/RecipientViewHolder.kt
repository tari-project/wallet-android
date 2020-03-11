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
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import butterknife.BindView
import butterknife.ButterKnife
import butterknife.OnClick
import com.tari.android.wallet.R
import com.tari.android.wallet.model.Contact
import com.tari.android.wallet.model.User
import com.tari.android.wallet.ui.component.EmojiIdSummaryViewController
import com.tari.android.wallet.ui.util.UiUtil
import java.lang.ref.WeakReference
import java.util.*

/**
 * Tx recipient view holder class.
 *
 * @author The Tari Development Team
 */
class RecipientViewHolder(view: View, listener: Listener) :
    RecyclerView.ViewHolder(view),
    View.OnClickListener {

    @BindView(R.id.add_recipient_list_item_txt_initial)
    lateinit var initialTextView: TextView
    @BindView(R.id.add_recipient_list_item_img_profile_icon)
    lateinit var profileIconImageView: ImageView
    @BindView(R.id.add_recipient_list_item_txt_alias)
    lateinit var aliasTextView: TextView
    @BindView(R.id.add_recipient_list_item_vw_emoji_summary)
    lateinit var emojiIdSummaryView: View

    private lateinit var userWR: WeakReference<User>
    private var listenerWR: WeakReference<Listener>
    private var emojiIdSummaryController: EmojiIdSummaryViewController

    init {
        ButterKnife.bind(this, view)
        listenerWR = WeakReference(listener)
        emojiIdSummaryController = EmojiIdSummaryViewController(emojiIdSummaryView)
    }

    @OnClick(R.id.add_recipient_list_item_vw_root)
    override fun onClick(view: View) {
        UiUtil.temporarilyDisableClick(view)
        listenerWR.get()?.onRecipientSelected(userWR.get()!!)
    }

    fun bind(user: User) {
        userWR = WeakReference(user)
        if (user is Contact) {
            aliasTextView.visibility = View.VISIBLE
            emojiIdSummaryView.visibility = View.GONE
            profileIconImageView.visibility = View.GONE
            initialTextView.visibility = View.VISIBLE

            initialTextView.text = user.alias.take(1).toUpperCase(Locale.getDefault())
            aliasTextView.text = user.alias
        } else {
            aliasTextView.visibility = View.GONE
            emojiIdSummaryView.visibility = View.VISIBLE
            profileIconImageView.visibility = View.VISIBLE
            initialTextView.visibility = View.GONE

            emojiIdSummaryController.display(
                user.publicKey.emojiId
            )
        }
    }

    interface Listener {

        fun onRecipientSelected(recipient: User)

    }

}