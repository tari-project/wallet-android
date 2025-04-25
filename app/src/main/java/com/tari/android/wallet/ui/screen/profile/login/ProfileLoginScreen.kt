package com.tari.android.wallet.ui.screen.profile.login

import android.annotation.SuppressLint
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.tari.android.wallet.R
import com.tari.android.wallet.ui.compose.TariDesignSystem
import com.tari.android.wallet.ui.compose.components.TariPrimaryButton
import com.tari.android.wallet.ui.screen.settings.themeSelector.TariTheme

@SuppressLint("SetJavaScriptEnabled")
@Composable
fun ProfileLoginScreen(
    authUrl: String,
    onConnectClick: () -> Unit,
) {
    Scaffold(
        modifier = Modifier.fillMaxSize(),
        containerColor = TariDesignSystem.colors.backgroundSecondary,
    ) { paddingValues ->
        // FIXME: WebView is not working properly, so we are using a browser redirect instead
//        val webViewState = rememberWebViewState(authUrl)
//
//        Box(modifier = Modifier.padding(paddingValues)) {
//            CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
//            WebView(
//                modifier = Modifier.fillMaxSize(),
//                state = webViewState,
//                onCreated = {
//                    it.settings.javaScriptEnabled = true
//                    it.settings.domStorageEnabled = true // It's essential for our web to work properly
//                }
//            )
//        }

        Column(
            modifier = Modifier
                .padding(paddingValues)
                .fillMaxSize(),
            verticalArrangement = Arrangement.Center,
        ) {
            Image(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp)
                    .clip(RoundedCornerShape(10.dp)),
                painter = painterResource(R.drawable.tari_airdrop_login),
                contentDescription = null,
                alignment = Alignment.Center,
                contentScale = ContentScale.FillWidth,
            )
            Spacer(Modifier.size(40.dp))
            Text(
                modifier = Modifier.padding(horizontal = 20.dp),
                text = stringResource(R.string.airdrop_login_title),
                style = TariDesignSystem.typography.heading2XLarge,
            )
            Spacer(Modifier.size(20.dp))
            Text(
                modifier = Modifier.padding(horizontal = 20.dp),
                text = stringResource(R.string.airdrop_login_subtitle),
                style = TariDesignSystem.typography.body1,
            )
            Spacer(Modifier.size(40.dp))
            TariPrimaryButton(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp),
                text = stringResource(R.string.airdrop_login_connect_button),
                onClick = onConnectClick,
            )
        }
    }
}

@Composable
@Preview
private fun ProfileLoginScreenPreview() {
    TariDesignSystem(TariTheme.Light) {
        ProfileLoginScreen(
            authUrl = "https://tari.com",
            onConnectClick = {},
        )
    }
}