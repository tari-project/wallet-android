package com.tari.android.wallet.ui.fragment.tx.adapter

import com.tari.android.wallet.R
import com.tari.android.wallet.databinding.ItemHomeTxListBinding
import com.tari.android.wallet.extension.applyFontStyle
import com.tari.android.wallet.model.CancelledTx
import com.tari.android.wallet.model.CompletedTx
import com.tari.android.wallet.model.PendingInboundTx
import com.tari.android.wallet.model.PendingOutboundTx
import com.tari.android.wallet.model.Tx
import com.tari.android.wallet.model.TxStatus
import com.tari.android.wallet.ui.common.recyclerView.CommonViewHolder
import com.tari.android.wallet.ui.common.recyclerView.ViewHolderBuilder
import com.tari.android.wallet.ui.component.tari.TariFont
import com.tari.android.wallet.ui.extension.gone
import com.tari.android.wallet.ui.extension.string
import com.tari.android.wallet.ui.extension.visible
import com.tari.android.wallet.ui.fragment.contact_book.data.contacts.ContactDto
import com.tari.android.wallet.util.WalletUtil
import com.tari.android.wallet.util.extractEmojis
import org.joda.time.DateTime
import org.joda.time.Hours
import org.joda.time.LocalDate
import org.joda.time.Minutes
import java.util.Locale

class TxListHomeViewHolder(view: ItemHomeTxListBinding) : CommonViewHolder<TransactionItem, ItemHomeTxListBinding>(view) {

    private lateinit var tx: Tx

    override fun bind(item: TransactionItem) {
        super.bind(item)

        with(item.tx) {
            this@TxListHomeViewHolder.tx = this
            displayAliasOrEmojiId(this, item.contact)
            displayAmount(this)
            displayDate(this)
        }

        item.viewModel.onNewTxNote(tx.message)
    }

    private fun displayAliasOrEmojiId(tx: Tx, contact: ContactDto?) {
        val txUser = tx.tariContact
        // display contact name or emoji id
        when {
            tx.isOneSided -> {
                val title = string(R.string.tx_list_someone) + " " + string(R.string.tx_list_paid_you)
                ui.participantTextView1.visible()
                ui.participantTextView2.gone()
                ui.participantEmojiIdView.gone()
                ui.participantTextView1.text = title
            }

            contact != null && contact.contact.getAlias().isNotEmpty() || txUser.walletAddress.isZeros() -> {
                val alias = contact?.contact?.getAlias().orEmpty().ifBlank { itemView.context.getString(R.string.unknown_source) }
                val fullText = when (tx.direction) {
                    Tx.Direction.INBOUND -> string(R.string.tx_list_sent_a_payment, alias)
                    Tx.Direction.OUTBOUND -> string(R.string.tx_list_you_paid_with_alias, alias)
                }
                ui.participantTextView1.visible()
                ui.participantTextView1.text = fullText.applyFontStyle(
                    itemView.context,
                    TariFont.AVENIR_LT_STD_LIGHT,
                    listOf(alias),
                    TariFont.AVENIR_LT_STD_HEAVY
                )
                ui.participantEmojiIdView.gone()
                ui.participantTextView2.gone()
            }

            else -> { // display emoji id
                ui.participantEmojiIdView.visible()
                ui.participantEmojiIdView.text = txUser.walletAddress.emojiId.extractEmojis().take(3).joinToString("")
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
    }

    private fun displayAmount(tx: Tx) {
        val amount = WalletUtil.amountFormatter.format(tx.amount.tariValue)
        val context = itemView.context
        val (amountText, textColor, backgroundColor) = when {
            tx is CancelledTx -> Triple(
                amount,
                paletteManager.getTextBody(context),
                paletteManager.getBackgroundPrimary(context)
            )

            tx is PendingInboundTx -> Triple(
                "+$amount",
                paletteManager.getYellow(context),
                paletteManager.getSecondaryYellow(context)
            )

            tx is PendingOutboundTx -> Triple(
                "-$amount",
                paletteManager.getYellow(context),
                paletteManager.getSecondaryYellow(context)
            )

            tx is CompletedTx && tx.status == TxStatus.MINED_UNCONFIRMED -> Triple(
                when (tx.direction) {
                    Tx.Direction.OUTBOUND -> "-$amount"
                    Tx.Direction.INBOUND -> "+$amount"
                },
                paletteManager.getYellow(context),
                paletteManager.getSecondaryYellow(context)
            )

            tx.direction == Tx.Direction.INBOUND -> Triple(
                "+$amount",
                paletteManager.getGreen(context),
                paletteManager.getSecondaryGreen(context)
            )

            else -> Triple(
                "-$amount",
                paletteManager.getRed(context),
                paletteManager.getSecondaryRed(context)
            )
        }
        ui.amountTextView.text = amountText
        ui.amountTextView.setTextColor(textColor)
        ui.amountTextViewRound.updateBack(backColor = backgroundColor)
        val measure = ui.amountTextView.paint.measureText("0".repeat(ui.amountTextView.text.length))
        val totalPadding = ui.amountTextView.paddingStart + ui.amountTextView.paddingEnd
        ui.amountTextView.width = totalPadding + measure.toInt()
    }

    private fun displayDate(tx: Tx) {
        val txDateTime = DateTime(tx.timestamp.toLong() * 1000L)
        val txDate = txDateTime.toLocalDate()
        val todayDate = LocalDate.now()
        val yesterdayDate = todayDate.minusDays(1)
        ui.dateTextView.text = when {
            txDate.isEqual(todayDate) -> {
                val minutesSinceTx = Minutes.minutesBetween(txDateTime, DateTime.now()).minutes
                when {
                    minutesSinceTx == 0 -> string(R.string.tx_list_now)
                    minutesSinceTx < 60 -> String.format(string(R.string.tx_list_minutes_ago), minutesSinceTx)
                    else -> String.format(string(R.string.tx_list_hours_ago), Hours.hoursBetween(txDateTime, DateTime.now()).hours)
                }
            }

            txDate.isEqual(yesterdayDate) -> string(R.string.home_tx_list_header_yesterday)
            else -> txDate.toString(dateFormat, Locale.ENGLISH)
        }
    }

    companion object {
        fun getBuilder(): ViewHolderBuilder =
            ViewHolderBuilder(ItemHomeTxListBinding::inflate, TransactionItem::class.java) { TxListHomeViewHolder(it as ItemHomeTxListBinding) }

        // e.g. Wed, Jun 2
        private const val dateFormat = "E, MMM d"
    }
}