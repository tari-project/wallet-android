package com.tari.android.wallet.ui.screen.qr

import androidx.lifecycle.SavedStateHandle
import com.tari.android.wallet.application.deeplinks.DeepLink
import com.tari.android.wallet.ui.common.CommonViewModel
import com.tari.android.wallet.ui.screen.qr.QrScannerActivity.Companion.EXTRA_QR_DATA_SOURCE
import com.tari.android.wallet.util.EffectFlow
import com.tari.android.wallet.util.extension.launchOnMain
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

class QrScannerViewModel(savedState: SavedStateHandle) : CommonViewModel() {

    init {
        component.inject(this)
    }

    private val _uiState = MutableStateFlow(QrScannerModel.UiState())
    val uiState: StateFlow<QrScannerModel.UiState> = _uiState.asStateFlow()

    private val _effect = EffectFlow<QrScannerModel.Effect>()
    val effect: Flow<QrScannerModel.Effect> = _effect.flow

    @Suppress("unused") // It's unused now, but maybe in future we will need where is the QR code scanned
    private val qrScannerSource: QrScannerSource = savedState.get<QrScannerSource>(EXTRA_QR_DATA_SOURCE) ?: QrScannerSource.None

    fun onScanResult(text: String?) {
        val deeplink = deeplinkManager.parseDeepLink(text.orEmpty())
        if (deeplink == null) {
            _uiState.update { it.copy(scanError = true) }
        } else {
            returnResult(deeplink)
        }
    }

    fun onRetry() {
        launchOnMain {
            _effect.send(QrScannerModel.Effect.ProceedScan)
            _uiState.update { it.copy(scanError = false) }
        }
    }

    private fun returnResult(deepLink: DeepLink) {
        launchOnMain { _effect.send(QrScannerModel.Effect.FinishWithResult(deepLink)) }
    }
}