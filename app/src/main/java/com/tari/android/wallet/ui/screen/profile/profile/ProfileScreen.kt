package com.tari.android.wallet.ui.screen.profile.profile

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.tari.android.wallet.ui.compose.TariDesignSystem
import com.tari.android.wallet.ui.screen.profile.profile.widget.GemsEarnedCard
import com.tari.android.wallet.ui.screen.profile.profile.widget.InviteLinkCard
import com.tari.android.wallet.ui.screen.profile.profile.widget.TariMinedCard
import com.tari.android.wallet.ui.screen.settings.themeSelector.TariTheme
import com.tari.android.wallet.util.extension.toMicroTari

@Composable
fun ProfileScreen(
    uiState: ProfileModel.UiState,
    onInviteLinkShareClick: () -> Unit,
) {
    Scaffold(
        modifier = Modifier.fillMaxSize(),
        containerColor = TariDesignSystem.colors.backgroundSecondary,
    ) { paddingValues ->
        Column(modifier = Modifier.padding(paddingValues)) {
            Spacer(Modifier.size(64.dp))
            Text(
                modifier = Modifier.align(Alignment.CenterHorizontally),
                text = uiState.userTag,
                style = TariDesignSystem.typography.heading2XLarge,
            )
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

        }
    }
}

@Preview
@Composable
fun ProfileScreenPreview() {
    TariDesignSystem(TariTheme.Light) {
        ProfileScreen(
            uiState = ProfileModel.UiState(
                userTag = "@NaveenSpark",
                tariMined = 24_836_150_000.toMicroTari().tariValue,
                ticker = "XTR",
                gemsEarned = 24_836_150,
                inviteLink = "tari-universe/129g78"
            ),
            onInviteLinkShareClick = {},
        )
    }
}