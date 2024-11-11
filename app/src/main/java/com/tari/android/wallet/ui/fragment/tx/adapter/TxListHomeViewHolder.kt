package com.tari.android.wallet.ui.fragment.tx.adapter

import com.tari.android.wallet.databinding.ItemHomeTxListBinding
import com.tari.android.wallet.model.Tx
import com.tari.android.wallet.ui.common.recyclerView.ViewHolderBuilder

class TxListHomeViewHolder(view: ItemHomeTxListBinding) : CommonTxListViewHolder<TxViewHolderItem, ItemHomeTxListBinding>(view) {

    private lateinit var tx: Tx

    override fun bind(item: TxViewHolderItem) {
        super.bind(item)

        with(item.txDto.tx) {
            this@TxListHomeViewHolder.tx = this
            displayAliasOrEmojiId(this, item.txDto.contact, ui.participantTextView1, ui.participantTextView2, ui.emojiIdViewContainer)
            displayAmount(this, ui.amountTextView, ui.amountTextViewRound)
            displayDate(this, ui.dateTextView)
        }

        item.gifViewModel.onNewTx(tx)
    }

    companion object {
        fun getBuilder(): ViewHolderBuilder =
            ViewHolderBuilder(ItemHomeTxListBinding::inflate, TxViewHolderItem::class.java) { TxListHomeViewHolder(it as ItemHomeTxListBinding) }
    }
}