package com.tari.android.wallet.ui.dialog.modular.modules.shareQr

import android.annotation.SuppressLint
import android.content.Context
import android.view.LayoutInflater
import android.view.ViewGroup
import com.tari.android.wallet.R
import com.tari.android.wallet.databinding.DialogModuleShareQrCodeBinding
import com.tari.android.wallet.ui.common.CommonViewModel
import com.tari.android.wallet.ui.component.common.CommonView
import com.tari.android.wallet.util.QrUtil
import com.tari.android.wallet.util.extension.dimenPx

@SuppressLint("ViewConstructor")
class ShareQRCodeModuleView(context: Context, buttonModule: ShareQrCodeModule) :
    CommonView<CommonViewModel, DialogModuleShareQrCodeBinding>(context) {

    override fun bindingInflate(layoutInflater: LayoutInflater, parent: ViewGroup?, attachToRoot: Boolean): DialogModuleShareQrCodeBinding =
        DialogModuleShareQrCodeBinding.inflate(layoutInflater, parent, attachToRoot)

    override fun setup() = Unit

    init {
        QrUtil.getQrEncodedBitmapOrNull(
            content = buttonModule.deeplink,
            size = context.dimenPx(R.dimen.wallet_info_img_qr_code_size),
        )?.let { ui.qrImageView.setImageBitmap(it) }
    }
}