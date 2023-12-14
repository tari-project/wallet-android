package com.tari.android.wallet.ui.fragment.tx.adapter

import android.net.Uri
import androidx.annotation.StringRes
import com.bumptech.glide.Glide
import com.bumptech.glide.load.resource.bitmap.RoundedCorners
import com.bumptech.glide.load.resource.drawable.DrawableTransitionOptions
import com.bumptech.glide.request.RequestOptions
import com.bumptech.glide.request.target.Target
import com.tari.android.wallet.R
import com.tari.android.wallet.databinding.ItemTxListBinding
import com.tari.android.wallet.extension.applyFontStyle
import com.tari.android.wallet.model.CancelledTx
import com.tari.android.wallet.model.CompletedTx
import com.tari.android.wallet.model.PendingInboundTx
import com.tari.android.wallet.model.PendingOutboundTx
import com.tari.android.wallet.model.Tx
import com.tari.android.wallet.model.TxNote
import com.tari.android.wallet.model.TxStatus
import com.tari.android.wallet.ui.common.gyphy.presentation.GIFStateConsumer
import com.tari.android.wallet.ui.common.gyphy.presentation.GlideGIFListener
import com.tari.android.wallet.ui.common.gyphy.repository.GIFItem
import com.tari.android.wallet.ui.common.recyclerView.CommonViewHolder
import com.tari.android.wallet.ui.common.recyclerView.ViewHolderBuilder
import com.tari.android.wallet.ui.component.fullEmojiId.EmojiIdSummaryViewController
import com.tari.android.wallet.ui.component.tari.TariFont
import com.tari.android.wallet.ui.extension.dimen
import com.tari.android.wallet.ui.extension.gone
import com.tari.android.wallet.ui.extension.setTopMargin
import com.tari.android.wallet.ui.extension.setVisible
import com.tari.android.wallet.ui.extension.string
import com.tari.android.wallet.ui.extension.visible
import com.tari.android.wallet.ui.fragment.contact_book.data.contacts.ContactDto
import com.tari.android.wallet.ui.fragment.contact_book.data.contacts.MergedContactDto
import com.tari.android.wallet.util.WalletUtil
import com.tari.android.wallet.util.extractEmojis
import org.joda.time.DateTime
import org.joda.time.Hours
import org.joda.time.LocalDate
import org.joda.time.Minutes
import java.util.Locale

class TxListViewHolder(view: ItemTxListBinding) : CommonViewHolder<TransactionItem, ItemTxListBinding>(view), GIFStateConsumer {

    private val glide = Glide.with(itemView.context)
    private val emojiIdSummaryController = EmojiIdSummaryViewController(ui.participantEmojiIdView)
    private lateinit var tx: Tx

    override fun bind(item: TransactionItem) {
        super.bind(item)

        with(item.tx) {
            this@TxListViewHolder.tx = this
            displayFirstEmojiOrAvatar(this, item.contact)
            displayAliasOrEmojiId(this, item.contact)
            displayAmount(this)
            displayDate(this)
            displayStatus(this)
            displayMessage(this)
        }

        item.viewModel.onNewTxNote(tx.message)
        ui.gifContainer.retryLoadingGifTextView.setOnClickListener { item.viewModel.retry() }
        item.viewModel.gifState.observeForever { it.handle(this) }
    }

    private fun displayFirstEmojiOrAvatar(tx: Tx, contact: ContactDto?) {
        val avatar = (contact?.contact as? MergedContactDto)?.phoneContactDto?.avatar.orEmpty()
        if (avatar.isEmpty()) {
            // display first emoji of emoji id
            val firstEmoji =
                if (tx.isOneSided) string(R.string.tx_list_emoji_one_side_payment_placeholder) else tx.tariContact.walletAddress.emojiId.extractEmojis()[0]
            ui.firstEmojiTextView.text = firstEmoji
        } else {
            // display avatar
            ui.avatar.setImageURI(Uri.parse(avatar))
        }
        ui.avatar.setVisible(avatar.isNotEmpty())
        ui.firstEmojiTextView.setVisible(avatar.isEmpty())
    }

    private fun displayAliasOrEmojiId(tx: Tx, contact: ContactDto?) {
        val txUser = tx.tariContact
        // display contact name or emoji id
        when {
            tx.isOneSided -> {
                val title = string(R.string.tx_list_someone) + " " + string(R.string.tx_list_paid_you)
                ui.participantTextView1.visible()
                ui.participantTextView2.gone()
                ui.participantEmojiIdView.root.gone()
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
                ui.participantEmojiIdView.root.gone()
                ui.participantTextView2.gone()
            }

            else -> { // display emoji id
                ui.participantEmojiIdView.root.visible()
                emojiIdSummaryController.display(txUser.walletAddress.emojiId, showEmojisFromEachEnd = 3)
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

    private fun displayStatus(tx: Tx) = when (tx) {
        is PendingInboundTx -> when (tx.status) {
            TxStatus.PENDING -> showStatusTextView(R.string.tx_detail_waiting_for_sender_to_complete)
            else -> showStatusTextViewFinalProcessing()
        }

        is PendingOutboundTx -> when (tx.status) {
            TxStatus.PENDING -> showStatusTextView(R.string.tx_detail_waiting_for_recipient)
            else -> showStatusTextViewFinalProcessing()
        }

        is CompletedTx -> {
            when (tx.status) {
                TxStatus.MINED_UNCONFIRMED -> showStatusTextViewFinalProcessing(tx.confirmationCount.toInt())
                else -> ui.statusTextView.gone()
            }
        }

        is CancelledTx -> showStatusTextView(R.string.tx_detail_payment_cancelled)
        else -> ui.statusTextView.gone()
    }

    private fun showStatusTextViewFinalProcessing(step: Int = 0) = showStatusTextView(
        string(R.string.tx_detail_completing_final_processing, step + 1, item!!.requiredConfirmationCount + 1)
    )

    private fun showStatusTextView(@StringRes messageId: Int) = showStatusTextView(string(messageId))

    private fun showStatusTextView(status: String) {
        ui.statusTextView.setVisible(status.isNotEmpty())
        ui.statusTextView.text = status
    }

    private fun displayMessage(tx: Tx) {
        val note = TxNote.fromNote(tx.message)
        if (note.message == null) {
            ui.messageTextView.gone()
            ui.messageTextView.text = ""
        } else {
            ui.messageTextView.visible()
            ui.messageTextView.text = if (tx.isOneSided) string(R.string.tx_list_you_received_one_side_payment) else note.message
        }
    }

    override fun onLoadingState() {
        glide.clear(ui.gifContainer.gifView)
        ui.gifContainer.gifStatusContainer.visible()
        ui.gifContainer.loadingGifTextView.visible()
        ui.gifContainer.retryLoadingGifTextView.gone()
        ui.gifContainer.loadingGifProgressBar.visible()
        ui.gifContainer.gifView.gone()
        ui.gifContainer.gifView.setTopMargin(0)
    }

    override fun onErrorState() {
        ui.gifContainer.gifStatusContainer.visible()
        ui.gifContainer.loadingGifProgressBar.gone()
        ui.gifContainer.loadingGifTextView.gone()
        ui.gifContainer.retryLoadingGifTextView.visible()
    }

    override fun onSuccessState(gifItem: GIFItem) {
        glide
            .asGif()
            .override(ui.gifContainer.gifContainerRootView.width, Target.SIZE_ORIGINAL)
            .apply(RequestOptions().transform(RoundedCorners(10)))
            .load(gifItem.uri)
            .listener(GlideGIFListener(this))
            .transition(DrawableTransitionOptions.withCrossFade(250))
            .into(ui.gifContainer.gifView)
    }

    override fun onResourceReady() {
        ui.gifContainer.gifStatusContainer.gone()
        ui.gifContainer.gifView.visible()
        ui.gifContainer.gifView.setTopMargin(dimen(R.dimen.tx_list_item_gif_container_top_margin))
    }

    override fun noGIFState() {
        glide.clear(ui.gifContainer.gifView)
        ui.gifContainer.gifView.gone()
        ui.gifContainer.gifStatusContainer.gone()
    }

    companion object {
        fun getBuilder(): ViewHolderBuilder =
            ViewHolderBuilder(ItemTxListBinding::inflate, TransactionItem::class.java) { TxListViewHolder(it as ItemTxListBinding) }

        // e.g. Wed, Jun 2
        private const val dateFormat = "E, MMM d"
    }
}