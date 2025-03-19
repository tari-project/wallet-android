package com.tari.android.wallet.ui.screen.profile.profile.widget

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.tari.android.wallet.R
import com.tari.android.wallet.ui.compose.PoppinsFontFamily
import com.tari.android.wallet.ui.compose.PreviewSecondarySurface
import com.tari.android.wallet.ui.compose.TariDesignSystem
import com.tari.android.wallet.ui.compose.components.TariButtonSize
import com.tari.android.wallet.ui.compose.components.TariSecondaryButton
import com.tari.android.wallet.ui.compose.components.TariVerticalGradient
import com.tari.android.wallet.ui.screen.settings.themeSelector.TariTheme

private val gradientStart = Color(0xFFC9EB00)
private val gradientEnd = Color(0xFFF1F1F2)

@Composable
fun InviteLinkCard(
    link: String,
    onShareClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Card(
        modifier = modifier,
        colors = CardDefaults.cardColors(TariDesignSystem.colors.backgroundPrimary),
        elevation = CardDefaults.cardElevation(0.dp),
        shape = TariDesignSystem.shapes.card,
    ) {
        TariVerticalGradient(
            from = gradientStart,
            to = gradientEnd,
        ) {
            Column(
                modifier = Modifier.padding(24.dp),
            ) {
                Text(
                    text = stringResource(R.string.airdrop_profile_invite_friends_title),
                    style = TextStyle(
                        fontSize = 20.sp,
                        lineHeight = 19.94.sp,
                        fontFamily = PoppinsFontFamily,
                        fontWeight = FontWeight(500),
                        color = Color.Black,
                    ),
                )
                Spacer(Modifier.height(16.dp))
                Text(
                    text = stringResource(R.string.airdrop_profile_invite_friends_description),
                    style = TextStyle(
                        fontSize = 14.sp,
                        lineHeight = 16.9.sp,
                        fontFamily = PoppinsFontFamily,
                        fontWeight = FontWeight(400),
                        color = Color.Black,
                    ),
                )
                Spacer(Modifier.height(20.dp))
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(color = Color.Black, shape = RoundedCornerShape(100.dp))
                        .padding(horizontal = 16.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        modifier = Modifier
                            .weight(1f)
                            .padding(horizontal = 8.dp),
                        text = link,
                        style = TariDesignSystem.typography.headingMedium.copy(color = Color.White),
                    )
                    TariSecondaryButton(
                        size = TariButtonSize.Small,
                        onClick = onShareClick,
                    ) {
                        Icon(
                            painter = painterResource(id = R.drawable.vector_profile_invite_friend_copy),
                            contentDescription = null,
                        )
                        Spacer(Modifier.size(4.dp))
                        Text(
                            text = stringResource(R.string.airdrop_profile_invite_friends_share).uppercase(),
                            color = Color.Black,
                            style = TariDesignSystem.typography.buttonSmall,
                        )
                    }
                }
            }
        }
    }
}

@Preview
@Composable
fun InviteLinkCardPreview() {
    PreviewSecondarySurface(TariTheme.Light) {
        InviteLinkCard(
            modifier = Modifier
                .padding(16.dp)
                .fillMaxWidth(),
            link = "tari-universe/129g78",
            onShareClick = {},
        )
    }
}