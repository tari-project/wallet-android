package com.tari.android.wallet.ui.fragment.utxos.list.adapters

import android.graphics.Color
import android.graphics.drawable.GradientDrawable
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.core.view.updateLayoutParams
import com.tari.android.wallet.R
import com.tari.android.wallet.databinding.ItemUtxosTileBinding
import com.tari.android.wallet.ui.common.domain.PaletteManager
import com.tari.android.wallet.ui.common.recyclerView.CommonViewHolder
import com.tari.android.wallet.ui.common.recyclerView.ViewHolderBuilder
import com.tari.android.wallet.ui.extension.dpToPx
import com.tari.android.wallet.ui.extension.setVisible
import com.tari.android.wallet.util.WalletUtil
import kotlin.random.Random

class UtxosTileListViewHolder(view: ItemUtxosTileBinding) : CommonViewHolder<UtxosViewHolderItem, ItemUtxosTileBinding>(view) {

    override fun bind(item: UtxosViewHolderItem) {
        super.bind(item)

        val wholeBalance = WalletUtil.amountFormatter.format(item.source.value.tariValue)
        val indexOfSeparator = wholeBalance.indexOfAny(charArrayOf(',', '.'))
        val amount = wholeBalance.take(indexOfSeparator)
        val decimal = wholeBalance.drop(indexOfSeparator)

        ui.amount.text = amount
        ui.amountDecimal.text = decimal
        ui.dateTime.text = item.formattedDate
        ui.dateTime.setVisible(item.showDate)

        ui.status.setVisible(item.showStatus)
        if (item.showStatus) {
            ui.status.setImageResource(item.status!!.icon)
        }

        ui.root.updateLayoutParams<ViewGroup.LayoutParams> { this.height = itemView.context.dpToPx(item.height.toFloat()).toInt() }

        val baseColor = Color.valueOf(PaletteManager.getPurpleBrand(itemView.context))
        val newColor = Color.valueOf(getNext(baseColor.red()), getNext(baseColor.green()), getNext(baseColor.blue()))

        val shapeDrawable = ContextCompat.getDrawable(itemView.context, R.drawable.vector_utxos_list_tile_bg) as GradientDrawable
        shapeDrawable.setColor(newColor.toArgb())
        ui.colorContainer.background = shapeDrawable

        ui.rootCard.alpha = if (item.enabled) 1.0F else 0.4F

        setCheckedSilently(item)
        item.checked.afterTileChangeListener = { _, _ -> setCheckedSilently(item) }

        ui.checkedState.setVisible(item.selectionState.value)
        item.selectionState.beforeTileChangeListener = { _, newValue -> ui.checkedState.setVisible(newValue) }
    }

    private fun setCheckedSilently(item: UtxosViewHolderItem) {
        ui.checkedState.setOnCheckedChangeListener { _, _ -> }
        ui.checkedState.isChecked = item.checked.value
        ui.checkedState.setOnCheckedChangeListener { _, isChecked -> item.checked.value = isChecked }

        ui.rootCard.cardElevation = if (item.checked.value) 15F else 0F
        if (item.checked.value) {
            val outlineDrawable = ContextCompat.getDrawable(itemView.context, R.drawable.vector_utxos_list_tile_outline_bg)
            ui.outlineContainer.background = outlineDrawable
        } else {
            ui.outlineContainer.setBackgroundColor(PaletteManager.getNeutralSecondary(itemView.context))
        }
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