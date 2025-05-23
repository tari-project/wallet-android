package com.tari.android.wallet.ui.screen.profile.profile.widget

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.tari.android.wallet.R
import com.tari.android.wallet.data.airdrop.ReferralStatusResponse.Referral
import com.tari.android.wallet.ui.compose.PoppinsFontFamily
import com.tari.android.wallet.ui.compose.PreviewSecondarySurface
import com.tari.android.wallet.ui.compose.TariDesignSystem
import com.tari.android.wallet.ui.screen.settings.themeSelector.TariTheme


@Composable
fun FriendListItem(
    friend: Referral,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier,
        shape = TariDesignSystem.shapes.card,
        colors = CardDefaults.cardColors(TariDesignSystem.colors.backgroundPrimary),
        border = BorderStroke(1.dp, TariDesignSystem.colors.elevationOutlined),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Box(
                modifier = Modifier
                    .size(34.dp)
                    .clip(CircleShape)
                    .background(color = TariDesignSystem.colors.componentsNavbarIcons), contentAlignment = Alignment.Center
            ) {
                Icon(
                    modifier = Modifier
                        .size(16.dp)
                        .clip(CircleShape),
                    painter = painterResource(R.drawable.tari_sample_avatar),
                    contentDescription = null,
                    tint = TariDesignSystem.colors.componentsNavbarBackground,
                )
            }
            Spacer(modifier = Modifier.width(10.dp))
            Text(
                text = "@${friend.name}",
                style = TextStyle(
                    fontSize = 13.sp,
                    lineHeight = 16.9.sp,
                    fontFamily = PoppinsFontFamily,
                    fontWeight = FontWeight(500),
                    color = TariDesignSystem.colors.textPrimary,
                ),
            )
        }
    }
}

@Composable
@Preview
private fun MinersListItemPreview() {
    PreviewSecondarySurface(TariTheme.Light) {
        FriendListItem(
            modifier = Modifier
                .padding(20.dp)
                .fillMaxWidth(),
            friend = Referral(
                name = "NaveenSpark",
                photos = null,
                completed = false,
            ),
        )
    }
}