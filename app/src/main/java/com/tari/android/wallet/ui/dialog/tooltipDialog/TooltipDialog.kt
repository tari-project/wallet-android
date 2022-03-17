package com.tari.android.wallet.ui.dialog.tooltipDialog

import android.content.Context
import com.tari.android.wallet.R
import com.tari.android.wallet.databinding.DialogTooltipBinding
import com.tari.android.wallet.ui.dialog.BottomSlideDialog

internal class TooltipDialog(context: Context, args: TooltipDialogArgs) : BottomSlideDialog(
    context,
    R.layout.dialog_tooltip,
    canceledOnTouchOutside = false,
) {

    val ui = DialogTooltipBinding.bind(dialog.findViewById(R.id.root))

    init {
        ui.title.text = args.title
        ui.description.text = args.description
        ui.txFeeTooltipDialogTxtClose.setOnClickListener { dismiss() }
    }

    override fun show() = dialog.show()

    override fun dismiss() = dialog.dismiss()

    override fun isShowing() : Boolean = dialog.isShowing
}