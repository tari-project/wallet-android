package com.tari.android.wallet.data.repository

import com.tari.android.wallet.data.sharedPrefs.network.NetworkPrefRepository

class SimpleRepository(networkRepository: NetworkPrefRepository): CommonRepository(networkRepository)