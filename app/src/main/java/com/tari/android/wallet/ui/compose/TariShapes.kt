package com.tari.android.wallet.ui.compose

import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.CornerBasedShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.unit.dp

@Immutable
data class TariShapes(
    val card: CornerBasedShape = RoundedCornerShape(16.dp),
    val button: CornerBasedShape = CircleShape,
    val startMiningButton: CornerBasedShape = RoundedCornerShape(10.dp),
    val bottomMenu: CornerBasedShape = RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp),
)

val LocalTariShapes = staticCompositionLocalOf { TariShapes() }
