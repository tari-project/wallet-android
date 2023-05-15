package com.tari.android.wallet.ui.fragment.settings.bluetoothSettings.adapter

import com.tari.android.wallet.data.sharedPrefs.bluetooth.BluetoothServerState
import com.tari.android.wallet.ui.common.recyclerView.CommonViewHolderItem

class BluetoothSettingsItem(val enabled: Boolean, val state: BluetoothServerState): CommonViewHolderItem()