package com.tari.android.wallet.infrastructure.bluetooth.devicesModule

import android.annotation.SuppressLint
import android.content.Context
import android.view.LayoutInflater
import android.view.ViewGroup
import com.tari.android.wallet.databinding.DialogModuleBleDevicesBinding
import com.tari.android.wallet.ui.common.CommonViewModel
import com.tari.android.wallet.ui.component.common.CommonView

@SuppressLint("ViewConstructor")
class DevicesModuleView(context: Context, val buttonModule: DevicesModule) : CommonView<CommonViewModel, DialogModuleBleDevicesBinding>(context) {

    override fun bindingInflate(layoutInflater: LayoutInflater, parent: ViewGroup?, attachToRoot: Boolean): DialogModuleBleDevicesBinding =
        DialogModuleBleDevicesBinding.inflate(layoutInflater, parent, attachToRoot)

    override fun setup() = Unit

    init {
        updateDevices()
        buttonModule.listUpdatedAction = {
            updateDevices()
        }
    }

    private fun updateDevices() {
        buttonModule.devices.distinct()
        ui.devicesContainer.removeAllViews()
        buttonModule.devices.forEach { device ->
            val deviceView = DeviceView(context).apply { applyData(device, buttonModule.checkedList.contains(device)) }
            deviceView.setOnClickListener {
                if (buttonModule.checkedList.contains(device)) {
                    buttonModule.checkedList.remove(device)
                } else {
                    buttonModule.checkedList.add(device)
                }
                updateDevices()
            }
            ui.devicesContainer.addView(deviceView)
        }
    }
}

