package com.tari.android.wallet.ui.screen.profile.login

import android.annotation.SuppressLint
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import com.kevinnzou.web.WebView
import com.kevinnzou.web.rememberWebViewState
import com.tari.android.wallet.ui.compose.TariDesignSystem

@SuppressLint("SetJavaScriptEnabled")
@Composable
fun ProfileLoginScreen() {
    Scaffold(
        modifier = Modifier.fillMaxSize(),
        containerColor = TariDesignSystem.colors.backgroundSecondary,
    ) { paddingValues ->
        val webViewState = rememberWebViewState("https://airdrop.tari.com/auth") // TODO save to string resource

        Box(modifier = Modifier.padding(paddingValues)) {
            CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))

            WebView(
                modifier = Modifier.fillMaxSize(),
                state = webViewState,
                onCreated = {
                    it.settings.javaScriptEnabled = true
                    it.settings.domStorageEnabled = true // It's essential for our web to work properly
                }
            )
        }
    }
}