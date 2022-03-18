package com.tari.android.wallet.ui.dialog.testnet

import android.content.Context
import com.tari.android.wallet.R
import com.tari.android.wallet.databinding.DialogHomeTestnetTariReceivedBinding
import com.tari.android.wallet.extension.applyFontStyle
import com.tari.android.wallet.ui.component.CustomFont
import com.tari.android.wallet.ui.dialog.BottomSlideDialog
import com.tari.android.wallet.ui.extension.string

internal class TestnetReceivedDialog(context: Context, args: TestnetReceivedDialogArgs) : BottomSlideDialog(
    context,
    R.layout.dialog_home_testnet_tari_received,
    canceledOnTouchOutside = false,
) {

    val ui = DialogHomeTestnetTariReceivedBinding.bind(dialog.findViewById(R.id.root))

    init {
        ui.homeTestnetTariReceivedDlgTxtTitle.text =
            context.string(R.string.home_tari_bot_you_got_tari_dlg_title).applyFontStyle(
                context,
                CustomFont.AVENIR_LT_STD_LIGHT,
                listOf(context.string(R.string.home_tari_bot_you_got_tari_dlg_title_bold_part)),
                CustomFont.AVENIR_LT_STD_BLACK
            )
        ui.homeTariBotDialogTxtTryLater.setOnClickListener { dismiss() }
        ui.homeTariBotDialogBtnSendTari.setOnClickListener {
            args.action()
            dismiss()
        }
    }

    override fun show() = dialog.show()

    override fun dismiss() = dialog.dismiss()

    override fun isShowing() : Boolean = dialog.isShowing
}