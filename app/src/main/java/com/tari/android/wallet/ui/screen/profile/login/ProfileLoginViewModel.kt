package com.tari.android.wallet.ui.screen.profile.login

import com.tari.android.wallet.ui.common.CommonViewModel

private const val AUTH_URL_ADDRESS = "https://airdrop.tari.com/login?mobileNetwork=%s"

class ProfileLoginViewModel : CommonViewModel() {

    init {
        component.inject(this)
    }

    val authUrl = String.format(AUTH_URL_ADDRESS, networkRepository.currentNetwork.network.uriComponent)
}