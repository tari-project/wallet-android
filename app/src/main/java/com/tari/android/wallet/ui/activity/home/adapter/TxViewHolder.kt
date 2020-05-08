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

import android.view.View
import androidx.recyclerview.widget.RecyclerView
import com.tari.android.wallet.R.color.home_tx_value_negative
import com.tari.android.wallet.R.color.home_tx_value_positive
import com.tari.android.wallet.R.drawable.home_tx_value_negative_bg
import com.tari.android.wallet.R.drawable.home_tx_value_positive_bg
import com.tari.android.wallet.databinding.HomeTxListItemBinding
import com.tari.android.wallet.model.Contact
import com.tari.android.wallet.model.Tx
import com.tari.android.wallet.ui.component.EmojiIdSummaryViewController
import com.tari.android.wallet.ui.extension.color
import com.tari.android.wallet.ui.extension.drawable
import com.tari.android.wallet.ui.extension.gone
import com.tari.android.wallet.ui.extension.visible
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

    private lateinit var txWR: WeakReference<Tx>
    private var emojiIdSummaryController: EmojiIdSummaryViewController
    private var listenerWR: WeakReference<Listener>

    private val ui = HomeTxListItemBinding.bind(view)

    init {
        emojiIdSummaryController = EmojiIdSummaryViewController(ui.txItemEmojiSummaryView)
        listenerWR = WeakReference(listener)
        ui.txItemRootView.setOnClickListener(this)
    }

    override fun onClick(view: View) {
        UiUtil.temporarilyDisableClick(view)
        listenerWR.get()?.onTxSelected(txWR.get()!!)
    }

    fun bind(tx: Tx) {
        txWR = WeakReference(tx)
        // display contact alias or user emoji id
        val txUser = tx.user
        if (txUser is Contact) {
            ui.txItemContactAliasTextView.visible()
            ui.txItemContactAliasTextView.text = txUser.alias
            ui.txItemEmojiSummaryView.root.gone()
        } else {
            ui.txItemContactAliasTextView.gone()
            ui.txItemEmojiSummaryView.root.visible()
            emojiIdSummaryController.display(
                txUser.publicKey.emojiId
            )
        }

        // display message
        ui.txItemMessageTextView.text = tx.message
        // display value
        if (tx.direction == Tx.Direction.INBOUND) {
            val formattedValue = "+" + WalletUtil.amountFormatter.format(tx.amount.tariValue)
            ui.txItemAmountTextView.text = formattedValue
            ui.txItemAmountTextView.setTextColor(color(home_tx_value_positive))
            ui.txItemAmountTextView.background = drawable(home_tx_value_positive_bg)
        } else {
            val formattedValue = "-" + WalletUtil.amountFormatter.format(tx.amount.tariValue)
            ui.txItemAmountTextView.text = formattedValue
            ui.txItemAmountTextView.setTextColor(color(home_tx_value_negative))
            ui.txItemAmountTextView.background = drawable(home_tx_value_negative_bg)
        }
        val measure =
            ui.txItemAmountTextView.paint.measureText("0".repeat(ui.txItemAmountTextView.text.length))
        val totalPadding = ui.txItemAmountTextView.paddingStart + ui.txItemAmountTextView.paddingEnd
        ui.txItemAmountTextView.width = totalPadding + measure.toInt()
    }

    interface Listener {

        fun onTxSelected(tx: Tx)

    }

}
