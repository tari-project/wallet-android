package com.tari.android.wallet.ui.fragment.settings.allSettings

import com.tari.android.wallet.BuildConfig
import com.tari.android.wallet.data.sharedPrefs.network.NetworkRepository

class TariVersionModel(networkRepository: NetworkRepository) {
    val versionInfo = "${networkRepository.currentNetwork!!.network.displayName} ${BuildConfig.VERSION_NAME} b${BuildConfig.VERSION_CODE}"
}