package com.tari.android.wallet.ui.screen.profile.profile

import java.math.BigDecimal

object ProfileModel {
    data class UiState(
        val userTag: String,
        val tariMined: BigDecimal,
        val ticker: String,
        val gemsEarned: Long,
        val inviteLink: String,
    )
}