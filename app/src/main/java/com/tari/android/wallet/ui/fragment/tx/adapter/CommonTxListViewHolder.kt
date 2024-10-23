package com.tari.android.wallet.ui.fragment.tx.adapter

import androidx.viewbinding.ViewBinding
import com.tari.android.wallet.R
import com.tari.android.wallet.application.walletManager.WalletConfig
import com.tari.android.wallet.databinding.ViewAddressShortSmallBinding
import com.tari.android.wallet.extension.makeTextBold
import com.tari.android.wallet.model.CancelledTx
import com.tari.android.wallet.model.CompletedTx
import com.tari.android.wallet.model.PendingInboundTx
import com.tari.android.wallet.model.PendingOutboundTx
import com.tari.android.wallet.model.Tx
import com.tari.android.wallet.model.TxStatus
import com.tari.android.wallet.ui.common.domain.PaletteManager
import com.tari.android.wallet.ui.common.recyclerView.CommonViewHolder
import com.tari.android.wallet.ui.common.recyclerView.CommonViewHolderItem
import com.tari.android.wallet.ui.component.tari.TariTextView
import com.tari.android.wallet.ui.component.tari.background.TariRoundBackground
import com.tari.android.wallet.ui.extension.gone
import com.tari.android.wallet.ui.extension.string
import com.tari.android.wallet.ui.extension.visible
import com.tari.android.wallet.ui.fragment.contactBook.data.contacts.ContactDto
import com.tari.android.wallet.util.addressFirstEmojis
import com.tari.android.wallet.util.addressLastEmojis
import com.tari.android.wallet.util.addressPrefixEmojis
import org.joda.time.DateTime
import org.joda.time.Hours
import org.joda.time.LocalDate
import org.joda.time.Minutes
import java.util.Locale

abstract class CommonTxListViewHolder<T : CommonViewHolderItem, VB : ViewBinding>(val view: VB) : CommonViewHolder<T, VB>(view) {

    protected fun displayAliasOrEmojiId(
        tx: Tx,
        contact: ContactDto?,
        participantTextView1: TariTextView,
        participantTextView2: TariTextView,
        emojiIdViewContainer: ViewAddressShortSmallBinding
    ) {
        val txUser = tx.tariContact
        // display contact name or emoji id
        when {
            tx.isCoinbase -> {
                participantTextView1.visible()
                participantTextView2.gone()
                emojiIdViewContainer.root.gone()
                participantTextView1.text = when (tx.direction) {
                    Tx.Direction.INBOUND -> string(R.string.tx_details_coinbase_inbound).makeTextBold(itemView.context)
                    Tx.Direction.OUTBOUND -> string(R.string.tx_details_coinbase_outbound)
                        .makeTextBold(itemView.context, string(R.string.tx_list_you), string(R.string.tx_list_miner))
                }
            }

            tx.isOneSided -> {
                val title = (string(R.string.tx_list_someone) + " " + string(R.string.tx_list_paid_you))
                    .makeTextBold(itemView.context, string(R.string.tx_list_you), string(R.string.tx_list_someone))
                participantTextView1.visible()
                participantTextView1.text = title
                participantTextView2.gone()
                emojiIdViewContainer.root.gone()
            }

            contact != null && contact.contactInfo.getAlias().isNotEmpty() || txUser.walletAddress.isUnknownUser() -> {
                val alias = contact?.contactInfo?.getAlias().orEmpty().ifBlank { itemView.context.getString(R.string.unknown_source) }
                val fullText = when (tx.direction) {
                    Tx.Direction.INBOUND -> string(R.string.tx_list_sent_a_payment, alias)
                    Tx.Direction.OUTBOUND -> string(R.string.tx_list_you_paid_with_alias, alias)
                }
                participantTextView1.visible()
                participantTextView1.text = fullText.makeTextBold(itemView.context, string(R.string.tx_list_you), alias)
                participantTextView2.gone()
                emojiIdViewContainer.root.gone()
            }

            else -> { // display emoji id
                emojiIdViewContainer.root.visible()
                emojiIdViewContainer.textViewEmojiPrefix.text = txUser.walletAddress.addressPrefixEmojis()
                emojiIdViewContainer.textViewEmojiFirstPart.text = txUser.walletAddress.addressFirstEmojis()
                emojiIdViewContainer.textViewEmojiLastPart.text = txUser.walletAddress.addressLastEmojis()

                when (tx.direction) {
                    Tx.Direction.INBOUND -> {
                        participantTextView1.gone()
                        participantTextView2.visible()
                        participantTextView2.text = string(R.string.tx_list_paid_you).makeTextBold(itemView.context, string(R.string.tx_list_you))
                    }

                    Tx.Direction.OUTBOUND -> {
                        participantTextView1.visible()
                        participantTextView1.text = string(R.string.tx_list_you_paid).makeTextBold(itemView.context, string(R.string.tx_list_you))
                        participantTextView2.gone()
                    }
                }
            }
        }
    }

    protected fun displayAmount(tx: Tx, amountTextView: TariTextView, amountTextViewRound: TariRoundBackground) {
        val amount = WalletConfig.amountFormatter.format(tx.amount.tariValue)
        val context = itemView.context
        val (amountText, textColor, backgroundColor) = when {
            tx is CancelledTx -> Triple(
                amount,
                PaletteManager.getTextBody(context),
                PaletteManager.getBackgroundPrimary(context)
            )

            tx is PendingInboundTx -> Triple(
                "+$amount",
                PaletteManager.getYellow(context),
                PaletteManager.getSecondaryYellow(context)
            )

            tx is PendingOutboundTx -> Triple(
                "-$amount",
                PaletteManager.getYellow(context),
                PaletteManager.getSecondaryYellow(context)
            )

            tx is CompletedTx && tx.status == TxStatus.MINED_UNCONFIRMED -> Triple(
                when (tx.direction) {
                    Tx.Direction.OUTBOUND -> "-$amount"
                    Tx.Direction.INBOUND -> "+$amount"
                },
                PaletteManager.getYellow(context),
                PaletteManager.getSecondaryYellow(context)
            )

            tx.direction == Tx.Direction.INBOUND -> Triple(
                "+$amount",
                PaletteManager.getGreen(context),
                PaletteManager.getSecondaryGreen(context)
            )

            else -> Triple(
                "-$amount",
                PaletteManager.getRed(context),
                PaletteManager.getSecondaryRed(context)
            )
        }
        amountTextView.text = amountText
        amountTextView.setTextColor(textColor)
        amountTextViewRound.updateBack(backColor = backgroundColor)
        val measure = amountTextView.paint.measureText("0".repeat(amountTextView.text.length))
        val totalPadding = amountTextView.paddingStart + amountTextView.paddingEnd
        amountTextView.width = totalPadding + measure.toInt()
    }

    protected fun displayDate(tx: Tx, dateTextView: TariTextView) {
        val txDateTime = DateTime(tx.timestamp.toLong() * 1000L)
        val txDate = txDateTime.toLocalDate()
        val todayDate = LocalDate.now()
        val yesterdayDate = todayDate.minusDays(1)
        dateTextView.text = when {
            txDate.isEqual(todayDate) -> {
                val minutesSinceTx = Minutes.minutesBetween(txDateTime, DateTime.now()).minutes
                when {
                    minutesSinceTx == 0 -> string(R.string.tx_list_now)
                    minutesSinceTx < 60 -> String.format(string(R.string.tx_list_minutes_ago), minutesSinceTx)
                    else -> String.format(string(R.string.tx_list_hours_ago), Hours.hoursBetween(txDateTime, DateTime.now()).hours)
                }
            }

            txDate.isEqual(yesterdayDate) -> string(R.string.home_tx_list_header_yesterday)
            else -> txDate.toString(DATE_FORMAT, Locale.ENGLISH)
        }
    }

    companion object {
        // e.g. Wed, Jun 2
        private const val DATE_FORMAT = "E, MMM d"
    }
}
