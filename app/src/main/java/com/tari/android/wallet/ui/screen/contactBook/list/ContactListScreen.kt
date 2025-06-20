package com.tari.android.wallet.ui.screen.contactBook.list

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.tari.android.wallet.R
import com.tari.android.wallet.ui.compose.PreviewSecondarySurface
import com.tari.android.wallet.ui.compose.TariDesignSystem
import com.tari.android.wallet.ui.compose.components.TariButtonSize
import com.tari.android.wallet.ui.compose.components.TariPrimaryButton
import com.tari.android.wallet.ui.compose.components.TariTopBar
import com.tari.android.wallet.ui.screen.settings.themeSelector.TariTheme

@Composable
fun ContactListScreen(
    uiState: ContactListViewModel.UiState,
    onBackClick: () -> Unit,
    onAddContactClick: () -> Unit,
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
            )
        }
    ) { paddingValues ->
        if (uiState.contacts.isEmpty()) {
            EmptyState(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues),
                onAddContactClick = onAddContactClick,
            )
        } else {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues),
            ) {
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
            text = stringResource(R.string.contact_book_add_title),
            onClick = onAddContactClick,
        )
    }
}

@Composable
@Preview
fun ContactListScreenPreview() {
    PreviewSecondarySurface(TariTheme.Light) {
        ContactListScreen(
            uiState = ContactListViewModel.UiState(

            ),
            onBackClick = {},
            onAddContactClick = {},
        )
    }
}