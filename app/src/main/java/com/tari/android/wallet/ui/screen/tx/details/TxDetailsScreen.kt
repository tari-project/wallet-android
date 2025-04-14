package com.tari.android.wallet.ui.screen.tx.details

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.tari.android.wallet.ui.compose.TariDesignSystem
import com.tari.android.wallet.ui.compose.components.TariTopBar

@Composable
fun TxDetailsScreen(
    uiState: TxDetailsModel.UiState,
    onBackClick: () -> Unit,
) {
    Scaffold(
        modifier = Modifier
            .fillMaxSize()
            .statusBarsPadding(),
        containerColor = TariDesignSystem.colors.backgroundSecondary,
        topBar = {
            TariTopBar(
                title = uiState.screenTitle,
                onBack = onBackClick,
            )
        }
    ) { paddingValues ->
        Column(modifier = Modifier.padding(paddingValues)) {

        }
    }
}
