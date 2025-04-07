package com.tari.android.wallet.ui.screen.send.confirm.widget

import androidx.annotation.DrawableRes
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.tari.android.wallet.R
import com.tari.android.wallet.ui.compose.PreviewSecondarySurface
import com.tari.android.wallet.ui.compose.TariDesignSystem
import com.tari.android.wallet.ui.screen.settings.themeSelector.TariTheme

@Composable
fun SenderCard(
    modifier: Modifier = Modifier,
    title: String,
    comment: String? = null,
    @DrawableRes iconRes: Int,
) {
    Row(
        modifier = modifier
            .clip(TariDesignSystem.shapes.card)
            .border(width = 1.dp, color = TariDesignSystem.colors.elevationOutlined, shape = TariDesignSystem.shapes.card)
            .background(color = TariDesignSystem.colors.backgroundPrimary)
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(
            modifier = Modifier.weight(1f),
        ) {
            Text(
                text = title,
                style = TariDesignSystem.typography.headingXLarge,
            )
            if (comment != null) {
                Spacer(Modifier.size(4.dp))
                Text(
                    modifier = Modifier
                        .clip(TariDesignSystem.shapes.button)
                        .background(color = TariDesignSystem.colors.backgroundAccent)
                        .padding(horizontal = 8.dp, vertical = 3.dp),
                    text = comment,
                    style = TariDesignSystem.typography.body2,
                )
            }
        }

        Box(
            modifier = Modifier
                .size(44.dp)
                .clip(CircleShape)
                .background(color = TariDesignSystem.colors.secondaryMain), contentAlignment = Alignment.Center
        ) {
            Icon(
                modifier = Modifier
                    .size(24.dp)
                    .clip(CircleShape),
                painter = painterResource(iconRes),
                contentDescription = null,
                tint = TariDesignSystem.colors.componentsNavbarBackground,
            )
        }
    }
}

@Composable
@Preview
fun SenderCardPreview() {
    PreviewSecondarySurface(TariTheme.Light) {
        SenderCard(
            modifier = Modifier.padding(16.dp),
            title = "Sender",
            comment = "$300",
            iconRes = R.drawable.vector_gem,
        )
    }
}

@Composable
@Preview
fun SenderCardNoValuePreview() {
    PreviewSecondarySurface(TariTheme.Light) {
        SenderCard(
            modifier = Modifier.padding(16.dp),
            title = "Sender",
            comment = null,
            iconRes = R.drawable.vector_gem,
        )
    }
}