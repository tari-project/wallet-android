package com.tari.android.wallet.ui.screen.tx.details.widget

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.tari.android.wallet.R
import com.tari.android.wallet.model.TariWalletAddress
import com.tari.android.wallet.ui.compose.PreviewSecondarySurface
import com.tari.android.wallet.ui.compose.TariDesignSystem
import com.tari.android.wallet.ui.compose.components.TariHorizontalDivider
import com.tari.android.wallet.ui.screen.settings.themeSelector.TariTheme
import com.tari.android.wallet.ui.screen.tx.details.TxDetailsModel
import com.tari.android.wallet.util.MockDataStub
import com.tari.android.wallet.util.base58Ellipsized
import com.tari.android.wallet.util.shortString

@Composable
fun TxDetailInfoItem(
    modifier: Modifier = Modifier,
    title: String,
    value: String,
    valueTextColor: Color = TariDesignSystem.colors.textPrimary,
    singleLine: Boolean = true,
    actionIcons: @Composable () -> Unit = {},
) {
    Column(modifier = modifier) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    style = TariDesignSystem.typography.body2,
                )
                Spacer(Modifier.size(4.dp))
                Text(
                    text = value,
                    style = TariDesignSystem.typography.body1,
                    color = valueTextColor,
                    overflow = TextOverflow.Ellipsis,
                    maxLines = if (singleLine) 1 else Int.MAX_VALUE,
                )
            }

            actionIcons()
        }

        Spacer(Modifier.size(10.dp))
        TariHorizontalDivider()
    }
}

@Composable
fun TxDetailInfoCopyItem(
    modifier: Modifier = Modifier,
    title: String,
    value: String,
    singleLine: Boolean = true,
    onCopyClicked: (value: String) -> Unit = {},
) {
    TxDetailInfoItem(
        modifier = modifier,
        title = title,
        value = value,
        singleLine = singleLine,
    ) {
        IconButton(onClick = { onCopyClicked(value) }) {
            Icon(
                painter = painterResource(R.drawable.vector_icon_copy),
                contentDescription = null,
                tint = TariDesignSystem.colors.componentsNavbarIcons,
            )
        }
    }
}

@Composable
fun TxDetailInfoContactNameItem(
    modifier: Modifier = Modifier,
    alias: String?,
    onEditClicked: () -> Unit,
) {
    Column(modifier = modifier) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = stringResource(R.string.tx_detail_contact_name),
                    style = TariDesignSystem.typography.body2,
                )
                Spacer(Modifier.size(4.dp))
                if (alias.isNullOrEmpty()) {
                    Text(
                        text = stringResource(R.string.tx_details_add_contact_name_hint),
                        style = TariDesignSystem.typography.body1,
                        color = TariDesignSystem.colors.actionDisabled,
                    )
                } else {
                    Text(
                        text = alias,
                        style = TariDesignSystem.typography.body1,
                        color = TariDesignSystem.colors.textPrimary,
                    )
                }
            }
            IconButton(onClick = onEditClicked) {
                Icon(
                    painter = painterResource(R.drawable.vector_icon_edit_contact_pencil),
                    contentDescription = null,
                    tint = TariDesignSystem.colors.componentsNavbarIcons,
                )
            }
        }

        Spacer(Modifier.size(10.dp))
        TariHorizontalDivider()
    }
}

@Composable
fun TxDetailInfoStatusItem(
    modifier: Modifier = Modifier,
    txStatus: TxDetailsModel.UiState.TxStatusText,
) {
    TxDetailInfoItem(
        modifier = modifier,
        title = stringResource(R.string.tx_details_status),
        value = when (txStatus) {
            is TxDetailsModel.UiState.TxStatusText.Completed -> stringResource(R.string.tx_details_completed)
            is TxDetailsModel.UiState.TxStatusText.Cancelled -> stringResource(txStatus.textRes)
            is TxDetailsModel.UiState.TxStatusText.InProgress -> stringResource(txStatus.textRes)
            is TxDetailsModel.UiState.TxStatusText.InProgressStep -> stringResource(txStatus.textRes, txStatus.step, txStatus.stepCount)
        },
        valueTextColor = when (txStatus) {
            is TxDetailsModel.UiState.TxStatusText.Completed -> TariDesignSystem.colors.successMain
            is TxDetailsModel.UiState.TxStatusText.Cancelled -> TariDesignSystem.colors.errorMain
            is TxDetailsModel.UiState.TxStatusText.InProgress,
            is TxDetailsModel.UiState.TxStatusText.InProgressStep -> TariDesignSystem.colors.warningMain
        },
        singleLine = false,
    )
}

@Composable
fun TxDetailInfoAddressItem(
    walletAddress: TariWalletAddress,
    onCopyClicked: (value: String) -> Unit,
    onEmojiIdDetailsClick: () -> Unit,
    modifier: Modifier = Modifier,
    title: String = stringResource(R.string.common_to),
) {
    var showEmojiId by remember { mutableStateOf(false) }

    val value = if (showEmojiId) {
        walletAddress.shortString()
    } else {
        walletAddress.base58Ellipsized()
    }

    TxDetailInfoItem(
        modifier = modifier
            .clickable(
                enabled = showEmojiId,
                onClick = onEmojiIdDetailsClick,
            ),
        title = title,
        value = value,
    ) {
        IconButton(
            modifier = Modifier.offset(x = 12.dp), // needed to remove "margin" between two IconButtons
            onClick = { showEmojiId = !showEmojiId },
        ) {
            Icon(
                painter = painterResource(if (showEmojiId) R.drawable.vector_icon_smiley else R.drawable.vector_icon_smiley_strikethrough),
                contentDescription = null,
                tint = TariDesignSystem.colors.componentsNavbarIcons,
            )
        }

        IconButton(onClick = { onCopyClicked(if (showEmojiId) walletAddress.fullEmojiId else walletAddress.fullBase58) }) {
            Icon(
                painter = painterResource(R.drawable.vector_icon_copy),
                contentDescription = null,
                tint = TariDesignSystem.colors.componentsNavbarIcons,
            )
        }
    }
}

@Composable
@Preview
private fun TxDetailInfoItemPreview() {
    PreviewSecondarySurface(TariTheme.Light) {
        Column(
            modifier = Modifier.padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            TxDetailInfoItem(
                title = "Transaction ID",
                value = "0x1234567890abcdef1234567890abcdef1234567890abcdef1234567890abcdef",
            )

            TxDetailInfoCopyItem(
                title = "Transaction Date",
                value = "2023-10-01 12:00:00",
            )

            TxDetailInfoItem(
                title = "Transaction Status",
                value = "Error occurred while processing",
                valueTextColor = TariDesignSystem.colors.errorMain,
            )

            TxDetailInfoContactNameItem(
                alias = null,
                onEditClicked = {},
            )

            TxDetailInfoContactNameItem(
                alias = "John Doe",
                onEditClicked = {},
            )

            TxDetailInfoAddressItem(
                walletAddress = MockDataStub.WALLET_ADDRESS,
                onCopyClicked = {},
                onEmojiIdDetailsClick = {},
            )
        }
    }
}
