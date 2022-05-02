package com.tari.android.wallet.data.sharedPrefs.tor

class TorBridgeConfigurationList(): ArrayList<TorBridgeConfiguration>() {
    constructor(e: List<TorBridgeConfiguration>) : this() {
        addAll(e)
    }
}

fun TorBridgeConfigurationList?.orEmpty() : TorBridgeConfigurationList = this ?: TorBridgeConfigurationList()