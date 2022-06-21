package com.tari.android.wallet.ui.fragment.utxos.list.adapters

import android.graphics.Color
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.core.view.updateLayoutParams
import com.tari.android.wallet.R
import com.tari.android.wallet.databinding.ItemUtxosTileBinding
import com.tari.android.wallet.ui.common.recyclerView.CommonViewHolder
import com.tari.android.wallet.ui.common.recyclerView.ViewHolderBuilder
import com.tari.android.wallet.ui.extension.dpToPx
import com.tari.android.wallet.util.WalletUtil
import kotlin.random.Random

class UtxosTileListViewHolder(view: ItemUtxosTileBinding) : CommonViewHolder<UtxosViewHolderItem, ItemUtxosTileBinding>(view) {

    override fun bind(item: UtxosViewHolderItem) {
        super.bind(item)

        val wholeBalance = WalletUtil.balanceFormatter.format(item.microTariAmount.tariValue)
        val amount = wholeBalance.dropLast(3)
        val decimal = wholeBalance.takeLast(3)

        ui.amount.text = amount
        ui.amountDecimal.text = decimal

        ui.status.text = itemView.context.getString(item.status.text)
        val icon = ContextCompat.getDrawable(itemView.context, item.status.icon)
        ui.status.setCompoundDrawablesRelativeWithIntrinsicBounds(icon, null, null, null)

        ui.root.updateLayoutParams<ViewGroup.LayoutParams> { this.height = itemView.context.dpToPx(item.heigth.toFloat()).toInt() }

        val baseColor = Color.valueOf(ContextCompat.getColor(itemView.context, R.color.purple))
        val newColor = Color.valueOf(getNext(baseColor.red()), getNext(baseColor.green()), getNext(baseColor.blue()))
        ui.root.setCardBackgroundColor(newColor.toArgb())
    }

    private fun getNext(baseValue: Float): Float {
        val diff = 0.1
        val min = (baseValue - diff).coerceAtLeast(0.0)
        val max = (baseValue + diff).coerceAtMost(1.0)
        return Random.nextDouble(min, max).toFloat()
    }

    companion object {
        fun getBuilder(): ViewHolderBuilder =
            ViewHolderBuilder(ItemUtxosTileBinding::inflate, UtxosViewHolderItem::class.java) { UtxosTileListViewHolder(it as ItemUtxosTileBinding) }
    }
}