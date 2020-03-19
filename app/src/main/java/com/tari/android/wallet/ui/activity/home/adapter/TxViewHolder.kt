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
package com.tari.android.wallet.ui.activity.home.adapter

import android.graphics.drawable.Drawable
import android.view.View
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import butterknife.*
import com.tari.android.wallet.R
import com.tari.android.wallet.model.Contact
import com.tari.android.wallet.model.Tx
import com.tari.android.wallet.ui.component.EmojiIdSummaryViewController
import com.tari.android.wallet.ui.util.UiUtil
import com.tari.android.wallet.util.WalletUtil
import java.lang.ref.WeakReference

/**
 * Transaction view holder class.
 *
 * @author The Tari Development Team
 */
class TxViewHolder(view: View, listener: Listener) :
    RecyclerView.ViewHolder(view),
    View.OnClickListener {

    @BindView(R.id.home_tx_list_item_img_icon)
    lateinit var iconImageView: ImageView
    @BindView(R.id.home_tx_list_item_txt_contact_alias)
    lateinit var aliasTextView: TextView
    @BindView(R.id.home_tx_list_item_vw_emoji_summary)
    lateinit var emojiIdSummaryView: View
    @BindView(R.id.home_tx_list_item_txt_message)
    lateinit var messageTextView: TextView
    @BindView(R.id.home_tx_list_item_txt_amount)
    lateinit var amountTextView: TextView
    @BindDrawable(R.drawable.home_tx_value_positive_bg)
    lateinit var positiveBgDrawable: Drawable
    @BindColor(R.color.home_tx_value_positive)
    @JvmField
    var positiveColor: Int = 0
    @BindDrawable(R.drawable.home_tx_value_negative_bg)
    lateinit var negativeBgDrawable: Drawable
    @BindColor(R.color.home_tx_value_negative)
    @JvmField
    var negativeColor: Int = 0

    private lateinit var txWR: WeakReference<Tx>
    private var emojiIdSummaryController: EmojiIdSummaryViewController
    private var listenerWR: WeakReference<Listener>

    init {
        ButterKnife.bind(this, view)
        emojiIdSummaryController = EmojiIdSummaryViewController(emojiIdSummaryView)
        listenerWR = WeakReference(listener)
    }

    @OnClick(R.id.home_tx_list_item_vw_root)
    override fun onClick(view: View) {
        UiUtil.temporarilyDisableClick(view)
        listenerWR.get()?.onTxSelected(txWR.get()!!)
    }

    fun bind(tx: Tx) {
        txWR = WeakReference(tx)
        // display contact alias or user emoji id
        val txUser = tx.user
        if (txUser is Contact) {
            aliasTextView.visibility = View.VISIBLE
            aliasTextView.text = txUser.alias
            emojiIdSummaryView.visibility = View.GONE
        } else {
            aliasTextView.visibility = View.GONE
            emojiIdSummaryView.visibility = View.VISIBLE
            emojiIdSummaryController.display(
                txUser.publicKey.emojiId
            )
        }

        // display message
        messageTextView.text = tx.message
        // display value
        if (tx.direction == Tx.Direction.INBOUND) {
            val formattedValue = "+" + WalletUtil.amountFormatter.format(tx.amount.tariValue)
            amountTextView.text = formattedValue
            amountTextView.setTextColor(positiveColor)
            amountTextView.background = positiveBgDrawable
        } else {
            val formattedValue = "-" + WalletUtil.amountFormatter.format(tx.amount.tariValue)
            amountTextView.text = formattedValue
            amountTextView.setTextColor(negativeColor)
            amountTextView.background = negativeBgDrawable
        }
        val measure = amountTextView.paint.measureText("0".repeat(amountTextView.text.length))
        val totalPadding = amountTextView.paddingStart + amountTextView.paddingEnd
        amountTextView.width = totalPadding + measure.toInt()
    }

    interface Listener {

        fun onTxSelected(tx: Tx)

    }

}