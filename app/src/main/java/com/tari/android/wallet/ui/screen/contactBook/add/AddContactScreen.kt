package com.tari.android.wallet.ui.screen.contactBook.add

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.tari.android.wallet.R
import com.tari.android.wallet.ui.compose.PreviewSecondarySurface
import com.tari.android.wallet.ui.compose.TariDesignSystem
import com.tari.android.wallet.ui.compose.components.TariHorizontalDivider
import com.tari.android.wallet.ui.compose.components.TariPrimaryButton
import com.tari.android.wallet.ui.compose.components.TariTextField
import com.tari.android.wallet.ui.compose.components.TariTopBar
import com.tari.android.wallet.ui.screen.settings.themeSelector.TariTheme
import com.tari.android.wallet.util.MockDataStub


@Composable
fun AddContactScreen(
    uiState: AddContactViewModel.UiState,
    onBackClick: () -> Unit,
    onScanQrClick: () -> Unit,
    onAliasChange: (alias: String) -> Unit,
    onAddressChange: (address: String) -> Unit,
    onSaveClick: () -> Unit,
) {
    Scaffold(
        modifier = Modifier
            .fillMaxSize()
            .statusBarsPadding()
            .imePadding(),
        containerColor = TariDesignSystem.colors.backgroundSecondary,
        topBar = {
            TariTopBar(
                title = stringResource(R.string.contact_book_add_contact_title),
                onBack = onBackClick,
            )
        },
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues),
        ) {
            Spacer(Modifier.size(24.dp))


            var aliasValue by remember { mutableStateOf(TextFieldValue(uiState.alias)) }
            // TODO maybe make it more elegant. make an extension function for TextFieldValue?
            uiState.alias.takeIf { it != aliasValue.text }?.let { aliasValue = TextFieldValue(it, selection = TextRange(it.length)) }
            TariTextField(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp),
                value = aliasValue,
                onValueChanged = {
                    onAliasChange(it.text)
                    aliasValue = it
                },
                title = stringResource(R.string.contact_book_add_contact_alias_field_title),
                hint = stringResource(R.string.contact_book_add_contact_alias_field_hint),
            )

            Spacer(Modifier.size(24.dp))
            TariHorizontalDivider(Modifier.padding(horizontal = 24.dp))
            Spacer(Modifier.size(24.dp))

            var addressValue by remember { mutableStateOf(TextFieldValue(uiState.walletAddress?.fullBase58.orEmpty())) }
            uiState.walletAddress?.let { addressValue = TextFieldValue(it.fullBase58, selection = TextRange(it.fullBase58.length)) }
            TariTextField(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp),
                value = addressValue,
                onValueChanged = {
                    addressValue = it
                    onAddressChange(it.text)
                },
                title = stringResource(R.string.contact_book_add_contact_address_field_title),
                hint = stringResource(R.string.contact_book_add_contact_address_field_hint),
                errorText = stringResource(R.string.contact_book_add_contact_address_field_error_text).takeIf { !uiState.isValidWalletAddress },
                trailingIcon = {
                    IconButton(onClick = onScanQrClick) {
                        Icon(
                            painter = painterResource(R.drawable.vector_icon_qr),
                            tint = TariDesignSystem.colors.componentsNavbarIcons,
                            contentDescription = null,
                        )
                    }
                },
            )

            Spacer(Modifier.weight(1f))

            TariPrimaryButton(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(24.dp),
                text = stringResource(R.string.contact_book_add_contact_save_button),
                onClick = onSaveClick,
                enabled = uiState.saveButtonEnabled,
            )
        }
    }
}

@Composable
@Preview
private fun AddContactScreenEmptyPreview() {
    PreviewSecondarySurface(TariTheme.Light) {
        AddContactScreen(
            uiState = AddContactViewModel.UiState(

            ),
            onBackClick = {},
            onScanQrClick = {},
            onAliasChange = {},
            onAddressChange = {},
            onSaveClick = {},
        )
    }
}

@Composable
@Preview
private fun AddContactScreenNotEmptyPreview() {
    PreviewSecondarySurface(TariTheme.Light) {
        AddContactScreen(
            uiState = AddContactViewModel.UiState(
                alias = "Alice",
                walletAddress = MockDataStub.WALLET_ADDRESS,
            ),
            onBackClick = {},
            onScanQrClick = {},
            onAliasChange = {},
            onAddressChange = {},
            onSaveClick = {},
        )
    }
}

@Composable
@Preview
private fun AddContactScreenNotEmptyErrorPreview() {
    PreviewSecondarySurface(TariTheme.Light) {
        AddContactScreen(
            uiState = AddContactViewModel.UiState(
                alias = "Alice",
                walletAddress = MockDataStub.WALLET_ADDRESS,
                isValidWalletAddress = false,
            ),
            onBackClick = {},
            onScanQrClick = {},
            onAliasChange = {},
            onAddressChange = {},
            onSaveClick = {},
        )
    }
}
