package com.tari.android.wallet.data.sharedPrefs.network

class NoSupportedNetworkException(val network: String): Exception(network)