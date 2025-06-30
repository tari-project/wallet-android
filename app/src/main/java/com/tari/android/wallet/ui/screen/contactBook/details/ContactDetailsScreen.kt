package com.tari.android.wallet.ui.screen.contactBook.details

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
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
import com.tari.android.wallet.data.tx.TxDto
import com.tari.android.wallet.ui.compose.PreviewSecondarySurface
import com.tari.android.wallet.ui.compose.TariDesignSystem
import com.tari.android.wallet.ui.compose.components.TariHorizontalDivider
import com.tari.android.wallet.ui.compose.components.TariInheritTextButton
import com.tari.android.wallet.ui.compose.components.TariTextButton
import com.tari.android.wallet.ui.compose.components.TariTopBar
import com.tari.android.wallet.ui.compose.widgets.AddressCard
import com.tari.android.wallet.ui.screen.home.overview.widget.TxItem
import com.tari.android.wallet.ui.screen.settings.themeSelector.TariTheme
import com.tari.android.wallet.util.MockDataStub

@Composable
fun ContactDetailsScreen(
    uiState: ContactDetailsViewModel.UiState,
    onBackClick: () -> Unit,
    onSendTariClicked: () -> Unit,
    onRequestTariClicked: () -> Unit,
    onEmojiCopyClick: () -> Unit,
    onBase58CopyClick: () -> Unit,
    onEmojiDetailClick: () -> Unit,
    onTxClick: (tx: TxDto) -> Unit,
    onEditAliasClicked: () -> Unit,
    onShareContactClicked: () -> Unit,
    onDeleteContactClicked: () -> Unit,
) {
    Scaffold(
        modifier = Modifier
            .fillMaxSize()
            .statusBarsPadding(),
        containerColor = TariDesignSystem.colors.backgroundSecondary,
        topBar = {
            TariTopBar(
                title = stringResource(R.string.contact_details_screen_title),
                onBack = onBackClick,
                action = {
                    IconButton(onClick = onShareContactClicked) {
                        Icon(
                            painter = painterResource(R.drawable.vector_icon_share),
                            contentDescription = null,
                            tint = TariDesignSystem.colors.componentsNavbarIcons,
                        )
                    }
                },
            )
        },
    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues),
        ) {
            item {
                Spacer(Modifier.size(36.dp))
                Box {
                    Text(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 56.dp)
                            .align(Alignment.Center),
                        text = uiState.contact.alias.orEmpty(),
                        style = TariDesignSystem.typography.headingXLarge,
                        textAlign = TextAlign.Center,
                    )

                    IconButton(
                        modifier = Modifier
                            .padding(horizontal = 8.dp)
                            .align(Alignment.CenterEnd),
                        onClick = onEditAliasClicked
                    ) {
                        Icon(
                            painter = painterResource(R.drawable.vector_icon_edit_contact_pencil),
                            contentDescription = null,
                            tint = TariDesignSystem.colors.componentsNavbarIcons,
                        )
                    }
                }
                Spacer(Modifier.size(16.dp))
                AddressCard(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp),
                    address = uiState.contact.walletAddress,
                    cardTitle = stringResource(R.string.contact_details_address),
                    onEmojiCopyClick = onEmojiCopyClick,
                    onBase58CopyClick = onBase58CopyClick,
                    onEmojiDetailClick = onEmojiDetailClick,
                )
                Spacer(Modifier.size(16.dp))
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp),
                ) {
                    TariInheritTextButton(
                        modifier = Modifier.weight(1f),
                        text = stringResource(R.string.send_tari_subtitle),
                        onClick = onSendTariClicked,
                    )
                    Spacer(modifier = Modifier.width(16.dp))
                    TariInheritTextButton(
                        modifier = Modifier.weight(1f),
                        text = stringResource(R.string.contact_details_request),
                        onClick = onRequestTariClicked,
                    )
                }
            }

            if (uiState.userTxs.isNotEmpty()) {
                item {
                    Spacer(Modifier.size(24.dp))
                    TariHorizontalDivider(Modifier.padding(horizontal = 16.dp))
                    Spacer(Modifier.size(24.dp))
                    Text(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp),
                        text = stringResource(R.string.contact_details_transaction_history_description, uiState.contact.alias.orEmpty()),
                        style = TariDesignSystem.typography.headingLarge,
                        textAlign = TextAlign.Center,
                    )
                    Spacer(Modifier.size(16.dp))
                }

                items(uiState.userTxs.size) { index ->
                    val txItem = uiState.userTxs[index]
                    TxItem(
                        modifier = Modifier
                            .padding(horizontal = 16.dp, vertical = 4.dp)
                            .animateItem(),
                        txDto = txItem,
                        ticker = uiState.ticker,
                        onTxClick = { onTxClick(txItem) },
                    )
                }
            }

            item {
                Spacer(Modifier.size(24.dp))
                TariHorizontalDivider(Modifier.padding(horizontal = 16.dp))
                Spacer(Modifier.size(24.dp))

                Box(Modifier.fillMaxWidth()) {
                    TariTextButton(
                        modifier = Modifier
                            .align(Alignment.Center)
                            .padding(horizontal = 20.dp),
                        text = stringResource(R.string.contact_details_delete_contact),
                        onClick = onDeleteContactClicked,
                        warningColor = true,
                    )
                }

                Spacer(Modifier.size(80.dp))
            }
        }
    }
}

@Composable
@Preview
fun ContactDetailsScreenEmptyPreview() {
    PreviewSecondarySurface(TariTheme.Light) {
        ContactDetailsScreen(
            uiState = ContactDetailsViewModel.UiState(
                contact = MockDataStub.createContact(),
                userTxs = emptyList(),
                ticker = "XTM",
            ),
            onBackClick = {},
            onSendTariClicked = {},
            onRequestTariClicked = {},
            onEmojiCopyClick = {},
            onBase58CopyClick = {},
            onEmojiDetailClick = {},
            onTxClick = {},
            onEditAliasClicked = {},
            onShareContactClicked = {},
            onDeleteContactClicked = {},
        )
    }
}

@Composable
@Preview
fun ContactDetailsScreenPreview() {
    PreviewSecondarySurface(TariTheme.Light) {
        ContactDetailsScreen(
            uiState = ContactDetailsViewModel.UiState(
                contact = MockDataStub.createContact(),
                ticker = "XTM",
                userTxs = MockDataStub.createTxList(),
            ),
            onBackClick = {},
            onSendTariClicked = {},
            onRequestTariClicked = {},
            onEmojiCopyClick = {},
            onBase58CopyClick = {},
            onEmojiDetailClick = {},
            onTxClick = {},
            onEditAliasClicked = {},
            onShareContactClicked = {},
            onDeleteContactClicked = {},
        )
    }
}
