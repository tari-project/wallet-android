package com.tari.android.wallet.ui.compose.widgets

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.tari.android.wallet.R
import com.tari.android.wallet.ui.compose.PreviewSecondarySurface
import com.tari.android.wallet.ui.compose.TariDesignSystem
import com.tari.android.wallet.ui.screen.settings.themeSelector.TariTheme

@Composable
fun QrCodeCard(
    qrBitmap: Bitmap?,
    modifier: Modifier = Modifier,
) {
    Card(
        colors = CardDefaults.cardColors(containerColor = TariDesignSystem.colors.backgroundPrimary),
        elevation = CardDefaults.elevatedCardElevation(defaultElevation = 2.dp),
        shape = RoundedCornerShape(24.dp),
        modifier = modifier.size(232.dp),
    ) {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center,
        ) {
            if (qrBitmap != null) {
                Image(
                    modifier = Modifier.fillMaxSize(),
                    bitmap = qrBitmap.asImageBitmap(),
                    contentDescription = null,
                )
            } else {
                CircularProgressIndicator(
                    color = TariDesignSystem.colors.textPrimary,
                )
            }
        }
    }
}

@Composable
@Preview
private fun QrCodeCardPreview() {
    PreviewSecondarySurface(TariTheme.Light) {
        Column(modifier = Modifier.padding(20.dp)) {
            QrCodeCard(qrBitmap = BitmapFactory.decodeResource(LocalContext.current.resources, R.drawable.tari_splash_screen))

            Spacer(Modifier.size(20.dp))

            QrCodeCard(qrBitmap = null)
        }
    }
}
