package com.tari.android.wallet.data.sharedPrefs

import com.tari.android.wallet.data.sharedPrefs.network.NetworkPrefRepository

class SimplePrefRepository(networkRepository: NetworkPrefRepository): CommonPrefRepository(networkRepository)