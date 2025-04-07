package com.tari.android.wallet.ui.screen.send.confirm.widget

import androidx.annotation.DrawableRes
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
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.tari.android.wallet.R
import com.tari.android.wallet.ui.compose.PreviewSecondarySurface
import com.tari.android.wallet.ui.compose.TariDesignSystem
import com.tari.android.wallet.ui.compose.components.TariHorizontalDivider
import com.tari.android.wallet.ui.screen.settings.themeSelector.TariTheme

@Composable
fun TxDetailInfoItem(
    modifier: Modifier = Modifier,
    title: String,
    value: String,
    singleLine: Boolean = true,
    @DrawableRes actionIconRes: Int? = null,
    onActionClicked: () -> Unit = {},
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
                    color = TariDesignSystem.colors.textPrimary,
                    overflow = TextOverflow.Ellipsis,
                    maxLines = if (singleLine) 1 else Int.MAX_VALUE,
                )
            }

            if (actionIconRes != null) {
                IconButton(onClick = onActionClicked) {
                    Icon(
                        painter = painterResource(actionIconRes),
                        contentDescription = null,
                        tint = TariDesignSystem.colors.componentsNavbarIcons,
                    )
                }
            }
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
        actionIconRes = R.drawable.vector_icon_copy,
        onActionClicked = { onCopyClicked(value) },
    )
}

@Composable
@Preview
private fun TxDetailInfoItemPreview() {
    PreviewSecondarySurface(TariTheme.Light) {
        TxDetailInfoItem(
            modifier = Modifier.padding(20.dp),
            title = "Transaction Fee",
            value = "0.0018 XTM",
            actionIconRes = R.drawable.vector_icon_copy,
            onActionClicked = {},
        )
    }
}
