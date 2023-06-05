package com.tari.android.wallet.ui.fragment.settings.bluetoothSettings.adapter

import com.tari.android.wallet.ui.common.recyclerView.CommonAdapter
import com.tari.android.wallet.ui.common.recyclerView.ViewHolderBuilder

class BluetoothSettingsAdapter: CommonAdapter<BluetoothSettingsItem>() {
    override var viewHolderBuilders: List<ViewHolderBuilder> = listOf(BluetoothStateViewHolder.getBuilder())
}