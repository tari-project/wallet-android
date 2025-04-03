package com.tari.android.wallet.ui.screen.send.confirm

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.tari.android.wallet.R
import com.tari.android.wallet.ui.compose.PreviewSecondarySurface
import com.tari.android.wallet.ui.compose.TariDesignSystem
import com.tari.android.wallet.ui.compose.components.TariPrimaryButton
import com.tari.android.wallet.ui.compose.components.TariTextButton
import com.tari.android.wallet.ui.compose.components.TariTopBar
import com.tari.android.wallet.ui.screen.send.common.TransactionData
import com.tari.android.wallet.ui.screen.settings.themeSelector.TariTheme
import com.tari.android.wallet.util.MockDataStub
import com.tari.android.wallet.util.extension.toMicroTari

@Composable
fun ConfirmScreen(
    uiState: ConfirmViewModel.UiState,
    onBackClick: () -> Unit,
    onConfirmClick: () -> Unit,
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
        Column(
            modifier = Modifier.padding(paddingValues),
        ) {
            Spacer(Modifier.weight(1f))
            TariPrimaryButton(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp),
                text = stringResource(R.string.confirm_tx_confirm_and_send_button),
                onClick = onConfirmClick,
            )
            Spacer(Modifier.size(16.dp))
            TariTextButton(
                modifier = Modifier
                    .align(Alignment.CenterHorizontally)
                    .padding(horizontal = 20.dp),
                text = stringResource(R.string.common_cancel),
                onClick = onBackClick,
            )
            Spacer(Modifier.size(16.dp))
        }
    }
}

@Composable
@Preview
private fun ConfirmScreenPreview() {
    PreviewSecondarySurface(TariTheme.Light) {
        ConfirmScreen(
            uiState = ConfirmViewModel.UiState(
                ticker = "TARI",
                transactionData = TransactionData(
                    amount = 1000.toMicroTari(),
                    feePerGram = 10.toMicroTari(),
                    note = "Test note",
                    recipientContact = MockDataStub.createContact(),
                    isOneSidePayment = true,
                ),
            ),
            onBackClick = {},
            onConfirmClick = {},
        )
    }
}