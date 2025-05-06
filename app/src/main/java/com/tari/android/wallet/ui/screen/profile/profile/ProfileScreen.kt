package com.tari.android.wallet.ui.screen.profile.profile

import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.tari.android.wallet.R
import com.tari.android.wallet.data.airdrop.ReferralStatusResponse.Referral
import com.tari.android.wallet.ui.compose.TariDesignSystem
import com.tari.android.wallet.ui.compose.components.TariErrorView
import com.tari.android.wallet.ui.compose.components.TariInheritTextButton
import com.tari.android.wallet.ui.compose.components.TariLoadingLayout
import com.tari.android.wallet.ui.compose.components.TariLoadingLayoutState
import com.tari.android.wallet.ui.compose.components.TariProgressView
import com.tari.android.wallet.ui.compose.components.TariPullToRefreshBox
import com.tari.android.wallet.ui.screen.home.overview.widget.EmptyTxList
import com.tari.android.wallet.ui.screen.profile.profile.widget.FriendListEmpty
import com.tari.android.wallet.ui.screen.profile.profile.widget.FriendListItem
import com.tari.android.wallet.ui.screen.profile.profile.widget.GemsEarnedCard
import com.tari.android.wallet.ui.screen.profile.profile.widget.InviteLinkCard
import com.tari.android.wallet.ui.screen.profile.profile.widget.TariMinedCard
import com.tari.android.wallet.ui.screen.settings.themeSelector.TariTheme
import com.tari.android.wallet.util.DebugConfig
import com.tari.android.wallet.util.extension.toMicroTari

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProfileScreen(
    uiState: ProfileModel.UiState,
    onInviteLinkShareClick: () -> Unit,
    onStartMiningClicked: () -> Unit,
    onPullToRefresh: () -> Unit,
    onDetailsRetryClick: () -> Unit,
    onFriendsRetryClick: () -> Unit,
    onDisconnectClick: () -> Unit,
) {
    Scaffold(
        modifier = Modifier.fillMaxSize(),
        containerColor = TariDesignSystem.colors.backgroundSecondary,
    ) { paddingValues ->
        TariPullToRefreshBox(
            modifier = Modifier.padding(paddingValues),
            onPullToRefresh = onPullToRefresh,
        ) {
            LazyColumn {
                item {
                    TariLoadingLayout(
                        targetLoadingState = when {
                            uiState.userDetailsError -> TariLoadingLayoutState.Error
                            uiState.userDetails == null -> TariLoadingLayoutState.Loading
                            else -> TariLoadingLayoutState.Content
                        },
                        errorLayout = {
                            TariErrorView(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 64.dp),
                                onTryAgainClick = onDetailsRetryClick,
                            )
                        },
                        loadingLayout = {
                            TariProgressView(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 64.dp),
                            )
                        },
                    ) {
                        uiState.userDetails?.let { userDetails ->
                            Spacer(Modifier.size(64.dp))
                            Text(
                                modifier = Modifier.align(Alignment.CenterHorizontally),
                                text = "@${uiState.userDetails.userTag}",
                                style = TariDesignSystem.typography.heading2XLarge,
                            )

                            if (uiState.noActivityYet) {
                                Spacer(Modifier.size(10.dp))
                                EmptyTxList(
                                    modifier = Modifier.fillMaxWidth(),
                                    showStartMiningButton = false,
                                    onStartMiningClicked = onStartMiningClicked,
                                )
                            }

                            Spacer(Modifier.size(76.dp))
                            Row(
                                modifier = Modifier
                                    .height(120.dp)
                                    .padding(horizontal = 20.dp),
                            ) {
                                TariMinedCard(
                                    modifier = Modifier.weight(1f),
                                    balance = uiState.tariMined,
                                    ticker = uiState.ticker,
                                )
                                Spacer(Modifier.width(10.dp))
                                GemsEarnedCard(
                                    modifier = Modifier.weight(1f),
                                    gemsCount = uiState.userDetails.gemsEarned,
                                )
                            }
                            Spacer(Modifier.size(10.dp))
                            InviteLinkCard(
                                link = uiState.userDetails.inviteLink,
                                onShareClick = onInviteLinkShareClick,
                                modifier = Modifier
                                    .padding(horizontal = 20.dp),
                            )
                        }
                    }
                }

                if (DebugConfig.showInvitedFriendsInProfile) {
                    item {
                        Spacer(Modifier.size(20.dp))
                        Text(
                            text = stringResource(R.string.airdrop_profile_friends_invited_title, uiState.friends?.size ?: "-"),
                            style = TariDesignSystem.typography.headingXLarge,
                            modifier = Modifier.padding(horizontal = 20.dp)
                        )
                        Spacer(Modifier.size(10.dp))
                    }

                    if (uiState.friendsError) {
                        item {
                            TariErrorView(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(top = 28.dp),
                                onTryAgainClick = onFriendsRetryClick,
                            )
                        }
                    } else if (uiState.friends == null) {
                        item {
                            TariProgressView(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(top = 28.dp),
                            )
                        }
                    } else if (uiState.friends.isEmpty()) {
                        item {
                            Spacer(Modifier.size(52.dp))
                            FriendListEmpty(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 40.dp),
                            )
                        }
                    } else {
                        items(uiState.friends.size) { index ->
                            FriendListItem(
                                modifier = Modifier
                                    .animateItem()
                                    .fillMaxWidth()
                                    .padding(horizontal = 20.dp, vertical = 5.dp),
                                friend = uiState.friends[index],
                            )
                        }
                    }
                }

                if (uiState.userDetails != null) {
                    item {
                        Spacer(Modifier.size(20.dp))
                        TariInheritTextButton(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 20.dp),
                            text = stringResource(R.string.airdrop_profile_disconnect_button),
                            onClick = onDisconnectClick,
                        )
                    }
                }

                item { Spacer(Modifier.size(52.dp)) }
            }
        }
    }
}

@Preview
@Composable
private fun ProfileScreenPreview() {
    TariDesignSystem(TariTheme.Light) {
        ProfileScreen(
            uiState = ProfileModel.UiState(
                tariMined = 2_836_150_000.toMicroTari().tariValue,
                ticker = "XTM",
                userDetails = ProfileModel.UiState.UserDetails(
                    userTag = "NaveenSpark",
                    gemsEarned = 24_836_150.0,
                    inviteLink = "tari-universe/129g78",
                ),
                friends = List(10) { index ->
                    Referral(
                        name = "sevi_$index",
                        photos = null,
                        completed = false
                    )
                },
            ),
            onInviteLinkShareClick = {},
            onStartMiningClicked = {},
            onPullToRefresh = {},
            onDetailsRetryClick = {},
            onFriendsRetryClick = {},
            onDisconnectClick = {},
        )
    }
}

@Preview
@Composable
private fun ProfileScreenNoDataPreview() {
    TariDesignSystem(TariTheme.Light) {
        ProfileScreen(
            uiState = ProfileModel.UiState(
                tariMined = 0.toBigDecimal(),
                ticker = "XTM",
                userDetails = ProfileModel.UiState.UserDetails(
                    userTag = "NaveenSpark",
                    gemsEarned = 0.0,
                    inviteLink = "tari-universe/129g78",
                ),
                friends = List(10) { index ->
                    Referral(
                        name = "sevi_$index",
                        photos = null,
                        completed = false
                    )
                },
            ),
            onInviteLinkShareClick = {},
            onStartMiningClicked = {},
            onPullToRefresh = {},
            onDetailsRetryClick = {},
            onFriendsRetryClick = {},
            onDisconnectClick = {},
        )
    }
}

@Preview
@Composable
private fun ProfileScreenLoadingPreview() {
    TariDesignSystem(TariTheme.Light) {
        ProfileScreen(
            uiState = ProfileModel.UiState(
                tariMined = 0.toMicroTari().tariValue,
                ticker = "XTM",
                userDetails = null,
                friends = null,
            ),
            onInviteLinkShareClick = {},
            onStartMiningClicked = {},
            onPullToRefresh = {},
            onDetailsRetryClick = {},
            onFriendsRetryClick = {},
            onDisconnectClick = {},
        )
    }
}

@Preview
@Composable
private fun ProfileScreenErrorPreview() {
    TariDesignSystem(TariTheme.Light) {
        ProfileScreen(
            uiState = ProfileModel.UiState(
                tariMined = 0.toMicroTari().tariValue,
                ticker = "XTM",
                userDetails = null,
                friends = null,
                userDetailsError = true,
                friendsError = true,
            ),
            onInviteLinkShareClick = {},
            onStartMiningClicked = {},
            onPullToRefresh = {},
            onDetailsRetryClick = {},
            onFriendsRetryClick = {},
            onDisconnectClick = {},
        )
    }
}