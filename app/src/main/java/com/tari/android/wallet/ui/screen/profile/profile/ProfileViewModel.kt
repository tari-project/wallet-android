package com.tari.android.wallet.ui.screen.profile.profile

import com.tari.android.wallet.data.airdrop.AirdropRepository
import com.tari.android.wallet.navigation.Navigation
import com.tari.android.wallet.ui.common.CommonViewModel
import com.tari.android.wallet.util.extension.launchOnIo
import com.tari.android.wallet.util.extension.toMicroTari
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import javax.inject.Inject

private const val FRIEND_INVITE_ADDRESS = "https://airdrop.tari.com/download/%s"
private const val FRIEND_INVITE_ADDRESS_SHORT = "tari-universe/%s"

class ProfileViewModel : CommonViewModel() {

    @Inject
    lateinit var airdropRepository: AirdropRepository

    init {
        component.inject(this)
    }

    // TODO change with real data and show progress
    private val _uiState = MutableStateFlow(
        ProfileModel.UiState(
            userTag = "NaveenSpark",
            noActivityYet = true,
            tariMined = 24_836_150_000.toMicroTari().tariValue,
            ticker = networkRepository.currentNetwork.ticker,
            gemsEarned = 24_836_150,
            inviteLink = "tari-universe/129g78",
        )
    )
    val uiState = _uiState.asStateFlow()

    init {
        launchOnIo {
            airdropRepository.getUserDetails().let { userDetails ->
                _uiState.update {
                    it.copy(
                        userTag = userDetails.user.displayName,
                        inviteLink = String.format(FRIEND_INVITE_ADDRESS_SHORT, userDetails.user.referralCode),
                    )
                }
            }
        }

        launchOnIo {
            airdropRepository.getReferralList().referrals.let { referrals ->
                _uiState.update {
                    it.copy(friends = referrals)
                }
            }
        }
    }

    fun onInviteLinkShareClick() {
        tariNavigator.navigate(Navigation.ShareText(String.format(FRIEND_INVITE_ADDRESS, uiState.value.inviteLink)))
    }

    fun onStartMiningClicked() {
        showNotReadyYetDialog()
    }
}