package com.tari.android.wallet.data.sharedPrefs.tor

data class TorBridgeConfiguration(val transportTechnology: String, val ip: String, val port: String, val fingerprint: String) {
    override fun toString(): String = "$transportTechnology $ip:$port"
}