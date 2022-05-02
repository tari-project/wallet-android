package com.tari.android.wallet.data.sharedPrefs.testnetFaucet

import com.google.gson.annotations.SerializedName

data class TestnetTariOutput(
    @SerializedName("version")
    val version: String,

    @SerializedName("commitment")
    val commitment: String,

    @SerializedName("proof")
    val proof: String,

    @SerializedName("script")
    val script: String,

    @SerializedName("sender_offset_public_key")
    val senderOffsetPublicKey: String,

    @SerializedName("metadata_signature")
    val metadataSignature: TestnetTariMetadataSignature,

    @SerializedName("covenant")
    val covenant: String
)