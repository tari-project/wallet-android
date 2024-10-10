package com.tari.android.wallet.ui.fragment.qr

import android.app.Activity
import androidx.lifecycle.SavedStateHandle
import com.tari.android.wallet.R
import com.tari.android.wallet.application.deeplinks.DeepLink
import com.tari.android.wallet.application.deeplinks.DeeplinkManager
import com.tari.android.wallet.event.EffectChannelFlow
import com.tari.android.wallet.extension.launchOnIo
import com.tari.android.wallet.extension.launchOnMain
import com.tari.android.wallet.model.TariWalletAddress
import com.tari.android.wallet.ui.common.CommonViewModel
import com.tari.android.wallet.ui.fragment.qr.QrScannerActivity.Companion.EXTRA_QR_DATA_SOURCE
import com.tari.android.wallet.util.shortString
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import javax.inject.Inject

class QrScannerViewModel(savedState: SavedStateHandle) : CommonViewModel() {

    @Inject
    lateinit var deeplinkManager: DeeplinkManager

    init {
        component.inject(this)
    }

    private val _uiState = MutableStateFlow(QrScannerModel.UiState())
    val uiState: StateFlow<QrScannerModel.UiState> = _uiState.asStateFlow()

    private val _effect = EffectChannelFlow<QrScannerModel.Effect>()
    val effect: Flow<QrScannerModel.Effect> = _effect.flow

    private val qrScannerSource: QrScannerSource = savedState.get<QrScannerSource>(EXTRA_QR_DATA_SOURCE) ?: QrScannerSource.None

    fun onAlternativeApply(context: Activity) {
        backPressed.postValue(Unit)
        launchOnIo {
            delay(500)
            launchOnMain {
                deeplinkManager.executeRawDeeplink(context, uiState.value.scannedDeeplink!!)
            }
        }
    }

    fun onAlternativeDeny() {
        launchOnMain {
            _uiState.update { it.copy(alternativeText = "") }
            _effect.send(QrScannerModel.Effect.ProceedScan)
        }
    }

    fun onScanResult(text: String?) {
        val deeplink = deeplinkManager.parseDeepLink(text.orEmpty())
        if (deeplink == null) {
            _uiState.update { it.copy(scanError = true) }
        } else {
            handleDeeplink(deeplink)
        }
    }

    private fun handleDeeplink(deepLink: DeepLink) {
        _uiState.update { it.copy(scannedDeeplink = deepLink) }

        when (qrScannerSource) {
            QrScannerSource.None,
            QrScannerSource.Home -> {
                when (deepLink) {
                    is DeepLink.Send,
                    is DeepLink.UserProfile,
                    is DeepLink.Contacts,
                    is DeepLink.TorBridges,
                    is DeepLink.AddBaseNode -> setAlternativeText(deepLink)

                    is DeepLink.PaperWallet -> returnResult(deepLink)
                }
            }

            QrScannerSource.TransactionSend -> {
                when (deepLink) {
                    is DeepLink.Send,
                    is DeepLink.UserProfile,
                    is DeepLink.PaperWallet -> returnResult(deepLink)

                    is DeepLink.Contacts,
                    is DeepLink.TorBridges,
                    is DeepLink.AddBaseNode -> setAlternativeText(deepLink)
                }
            }

            QrScannerSource.AddContact -> {
                when (deepLink) {
                    is DeepLink.UserProfile,
                    is DeepLink.PaperWallet -> returnResult(deepLink)

                    is DeepLink.Send,
                    is DeepLink.Contacts,
                    is DeepLink.TorBridges,
                    is DeepLink.AddBaseNode -> setAlternativeText(deepLink)
                }
            }

            QrScannerSource.ContactBook -> {
                when (deepLink) {
                    is DeepLink.Send,
                    is DeepLink.UserProfile,
                    is DeepLink.Contacts,
                    is DeepLink.PaperWallet -> returnResult(deepLink)

                    is DeepLink.TorBridges,
                    is DeepLink.AddBaseNode -> setAlternativeText(deepLink)

                }
            }

            QrScannerSource.TorBridges -> {
                when (deepLink) {
                    is DeepLink.Send,
                    is DeepLink.UserProfile,
                    is DeepLink.AddBaseNode,
                    is DeepLink.Contacts,
                    is DeepLink.PaperWallet -> setAlternativeText(deepLink)

                    is DeepLink.TorBridges -> returnResult(deepLink)
                }
            }

            QrScannerSource.PaperWallet -> {
                when (deepLink) {
                    is DeepLink.Send,
                    is DeepLink.UserProfile,
                    is DeepLink.Contacts,
                    is DeepLink.TorBridges,
                    is DeepLink.AddBaseNode -> _uiState.update { it.copy(scanError = true) }

                    is DeepLink.PaperWallet -> returnResult(deepLink)
                }
            }
        }
    }

    private fun setAlternativeText(deepLink: DeepLink) {
        val text = when (deepLink) {
            is DeepLink.Send -> {
                val walletAddress = TariWalletAddress.fromBase58OrNull(deepLink.walletAddress) ?: return
                resourceManager.getString(R.string.qr_code_scanner_labels_actions_transaction_send, walletAddress.shortString())
            }

            is DeepLink.UserProfile -> resourceManager.getString(R.string.qr_code_scanner_labels_actions_profile)
            is DeepLink.Contacts -> resourceManager.getString(R.string.qr_code_scanner_labels_actions_contacts)
            is DeepLink.AddBaseNode -> resourceManager.getString(R.string.qr_code_scanner_labels_actions_base_node_add)
            is DeepLink.TorBridges -> resourceManager.getString(R.string.qr_code_scanner_labels_actions_tor_bridges)
            is DeepLink.PaperWallet -> resourceManager.getString(R.string.qr_code_scanner_labels_actions_paper_wallet) // should never show. Show PW dialog instead
        }
        _uiState.update { it.copy(alternativeText = text) }
    }

    private fun returnResult(deepLink: DeepLink) {
        launchOnMain { _effect.send(QrScannerModel.Effect.FinishWithResult(deepLink)) }
    }

    fun onRetry() {
        launchOnMain {
            _effect.send(QrScannerModel.Effect.ProceedScan)
            _uiState.update { it.copy(scanError = false) }
        }
    }
}