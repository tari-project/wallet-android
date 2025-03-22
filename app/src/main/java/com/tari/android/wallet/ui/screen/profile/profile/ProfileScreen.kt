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
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.tari.android.wallet.R
import com.tari.android.wallet.data.airdrop.ReferralStatusResponse.Referral
import com.tari.android.wallet.ui.compose.TariDesignSystem
import com.tari.android.wallet.ui.screen.home.overview.widget.EmptyTxList
import com.tari.android.wallet.ui.screen.profile.profile.widget.FriendListEmpty
import com.tari.android.wallet.ui.screen.profile.profile.widget.FriendListItem
import com.tari.android.wallet.ui.screen.profile.profile.widget.GemsEarnedCard
import com.tari.android.wallet.ui.screen.profile.profile.widget.InviteLinkCard
import com.tari.android.wallet.ui.screen.profile.profile.widget.TariMinedCard
import com.tari.android.wallet.ui.screen.settings.themeSelector.TariTheme
import com.tari.android.wallet.util.extension.toMicroTari

@Composable
fun ProfileScreen(
    uiState: ProfileModel.UiState,
    onInviteLinkShareClick: () -> Unit,
    onStartMiningClicked: () -> Unit,
) {
    Scaffold(
        modifier = Modifier.fillMaxSize(),
        containerColor = TariDesignSystem.colors.backgroundSecondary,
    ) { paddingValues ->
        LazyColumn(modifier = Modifier.padding(paddingValues)) {
            item {
                Spacer(Modifier.size(64.dp))
                Text(
                    modifier = Modifier.fillMaxWidth(),
                    text = "@${uiState.userTag}",
                    textAlign = TextAlign.Center,
                    style = TariDesignSystem.typography.heading2XLarge,
                )

                if (uiState.noActivityYet) {
                    Spacer(Modifier.size(10.dp))
                    EmptyTxList(
                        modifier = Modifier.fillMaxWidth(),
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
                        gemsCount = uiState.gemsEarned,
                    )
                }
                Spacer(Modifier.size(10.dp))
                InviteLinkCard(
                    link = uiState.inviteLink,
                    onShareClick = onInviteLinkShareClick,
                    modifier = Modifier
                        .padding(horizontal = 20.dp),
                )
                Spacer(Modifier.size(20.dp))
                Text(
                    text = stringResource(R.string.airdrop_profile_friends_invited_title, uiState.friends.size),
                    style = TariDesignSystem.typography.headingXLarge,
                    modifier = Modifier.padding(horizontal = 20.dp)
                )
                Spacer(Modifier.size(10.dp))
            }

            if (uiState.friends.isEmpty()) {
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
                            .fillMaxWidth()
                            .padding(horizontal = 20.dp, vertical = 5.dp),
                        friend = uiState.friends[index],
                    )
                }
            }

            item { Spacer(Modifier.size(52.dp)) }
        }
    }
}


@Preview
@Composable
fun ProfileScreenPreview() {
    TariDesignSystem(TariTheme.Light) {
        ProfileScreen(
            uiState = ProfileModel.UiState(
                userTag = "NaveenSpark",
                noActivityYet = true,
                tariMined = 24_836_150_000.toMicroTari().tariValue,
                ticker = "XTR",
                gemsEarned = 24_836_150,
                inviteLink = "tari-universe/129g78",
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
        )
    }
}