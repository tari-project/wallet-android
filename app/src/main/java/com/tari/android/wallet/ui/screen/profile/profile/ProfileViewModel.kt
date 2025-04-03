package com.tari.android.wallet.ui.screen.profile.profile

import com.tari.android.wallet.data.airdrop.AirdropRepository
import com.tari.android.wallet.data.tx.TxListData
import com.tari.android.wallet.data.tx.TxRepository
import com.tari.android.wallet.navigation.Navigation
import com.tari.android.wallet.ui.common.CommonViewModel
import com.tari.android.wallet.util.extension.collectFlow
import com.tari.android.wallet.util.extension.launchOnIo
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import java.math.BigDecimal
import javax.inject.Inject

private const val FRIEND_INVITE_ADDRESS = "https://airdrop.tari.com/download/%s"
private const val FRIEND_INVITE_ADDRESS_SHORT = "tari-universe/%s"

class ProfileViewModel : CommonViewModel() {

    @Inject
    lateinit var airdropRepository: AirdropRepository

    @Inject
    lateinit var txRepository: TxRepository

    init {
        component.inject(this)
    }

    private val _uiState = MutableStateFlow(
        ProfileModel.UiState(
            tariMined = txRepository.txs.value.minedTariCount(),
            ticker = networkRepository.currentNetwork.ticker,
        )
    )
    val uiState = _uiState.asStateFlow()

    init {
        collectFlow(txRepository.txs) { txs ->
            _uiState.update { it.copy(tariMined = txs.minedTariCount()) }
        }

        refreshData()
    }

    fun onInviteLinkShareClick() {
        tariNavigator.navigate(Navigation.ShareText(String.format(FRIEND_INVITE_ADDRESS, uiState.value.userDetails?.inviteLink)))
    }

    fun onStartMiningClicked() {
        showNotReadyYetDialog()
    }

    fun refreshData() {
        refreshUserDetails()
        refreshFriendList()
    }

    fun refreshUserDetails() {
        launchOnIo {
            _uiState.update { it.copy(userDetailsError = false) } // To start loading animation

            airdropRepository.getUserDetails().let { userDetailsResult ->
                userDetailsResult
                    .onSuccess { userDetails ->
                        _uiState.update {
                            it.copy(
                                userDetailsError = false,
                                userDetails = ProfileModel.UiState.UserDetails(
                                    userTag = userDetails.user.displayName,
                                    gemsEarned = userDetails.user.rank.gemsCount,
                                    inviteLink = String.format(FRIEND_INVITE_ADDRESS_SHORT, userDetails.user.referralCode),
                                )
                            )
                        }
                    }
                    .onFailure {
                        // show error only if userDetails has not been loaded yet
                        _uiState.update { it.copy(userDetailsError = it.userDetails == null) }
                    }
            }
        }
    }

    fun refreshFriendList() {
        launchOnIo {
            _uiState.update { it.copy(friendsError = false) } // To start loading animation

            airdropRepository.getReferralList().let { referralsResult ->
                referralsResult
                    .onSuccess { response ->
                        _uiState.update { it.copy(friends = response.referrals) }
                    }
                    .onFailure {
                        // show error only if friends has not been loaded yet
                        _uiState.update { it.copy(friendsError = it.friends == null) }
                    }
            }
        }
    }

    private fun TxListData.minedTariCount(): BigDecimal = this.completedTxs
        .filter { it.isCoinbase }
        .sumOf { it.amount.tariValue }

}