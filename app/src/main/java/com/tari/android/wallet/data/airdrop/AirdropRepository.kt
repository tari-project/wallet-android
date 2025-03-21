package com.tari.android.wallet.data.airdrop

import com.tari.android.wallet.data.airdrop.ReferralStatusResponse.Referral
import com.tari.android.wallet.util.extension.switchToIo
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import retrofit2.HttpException
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AirdropRepository @Inject constructor(
    private val airdropRetrofit: AirdropRetrofitService,
) {

    @Throws(HttpException::class)
    suspend fun getMinerStats(): Int = switchToIo { airdropRetrofit.getMinerStats().totalMiners }

    fun getMinerStatsFlow(): Flow<Int> = flow<Int> {
        while (true) {
            try {
                emit(getMinerStats())
            } catch (e: Exception) {
                e.printStackTrace()
            }
            delay(MINERS_COUNT_REFRESH_INTERVAL)
        }
    }

    @Throws(HttpException::class)
    suspend fun getMiningStatus(id: String): Boolean = switchToIo { airdropRetrofit.getMinerStatus(id).mining }

    // TODO: Use real data
    @Throws(HttpException::class)
//    suspend fun getUserDetails(): UserDetailsResponse = switchToIo { airdropRetrofit.getUserDetails() }
    suspend fun getUserDetails(): UserDetailsResponse = UserDetailsResponse(
        user = UserDetailsResponse.User(
            imageUrl = null,
            isBot = false,
            twitterFollowers = 4,
            id = "7a135d32-e6b6-41fa-b195-2680480c73b8",
            referralCode = "wydONTtUAt",
            yatUserId = null,
            displayName = "Balázs Sevecsek",
            name = "Balázs Sevecsek",
            role = "vip",
            provider = "twitter",
            providerId = "1755636284070957056",
            profileImageUrl = "https://pbs.twimg.com/profile_images/1755636378069549056/54qKGALt_normal.png",
        )
    )

    // TODO: Use real data
    @Throws(HttpException::class)
//    suspend fun getReferralStatus(): ReferralStatusResponse = switchToIo { airdropRetrofit.getReferralStatus() }
    suspend fun getReferralStatus(): ReferralStatusResponse = ReferralStatusResponse(
        referrals = List(10) { index ->
            Referral(
                name = "sevi_$index",
                photos = null,
                completed = false
            )
        }
    )

    companion object {
        private const val MINERS_COUNT_REFRESH_INTERVAL = 60_000L
    }
}