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
package com.tari.android.wallet.ui.fragment.tx.adapter

import android.view.View
import androidx.recyclerview.widget.RecyclerView
import com.tari.android.wallet.R
import com.tari.android.wallet.databinding.HomeTxListItemBinding
import com.tari.android.wallet.extension.applyFontStyle
import com.tari.android.wallet.model.*
import com.tari.android.wallet.ui.component.CustomFont
import com.tari.android.wallet.ui.component.EmojiIdSummaryViewController
import com.tari.android.wallet.ui.extension.*
import com.tari.android.wallet.ui.extension.color
import com.tari.android.wallet.ui.util.UiUtil
import com.tari.android.wallet.util.WalletUtil
import com.tari.android.wallet.util.extractEmojis
import org.joda.time.DateTime
import org.joda.time.Hours
import org.joda.time.LocalDate
import org.joda.time.Minutes

/**
 * Transaction view holder class.
 *
 * @author The Tari Development Team
 */
class TxViewHolder(view: View, private val listener: (Tx) -> Unit) :
    RecyclerView.ViewHolder(view),
    View.OnClickListener {

    private lateinit var tx: Tx
    private var emojiIdSummaryController: EmojiIdSummaryViewController

    // e.g. Wed, Jun 2
    private val dateFormat = "E, MMM d"

    private val ui = HomeTxListItemBinding.bind(view)

    init {
        emojiIdSummaryController = EmojiIdSummaryViewController(ui.participantEmojiIdView)
        ui.rootView.setOnClickListener(this)
    }

    override fun onClick(view: View) {
        UiUtil.temporarilyDisableClick(view)
        listener(tx)
    }

    fun bind(tx: Tx) {
        this.tx = tx
        displayFirstEmoji()
        displayAliasOrEmojiId()
        displayAmount()
        displayDate()
        displayStatus()
        displayMessageAndGIF()
    }

    private fun  displayFirstEmoji() {
        // display first emoji of emoji id
        ui.firstEmojiTextView.text = tx.user.publicKey.emojiId.extractEmojis()[0]
    }

    private fun displayAliasOrEmojiId() {
        val txUser = tx.user
        // display contact name or emoji id
        if (txUser is Contact) {
            ui.participantTextView1.visible()
            val fullText = String.format(
                string(R.string.tx_list_sent_a_payment),
                txUser.alias
            )
            ui.participantTextView1.text = fullText.applyFontStyle(
                itemView.context,
                CustomFont.AVENIR_LT_STD_LIGHT,
                txUser.alias,
                CustomFont.AVENIR_LT_STD_HEAVY
            )
            ui.participantEmojiIdView.root.gone()
            ui.participantTextView2.gone()
        } else { // display emoji id
            ui.participantEmojiIdView.root.visible()
            emojiIdSummaryController.display(
                txUser.publicKey.emojiId,
                showEmojisFromEachEnd = 2
            )
            when (tx.direction) {
                Tx.Direction.INBOUND -> {
                    ui.participantTextView1.gone()
                    ui.participantTextView2.visible()
                    ui.participantTextView2.text = string(R.string.tx_list_paid_you)
                    // paid you
                }
                Tx.Direction.OUTBOUND -> {
                    ui.participantTextView1.visible()
                    ui.participantTextView1.text = string(R.string.tx_list_you_paid)
                    ui.participantTextView2.gone()
                }
            }
        }
    }

    private fun displayAmount() {
        val amount = WalletUtil.amountFormatter.format(tx.amount.tariValue)
        val (amountText, textColor, background) = when {
            tx is CancelledTx -> Triple(
                amount,
                color(R.color.home_tx_value_canceled),
                drawable(R.drawable.home_tx_value_canceled_bg)!!
            )
            tx is PendingInboundTx -> Triple(
                "+$amount",
                color(R.color.home_tx_value_pending),
                drawable(R.drawable.home_tx_value_pending_bg)!!
            )
            tx is PendingOutboundTx -> Triple(
                "-$amount",
                color(R.color.home_tx_value_pending),
                drawable(R.drawable.home_tx_value_pending_bg)!!
            )
            tx.direction == Tx.Direction.INBOUND -> Triple(
                "+$amount",
                color(R.color.home_tx_value_positive),
                drawable(R.drawable.home_tx_value_positive_bg)!!
            )
            else -> Triple(
                "-$amount",
                color(R.color.home_tx_value_negative),
                drawable(R.drawable.home_tx_value_negative_bg)!!
            )
        }
        ui.amountTextView.text = amountText
        ui.amountTextView.setTextColor(textColor)
        ui.amountTextView.background = background
        val measure =
            ui.amountTextView.paint.measureText("0".repeat(ui.amountTextView.text.length))
        val totalPadding = ui.amountTextView.paddingStart + ui.amountTextView.paddingEnd
        ui.amountTextView.width = totalPadding + measure.toInt()
    }

    private fun displayDate() {
        val txDateTime = DateTime(tx.timestamp.toLong() * 1000L)
        val txDate = txDateTime.toLocalDate()
        val todayDate = LocalDate.now()
        val yesterdayDate = todayDate.minusDays(1)
        ui.dateTextView.text = when {
            txDate.isEqual(todayDate) -> {
                val minutes = Minutes.minutesBetween(txDateTime, DateTime.now()).minutes
                when {
                    minutes == 0 -> {
                        string(R.string.tx_list_now)
                    }
                    minutes < 60 -> {
                        String.format(
                            string(R.string.tx_list_minutes_ago),
                            minutes
                        )
                    }
                    else -> {
                        val hours = Hours.hoursBetween(txDateTime, DateTime.now()).hours
                        String.format(
                            string(R.string.tx_list_hours_ago),
                            hours
                        )
                    }
                }
            }
            txDate.isEqual(yesterdayDate) -> string(R.string.home_tx_list_header_yesterday)
            else -> txDate.toString(dateFormat)
        }
    }

    private fun displayStatus() {
        when (tx) {
            is PendingInboundTx -> {
                ui.statusTextView.visible()
                ui.statusTextView.text = when ((tx as PendingInboundTx).status) {
                    TxStatus.PENDING -> {
                        string(R.string.tx_detail_waiting_for_sender_to_complete)
                    }
                    else -> {
                        string(R.string.tx_detail_broadcasting)
                    }
                }
            }
            is PendingOutboundTx -> {
                ui.statusTextView.visible()
                ui.statusTextView.text = when ((tx as PendingOutboundTx).status) {
                    TxStatus.PENDING -> {
                        string(R.string.tx_detail_waiting_for_recipient)
                    }
                    else -> {
                        string(R.string.tx_detail_broadcasting)
                    }
                }
            }
            else -> {
                ui.statusTextView.gone()
            }
        }
    }

    private fun displayMessageAndGIF() {
        ui.messageTextView.text = tx.message
        // TODO load GIF here
        ui.gifContainer.gone()
    }

}