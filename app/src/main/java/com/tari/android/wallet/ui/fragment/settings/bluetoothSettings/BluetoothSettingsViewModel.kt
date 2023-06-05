package com.tari.android.wallet.ui.fragment.settings.bluetoothSettings

import androidx.lifecycle.MutableLiveData
import com.tari.android.wallet.data.sharedPrefs.bluetooth.BluetoothServerState
import com.tari.android.wallet.data.sharedPrefs.bluetooth.ShareSettingsRepository
import com.tari.android.wallet.ui.common.CommonViewModel
import com.tari.android.wallet.ui.fragment.settings.bluetoothSettings.adapter.BluetoothSettingsItem
import javax.inject.Inject

class BluetoothSettingsViewModel : CommonViewModel() {

    @Inject
    lateinit var bluetoothSettingsRepository: ShareSettingsRepository

    val list = MutableLiveData<MutableList<BluetoothSettingsItem>>()

    init {
        component.inject(this)

        updateList()
    }

    private fun updateList() {
        val currentValue = bluetoothSettingsRepository.bluetoothSettingsState ?: BluetoothServerState.ENABLED
        val values = BluetoothServerState.values().map { BluetoothSettingsItem(currentValue == it, it) }.toMutableList()
        list.postValue(values)
    }

    fun setSettings(newValue: BluetoothServerState) {
        bluetoothSettingsRepository.bluetoothSettingsState = newValue
        updateList()
    }
}

