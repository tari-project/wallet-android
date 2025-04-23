package com.tari.android.wallet.ui.screen.tx.adapter

import android.content.Context
import com.bumptech.glide.Glide
import com.bumptech.glide.load.resource.bitmap.RoundedCorners
import com.bumptech.glide.load.resource.drawable.DrawableTransitionOptions
import com.bumptech.glide.request.RequestOptions
import com.bumptech.glide.request.target.Target
import com.tari.android.wallet.R
import com.tari.android.wallet.databinding.ItemTxListBinding
import com.tari.android.wallet.model.TxNote
import com.tari.android.wallet.model.TxStatus.BROADCAST
import com.tari.android.wallet.model.TxStatus.COINBASE
import com.tari.android.wallet.model.TxStatus.COINBASE_CONFIRMED
import com.tari.android.wallet.model.TxStatus.COINBASE_NOT_IN_BLOCKCHAIN
import com.tari.android.wallet.model.TxStatus.COINBASE_UNCONFIRMED
import com.tari.android.wallet.model.TxStatus.COMPLETED
import com.tari.android.wallet.model.TxStatus.IMPORTED
import com.tari.android.wallet.model.TxStatus.MINED_CONFIRMED
import com.tari.android.wallet.model.TxStatus.MINED_UNCONFIRMED
import com.tari.android.wallet.model.TxStatus.ONE_SIDED_CONFIRMED
import com.tari.android.wallet.model.TxStatus.ONE_SIDED_UNCONFIRMED
import com.tari.android.wallet.model.TxStatus.PENDING
import com.tari.android.wallet.model.TxStatus.QUEUED
import com.tari.android.wallet.model.TxStatus.REJECTED
import com.tari.android.wallet.model.TxStatus.TX_NULL_ERROR
import com.tari.android.wallet.model.TxStatus.UNKNOWN
import com.tari.android.wallet.model.tx.CancelledTx
import com.tari.android.wallet.model.tx.Tx
import com.tari.android.wallet.model.tx.Tx.Direction.INBOUND
import com.tari.android.wallet.model.tx.Tx.Direction.OUTBOUND
import com.tari.android.wallet.ui.common.giphy.presentation.GifStateConsumer
import com.tari.android.wallet.ui.common.giphy.presentation.GlideGifListener
import com.tari.android.wallet.ui.common.giphy.repository.GifItem
import com.tari.android.wallet.ui.common.recyclerView.ViewHolderBuilder
import com.tari.android.wallet.util.extension.dimen
import com.tari.android.wallet.util.extension.gone
import com.tari.android.wallet.util.extension.setTopMargin
import com.tari.android.wallet.util.extension.setVisible
import com.tari.android.wallet.util.extension.string
import com.tari.android.wallet.util.extension.visible

class TxListViewHolder(view: ItemTxListBinding) : CommonTxListViewHolder<TxViewHolderItem, ItemTxListBinding>(view), GifStateConsumer {

    private val glide = Glide.with(itemView.context)
    private lateinit var tx: Tx

    override fun bind(item: TxViewHolderItem) {
        super.bind(item)

        with(item.txDto.tx) {
            this@TxListViewHolder.tx = this
            displayAliasOrEmojiId(this, item.txDto.contact, ui.participantTextView1, ui.participantTextView2, ui.emojiIdViewContainer)
            displayAmount(this, ui.amountTextView, ui.amountTextViewRound)
            displayDate(this, ui.dateTextView)
            displayStatus(this)
            displayMessage(this)
        }

        item.gifViewModel.onNewTx(tx)
        ui.gifContainer.retryLoadingGifTextView.setOnClickListener { item.gifViewModel.retry() }
        item.gifViewModel.gifState.observeForever { it.handle(this) }
    }

    private fun displayStatus(tx: Tx) {
        val status = tx.statusString(context = itemView.context)
        ui.statusTextView.setVisible(status.isNotEmpty())
        ui.statusTextView.text = status
    }

    private fun displayMessage(tx: Tx) {
        val note = TxNote.fromTx(tx)
        if (note.message.isNullOrBlank() || tx.isCoinbase) { // FIXME: we temporarily hide the message for coinbase txs because it's always "None"
            ui.messageTextView.gone()
        } else {
            ui.messageTextView.visible()
            ui.messageTextView.text = note.message
        }
    }

    private fun Tx.statusString(context: Context): String {
        return if (this is CancelledTx) "" else when (this.status) {
            PENDING -> when (this.direction) {
                INBOUND -> context.string(R.string.tx_detail_waiting_for_sender_to_complete)
                OUTBOUND -> context.string(R.string.tx_detail_waiting_for_recipient)
            }

            BROADCAST, COMPLETED, MINED_UNCONFIRMED -> context.string(R.string.tx_detail_in_progress)

            TX_NULL_ERROR, IMPORTED, COINBASE, MINED_CONFIRMED, REJECTED, ONE_SIDED_UNCONFIRMED, ONE_SIDED_CONFIRMED, QUEUED, COINBASE_UNCONFIRMED,
            COINBASE_CONFIRMED, COINBASE_NOT_IN_BLOCKCHAIN, UNKNOWN -> ""
        }
    }

    override fun onGifLoadingState() {
        glide.clear(ui.gifContainer.gifView)
        ui.gifContainer.gifStatusContainer.visible()
        ui.gifContainer.loadingGifTextView.visible()
        ui.gifContainer.retryLoadingGifTextView.gone()
        ui.gifContainer.loadingGifProgressBar.visible()
        ui.gifContainer.gifView.gone()
        ui.gifContainer.gifView.setTopMargin(0)
    }

    override fun onGifErrorState() {
        ui.gifContainer.gifStatusContainer.visible()
        ui.gifContainer.loadingGifProgressBar.gone()
        ui.gifContainer.loadingGifTextView.gone()
        ui.gifContainer.retryLoadingGifTextView.visible()
    }

    override fun onGifSuccessState(gifItem: GifItem) {
        glide
            .asGif()
            .override(ui.gifContainer.gifContainerRootView.width, Target.SIZE_ORIGINAL)
            .apply(RequestOptions().transform(RoundedCorners(10)))
            .load(gifItem.uri)
            .listener(GlideGifListener(this))
            .transition(DrawableTransitionOptions.withCrossFade(250))
            .into(ui.gifContainer.gifView)
    }

    override fun onGifResourceReady() {
        ui.gifContainer.gifStatusContainer.gone()
        ui.gifContainer.gifView.visible()
        ui.gifContainer.gifView.setTopMargin(dimen(R.dimen.tx_list_item_gif_container_top_margin))
    }

    override fun noGifState() {
        glide.clear(ui.gifContainer.gifView)
        ui.gifContainer.gifView.gone()
        ui.gifContainer.gifStatusContainer.gone()
    }

    companion object {
        fun getBuilder(): ViewHolderBuilder =
            ViewHolderBuilder(ItemTxListBinding::inflate, TxViewHolderItem::class.java) { TxListViewHolder(it as ItemTxListBinding) }
    }
}