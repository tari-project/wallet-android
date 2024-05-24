package com.tari.android.wallet.ui.fragment.settings.bluetoothSettings

import androidx.lifecycle.MutableLiveData
import com.tari.android.wallet.data.sharedPrefs.bluetooth.BluetoothPrefRepository
import com.tari.android.wallet.data.sharedPrefs.bluetooth.BluetoothServerState
import com.tari.android.wallet.ui.common.CommonViewModel
import com.tari.android.wallet.ui.fragment.settings.bluetoothSettings.adapter.BluetoothSettingsItem
import javax.inject.Inject

class BluetoothSettingsViewModel : CommonViewModel() {

    @Inject
    lateinit var bluetoothSettingsRepository: BluetoothPrefRepository

    val list = MutableLiveData<List<BluetoothSettingsItem>>()

    init {
        component.inject(this)

        updateList()
    }

    private fun updateList() {
        val currentValue = bluetoothSettingsRepository.bluetoothSettingsState
        val values = BluetoothServerState.entries.map { BluetoothSettingsItem(currentValue == it, it) }
        list.postValue(values)
    }

    fun setSettings(newValue: BluetoothServerState) {
        bluetoothSettingsRepository.bluetoothSettingsState = newValue
        updateList()
    }
}

