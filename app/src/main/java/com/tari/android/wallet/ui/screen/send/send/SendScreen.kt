package com.tari.android.wallet.ui.screen.send.send

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.tari.android.wallet.R
import com.tari.android.wallet.application.walletManager.WalletConfig
import com.tari.android.wallet.model.TariWalletAddress
import com.tari.android.wallet.ui.compose.PreviewSecondarySurface
import com.tari.android.wallet.ui.compose.TariDesignSystem
import com.tari.android.wallet.ui.compose.components.TariHorizontalDivider
import com.tari.android.wallet.ui.compose.components.TariPrimaryButton
import com.tari.android.wallet.ui.compose.components.TariProgressView
import com.tari.android.wallet.ui.compose.components.TariTextField
import com.tari.android.wallet.ui.compose.components.TariTopBar
import com.tari.android.wallet.ui.screen.settings.themeSelector.TariTheme
import com.tari.android.wallet.util.MockDataStub
import com.tari.android.wallet.util.extension.newValueIfChanged
import com.tari.android.wallet.util.extension.toMicroTari

@Composable
fun SendScreen(
    uiState: SendViewModel.UiState,
    onBackClick: () -> Unit,
    onRecipientAddressChange: (address: String) -> Unit,
    onAmountChange: (amount: String) -> Unit,
    onNoteChange: (note: String) -> Unit,
    onFeeHelpClicked: () -> Unit,
    onContinueClick: () -> Unit,
    onScanQrClick: () -> Unit,
    onContactBookClick: () -> Unit,
) {
    Scaffold(
        modifier = Modifier
            .fillMaxSize()
            .statusBarsPadding()
            .imePadding(),
        containerColor = TariDesignSystem.colors.backgroundSecondary,
        topBar = {
            TariTopBar(
                title = stringResource(R.string.send_screen_title),
                onBack = onBackClick,
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .verticalScroll(rememberScrollState()),
        ) {
            Spacer(Modifier.size(36.dp))

            var addressValue by remember { mutableStateOf(TextFieldValue(uiState.contact?.walletAddress?.fullBase58.orEmpty())) }
            addressValue = addressValue.newValueIfChanged(uiState.contact?.walletAddress?.fullBase58)
            TariTextField(
                modifier = Modifier.padding(horizontal = 24.dp),
                value = addressValue,
                onValueChanged = {
                    addressValue = it
                    onRecipientAddressChange(it.text)
                },
                title = stringResource(R.string.send_recipient_address_field_title),
                hint = stringResource(R.string.send_recipient_address_field_hint),
                titleAdditionalLayout = {
                    Row {
                        IconButton(onClick = onScanQrClick) {
                            Icon(
                                painter = painterResource(R.drawable.vector_icon_qr),
                                tint = TariDesignSystem.colors.componentsNavbarIcons,
                                contentDescription = null,
                            )
                        }
                        IconButton(onClick = onContactBookClick) {
                            Icon(
                                painter = painterResource(R.drawable.vector_contact_book_icon),
                                tint = TariDesignSystem.colors.componentsNavbarIcons,
                                contentDescription = null,
                            )
                        }
                    }
                },
                errorText = stringResource(R.string.contact_book_add_contact_address_field_error_text).takeIf { !uiState.isContactAddressValid },
            )

            TariHorizontalDivider(Modifier.padding(horizontal = 24.dp, vertical = 16.dp))

            var amountValue by remember { mutableStateOf(TextFieldValue(uiState.amount?.formattedTariValue.orEmpty())) }
            amountValue = amountValue.newValueIfChanged(uiState.amountValue)
            TariTextField(
                modifier = Modifier.padding(horizontal = 24.dp),
                value = amountValue,
                onValueChanged = { newValue ->
                    if (newValue.text != amountValue.text) onAmountChange(newValue.text)
                    amountValue = newValue
                },
                title = stringResource(R.string.send_amount_field_title),
                hint = stringResource(R.string.send_amount_field_hint),
                titleAdditionalLayout = {
                    Column {
                        uiState.availableBalance?.let { availableBalance ->
                            Text(
                                text = stringResource(
                                    R.string.home_available_to_spend_balance,
                                    WalletConfig.balanceFormatter.format(uiState.availableBalance.tariValue) + " " + uiState.ticker
                                ),
                                style = TariDesignSystem.typography.body1,
                                color = if (uiState.availableBalanceError) {
                                    TariDesignSystem.colors.errorMain
                                } else {
                                    TariDesignSystem.colors.textSecondary
                                },
                            )
                        } ?: run {
                            TariProgressView(Modifier.size(16.dp))
                        }

                        Spacer(Modifier.size(8.dp))
                    }
                },
                errorText = uiState.amountError?.let { stringResource(it) },
                numberKeyboard = true,
            )

            Spacer(Modifier.size(16.dp))
            TariHorizontalDivider(Modifier.padding(horizontal = 24.dp))

            Row(
                modifier = Modifier.padding(horizontal = 24.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = stringResource(R.string.add_amount_tx_fee, uiState.ticker),
                    style = TariDesignSystem.typography.body1,
                )
                IconButton(onClick = onFeeHelpClicked) {
                    Icon(
                        painter = painterResource(id = R.drawable.vector_help_circle_filled),
                        tint = TariDesignSystem.colors.actionActive,
                        contentDescription = null,
                    )
                }
                Spacer(Modifier.weight(1f))
                Text(
                    text = "${uiState.fee?.tariValue?.let { WalletConfig.amountFormatter.format(it) } ?: "-"} ${uiState.ticker}",
                    style = TariDesignSystem.typography.body1.copy(color = TariDesignSystem.colors.textPrimary),
                )
            }

            TariHorizontalDivider(Modifier.padding(horizontal = 24.dp))
            Spacer(Modifier.size(16.dp))

            var noteValue by remember { mutableStateOf(TextFieldValue(uiState.note)) }
            noteValue = if (uiState.disabledNoteField) {
                TextFieldValue(stringResource(R.string.send_note_field_disabled_payment_id))
            } else {
                noteValue.newValueIfChanged(uiState.note)
            }
            TariTextField(
                modifier = Modifier.padding(horizontal = 24.dp),
                value = noteValue,
                onValueChanged = { newValue ->
                    if (noteValue.text != newValue.text) onNoteChange(newValue.text)
                    noteValue = newValue
                },
                title = stringResource(R.string.send_note_field_title),
                hint = stringResource(R.string.send_note_field_hint),
                enabled = !uiState.disabledNoteField,
            )

            Spacer(Modifier.weight(1f))

            TariPrimaryButton(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(24.dp),
                text = stringResource(R.string.common_continue),
                onClick = onContinueClick,
                enabled = uiState.continueButtonEnabled,
            )
        }
    }
}

@Composable
@Preview
private fun SendScreenEmptyPreview() {
    PreviewSecondarySurface(TariTheme.Light) {
        SendScreen(
            uiState = SendViewModel.UiState(
                ticker = "TXM",
                availableBalance = null,
            ),
            onBackClick = {},
            onRecipientAddressChange = {},
            onAmountChange = {},
            onNoteChange = {},
            onFeeHelpClicked = {},
            onContinueClick = {},
            onScanQrClick = {},
            onContactBookClick = {},
        )
    }
}

@Composable
@Preview
private fun SendScreenPreview() {
    PreviewSecondarySurface(TariTheme.Light) {
        SendScreen(
            uiState = SendViewModel.UiState(
                ticker = "TXM",
                contact = MockDataStub.createContact(),
                amountValue = "123,56",
                availableBalance = 124_000_000.toMicroTari(),
                fee = 1234.toMicroTari(),
                note = "Lorem ipsum dolor sit amet, consectetur adipiscing elit. Sed do eiusmod tempor incididunt ut labore et dolore magna aliqua.",
            ),
            onBackClick = {},
            onRecipientAddressChange = {},
            onAmountChange = {},
            onNoteChange = {},
            onFeeHelpClicked = {},
            onContinueClick = {},
            onScanQrClick = {},
            onContactBookClick = {},
        )
    }
}

@Composable
@Preview
private fun SendScreenErrorPreview() {
    PreviewSecondarySurface(TariTheme.Light) {
        SendScreen(
            uiState = SendViewModel.UiState(
                ticker = "TXM",
                contact = MockDataStub.createContact(),
                isContactAddressValid = false,
                amountValue = "123456.789",
                availableBalance = 124_000_000.toMicroTari(),
                fee = 1234.toMicroTari(),
                note = "Lorem ipsum dolor sit amet, consectetur adipiscing elit. Sed do eiusmod tempor incididunt ut labore et dolore magna aliqua.",
            ),
            onBackClick = {},
            onRecipientAddressChange = {},
            onAmountChange = {},
            onNoteChange = {},
            onFeeHelpClicked = {},
            onContinueClick = {},
            onScanQrClick = {},
            onContactBookClick = {},
        )
    }
}

@Composable
@Preview
private fun SendScreenPaymentIdPreview() {
    PreviewSecondarySurface(TariTheme.Light) {
        SendScreen(
            uiState = SendViewModel.UiState(
                ticker = "TXM",
                contact = MockDataStub.createContact().copy(
                    walletAddress = MockDataStub.WALLET_ADDRESS.copy(
                        features = listOf(TariWalletAddress.Feature.PAYMENT_ID),
                    )
                ),
                amountValue = "123.456789",
                availableBalance = 124_000_000.toMicroTari(),
                fee = 1234.toMicroTari(),
                note = "Lorem ipsum dolor sit amet, consectetur adipiscing elit. Sed do eiusmod tempor incididunt ut labore et dolore magna aliqua.",
            ),
            onBackClick = {},
            onRecipientAddressChange = {},
            onAmountChange = {},
            onNoteChange = {},
            onFeeHelpClicked = {},
            onContinueClick = {},
            onScanQrClick = {},
            onContactBookClick = {},
        )
    }
}