package com.tari.android.wallet.ui.screen.profile.login

import com.tari.android.wallet.data.airdrop.AirdropRepository
import com.tari.android.wallet.ui.common.CommonViewModel
import javax.inject.Inject

private const val AUTH_URL_ADDRESS = "https://airdrop.tari.com/auth?mobile=%s"

class ProfileLoginViewModel : CommonViewModel() {

    @Inject
    lateinit var airdropRepository: AirdropRepository

    init {
        component.inject(this)
    }

    val authUrl = String.format(AUTH_URL_ADDRESS, networkRepository.currentNetwork.network.uriComponent)

    fun onConnectAirdropClick() {
        deeplinkManager.cancelAirdropLoginConfirmation()
        airdropRepository.markLoginPending(System.currentTimeMillis())
        openUrl(authUrl)
    }
}