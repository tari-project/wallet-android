package com.tari.android.wallet.ui.dialog.modular.modules.shareOptions

import android.content.Context
import android.view.LayoutInflater
import android.view.ViewGroup
import com.tari.android.wallet.R
import com.tari.android.wallet.databinding.DialogModuleShareOptionsBinding
import com.tari.android.wallet.ui.common.CommonViewModel
import com.tari.android.wallet.ui.component.common.CommonView
import com.tari.android.wallet.ui.extension.string
import com.tari.android.wallet.ui.fragment.contactBook.root.share.ShareOptionArgs
import com.tari.android.wallet.ui.fragment.contactBook.root.share.ShareOptionView
import com.tari.android.wallet.ui.fragment.contactBook.root.share.ShareType

class ShareOptionsModuleView(context: Context, shareOptionsModule: ShareOptionsModule) :
    CommonView<CommonViewModel, DialogModuleShareOptionsBinding>(context) {

    override fun bindingInflate(layoutInflater: LayoutInflater, parent: ViewGroup?, attachToRoot: Boolean): DialogModuleShareOptionsBinding =
        DialogModuleShareOptionsBinding.inflate(layoutInflater, parent, attachToRoot)

    override fun setup() = Unit

    init {
        val qrCodeArgs = ShareOptionArgs(
            type = ShareType.QR_CODE,
            title = string(R.string.share_contact_via_qr_code),
            icon = R.drawable.vector_share_qr_code,
            onClick =  shareOptionsModule.shareQr,
        )

        val linkArgs = ShareOptionArgs(
            type = ShareType.LINK,
            title = string(R.string.share_contact_via_qr_link),
            icon = R.drawable.vector_share_link,
            onClick = shareOptionsModule.shareDeeplink,
        )

        val bleArgs = ShareOptionArgs(
            type = ShareType.BLE,
            title = string(R.string.share_contact_via_qr_ble),
            icon = R.drawable.vector_share_ble,
            onClick = shareOptionsModule.shareBle,
        )

        ui.root.addView(ShareOptionView(context).apply { setArgs(qrCodeArgs, ShareOptionView.Size.Medium) })
        ui.root.addView(ShareOptionView(context).apply { setArgs(linkArgs, ShareOptionView.Size.Medium) })
        ui.root.addView(ShareOptionView(context).apply { setArgs(bleArgs, ShareOptionView.Size.Medium) })
    }
}

