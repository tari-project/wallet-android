package com.tari.android.wallet.ui.compose.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color

@Composable
fun TariVerticalGradient(
    modifier: Modifier = Modifier,
    from: Color,
    to: Color,
) {
    Box(modifier.background(Brush.verticalGradient(listOf(from, to))))
}