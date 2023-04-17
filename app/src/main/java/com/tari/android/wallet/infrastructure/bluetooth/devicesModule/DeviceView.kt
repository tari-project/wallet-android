package com.tari.android.wallet.infrastructure.bluetooth.devicesModule

import android.annotation.SuppressLint
import android.bluetooth.BluetoothDevice
import android.content.Context
import android.util.AttributeSet
import android.view.LayoutInflater
import android.widget.FrameLayout
import com.tari.android.wallet.databinding.ItemBleDeviceBinding

class DeviceView @JvmOverloads constructor(context: Context, attrs: AttributeSet? = null, defStyleAttr: Int = 0) : FrameLayout(
    context,
    attrs,
    defStyleAttr
) {

    private var binding: ItemBleDeviceBinding

    init {
        isClickable = true
        isFocusable = true
        ItemBleDeviceBinding.inflate(LayoutInflater.from(context), this, true).apply {
            binding = this
        }
    }

    @SuppressLint("MissingPermission")
    fun applyData(device: BluetoothDevice, checked: Boolean) {
        binding.deviceName.text = device.name.orEmpty().ifBlank { "Unknown device" }
        binding.checkbox.isChecked = checked
    }
}