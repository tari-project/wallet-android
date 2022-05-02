package com.tari.android.wallet.data.sharedPrefs.testnetFaucet

import com.google.gson.annotations.SerializedName

data class TestnetTariMetadataSignature(
    @SerializedName("public_nonce")
    val public_nonce: String,

    @SerializedName("u")
    val u: String,

    @SerializedName("v")
    val v: String,
)