package com.tari.android.wallet.ui.screen.settings.allSettings

import com.tari.android.wallet.BuildConfig
import com.tari.android.wallet.data.sharedPrefs.network.NetworkPrefRepository

class TariVersionModel(networkRepository: NetworkPrefRepository) {
    val versionInfo = "${networkRepository.currentNetwork.network.displayName} ${BuildConfig.VERSION_NAME} b${BuildConfig.VERSION_CODE}"
}