package com.tari.android.wallet.ui.screen.tx.details.widget

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.tari.android.wallet.R
import com.tari.android.wallet.ui.compose.PreviewSecondarySurface
import com.tari.android.wallet.ui.compose.TariDesignSystem
import com.tari.android.wallet.ui.compose.components.TariHorizontalDivider
import com.tari.android.wallet.ui.screen.settings.themeSelector.TariTheme
import com.tari.android.wallet.ui.screen.tx.details.TxDetailsModel

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
@Preview
private fun TxDetailInfoItemPreview() {
    PreviewSecondarySurface(TariTheme.Light) {
        TxDetailInfoItem(
            modifier = Modifier.padding(20.dp),
            title = "Transaction ID",
            value = "0x1234567890abcdef1234567890abcdef1234567890abcdef1234567890abcdef",
        )

        TxDetailInfoCopyItem(
            modifier = Modifier.padding(20.dp),
            title = "Transaction Date",
            value = "2023-10-01 12:00:00",
        )

        TxDetailInfoItem(
            modifier = Modifier.padding(20.dp),
            title = "Transaction Status",
            value = "Pending",
            valueTextColor = TariDesignSystem.colors.errorMain,
        )
    }
}
