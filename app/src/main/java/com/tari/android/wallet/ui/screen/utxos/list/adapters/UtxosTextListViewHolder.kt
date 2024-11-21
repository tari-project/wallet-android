package com.tari.android.wallet.ui.screen.utxos.list.adapters

import androidx.core.content.ContextCompat
import com.tari.android.wallet.databinding.ItemUtxosTextBinding
import com.tari.android.wallet.ui.common.recyclerView.CommonViewHolder
import com.tari.android.wallet.ui.common.recyclerView.ViewHolderBuilder
import com.tari.android.wallet.util.extension.setVisible

class UtxosTextListViewHolder(view: ItemUtxosTextBinding) : CommonViewHolder<UtxosViewHolderItem, ItemUtxosTextBinding>(view) {


    override fun bind(item: UtxosViewHolderItem) {
        super.bind(item)

        val amountText = item.amount + " XTR"
        ui.amount.text = amountText
        val statusStr = if (item.showStatus) itemView.context.getString(item.status!!.text) else ""
        val dateStr = if (item.showDate) " | ${item.formattedDate} | ${item.formattedTime}" else ""
        val additionalDataText = statusStr + dateStr
        ui.additionalData.text = additionalDataText
        ui.hash.text = item.source.commitment

        val drawable = if (item.showStatus) ContextCompat.getDrawable(itemView.context, item.status!!.textIcon) else null
        ui.additionalData.setCompoundDrawablesRelativeWithIntrinsicBounds(drawable, null, null, null)

        setCheckedSilently(item)
        item.checked.afterTextChangeListener = { _, _ -> setCheckedSilently(item) }

        ui.checkedState.setVisible(item.selectionState.value)
        item.selectionState.beforeTextChangeListener = { _, newValue -> ui.checkedState.setVisible(newValue) }

        ui.root.alpha = if (item.enabled) 1.0F else 0.4F
    }

    private fun setCheckedSilently(item: UtxosViewHolderItem) {
        ui.checkedState.setOnCheckedChangeListener { _, _ -> }
        ui.checkedState.isChecked = item.checked.value
        ui.checkedState.setOnCheckedChangeListener { _, isChecked -> item.checked.value = isChecked }
    }

    companion object {
        fun getBuilder(): ViewHolderBuilder =
            ViewHolderBuilder(ItemUtxosTextBinding::inflate, UtxosViewHolderItem::class.java) { UtxosTextListViewHolder(it as ItemUtxosTextBinding) }
    }
}