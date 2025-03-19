package com.tari.android.wallet.ui.screen.profile.profile

import com.tari.android.wallet.navigation.Navigation
import com.tari.android.wallet.ui.common.CommonViewModel
import com.tari.android.wallet.util.extension.toMicroTari
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow

class ProfileViewModel : CommonViewModel() {

    init {
        component.inject(this)
    }

    // TODO change with real data
    private val _uiState = MutableStateFlow(
        ProfileModel.UiState(
            userTag = "@NaveenSpark",
            tariMined = 24_836_150_000.toMicroTari().tariValue,
            ticker = networkRepository.currentNetwork.ticker,
            gemsEarned = 24_836_150,
            inviteLink = "tari-universe/129g78"
        )
    )
    val uiState = _uiState.asStateFlow()

    fun onInviteLinkShareClick() {
        tariNavigator.navigate(Navigation.ShareText(uiState.value.inviteLink))
    }
}