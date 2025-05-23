package com.tari.android.wallet.ui.compose.components

import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.VerticalDivider
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.tari.android.wallet.ui.compose.TariDesignSystem

@Composable
fun TariHorizontalDivider(
    modifier: Modifier = Modifier,
) {
    HorizontalDivider(
        modifier = modifier,
        color = TariDesignSystem.colors.divider,
        thickness = 1.dp,
    )
}

@Composable
fun TariVerticalDivider(
    modifier: Modifier = Modifier,
) {
    VerticalDivider(
        modifier = modifier,
        color = TariDesignSystem.colors.divider,
        thickness = 1.dp,
    )
}