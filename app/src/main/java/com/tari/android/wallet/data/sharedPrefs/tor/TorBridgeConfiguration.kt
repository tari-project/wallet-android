package com.tari.android.wallet.data.sharedPrefs.tor

import android.os.Parcelable
import kotlinx.parcelize.Parcelize

@Parcelize
data class TorBridgeConfiguration(
    val transportTechnology: String,
    val ip: String,
    val port: String,
    val fingerprint: String,
    val certificate: String = "",
    val iatMode: String = ""
) : Parcelable {
    override fun toString(): String = ("$transportTechnology $ip:$port $fingerprint ${if (certificate.isNotBlank()) "cert=$certificate" else ""} " +
            if (iatMode.isNotBlank()) "iat-mode=$iatMode" else "").trim()
}