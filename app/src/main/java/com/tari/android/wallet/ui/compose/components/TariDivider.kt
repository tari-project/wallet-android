package com.tari.android.wallet.ui.compose.components

import androidx.compose.material.Divider
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.tari.android.wallet.ui.compose.TariDesignSystem

@Composable
fun TariDivider(
    modifier: Modifier = Modifier,
) {
    Divider(
        modifier = modifier,
        color = TariDesignSystem.colors.divider,
        thickness = 1.dp,
    )
}