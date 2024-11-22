package com.tari.android.wallet.ui.screen.settings.bluetoothSettings.adapter

import com.tari.android.wallet.databinding.ItemBridgeBinding
import com.tari.android.wallet.ui.common.recyclerView.CommonViewHolder
import com.tari.android.wallet.ui.common.recyclerView.ViewHolderBuilder
import com.tari.android.wallet.util.extension.setVisible

class BluetoothStateViewHolder(view: ItemBridgeBinding) : CommonViewHolder<BluetoothSettingsItem, ItemBridgeBinding>(view) {

    override fun bind(item: BluetoothSettingsItem) {
        super.bind(item)
        ui.tvName.text = itemView.context.getString(item.state.title)
        ui.done.setVisible(item.enabled)
    }

    companion object {
        fun getBuilder(): ViewHolderBuilder =
            ViewHolderBuilder(ItemBridgeBinding::inflate, BluetoothSettingsItem::class.java) { BluetoothStateViewHolder(it as ItemBridgeBinding) }
    }
}