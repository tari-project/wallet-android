package com.tari.android.wallet.data.sharedPrefs.tor

data class TorBridgeConfiguration(
    val transportTechnology: String,
    val ip: String,
    val port: String,
    val fingerprint: String,
    val certificate: String = "",
    val iatMode: String = ""
) {
    override fun toString(): String = "$transportTechnology $ip:$port $fingerprint ${if (certificate.isNotBlank()) "cert=$certificate" else ""}" +
            "${if (iatMode.isNotBlank()) "iat-mode=$iatMode" else ""}\""
}