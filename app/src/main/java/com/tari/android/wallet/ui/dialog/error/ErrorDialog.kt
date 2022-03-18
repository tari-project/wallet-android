package com.tari.android.wallet.ui.dialog.error

import android.app.Dialog
import android.content.Context
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.view.Gravity
import android.widget.LinearLayout
import com.tari.android.wallet.R
import com.tari.android.wallet.databinding.DialogErrorBinding
import com.tari.android.wallet.ui.dialog.TariDialog
import yat.android.ui.extension.HtmlHelper

class ErrorDialog constructor(context: Context, errorDialogArgs: ErrorDialogArgs) : TariDialog {

    constructor(context: Context, errorArgs: WalletErrorArgs) : this(
        context,
        ErrorDialogArgs(errorArgs.title, errorArgs.description, onClose = errorArgs.dismissAction)
    )

    private var dialog: Dialog

    init {
        with(errorDialogArgs) {
            dialog = Dialog(context, R.style.BottomSlideDialog).apply {
                setContentView(R.layout.dialog_error)
                val ui = DialogErrorBinding.bind(findViewById(R.id.root))
                window?.apply {
                    setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
                    setLayout(
                        LinearLayout.LayoutParams.MATCH_PARENT,
                        LinearLayout.LayoutParams.WRAP_CONTENT
                    )
                    setGravity(Gravity.BOTTOM)
                }
                setCancelable(cancelable)
                setCanceledOnTouchOutside(canceledOnTouchOutside)
                ui.errorDialogTitleTextView.text = title
                ui.errorDialogDescriptionTextView.text = HtmlHelper.getSpannedText(description.toString())
                ui.errorDialogCloseView.setOnClickListener { dismiss() }
                setOnDismissListener { onClose() }
            }
        }
    }

    override fun show() = dialog.show()

    override fun dismiss() = dialog.dismiss()

    override fun isShowing() : Boolean = dialog.isShowing
}