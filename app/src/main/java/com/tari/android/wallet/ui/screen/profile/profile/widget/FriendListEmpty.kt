package com.tari.android.wallet.ui.screen.profile.profile.widget

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.tari.android.wallet.R
import com.tari.android.wallet.ui.compose.PreviewSecondarySurface
import com.tari.android.wallet.ui.compose.TariDesignSystem
import com.tari.android.wallet.ui.screen.settings.themeSelector.TariTheme

@Composable
fun FriendListEmpty(
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Image(
            modifier = Modifier.size(84.dp),
            painter = painterResource(id = R.drawable.vector_profile_empty_list_turtle),
            contentDescription = null,
        )
        Spacer(Modifier.size(12.dp))
        Text(
            text = stringResource(R.string.airdrop_profile_friends_invited_empty_title),
            style = TariDesignSystem.typography.headingLarge,
        )
        Text(
            text = stringResource(R.string.airdrop_profile_friends_invited_empty_description),
            textAlign = TextAlign.Center,
            style = TariDesignSystem.typography.body2.copy(color = TariDesignSystem.colors.textPrimary),
        )
    }
}

@Composable
@Preview
private fun MinersListEmptyPreview() {
    PreviewSecondarySurface(TariTheme.Light) {
        FriendListEmpty(modifier = Modifier.padding(20.dp))
    }
}