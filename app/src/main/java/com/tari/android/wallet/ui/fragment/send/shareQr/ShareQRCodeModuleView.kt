package com.tari.android.wallet.ui.fragment.send.shareQr

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Bitmap
import android.view.LayoutInflater
import android.view.ViewGroup
import com.google.zxing.BarcodeFormat
import com.google.zxing.EncodeHintType
import com.journeyapps.barcodescanner.BarcodeEncoder
import com.tari.android.wallet.R
import com.tari.android.wallet.databinding.DialogModuleShareQrCodeBinding
import com.tari.android.wallet.ui.common.CommonViewModel
import com.tari.android.wallet.ui.component.common.CommonView
import com.tari.android.wallet.ui.extension.dimenPx
import java.util.*

@SuppressLint("ViewConstructor")
class ShareQRCodeModuleView(context: Context, buttonModule: ShareQrCodeModule) :
    CommonView<CommonViewModel, DialogModuleShareQrCodeBinding>(context) {

    override fun bindingInflate(layoutInflater: LayoutInflater, parent: ViewGroup?, attachToRoot: Boolean): DialogModuleShareQrCodeBinding =
        DialogModuleShareQrCodeBinding.inflate(layoutInflater, parent, attachToRoot)

    override fun setup() = Unit

    init {
        getQREncodedBitmap(buttonModule.deeplink, context.dimenPx(R.dimen.wallet_info_img_qr_code_size))?.let { ui.qrImageView.setImageBitmap(it) }
    }

    private fun getQREncodedBitmap(content: String, size: Int): Bitmap? = runCatching {
        val hints: MutableMap<EncodeHintType, String> = EnumMap(EncodeHintType::class.java)
        hints[EncodeHintType.CHARACTER_SET] = "UTF-8"
        val barcodeEncoder = BarcodeEncoder()
        val map = barcodeEncoder.encode(content, BarcodeFormat.QR_CODE, size, size, hints)
        barcodeEncoder.createBitmap(map)
    }.getOrNull()
}