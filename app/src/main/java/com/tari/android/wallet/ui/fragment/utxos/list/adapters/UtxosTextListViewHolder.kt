package com.tari.android.wallet.ui.fragment.utxos.list.adapters

import androidx.core.content.ContextCompat
import com.tari.android.wallet.databinding.ItemUtxosTextBinding
import com.tari.android.wallet.ui.common.recyclerView.CommonViewHolder
import com.tari.android.wallet.ui.common.recyclerView.ViewHolderBuilder
import com.tari.android.wallet.ui.extension.setVisible

class UtxosTextListViewHolder(view: ItemUtxosTextBinding) : CommonViewHolder<UtxosViewHolderItem, ItemUtxosTextBinding>(view) {


    override fun bind(item: UtxosViewHolderItem) {
        super.bind(item)

        ui.amount.text = item.amount + " XTR"
        val statusStr = if (item.isShowingStatus) itemView.context.getString(item.status!!.text) else ""
        val dateStr = if (item.isShowDate) " | ${item.formattedDate} | ${item.formattedTime}" else ""
        ui.additionalData.text = statusStr + dateStr
        ui.hash.text = item.source.commitment

        val drawable = if (item.isShowingStatus) ContextCompat.getDrawable(itemView.context, item.status!!.textIcon) else null
        ui.additionalData.setCompoundDrawablesRelativeWithIntrinsicBounds(drawable, null, null, null)

        setCheckedSilently(item)
        item.checked.afterTextChangeListener = { _, _ -> setCheckedSilently(item) }

        ui.checkedState.setVisible(item.selectionState.value)
        item.selectionState.beforeTextChangeListener = { _, newValue -> ui.checkedState.setVisible(newValue) }
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