package com.tari.android.wallet.ui.fragment.tx.adapter

import androidx.annotation.StringRes
import com.bumptech.glide.Glide
import com.bumptech.glide.load.resource.bitmap.RoundedCorners
import com.bumptech.glide.load.resource.drawable.DrawableTransitionOptions
import com.bumptech.glide.request.RequestOptions
import com.bumptech.glide.request.target.Target
import com.tari.android.wallet.R
import com.tari.android.wallet.databinding.ItemHomeTxListBinding
import com.tari.android.wallet.extension.applyFontStyle
import com.tari.android.wallet.model.*
import com.tari.android.wallet.ui.common.gyphy.presentation.GIFStateConsumer
import com.tari.android.wallet.ui.common.gyphy.presentation.GlideGIFListener
import com.tari.android.wallet.ui.common.gyphy.repository.GIFItem
import com.tari.android.wallet.ui.common.recyclerView.CommonViewHolder
import com.tari.android.wallet.ui.common.recyclerView.ViewHolderBuilder
import com.tari.android.wallet.ui.component.CustomFont
import com.tari.android.wallet.ui.component.EmojiIdSummaryViewController
import com.tari.android.wallet.ui.extension.*
import com.tari.android.wallet.model.TxNote
import com.tari.android.wallet.util.WalletUtil
import com.tari.android.wallet.util.extractEmojis
import org.joda.time.DateTime
import org.joda.time.Hours
import org.joda.time.LocalDate
import org.joda.time.Minutes
import java.util.*

class TxListViewHolder(view: ItemHomeTxListBinding) : CommonViewHolder<TransactionItem, ItemHomeTxListBinding>(view), GIFStateConsumer {

    private val glide = Glide.with(itemView.context)
    private val emojiIdSummaryController = EmojiIdSummaryViewController(ui.participantEmojiIdView)
    private lateinit var tx: Tx

    init {
        ui.gifContainer.loadingGifProgressBar.setColor(color(R.color.tx_list_loading_gif_gray))
    }

    override fun bind(item: TransactionItem) {
        super.bind(item)

        setContentTopMargin()
        with(item.tx) {
            this@TxListViewHolder.tx = this
            displayFirstEmoji(this)
            displayAliasOrEmojiId(this)
            displayAmount(this)
            displayDate(this)
            displayStatus(this)
            displayMessage(this)
        }

        item.viewModel.onNewTxNote(tx.message)
        ui.gifContainer.retryLoadingGifTextView.setOnClickListener { item.viewModel.retry() }
        item.viewModel.gifState.observeForever { it.handle(this) }
    }

    private fun setContentTopMargin() {
        ui.firstEmojiShadowImageView.setTopMargin(dimen(R.dimen.tx_list_item_emoji_text_view_shadow_normal_top_margin))
        ui.firstEmojiTextView.setTopMargin(dimen(R.dimen.tx_list_item_emoji_text_view_normal_top_margin))
        ui.contentContainerView.setTopMargin(dimen(R.dimen.tx_list_item_content_container_view_normal_top_margin))
    }

    private fun displayFirstEmoji(tx: Tx) {
        // display first emoji of emoji id
        val firstEmoji =
            if (tx.isOneSided) string(R.string.tx_list_emoji_one_side_payment_placeholder) else tx.user.walletAddress.emojiId.extractEmojis()[0]
        ui.firstEmojiTextView.text = firstEmoji
    }

    private fun displayAliasOrEmojiId(tx: Tx) {
        val txUser = tx.user
        // display contact name or emoji id
        when {
            tx.isOneSided -> {
                val title = string(R.string.tx_list_someone) + " " + string(R.string.tx_list_paid_you)
                ui.participantTextView1.visible()
                ui.participantTextView2.gone()
                ui.participantEmojiIdView.root.gone()
                ui.participantTextView1.text = title
            }
            txUser is Contact -> {
                val fullText = when (tx.direction) {
                    Tx.Direction.INBOUND -> string(R.string.tx_list_sent_a_payment, txUser.alias)
                    Tx.Direction.OUTBOUND -> string(R.string.tx_list_you_paid_with_alias, txUser.alias)
                }
                ui.participantTextView1.visible()
                ui.participantTextView1.text = fullText.applyFontStyle(
                    itemView.context,
                    CustomFont.AVENIR_LT_STD_LIGHT,
                    listOf(txUser.alias),
                    CustomFont.AVENIR_LT_STD_HEAVY
                )
                ui.participantEmojiIdView.root.gone()
                ui.participantTextView2.gone()
            }
            else -> { // display emoji id
                ui.participantEmojiIdView.root.visible()
                emojiIdSummaryController.display(txUser.walletAddress.emojiId, showEmojisFromEachEnd = 2)
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
            tx is CompletedTx && tx.status == TxStatus.MINED_UNCONFIRMED -> Triple(
                when (tx.direction) {
                    Tx.Direction.OUTBOUND -> "-$amount"
                    Tx.Direction.INBOUND -> "+$amount"
                },
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

    private fun showStatusTextView(@StringRes messageId: Int) =
        showStatusTextView(string(messageId))

    private fun showStatusTextView(status: String) {
        ui.statusTextView.visible()
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
            ViewHolderBuilder(ItemHomeTxListBinding::inflate, TransactionItem::class.java) { TxListViewHolder(it as ItemHomeTxListBinding) }

        // e.g. Wed, Jun 2
        private const val dateFormat = "E, MMM d"
    }
}