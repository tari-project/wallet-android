package com.tari.android.wallet.data.airdrop

import com.google.gson.annotations.SerializedName
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.Header
import retrofit2.http.Headers
import retrofit2.http.POST
import retrofit2.http.Path

interface AirdropRetrofitService {

    @GET("/api/miner/stats")
    suspend fun getMinerStats(): MinerStatsResponse

    @GET("/api/miner/status/{id}")
    suspend fun getMinerStatus(@Header("Authorization") token: String, @Path("id") anonId: String): MiningStatusResponse

    @GET("/api/user/details")
    suspend fun getUserDetails(@Header("Authorization") token: String): UserDetailsResponse

    @GET("/api/user/referral-status")
    suspend fun getReferralStatus(@Header("Authorization") token: String): ReferralStatusResponse

    @POST("/api/auth/local/refresh")
    @Headers("Content-Type: application/json", "Cache-Control: no-store")
    suspend fun refreshAuthToken(@Body request: RefreshTokenRequest): RefreshTokenResponse
}

data class MinerStatsResponse(
    val totalMiners: Int,
)

data class MiningStatusResponse(
    val mining: Boolean
)

data class UserDetailsResponse(
    @SerializedName("user") val user: User,
) {
    data class User(
        @SerializedName("image_url") val imageUrl: String?,
        @SerializedName("id") val id: String,
        @SerializedName("referral_code") val referralCode: String,
        @SerializedName("yat_user_id") val yatUserId: String?,
        @SerializedName("display_name") val displayName: String,
        @SerializedName("name") val name: String,
        @SerializedName("profileimageurl") val profileImageUrl: String,
        @SerializedName("rank") val rank: Rank,
    ) {
        data class Rank(
            @SerializedName("gems") val gemsCount: Double,
        )
    }
}

data class ReferralStatusResponse(
    @SerializedName("referrals") val referrals: List<Referral>,
) {
    data class Referral(
        @SerializedName("name") val name: String,
        @SerializedName("photos") val photos: String?,
        @SerializedName("completed") val completed: Boolean,
    )
}

data class RefreshTokenRequest(
    @SerializedName("refreshToken") val refreshToken: String
)

data class RefreshTokenResponse(
    @SerializedName("token") val token: String,
    @SerializedName("refreshToken") val refreshToken: String
)
