package com.tari.android.wallet.ui.screen.profile.profile

import com.tari.android.wallet.data.airdrop.ReferralStatusResponse
import java.math.BigDecimal

object ProfileModel {
    data class UiState(
        val ticker: String,
        val tariMined: BigDecimal,

        val userDetails: UserDetails? = null,
        val userDetailsError: Boolean = false,

        val friends: List<ReferralStatusResponse.Referral>? = null,
        val friendsError: Boolean = false,
    ) {
        val noActivityYet: Boolean
            get() = tariMined == 0.toBigDecimal()

        data class UserDetails(
            val userTag: String,
            val gemsEarned: Double,
            val referralCode: String,
        )
    }
}