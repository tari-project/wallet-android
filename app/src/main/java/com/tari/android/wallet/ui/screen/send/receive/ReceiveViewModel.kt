package com.tari.android.wallet.ui.screen.send.receive

import android.graphics.Bitmap
import com.tari.android.wallet.R
import com.tari.android.wallet.application.Navigation
import com.tari.android.wallet.application.deeplinks.DeepLink
import com.tari.android.wallet.model.TariWalletAddress
import com.tari.android.wallet.ui.common.CommonViewModel
import com.tari.android.wallet.util.ContactUtil
import com.tari.android.wallet.util.QrUtil
import com.tari.android.wallet.util.extension.launchOnIo
import com.tari.android.wallet.util.extension.launchOnMain
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.withContext
import javax.inject.Inject

class ReceiveViewModel : CommonViewModel() {

    @Inject
    lateinit var contactUtil: ContactUtil

    private val _uiState = MutableStateFlow(
        UiState(
            ticker = networkRepository.currentNetwork.ticker,
            networkName = networkRepository.currentNetwork.network.displayName,
            tariAddress = sharedPrefsRepository.walletAddress,
        )
    )
    val uiState = _uiState.asStateFlow()

    init {
        component.inject(this)

        launchOnIo {
            val bitmap = getQrCodeBitmap()
            launchOnMain {
                _uiState.update { it.copy(qrBitmap = bitmap) }
            }
        }
    }

    fun onEmojiCopyClick() {
        tariNavigator.navigate(Navigation.ShareText(sharedPrefsRepository.walletAddress.fullEmojiId))
    }

    fun onBase58CopyClick() {
        tariNavigator.navigate(Navigation.ShareText(sharedPrefsRepository.walletAddress.fullBase58))
    }

    fun onAddressDetailsClicked() {
        showAddressDetailsDialog(sharedPrefsRepository.walletAddress)
    }

    fun onShareClick() {
        tariNavigator.navigate(Navigation.ShareText(sharedPrefsRepository.walletAddress.fullBase58))
    }

    private suspend fun getQrCodeBitmap(): Bitmap? = withContext(Dispatchers.IO) {
        QrUtil.getQrEncodedBitmapOrNull(
            content = deeplinkManager.getDeeplinkString(
                DeepLink.UserProfile(
                    tariAddress = sharedPrefsRepository.walletAddressBase58.orEmpty(),
                    alias = contactUtil.normalizeAlias(
                        alias = sharedPrefsRepository.alias.orEmpty(),
                        walletAddress = sharedPrefsRepository.walletAddress,
                    ),
                )
            ),
            size = resourceManager.getDimenInPx(R.dimen.wallet_info_img_qr_code_size),
        )
    }

    data class UiState(
        val ticker: String,
        val networkName: String,
        val tariAddress: TariWalletAddress,
        val qrBitmap: Bitmap? = null,
    )
}