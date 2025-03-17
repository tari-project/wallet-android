package com.tari.android.wallet.ui.screen.home.overview.widget

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.tari.android.wallet.R
import com.tari.android.wallet.ui.compose.PreviewSecondarySurface
import com.tari.android.wallet.ui.compose.TariDesignSystem
import com.tari.android.wallet.ui.compose.components.TariButtonSize
import com.tari.android.wallet.ui.compose.components.TariPrimaryButton
import com.tari.android.wallet.ui.screen.settings.themeSelector.TariTheme
import com.tari.android.wallet.util.DebugConfig

@Composable
fun EmptyTxList(
    onStartMiningClicked: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier,
        contentAlignment = Alignment.Center,
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                text = stringResource(R.string.home_empty_state_title),
                style = TariDesignSystem.typography.headingMedium,
            )
            Text(
                text = stringResource(R.string.home_empty_state_description),
                style = TariDesignSystem.typography.body1,
            )
            if (DebugConfig.showActiveMinersButton) {
                Spacer(modifier = Modifier.height(8.dp))
                TariPrimaryButton(
                    size = TariButtonSize.Small,
                    text = stringResource(R.string.home_empty_state_button),
                    onClick = onStartMiningClicked,
                )
            }
        }
    }
}

@Composable
@Preview
private fun EmptyTxListPreview() {
    PreviewSecondarySurface(TariTheme.Light) {
        EmptyTxList(
            onStartMiningClicked = {},
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 48.dp),
        )
    }
}
