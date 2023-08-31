package com.tari.android.wallet.ui.fragment.qr

import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.tari.android.wallet.R
import com.tari.android.wallet.application.deeplinks.DeepLink
import com.tari.android.wallet.application.deeplinks.DeeplinkHandler
import com.tari.android.wallet.application.deeplinks.DeeplinkViewModel
import com.tari.android.wallet.ui.common.CommonViewModel
import com.tari.android.wallet.ui.common.SingleLiveEvent
import com.tari.android.wallet.ui.fragment.home.HomeActivity
import com.tari.android.wallet.util.extractEmojis
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import javax.inject.Inject

class QRScannerViewModel : CommonViewModel() {

    @Inject
    lateinit var deeplinkHandler: DeeplinkHandler

    init {
        component.inject(this)
    }

    val qrScannerSource: MutableLiveData<QrScannerSource> = MutableLiveData()

    val scanError: MutableLiveData<Boolean> = MutableLiveData(false)

    val alternativeText: MutableLiveData<String> = MutableLiveData("")

    val scannedDeeplink: MutableLiveData<DeepLink> = MutableLiveData()

    val navigationBackWithData: SingleLiveEvent<String> = SingleLiveEvent()

    val deeplinkViewModel: DeeplinkViewModel
        get() = HomeActivity.instance.get()?.deeplinkViewModel!!

    val proceedScan = SingleLiveEvent<Unit>()

    fun init(source: QrScannerSource) {
        qrScannerSource.postValue(source)
    }

    fun onAlternativeApply() {
        _backPressed.postValue(Unit)
        executeWithDelay(deeplinkViewModel) {
            deeplinkViewModel.executeRawDeeplink(scannedDeeplink.value!!)
        }
    }

    private fun executeWithDelay(commonViewModel: CommonViewModel, action: () -> Unit) {
        commonViewModel.viewModelScope.launch(Dispatchers.IO) {
            delay(500)
            commonViewModel.viewModelScope.launch(Dispatchers.Main) {
                action()
            }
        }
    }

    fun onAlternativeDeny() {
        alternativeText.postValue("")
        proceedScan.postValue(Unit)
    }

    fun onScanResult(text: String?) {
        val deeplink = deeplinkHandler.handle(text.orEmpty())
        if (deeplink == null) {
            scanError.postValue(true)
        } else {
            handleDeeplink(deeplink)
        }
    }

    private fun handleDeeplink(deepLink: DeepLink) {
        scannedDeeplink.postValue(deepLink)

        when (qrScannerSource.value) {
            QrScannerSource.None,
            QrScannerSource.Home -> setAlternativeText(deepLink)

            QrScannerSource.TransactionSend -> {
                when (deepLink) {
                    is DeepLink.Send,
                    is DeepLink.UserProfile -> navigateBack(deepLink)

                    is DeepLink.Contacts,
                    is DeepLink.AddBaseNode -> setAlternativeText(deepLink)
                }
            }

            QrScannerSource.AddContact -> {
                when (deepLink) {
                    is DeepLink.UserProfile -> navigateBack(deepLink)

                    is DeepLink.Send,
                    is DeepLink.Contacts,
                    is DeepLink.AddBaseNode -> setAlternativeText(deepLink)
                }
            }

            QrScannerSource.ContactBook -> {
                when (deepLink) {
                    is DeepLink.Send,
                    is DeepLink.UserProfile,
                    is DeepLink.Contacts -> navigateBack(deepLink)

                    is DeepLink.AddBaseNode -> setAlternativeText(deepLink)
                }
            }

            QrScannerSource.TorBridges -> {
                when (deepLink) {
                    is DeepLink.Send,
                    is DeepLink.UserProfile,
                    is DeepLink.Contacts -> setAlternativeText(deepLink)

                    is DeepLink.AddBaseNode -> navigateBack(deepLink)
                }
            }

            null -> Unit
        }
    }

    private fun setAlternativeText(deepLink: DeepLink) {
        val text = when (deepLink) {
            is DeepLink.Send -> {
                val emojiId = walletService.getWalletAddressFromHexString(deepLink.walletAddressHex).emojiId.extractEmojis().take(3).joinToString("")
                resourceManager.getString(R.string.qr_code_scanner_labels_actions_transaction_send, emojiId)
            }

            is DeepLink.UserProfile -> resourceManager.getString(R.string.qr_code_scanner_labels_actions_profile)
            is DeepLink.Contacts -> resourceManager.getString(R.string.qr_code_scanner_labels_actions_contacts)
            is DeepLink.AddBaseNode -> resourceManager.getString(R.string.qr_code_scanner_labels_actions_base_node_add)
//            is DeepLink.TorBridge -> resourceManager.getString(R.string.qr_code_scanner_labels_actions_transaction_send)
        }
        alternativeText.postValue(text)
    }

    private fun navigateBack(deepLink: DeepLink) {
        navigationBackWithData.postValue(deeplinkHandler.getDeeplink(deepLink))
    }

    fun onRetry() {
        proceedScan.postValue(Unit)
    }
}