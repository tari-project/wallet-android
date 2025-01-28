package com.tari.android.wallet.ui.screen.onboarding.inroduction

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.Icon
import androidx.compose.material.Scaffold
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.LinkAnnotation
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.withLink
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.tari.android.wallet.R
import com.tari.android.wallet.ui.compose.PreviewSurface
import com.tari.android.wallet.ui.compose.TariDesignSystem
import com.tari.android.wallet.ui.compose.components.TariDivider
import com.tari.android.wallet.ui.compose.components.TariOutlinedButton
import com.tari.android.wallet.ui.compose.components.TariPrimaryButton
import com.tari.android.wallet.ui.compose.components.TariVerticalGradient
import com.tari.android.wallet.ui.screen.settings.themeSelector.TariTheme

@Composable
fun IntroductionScreen(
    uiState: IntroductionModel.UiState,
    onImportWalletClick: () -> Unit,
    onCreateWalletClick: () -> Unit,
) {
    Scaffold(
        backgroundColor = TariDesignSystem.colors.backgroundPrimary,
        modifier = Modifier,
    ) { padding ->
        Box(
            modifier = Modifier
                .padding(padding)
                .fillMaxSize(),
        ) {
            Column(modifier = Modifier.fillMaxSize()) {
                Icon(
                    painter = painterResource(R.drawable.vector_introduction_tari_logo),
                    contentDescription = null,
                    tint = TariDesignSystem.colors.textPrimary,
                    modifier = Modifier
                        .padding(24.dp)
                        .align(Alignment.CenterHorizontally)
                        .fillMaxWidth(),
                )
                Image(
                    painter = painterResource(R.drawable.tari_tower_coins_helix),
                    contentDescription = null,
                    contentScale = ContentScale.FillHeight,
                    modifier = Modifier
                        .padding(bottom = 120.dp)
                        .fillMaxSize(),
                )
            }

            Column(modifier = Modifier.align(Alignment.BottomCenter)) {
                TariVerticalGradient(
                    modifier = Modifier
                        .height(200.dp)
                        .fillMaxWidth(),
                    from = Color.Transparent,
                    to = TariDesignSystem.colors.backgroundPrimary,
                )

                Column(modifier = Modifier.background(TariDesignSystem.colors.backgroundPrimary)) {
                    Text(
                        text = stringResource(R.string.introduction_import_wallet_info),
                        style = TariDesignSystem.typography.body2,
                        modifier = Modifier
                            .padding(horizontal = 24.dp)
                            .align(Alignment.CenterHorizontally),
                    )
                    Spacer(modifier = Modifier.size(16.dp))
                    TariPrimaryButton(
                        text = stringResource(R.string.introduction_import_wallet),
                        onClick = onImportWalletClick,
                        modifier = Modifier
                            .padding(horizontal = 24.dp)
                            .fillMaxWidth(),
                    )
                    Spacer(modifier = Modifier.size(16.dp))
                    TariOutlinedButton(
                        text = stringResource(R.string.introduction_create_wallet),
                        onClick = onCreateWalletClick,
                        modifier = Modifier
                            .padding(horizontal = 24.dp)
                            .fillMaxWidth(),
                    )
                    Spacer(modifier = Modifier.size(16.dp))
                    TariDivider(
                        modifier = Modifier
                            .padding(horizontal = 24.dp)
                            .fillMaxWidth(),
                    )
                    Spacer(modifier = Modifier.size(16.dp))
                    Text(
                        text = uiState.versionInfo,
                        style = TariDesignSystem.typography.body2,
                        modifier = Modifier
                            .padding(horizontal = 24.dp)
                            .align(Alignment.CenterHorizontally),
                    )
                    Spacer(modifier = Modifier.size(8.dp))
                    AgreementLinkedText(
                        modifier = Modifier
                            .padding(horizontal = 64.dp)
                            .align(Alignment.CenterHorizontally),
                    )
                    Spacer(modifier = Modifier.size(16.dp))
                }
            }
        }
    }
}

@Composable
private fun AgreementLinkedText(
    modifier: Modifier = Modifier,
) {
    Text(
        modifier = modifier,
        style = TariDesignSystem.typography.body2,
        textAlign = TextAlign.Center,
        text = buildAnnotatedString {
            append(stringResource(R.string.create_wallet_user_agreement_and_privacy_policy_part1) + " ")
            withLink(
                LinkAnnotation.Url(
                    url = stringResource(R.string.user_agreement_url),
                    styles = TariDesignSystem.typography.linkSpan,
                )
            ) {
                append(stringResource(R.string.create_wallet_user_agreement))
            }
            append(" " + stringResource(R.string.create_wallet_user_agreement_and_privacy_policy_part2) + " ")
            withLink(
                LinkAnnotation.Url(
                    url = stringResource(R.string.privacy_policy_url),
                    styles = TariDesignSystem.typography.linkSpan,
                )
            ) {
                append(stringResource(R.string.create_wallet_privacy_policy))
            }
        },
    )
}


@Preview
@Composable
private fun IntroductionScreenLightPreview() {
    PreviewSurface(TariTheme.Light) {
        IntroductionScreen(
            uiState = IntroductionModel.UiState(
                versionInfo = "Version 1.0.0",
                networkName = "Mainnet",
            ),
            onImportWalletClick = {},
            onCreateWalletClick = {},
        )
    }
}

@Preview
@Composable
private fun IntroductionScreenDarkPreview() {
    PreviewSurface(TariTheme.Dark) {
        IntroductionScreen(
            uiState = IntroductionModel.UiState(
                versionInfo = "Version 1.0.0",
                networkName = "Mainnet",
            ),
            onImportWalletClick = {},
            onCreateWalletClick = {},
        )
    }
}
