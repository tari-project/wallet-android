package com.tari.android.wallet.ui.fragment.send.shareQr

import android.app.Dialog
import android.content.Context
import android.graphics.Bitmap
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.view.Gravity
import android.widget.LinearLayout
import com.google.zxing.BarcodeFormat
import com.google.zxing.EncodeHintType
import com.journeyapps.barcodescanner.BarcodeEncoder
import com.tari.android.wallet.R
import com.tari.android.wallet.databinding.DialogShareQrCodeBinding
import com.tari.android.wallet.ui.dialog.TariDialog
import com.tari.android.wallet.ui.extension.dimenPx
import com.tari.android.wallet.ui.extension.setVisible
import java.util.*

class ShareQRCodeDialog constructor(context: Context, args: QRCodeDialogArgs) : TariDialog {

    private var dialog: Dialog

    private var ui: DialogShareQrCodeBinding

    init {
        dialog = Dialog(context, R.style.BottomSlideDialog).apply {
            setContentView(R.layout.dialog_share_qr_code)
            ui = DialogShareQrCodeBinding.bind(findViewById(R.id.root))
            window?.apply {
                setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
                setLayout(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT
                )
                setGravity(Gravity.BOTTOM)
            }
        }
        applyArgs(args)
    }

    fun applyArgs(args: QRCodeDialogArgs) = with(args) {
        dialog.setCancelable(cancelable)
        dialog.setCanceledOnTouchOutside(canceledOnTouchOutside)
        ui.errorDialogCloseView.setVisible(cancelable)
        ui.errorDialogCloseView.setOnClickListener {
            onClose()
            dismiss()
        }
        ui.shareButton.setOnClickListener { args.shareAction.invoke() }
        getQREncodedBitmap(args.deeplink, args.context.dimenPx(R.dimen.wallet_info_img_qr_code_size))?.let { ui.qrImageView.setImageBitmap(it) }
    }

    private fun getQREncodedBitmap(content: String, size: Int): Bitmap? {
        return try {
            val hints: MutableMap<EncodeHintType, String> = EnumMap(EncodeHintType::class.java)
            hints[EncodeHintType.CHARACTER_SET] = "UTF-8"
            val barcodeEncoder = BarcodeEncoder()
            val map = barcodeEncoder.encode(content, BarcodeFormat.QR_CODE, size, size, hints)
            barcodeEncoder.createBitmap(map)
        } catch (e: Exception) {
            null
        }
    }

    override fun show() = dialog.show()

    override fun dismiss() = dialog.dismiss()

    override fun isShowing() : Boolean = dialog.isShowing
}

