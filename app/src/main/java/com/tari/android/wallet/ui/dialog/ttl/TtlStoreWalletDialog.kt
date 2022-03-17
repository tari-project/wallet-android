package com.tari.android.wallet.ui.dialog.ttl

import android.content.Context
import com.tari.android.wallet.R
import com.tari.android.wallet.databinding.DialogHomeTtlStoreBinding
import com.tari.android.wallet.extension.applyFontStyle
import com.tari.android.wallet.ui.component.CustomFont
import com.tari.android.wallet.ui.dialog.BottomSlideDialog
import com.tari.android.wallet.ui.extension.string

internal class TtlStoreWalletDialog(context: Context, args: TtlStoreWalletDialogArgs) : BottomSlideDialog(context, R.layout.dialog_home_ttl_store) {

    val ui = DialogHomeTtlStoreBinding.bind(dialog.findViewById(R.id.root))

    init {
        ui.homeTtlStoreDialogTxtTitle.text =
            context.string(R.string.home_ttl_store_dlg_title).applyFontStyle(
                context,
                CustomFont.AVENIR_LT_STD_LIGHT,
                listOf(context.string(R.string.home_ttl_store_dlg_title_bold_part)),
                CustomFont.AVENIR_LT_STD_BLACK
            )
        ui.homeTtlStoreDialogBtnLater.setOnClickListener { dismiss() }
        ui.homeTtlStoreDialogVwStoreButton.setOnClickListener {
            args.toStoreAction()
            dismiss()
        }
        ui.homeTtlStoreDialogVwTopSpacer.setOnClickListener { dismiss() }
    }

    override fun show() = dialog.show()

    override fun dismiss() = dialog.dismiss()

    override fun isShowing() : Boolean = dialog.isShowing
}