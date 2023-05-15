package com.tari.android.wallet.data.sharedPrefs.bluetooth

import androidx.annotation.StringRes
import com.tari.android.wallet.R

enum class BluetoothServerState(@StringRes val title: Int) {
    DISABLED(R.string.bluetooth_settings_option_off),
    WHILE_UP(R.string.bluetooth_settings_option_while_up),
    ENABLED(R.string.bluetooth_settings_option_on)
}