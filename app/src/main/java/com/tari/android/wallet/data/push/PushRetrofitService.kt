package com.tari.android.wallet.data.push

import com.google.gson.annotations.SerializedName
import retrofit2.http.Body
import retrofit2.http.POST
import retrofit2.http.Path

interface PushRetrofitService {

    @POST("/register/{publicKey}")
    suspend fun register(
        @Path("publicKey") publicKey: String,
        @Body body: PushRegisterRequestBody,
    )
}

data class PushRegisterRequestBody(
    @SerializedName("token") val token: String,
    @SerializedName("signature") val signature: String,
    @SerializedName("appId") val appId: String? = null,
    @SerializedName("userId") val userId: String? = null,
    @SerializedName("public_nonce") val publicNonce: String,
    @SerializedName("platform") val platform: String = "android",
    @SerializedName("sandbox") val sandbox: Boolean = false,
    @SerializedName("walletViewKeyHashed") val walletViewKeyHashed: String,
)