package com.tari.android.wallet.infrastructure.bluetooth.devicesModule

import android.bluetooth.BluetoothDevice
import com.tari.android.wallet.ui.dialog.modular.IDialogModule

class DevicesModule(val devices: MutableList<BluetoothDevice>, var listUpdatedAction: () -> Unit = {}) : IDialogModule() {
    var checkedList = mutableListOf<BluetoothDevice>()
}