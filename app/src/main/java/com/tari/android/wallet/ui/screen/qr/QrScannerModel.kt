package com.tari.android.wallet.ui.screen.qr

import com.tari.android.wallet.application.deeplinks.DeepLink

object QrScannerModel {
    data class UiState(
        val scanError: Boolean = false,
    )

    sealed class Effect {
        data class FinishWithResult(val deepLink: DeepLink) : Effect()
        data object ProceedScan : Effect()
    }
}