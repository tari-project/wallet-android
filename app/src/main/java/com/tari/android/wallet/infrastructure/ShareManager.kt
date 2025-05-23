package com.tari.android.wallet.infrastructure

import com.tari.android.wallet.R
import com.tari.android.wallet.navigation.Navigation
import com.tari.android.wallet.navigation.TariNavigator
import com.tari.android.wallet.ui.common.DialogHandler
import com.tari.android.wallet.ui.common.domain.ResourceManager
import com.tari.android.wallet.ui.dialog.modular.DialogArgs
import com.tari.android.wallet.ui.dialog.modular.ModularDialogArgs
import com.tari.android.wallet.ui.dialog.modular.modules.body.BodyModule
import com.tari.android.wallet.ui.dialog.modular.modules.button.ButtonModule
import com.tari.android.wallet.ui.dialog.modular.modules.button.ButtonStyle
import com.tari.android.wallet.ui.dialog.modular.modules.head.HeadModule
import com.tari.android.wallet.ui.dialog.modular.modules.icon.IconModule
import com.tari.android.wallet.ui.dialog.modular.modules.shareQr.ShareQrCodeModule
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ShareManager @Inject constructor(
    private val tariNavigator: TariNavigator,
    private val resourceManager: ResourceManager,
) {

    fun share(dialogManager: DialogHandler, type: ShareType, deeplink: String) {
        when (type) {
            ShareType.QR_CODE -> doShareViaQrCode(dialogManager, deeplink)
            ShareType.LINK -> doShareViaLink(dialogManager, deeplink)
        }
    }

    private fun doShareViaQrCode(dialogManager: DialogHandler, deeplink: String) {
        dialogManager.showModularDialog(
            ModularDialogArgs(
                DialogArgs(true, canceledOnTouchOutside = true), listOf(
                    HeadModule(resourceManager.getString(R.string.share_via_qr_code_title)),
                    ShareQrCodeModule(deeplink),
                    ButtonModule(resourceManager.getString(R.string.common_close), ButtonStyle.Close),
                )
            )
        )
    }

    private fun doShareViaLink(dialogManager: DialogHandler, deeplink: String) {
        tariNavigator.navigate(Navigation.ShareText(deeplink))
        showShareSuccessDialog(dialogManager)
    }

    private fun showShareSuccessDialog(dialogManager: DialogHandler) {
        dialogManager.showModularDialog(
            IconModule(R.drawable.vector_sharing_success),
            HeadModule(resourceManager.getString(R.string.share_success_title)),
            BodyModule(resourceManager.getString(R.string.share_success_message)),
            ButtonModule(resourceManager.getString(R.string.common_close), ButtonStyle.Close),
        )
    }
}

enum class ShareType {
    QR_CODE,
    LINK,
}