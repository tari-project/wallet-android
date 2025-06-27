package com.tari.android.wallet.ui.screen.contactBook.list

import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.tari.android.wallet.R
import com.tari.android.wallet.data.contacts.Contact
import com.tari.android.wallet.data.tx.TxListData
import com.tari.android.wallet.ui.compose.PreviewSecondarySurface
import com.tari.android.wallet.ui.compose.TariDesignSystem
import com.tari.android.wallet.ui.compose.components.TariButtonSize
import com.tari.android.wallet.ui.compose.components.TariPrimaryButton
import com.tari.android.wallet.ui.compose.components.TariSearchField
import com.tari.android.wallet.ui.compose.components.TariTopBar
import com.tari.android.wallet.ui.screen.settings.themeSelector.TariTheme
import com.tari.android.wallet.util.MockDataStub
import com.tari.android.wallet.util.base58Ellipsized

@Composable
fun ContactListScreen(
    uiState: ContactListViewModel.UiState,
    onBackClick: () -> Unit,
    onAddContactClick: () -> Unit,
    onSearchQueryChange: (query: String) -> Unit,
    onContactItemClick: (contact: Contact) -> Unit,
) {
    Scaffold(
        modifier = Modifier
            .fillMaxSize()
            .statusBarsPadding(),
        containerColor = TariDesignSystem.colors.backgroundSecondary,
        topBar = {
            TariTopBar(
                title = stringResource(R.string.contact_book_contacts_book_title),
                onBack = onBackClick,
                action = {
                    IconButton(onClick = onAddContactClick) {
                        Icon(
                            painter = painterResource(R.drawable.vector_add_contact),
                            contentDescription = null,
                            tint = TariDesignSystem.colors.componentsNavbarIcons,
                        )
                    }
                },
            )
        }
    ) { paddingValues ->
        var searchQuery by rememberSaveable { mutableStateOf(uiState.searchQuery) }

        if (uiState.showEmptyState) {
            EmptyState(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues),
                onAddContactClick = onAddContactClick,
            )
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues),
            ) {
                item {
                    Spacer(Modifier.size(20.dp))
                    TariSearchField(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp),
                        searchQuery = searchQuery,
                        hint = stringResource(R.string.contact_book_search_hint),
                        onQueryChanged = {
                            onSearchQueryChange(it)
                            searchQuery = it
                        },
                    )
                }

                if (uiState.searchQuery.isNotBlank()) {
                    item { Spacer(Modifier.size(16.dp)) }
                }

                if (uiState.searchQuery.isBlank()) {
                    item {
                        Text(
                            modifier = Modifier.padding(start = 32.dp, top = 32.dp, bottom = 16.dp),
                            text = stringResource(R.string.contact_book_recents),
                            style = TariDesignSystem.typography.headingLarge,
                        )
                    }

                    items(uiState.recentContactList.size) { index ->
                        val contact = uiState.recentContactList[index]
                        ContactListItem(
                            modifier = Modifier
                                .padding(horizontal = 16.dp, vertical = 4.dp)
                                .animateItem(),
                            contact = contact,
                            onContactClick = { onContactItemClick(contact) },
                        )
                    }
                }

                if (uiState.searchQuery.isBlank()) {
                    item {
                        Text(
                            modifier = Modifier.padding(start = 32.dp, top = 32.dp, bottom = 16.dp),
                            text = stringResource(R.string.contact_book_contacts),
                            style = TariDesignSystem.typography.headingLarge,
                        )
                    }
                }

                items(uiState.sortedContactList.size) { index ->
                    val contact = uiState.sortedContactList[index]
                    ContactListItem(
                        modifier = Modifier
                            .padding(horizontal = 16.dp, vertical = 4.dp)
                            .animateItem(),
                        contact = contact,
                        onContactClick = { onContactItemClick(contact) },
                    )
                }
            }
        }
    }
}

@Composable
private fun EmptyState(modifier: Modifier, onAddContactClick: () -> Unit) {
    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Image(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp),
            painter = painterResource(R.drawable.tari_contact_book_empty_state),
            contentDescription = null,
            alignment = Alignment.Center,
            contentScale = ContentScale.Fit,
        )
        Spacer(Modifier.size(40.dp))
        Text(
            text = stringResource(R.string.contact_book_empty_title),
            style = TariDesignSystem.typography.headingLarge,
        )
        Spacer(Modifier.size(24.dp))
        Text(
            text = stringResource(R.string.contact_book_empty_description),
            textAlign = TextAlign.Center,
            style = TariDesignSystem.typography.body1,
        )
        Spacer(Modifier.size(24.dp))
        TariPrimaryButton(
            size = TariButtonSize.Small,
            text = stringResource(R.string.contact_book_empty_add_contact),
            onClick = onAddContactClick,
        )
    }
}

@Composable
fun ContactListItem(modifier: Modifier, contact: Contact, onContactClick: () -> Unit) {
    Card(
        modifier = modifier,
        shape = TariDesignSystem.shapes.card,
        colors = CardDefaults.cardColors(TariDesignSystem.colors.backgroundPrimary),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
    ) {
        Row(
            modifier = Modifier
                .clickable(onClick = onContactClick)
                .padding(horizontal = 16.dp, vertical = 16.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = contact.alias ?: stringResource(R.string.contact_book_no_alias),
                    style = TariDesignSystem.typography.headingMedium,
                )
                Text(
                    text = contact.walletAddress.base58Ellipsized(),
                    style = TariDesignSystem.typography.body2,
                )
            }
            Icon(
                modifier = Modifier.size(16.dp),
                painter = painterResource(R.drawable.vector_arrow_right),
                contentDescription = null,
                tint = TariDesignSystem.colors.componentsNavbarIcons,
            )
        }
    }
}

@Composable
@Preview
fun ContactListScreenPreview() {
    PreviewSecondarySurface(TariTheme.Light) {
        ContactListScreen(
            uiState = ContactListViewModel.UiState(
                searchQuery = "",
                contacts = MockDataStub.createContactList(),
                txs = TxListData(),
            ),
            onBackClick = {},
            onAddContactClick = {},
            onSearchQueryChange = {},
            onContactItemClick = {},
        )
    }
}

@Composable
@Preview
fun ContactListScreenSearchPreview() {
    PreviewSecondarySurface(TariTheme.Light) {
        ContactListScreen(
            uiState = ContactListViewModel.UiState(
                searchQuery = "ali",
                contacts = MockDataStub.createContactList(),
                txs = TxListData(),
            ),
            onBackClick = {},
            onAddContactClick = {},
            onSearchQueryChange = {},
            onContactItemClick = {},
        )
    }
}

@Composable
@Preview
fun ContactListScreenEmptyPreview() {
    PreviewSecondarySurface(TariTheme.Light) {
        ContactListScreen(
            uiState = ContactListViewModel.UiState(
                contacts = emptyList(),
                txs = TxListData(),
            ),
            onBackClick = {},
            onAddContactClick = {},
            onSearchQueryChange = {},
            onContactItemClick = {},
        )
    }
}