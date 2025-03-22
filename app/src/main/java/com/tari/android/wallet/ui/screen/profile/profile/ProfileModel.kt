package com.tari.android.wallet.ui.screen.profile.profile

import com.tari.android.wallet.data.airdrop.ReferralStatusResponse
import java.math.BigDecimal

object ProfileModel {
    data class UiState(
        val userTag: String,
        val noActivityYet: Boolean,
        val tariMined: BigDecimal,
        val ticker: String,
        val gemsEarned: Long,
        val inviteLink: String,
        val friends: List<ReferralStatusResponse.Referral> = emptyList(),
    )
}